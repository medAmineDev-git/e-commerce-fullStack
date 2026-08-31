import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-login-page',
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly identifier = signal('');
  readonly password = signal('');
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  async login(): Promise<void> {
    if (!this.identifier().trim() || !this.password()) {
      this.error.set('Saisissez votre pseudo ou email et votre mot de passe.');
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    try {
      await this.authService.login(this.identifier().trim(), this.password());
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/admin/dashboard';
      await this.router.navigateByUrl(returnUrl);
    } catch {
      this.error.set('Identifiants incorrects.');
    } finally {
      this.submitting.set(false);
    }
  }
}
