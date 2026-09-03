import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { StoreAdminService } from '../../../core/services/store-admin.service';
import { StoreSummary } from '../../../core/models/store.model';
import { AuthService } from '../../../core/services/auth';
import { SeoService } from '../../../core/seo/seo.service';

/**
 * Exploitation de la plateforme : inventaire des boutiques, activation,
 * rattachement de domaine. Réservé au rôle plateforme, côté serveur comme ici.
 */
@Component({
  selector: 'app-platform-console',
  imports: [FormsModule, DatePipe],
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

  async toggleActive(store: StoreSummary): Promise<void> {
    try {
      const updated = await this.storeAdminService.toggleStoreActive(store.id);
      this.replace(updated);
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
    const domain = this.domainDraft().trim();
    try {
      const updated = await this.storeAdminService.attachDomain(store.id, domain || null);
      this.replace(updated);
      this.cancelDomainEdit();
    } catch {
      this.error.set('Ce domaine est déjà rattaché à une autre boutique.');
    }
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/']);
  }

  private replace(updated: StoreSummary): void {
    this.stores.update((stores) =>
      stores.map((store) => (store.id === updated.id ? updated : store)),
    );
  }
}
