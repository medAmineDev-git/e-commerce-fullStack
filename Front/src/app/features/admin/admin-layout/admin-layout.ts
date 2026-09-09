import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-admin-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private readonly breakpoints = inject(BreakpointObserver);

  /**
   * Sur telephone, le menu recouvre la zone de travail : il doit s'effacer et
   * ne revenir qu'a la demande. Sur grand ecran il reste ancre a gauche, la
   * place ne manquant pas.
   */
  readonly isHandset = toSignal(
    this.breakpoints
      .observe([Breakpoints.Handset, Breakpoints.TabletPortrait])
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  readonly menuOpen = signal(false);

  readonly username = this.authService.username;

  /** Un exploitant qui tient aussi une boutique doit pouvoir revenir a la console. */
  readonly isPlatformOperator = this.authService.isPlatformOperator;

  /**
   * Adresse de sa propre vitrine. Le slug vient de la session, pas d'une devinette.
   *
   * Une adresse et non un lien de routeur : la vitrine s'ouvre dans un autre
   * onglet, pour que le proprietaire garde son back-office ouvert derriere. Y
   * naviguer dans le meme onglet lui faisait perdre sa page en cours.
   */
  readonly storefrontUrl = computed(() => {
    const slug = this.authService.storeSlug();
    return slug ? `/boutique/${slug}` : '/';
  });

  constructor() {
    // Sur telephone, le menu recouvre la page : le laisser ouvert apres un clic
    // masquerait justement l'ecran qu'on vient de demander.
    this.router.events
      .pipe(
        takeUntilDestroyed(),
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      )
      .subscribe(() => this.closeMenu());
  }

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/connexion']);
  }
}
