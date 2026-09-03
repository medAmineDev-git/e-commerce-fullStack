import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth';
import { SeoService } from '../../../core/seo/seo.service';

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
  private readonly seo = inject(SeoService);

  readonly identifier = signal('');
  readonly password = signal('');
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.seo.apply({
      title: 'Connexion',
      description: 'Accédez à la gestion de votre boutique.',
      path: '/connexion',
      noIndex: true,
    });
    this.seo.removeStructuredData();
  }

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
