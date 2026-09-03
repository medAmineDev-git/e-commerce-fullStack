import { Component, computed, inject } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
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
  ],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly username = this.authService.username;

  /** Lien vers sa propre vitrine. Le slug vient de la session, pas d'une devinette. */
  readonly storefrontLink = computed(() => {
    const slug = this.authService.storeSlug();
    return slug ? ['/boutique', slug] : ['/'];
  });

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/connexion']);
  }
}
