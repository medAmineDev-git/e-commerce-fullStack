import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { formatAmountInput, hasAtMostThreeDecimals, parseAmount } from '../../../core/models/amount';
import { StoreAdminService } from '../../../core/services/store-admin.service';

/**
 * Réglages divers de la boutique, par section indépendante.
 *
 * L'identité et les visuels restent dans Configuration : ce qu'on règle ici
 * change le fonctionnement de la vente, pas l'apparence de la vitrine. Chaque
 * section s'enregistre seule, pour qu'une erreur dans l'une ne bloque pas l'autre.
 */
@Component({
  selector: 'app-settings-page',
  imports: [CurrencyPipe],
  templateUrl: './settings-page.html',
  styleUrl: './settings-page.scss',
})
export class SettingsPage {
  private readonly storeAdminService = inject(StoreAdminService);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);

  // ------------------------------------------------------------ livraison

  /** Texte saisi, conservé tel quel : « 6, » ne doit pas perdre sa virgule sous les doigts. */
  readonly feeText = signal('');
  readonly freeDeliveryEnabled = signal(true);
  readonly freeFromText = signal('');

  readonly deliverySaving = signal(false);
  readonly deliverySavedAt = signal<Date | null>(null);
  readonly deliveryError = signal<string | null>(null);
  readonly deliverySubmitted = signal(false);

  readonly fee = computed(() => parseAmount(this.feeText()));
  readonly freeFrom = computed(() => (this.freeDeliveryEnabled() ? parseAmount(this.freeFromText()) : null));

  readonly deliveryErrors = computed(() => {
    const fee = this.fee();
    const freeFrom = this.freeFrom();
    return {
      fee:
        fee === null || fee < 0
          ? 'Indiquez un montant, 0 si la livraison est toujours offerte.'
          : !hasAtMostThreeDecimals(fee)
            ? 'Trois décimales au plus : le dinar compte en millimes.'
            : '',
      freeFrom: !this.freeDeliveryEnabled()
        ? ''
        : freeFrom === null || freeFrom <= 0
          ? "Indiquez le montant d'achat à partir duquel la livraison est offerte."
          : !hasAtMostThreeDecimals(freeFrom)
            ? 'Trois décimales au plus : le dinar compte en millimes.'
            : '',
    };
  });

  readonly deliveryValid = computed(() => !this.deliveryErrors().fee && !this.deliveryErrors().freeFrom);

  /** Ce que paiera le client, dit en une phrase : plus parlant que deux champs. */
  readonly deliverySummary = computed<'free' | 'threshold' | 'always' | null>(() => {
    if (!this.deliveryValid()) {
      return null;
    }
    if (this.fee() === 0) {
      return 'free';
    }
    return this.freeFrom() === null ? 'always' : 'threshold';
  });

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    try {
      const store = await this.storeAdminService.getMyStore();
      this.feeText.set(formatAmountInput(store.deliveryFee));
      this.freeDeliveryEnabled.set(store.freeDeliveryFrom !== null);
      this.freeFromText.set(formatAmountInput(store.freeDeliveryFrom));
    } catch {
      this.loadError.set('Impossible de charger les paramètres de la boutique.');
    } finally {
      this.loading.set(false);
    }
  }

  updateFee(value: string): void {
    this.feeText.set(value);
    this.deliverySavedAt.set(null);
  }

  updateFreeFrom(value: string): void {
    this.freeFromText.set(value);
    this.deliverySavedAt.set(null);
  }

  setFreeDeliveryEnabled(enabled: boolean): void {
    this.freeDeliveryEnabled.set(enabled);
    this.deliverySavedAt.set(null);
    // Réactiver le seuil propose la valeur habituelle plutôt qu'un champ vide.
    if (enabled && !this.freeFromText().trim()) {
      this.freeFromText.set('100');
    }
  }

  async saveDelivery(): Promise<void> {
    this.deliverySubmitted.set(true);
    this.deliveryError.set(null);
    if (!this.deliveryValid() || this.deliverySaving()) {
      return;
    }

    this.deliverySaving.set(true);
    try {
      const updated = await this.storeAdminService.updateDeliverySettings({
        deliveryFee: this.fee()!,
        freeDeliveryFrom: this.freeFrom(),
      });
      this.feeText.set(formatAmountInput(updated.deliveryFee));
      this.freeFromText.set(formatAmountInput(updated.freeDeliveryFrom) || this.freeFromText());
      this.deliverySavedAt.set(new Date());
      this.deliverySubmitted.set(false);
    } catch {
      this.deliveryError.set("L'enregistrement a échoué. Réessayez dans un instant.");
    } finally {
      this.deliverySaving.set(false);
    }
  }
}
