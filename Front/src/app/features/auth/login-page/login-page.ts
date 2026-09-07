import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth';
import { StoreContextService } from '../../../core/services/store-context.service';
import { SeoService } from '../../../core/seo/seo.service';

/**
 * Connexion au back-office.
 *
 * Deux adresses mènent ici : `/connexion` depuis le site principal, et
 * `/boutique/<slug>/connexion` depuis l'adresse d'une boutique. La seconde ne
 * change rien à l'authentification — elle affiche seulement le nom et le logo
 * de la boutique, pour que le propriétaire reconnaisse son entrée.
 *
 * Le slug de l'adresse n'est pas une frontière d'accès : un compte valide entre
 * quelle que soit la porte, et repart vers sa propre boutique. Refuser au motif
 * que le compte n'appartient pas à cette boutique-là indiquerait à un inconnu
 * quels comptes gèrent quelle vitrine.
 */
@Component({
  selector: 'app-login-page',
  imports: [RouterLink],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly storeContext = inject(StoreContextService);
  private readonly seo = inject(SeoService);

  readonly identifier = signal('');
  readonly password = signal('');
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  /**
   * La boutique vient du paramètre d'adresse, jamais du contexte partagé : ce
   * dernier garde la dernière vitrine visitée, et le site principal afficherait
   * alors une enseigne qui n'a rien à faire là.
   */
  private readonly storeSlug = this.route.snapshot.paramMap.get('slug');
  readonly store = computed(() => (this.storeSlug ? this.storeContext.store() : null));

  constructor() {
    const store = this.store();
    this.seo.apply({
      title: store ? `Connexion — ${store.name}` : 'Connexion',
      description: 'Accédez à la gestion de votre boutique.',
      path: store ? `/boutique/${store.slug}/connexion` : '/connexion',
      noIndex: true,
    });
    this.seo.removeStructuredData();
  }

  /** Lien de retour vers la vitrine, seulement quand on en vient. */
  readonly storefrontLink = computed(() => {
    const store = this.store();
    return store ? ['/boutique', store.slug] : null;
  });

  async login(): Promise<void> {
    if (!this.identifier().trim() || !this.password()) {
      this.error.set('Saisissez votre pseudo ou email et votre mot de passe.');
      return;
    }

    this.submitting.set(true);
    this.error.set(null);

    try {
      await this.authService.login(this.identifier().trim(), this.password());
      await this.router.navigateByUrl(this.destination());
    } catch (error) {
      this.error.set(this.readableError(error));
    } finally {
      this.submitting.set(false);
    }
  }

  /** Le rôle décide de la destination : back-office ou console plateforme. */
  private destination(): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    if (returnUrl) {
      return returnUrl;
    }
    return this.authService.isPlatformOperator() ? '/plateforme' : '/admin';
  }

  private readableError(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      const message = error.error?.message;
      if (message === 'This store has been deactivated') {
        return 'Cette boutique a été désactivée. Contactez la plateforme.';
      }
    }
    return 'Identifiants incorrects.';
  }
}
