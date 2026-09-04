import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { StoreAdminService } from '../../../core/services/store-admin.service';
import { OwnedStore } from '../../../core/models/store.model';

/** Identité de la boutique : ce que voient ses clients sur la vitrine. */
@Component({
  selector: 'app-store-settings',
  imports: [FormsModule],
  templateUrl: './store-settings.html',
  styleUrl: './store-settings.scss',
})
export class StoreSettings {
  private readonly storeAdminService = inject(StoreAdminService);

  readonly store = signal<OwnedStore | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly savedAt = signal<Date | null>(null);
  readonly error = signal<string | null>(null);

  readonly name = signal('');
  readonly description = signal('');
  readonly phone = signal('');
  readonly email = signal('');
  readonly address = signal('');
  readonly logoUrl = signal('');
  readonly bannerUrl = signal('');

  /** La section Informations n'apparait sur la vitrine que si elle est remplie. */
  readonly hasInformation = computed(
    () => !!(this.address().trim() || this.phone().trim() || this.email().trim()),
  );

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    try {
      const store = await this.storeAdminService.getMyStore();
      this.store.set(store);
      this.name.set(store.name);
      this.description.set(store.description ?? '');
      this.phone.set(store.phone ?? '');
      this.email.set(store.email ?? '');
      this.address.set(store.address ?? '');
      this.logoUrl.set(store.logoUrl ?? '');
      this.bannerUrl.set(store.bannerUrl ?? '');
    } catch {
      this.error.set('Impossible de charger les informations de la boutique.');
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    if (!this.name().trim()) {
      this.error.set('Le nom de la boutique est obligatoire.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    try {
      const updated = await this.storeAdminService.updateMyStore({
        name: this.name().trim(),
        description: this.description().trim() || null,
        phone: this.phone().trim() || null,
        email: this.email().trim() || null,
        address: this.address().trim() || null,
        logoUrl: this.logoUrl().trim() || null,
        bannerUrl: this.bannerUrl().trim() || null,
        // Le domaine est rattaché par la plateforme, pas par le propriétaire :
        // il engage un certificat et une entrée DNS.
        domain: this.store()?.domain ?? null,
      });
      this.store.set(updated);
      this.savedAt.set(new Date());
    } catch {
      this.error.set("L'enregistrement a échoué. Réessayez dans un instant.");
    } finally {
      this.saving.set(false);
    }
  }
}
