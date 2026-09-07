import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { StoreAdminService } from '../../../core/services/store-admin.service';
import { StoreDetail, StoreSummary } from '../../../core/models/store.model';
import { AuthService } from '../../../core/services/auth';
import { SeoService } from '../../../core/seo/seo.service';

/**
 * Exploitation de la plateforme : inventaire des boutiques, fiche détaillée,
 * activation, rattachement de domaine et suppression.
 * Réservé au rôle plateforme, côté serveur comme ici.
 */
@Component({
  selector: 'app-platform-console',
  imports: [FormsModule, DatePipe, DecimalPipe],
  templateUrl: './platform-console.html',
  styleUrl: './platform-console.scss',
})
export class PlatformConsole {
  private readonly storeAdminService = inject(StoreAdminService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly seo = inject(SeoService);

  readonly stores = signal<StoreSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly editingDomainFor = signal<number | null>(null);
  readonly domainDraft = signal('');

  /** Fiche ouverte, chargée à la demande. */
  readonly openDetailId = signal<number | null>(null);
  readonly detail = signal<StoreDetail | null>(null);
  readonly detailLoading = signal(false);

  /**
   * Suppression en deux temps : on demande de retaper le slug.
   * L'opération est irréversible et emporte les commandes.
   */
  readonly deletingId = signal<number | null>(null);
  readonly deleteConfirmation = signal('');
  readonly deleting = signal(false);

  readonly currentUsername = this.authService.username;

  /**
   * Modification des identifiants du proprietaire.
   *
   * Le mot de passe se remplace sans connaitre l ancien : c est precisement le
   * cas ou l exploitant intervient, celui d un proprietaire qui a perdu le
   * sien. Laisse vide, il n est pas touche.
   */
  readonly editingOwner = signal(false);
  readonly ownerUsernameDraft = signal('');
  readonly ownerEmailDraft = signal('');
  readonly ownerPasswordDraft = signal('');
  readonly savingOwner = signal(false);
  readonly ownerError = signal<string | null>(null);

  readonly canSaveOwner = computed(
    () =>
      !!this.ownerUsernameDraft().trim() &&
      !!this.ownerEmailDraft().trim() &&
      // Vide : on ne change pas le mot de passe. Rempli : il doit tenir la regle.
      (this.ownerPasswordDraft().length === 0 || this.ownerPasswordDraft().length >= 8) &&
      !this.savingOwner(),
  );

  startOwnerEdit(detail: StoreDetail): void {
    this.ownerUsernameDraft.set(detail.ownerUsername ?? '');
    this.ownerEmailDraft.set(detail.ownerEmail ?? '');
    this.ownerPasswordDraft.set('');
    this.ownerError.set(null);
    this.editingOwner.set(true);
  }

  cancelOwnerEdit(): void {
    this.editingOwner.set(false);
    this.ownerPasswordDraft.set('');
    this.ownerError.set(null);
  }

  async saveOwner(store: StoreSummary): Promise<void> {
    if (!this.canSaveOwner()) {
      return;
    }

    this.savingOwner.set(true);
    this.ownerError.set(null);

    const password = this.ownerPasswordDraft();

    try {
      const updated = await this.storeAdminService.updateOwner(store.id, {
        username: this.ownerUsernameDraft().trim(),
        email: this.ownerEmailDraft().trim(),
        ...(password ? { password } : {}),
      });
      this.detail.set(updated);
      // La liste affiche le nom du compte : elle doit suivre la fiche.
      this.stores.update((stores) =>
        stores.map((one) =>
          one.id === store.id ? { ...one, ownerUsername: updated.ownerUsername } : one,
        ),
      );
      this.cancelOwnerEdit();
    } catch (error) {
      this.ownerError.set(this.readableOwnerError(error));
    } finally {
      this.savingOwner.set(false);
    }
  }

  /** Le serveur nomme le conflit ; le reste reste generique. */
  private readableOwnerError(error: unknown): string {
    const response = error as { error?: { message?: string } };
    return response?.error?.message ?? "L'enregistrement a échoué. Réessayez dans un instant.";
  }

  constructor() {
    this.seo.apply({
      title: 'Console plateforme',
      description: 'Exploitation des boutiques.',
      path: '/plateforme',
      noIndex: true,
    });
    this.seo.removeStructuredData();
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.stores.set(await this.storeAdminService.listAllStores());
    } catch {
      this.error.set('Impossible de charger la liste des boutiques.');
    } finally {
      this.loading.set(false);
    }
  }

  async toggleDetail(store: StoreSummary): Promise<void> {
    // Changer de fiche referme l'edition : sinon les champs saisis pour une
    // boutique se retrouveraient au-dessus de la suivante.
    this.cancelOwnerEdit();

    if (this.openDetailId() === store.id) {
      this.openDetailId.set(null);
      this.detail.set(null);
      return;
    }

    this.openDetailId.set(store.id);
    this.detail.set(null);
    this.detailLoading.set(true);

    try {
      this.detail.set(await this.storeAdminService.getStoreDetail(store.id));
    } catch {
      this.error.set(`Impossible de charger la fiche de ${store.name}.`);
      this.openDetailId.set(null);
    } finally {
      this.detailLoading.set(false);
    }
  }

  async toggleActive(store: StoreSummary): Promise<void> {
    try {
      this.replace(await this.storeAdminService.toggleStoreActive(store.id));
    } catch {
      this.error.set(`Impossible de modifier l'état de ${store.name}.`);
    }
  }

  startDomainEdit(store: StoreSummary): void {
    this.editingDomainFor.set(store.id);
    this.domainDraft.set(store.domain ?? '');
    this.error.set(null);
  }

  cancelDomainEdit(): void {
    this.editingDomainFor.set(null);
    this.domainDraft.set('');
  }

  async saveDomain(store: StoreSummary): Promise<void> {
    try {
      this.replace(await this.storeAdminService.attachDomain(store.id, this.domainDraft().trim() || null));
      this.cancelDomainEdit();
    } catch {
      this.error.set('Ce domaine est déjà rattaché à une autre boutique.');
    }
  }

  startDelete(store: StoreSummary): void {
    this.deletingId.set(store.id);
    this.deleteConfirmation.set('');
    this.error.set(null);
  }

  cancelDelete(): void {
    this.deletingId.set(null);
    this.deleteConfirmation.set('');
  }

  /** La saisie doit correspondre exactement au slug, pour écarter le clic distrait. */
  canConfirmDelete(store: StoreSummary): boolean {
    return this.deleteConfirmation().trim() === store.slug && !this.deleting();
  }

  async confirmDelete(store: StoreSummary): Promise<void> {
    if (!this.canConfirmDelete(store)) {
      return;
    }

    this.deleting.set(true);
    this.error.set(null);

    try {
      await this.storeAdminService.deleteStore(store.id);
      this.stores.update((stores) => stores.filter((s) => s.id !== store.id));
      this.cancelDelete();
      if (this.openDetailId() === store.id) {
        this.openDetailId.set(null);
        this.detail.set(null);
      }
    } catch {
      this.error.set(`Impossible de supprimer ${store.name}.`);
    } finally {
      this.deleting.set(false);
    }
  }

  /** Un exploitant ne peut pas supprimer sa propre boutique : le serveur le refuse aussi. */
  isOwnStore(store: StoreSummary): boolean {
    return !!store.ownerUsername && store.ownerUsername === this.currentUsername();
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/']);
  }

  private replace(updated: StoreSummary): void {
    this.stores.update((stores) => stores.map((s) => (s.id === updated.id ? updated : s)));
  }
}
