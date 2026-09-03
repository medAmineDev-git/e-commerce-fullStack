import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth';
import { StoreAdminService } from '../../../core/services/store-admin.service';
import { SeoService } from '../../../core/seo/seo.service';

@Component({
  selector: 'app-register-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './register-page.html',
  styleUrl: './register-page.scss',
})
export class RegisterPage {
  private readonly authService = inject(AuthService);
  private readonly storeAdminService = inject(StoreAdminService);
  private readonly router = inject(Router);
  private readonly seo = inject(SeoService);

  readonly storeName = signal('');
  readonly username = signal('');
  readonly email = signal('');
  readonly password = signal('');
  readonly description = signal('');

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  /** Disponibilité du slug, vérifiée en direct pendant la saisie du nom. */
  readonly slugPreview = signal<string | null>(null);
  readonly slugAvailable = signal<boolean | null>(null);
  readonly checkingSlug = signal(false);

  readonly canSubmit = computed(
    () =>
      this.storeName().trim().length > 1 &&
      this.username().trim().length > 2 &&
      this.email().trim().includes('@') &&
      this.password().length >= 4 &&
      !this.submitting(),
  );

  private slugCheckToken = 0;

  constructor() {
    // La page d'inscription ne doit pas être indexée : elle n'a pas de contenu
    // propre et concurrencerait la landing sur les mêmes termes.
    this.seo.apply({
      title: 'Créer ma boutique',
      description: 'Créez votre boutique en ligne en quelques minutes.',
      path: '/inscription',
      noIndex: true,
    });
    this.seo.removeStructuredData();
  }

  async onStoreNameChange(value: string): Promise<void> {
    this.storeName.set(value);
    const trimmed = value.trim();

    if (trimmed.length < 2) {
      this.slugPreview.set(null);
      this.slugAvailable.set(null);
      return;
    }

    const token = ++this.slugCheckToken;
    this.checkingSlug.set(true);

    try {
      const result = await this.storeAdminService.checkSlug(trimmed);
      // Une réponse arrivée après une saisie plus récente est ignorée.
      if (token !== this.slugCheckToken) {
        return;
      }
      this.slugPreview.set(result.slug);
      this.slugAvailable.set(result.available);
    } catch {
      if (token === this.slugCheckToken) {
        this.slugPreview.set(null);
        this.slugAvailable.set(null);
      }
    } finally {
      if (token === this.slugCheckToken) {
        this.checkingSlug.set(false);
      }
    }
  }

  async submit(): Promise<void> {
    if (!this.canSubmit()) {
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    try {
      await this.authService.registerStore({
        username: this.username().trim(),
        email: this.email().trim(),
        password: this.password(),
        storeName: this.storeName().trim(),
        description: this.description().trim() || undefined,
      });
      await this.router.navigate(['/admin']);
    } catch (error) {
      this.errorMessage.set(this.readableError(error));
    } finally {
      this.submitting.set(false);
    }
  }

  private readableError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const message = error.error?.message;
      if (typeof message === 'string' && message.trim()) {
        return message === 'Username or email already exists'
          ? 'Ce nom d utilisateur ou cette adresse e-mail est déjà pris.'
          : message;
      }
    }
    return 'La création de la boutique a échoué. Réessayez dans un instant.';
  }
}
