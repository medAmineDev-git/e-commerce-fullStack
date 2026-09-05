import { Component, computed, inject, signal } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { StoreHighlightService } from '../../../core/services/store-highlight.service';
import { StoreHighlight } from '../../../core/models/store-highlight.model';
import { HighlightIcon, HIGHLIGHT_ICON_KEYS } from '../../../shared/highlight-icon/highlight-icon';

/** Libellés lisibles des icônes, pour le sélecteur. */
const ICON_LABELS: Record<string, string> = {
  livraison: 'Camion de livraison',
  assistance: 'Casque d’assistance',
  retours: 'Retour et échange',
  paiement: 'Carte bancaire',
  retrait: 'Retrait en boutique',
  qualite: 'Médaille',
  emballage: 'Paquet cadeau',
  confiance: 'Bouclier',
};

/**
 * Bandeau de réassurance : ce que la vitrine promet, et où elle le promet.
 *
 * Chaque ligne s'active d'un interrupteur plutôt que de se supprimer : couper
 * une promesse le temps des congés ne doit pas obliger à la ressaisir ensuite.
 */
@Component({
  selector: 'app-highlight-manager',
  imports: [HighlightIcon],
  templateUrl: './highlight-manager.html',
  styleUrl: './highlight-manager.scss',
})
export class HighlightManager {
  private readonly highlightService = inject(StoreHighlightService);
  private readonly snackBar = inject(MatSnackBar);

  readonly iconKeys = HIGHLIGHT_ICON_KEYS;

  readonly items = signal<StoreHighlight[]>([]);
  readonly topEnabled = signal(true);
  readonly bottomEnabled = signal(false);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  /** Ligne ouverte dans l'éditeur ; `null` pour une création. */
  readonly editingId = signal<number | null>(null);
  readonly editing = signal(false);
  readonly iconDraft = signal('livraison');
  readonly labelDraft = signal('');
  readonly detailDraft = signal('');

  readonly deletingId = signal<number | null>(null);

  readonly isCreating = computed(() => this.editing() && this.editingId() === null);
  readonly canSave = computed(() => !!this.labelDraft().trim() && !this.saving());

  /** Ce qui paraîtra réellement : l'aperçu ne montre que les lignes actives. */
  readonly visibleItems = computed(() => this.items().filter((item) => item.enabled));

  readonly isHidden = computed(() => !this.topEnabled() && !this.bottomEnabled());

  constructor() {
    void this.load();
  }

  iconLabel(iconKey: string): string {
    return ICON_LABELS[iconKey] ?? iconKey;
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const highlights = await this.highlightService.getHighlights();
      this.items.set(highlights.items);
      this.topEnabled.set(highlights.topEnabled);
      this.bottomEnabled.set(highlights.bottomEnabled);
    } catch {
      this.error.set('Impossible de charger le bandeau. Réessayez dans un instant.');
    } finally {
      this.loading.set(false);
    }
  }

  /** L'interrupteur d'une ligne enregistre aussitôt : rien à valider ensuite. */
  async toggleItem(item: StoreHighlight): Promise<void> {
    this.error.set(null);
    try {
      const updated = await this.highlightService.update(item.id, {
        iconKey: item.iconKey,
        label: item.label,
        detail: item.detail,
        enabled: !item.enabled,
      });
      this.items.update((items) => items.map((one) => (one.id === item.id ? updated : one)));
    } catch {
      this.error.set("L'enregistrement a échoué. Réessayez dans un instant.");
    }
  }

  async togglePlacement(placement: 'top' | 'bottom'): Promise<void> {
    const top = placement === 'top' ? !this.topEnabled() : this.topEnabled();
    const bottom = placement === 'bottom' ? !this.bottomEnabled() : this.bottomEnabled();

    this.error.set(null);
    try {
      const highlights = await this.highlightService.updateSettings(top, bottom);
      this.topEnabled.set(highlights.topEnabled);
      this.bottomEnabled.set(highlights.bottomEnabled);
    } catch {
      this.error.set("L'enregistrement a échoué. Réessayez dans un instant.");
    }
  }

  startCreate(): void {
    this.editingId.set(null);
    this.iconDraft.set('livraison');
    this.labelDraft.set('');
    this.detailDraft.set('');
    this.editing.set(true);
    this.deletingId.set(null);
  }

  startEdit(item: StoreHighlight): void {
    this.editingId.set(item.id);
    this.iconDraft.set(item.iconKey);
    this.labelDraft.set(item.label);
    this.detailDraft.set(item.detail ?? '');
    this.editing.set(true);
    this.deletingId.set(null);
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.editingId.set(null);
  }

  async save(): Promise<void> {
    if (!this.canSave()) {
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const input = {
      iconKey: this.iconDraft(),
      label: this.labelDraft().trim(),
      detail: this.detailDraft().trim() || null,
    };
    const id = this.editingId();

    try {
      if (id === null) {
        const created = await this.highlightService.create({ ...input, enabled: true });
        this.items.update((items) => [...items, created]);
        this.snackBar.open('Argument ajouté.', 'Fermer', { duration: 3000 });
      } else {
        const existing = this.items().find((item) => item.id === id);
        const updated = await this.highlightService.update(id, {
          ...input,
          enabled: existing?.enabled ?? true,
        });
        this.items.update((items) => items.map((item) => (item.id === id ? updated : item)));
        this.snackBar.open('Argument enregistré.', 'Fermer', { duration: 3000 });
      }
      this.cancelEdit();
    } catch {
      this.error.set("L'enregistrement a échoué. Réessayez dans un instant.");
    } finally {
      this.saving.set(false);
    }
  }

  askDelete(item: StoreHighlight): void {
    this.deletingId.set(item.id);
  }

  cancelDelete(): void {
    this.deletingId.set(null);
  }

  async confirmDelete(item: StoreHighlight): Promise<void> {
    this.error.set(null);
    try {
      await this.highlightService.remove(item.id);
      this.items.update((items) => items.filter((one) => one.id !== item.id));
      if (this.editingId() === item.id) {
        this.cancelEdit();
      }
      this.deletingId.set(null);
      this.snackBar.open('Argument supprimé.', 'Fermer', { duration: 3000 });
    } catch {
      this.error.set('La suppression a échoué. Réessayez dans un instant.');
    }
  }
}
