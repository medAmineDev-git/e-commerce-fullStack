import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { StoreAdminService } from '../../../core/services/store-admin.service';
import { OwnedStore } from '../../../core/models/store.model';
import { DEFAULT_BANNER } from '../../../core/models/default-banner';
import { environment } from '../../../../environments/environment';

type ImageSlot = 'logo' | 'banner' | 'bannerMobile';

/** Identité de la boutique : ce que voient ses clients sur la vitrine. */
@Component({
  selector: 'app-store-settings',
  imports: [FormsModule],
  templateUrl: './store-settings.html',
  styleUrl: './store-settings.scss',
})
export class StoreSettings {
  private readonly storeAdminService = inject(StoreAdminService);
  private readonly apiOrigin = environment.apiBaseUrl.replace(/\/api\/?$/, '');

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
  /** Visuel composé pour l'écran étroit ; à défaut, la bannière large sert. */
  readonly bannerMobileUrl = signal('');

  /** Chaque emplacement d'image tient dans la même mécanique de téléversement. */
  private readonly slots: Record<ImageSlot, ReturnType<typeof signal<string>>> = {
    logo: this.logoUrl,
    banner: this.bannerUrl,
    bannerMobile: this.bannerMobileUrl,
  };

  /** Slot en cours de téléversement, pour n'afficher l'attente que sur celui-ci. */
  readonly uploading = signal<ImageSlot | null>(null);

  readonly hasInformation = computed(
    () => !!(this.address().trim() || this.phone().trim() || this.email().trim()),
  );

  /**
   * Les valeurs proposées à la création portent des crochets. Tant qu'il en
   * reste, le vendeur publie des exemples : mieux vaut le lui dire ici que le
   * lui laisser découvrir sur sa vitrine.
   */
  readonly hasPlaceholders = computed(() =>
    [this.address(), this.phone(), this.description()].some(
      (value) => value.includes('[') && value.includes(']'),
    ),
  );

  /** Les URL stockées sont relatives à l'API : l'aperçu a besoin de l'origine. */
  readonly logoPreview = computed(() => this.absolute(this.logoUrl()));
  readonly bannerPreview = computed(() => this.absolute(this.bannerUrl()));
  readonly bannerMobilePreview = computed(() => this.absolute(this.bannerMobileUrl()));

  /** Ce que voit le visiteur tant que la boutique n'a pas ses propres visuels. */
  readonly defaultBanner = DEFAULT_BANNER;

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
      this.bannerMobileUrl.set(store.bannerMobileUrl ?? '');
    } catch {
      this.error.set('Impossible de charger les informations de la boutique.');
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Le fichier part immédiatement, mais l'URL n'est enregistrée qu'avec le reste
   * du formulaire : on évite de publier un visuel sur la vitrine avant que le
   * propriétaire ait validé.
   */
  async onFileSelected(slot: ImageSlot, input: HTMLInputElement): Promise<void> {
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.uploading.set(slot);
    this.error.set(null);

    try {
      const { url } = await this.storeAdminService.uploadImage(file);
      this.slots[slot].set(url);
    } catch {
      this.error.set(
        "Le fichier n'a pas pu être envoyé. Formats acceptés : JPEG, PNG, GIF, WebP, 5 Mo maximum.",
      );
    } finally {
      this.uploading.set(null);
      // Permet de re-sélectionner le même fichier après une erreur.
      input.value = '';
    }
  }

  removeImage(slot: ImageSlot): void {
    this.slots[slot].set('');
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
        bannerMobileUrl: this.bannerMobileUrl().trim() || null,
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

  private absolute(url: string): string | null {
    if (!url) {
      return null;
    }
    return url.startsWith('/') ? `${this.apiOrigin}${url}` : url;
  }
}
