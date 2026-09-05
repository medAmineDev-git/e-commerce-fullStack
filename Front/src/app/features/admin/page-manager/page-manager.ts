import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatSnackBar } from '@angular/material/snack-bar';
import { StorePageService } from '../../../core/services/store-page.service';
import { StorePage } from '../../../core/models/store-page.model';
import { AuthService } from '../../../core/services/auth';

/**
 * Pages de contenu de la boutique : mentions légales, livraison, retours.
 *
 * Liste et éditeur tiennent sur un seul écran : une page se résume à un titre
 * et à un texte, et passer par une vue séparée pour corriger une ligne aurait
 * coûté plus de navigation que le contenu n'en vaut.
 */
@Component({
  selector: 'app-page-manager',
  imports: [DatePipe],
  templateUrl: './page-manager.html',
  styleUrl: './page-manager.scss',
})
export class PageManager {
  private readonly storePageService = inject(StorePageService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly authService = inject(AuthService);

  readonly pages = signal<StorePage[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  /** Page ouverte dans l'éditeur ; `null` pour une création. */
  readonly selectedId = signal<number | null>(null);
  readonly editing = signal(false);

  readonly titleDraft = signal('');
  readonly contentDraft = signal('');

  /** Confirmation de suppression : une page effacée ne se récupère pas. */
  readonly deletingId = signal<number | null>(null);

  readonly storeSlug = this.authService.storeSlug;

  readonly isCreating = computed(() => this.editing() && this.selectedId() === null);

  readonly selectedPage = computed(() => {
    const id = this.selectedId();
    return id === null ? null : (this.pages().find((page) => page.id === id) ?? null);
  });

  /** L'adresse publique se déduit du titre tant que la page n'existe pas. */
  readonly previewSlug = computed(() => this.selectedPage()?.slug ?? this.slugify(this.titleDraft()));

  readonly canSave = computed(
    () => !!this.titleDraft().trim() && !!this.contentDraft().trim() && !this.saving(),
  );

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.pages.set(await this.storePageService.listPages());
    } catch {
      this.error.set('Impossible de charger les pages. Réessayez dans un instant.');
    } finally {
      this.loading.set(false);
    }
  }

  startCreate(): void {
    this.selectedId.set(null);
    this.titleDraft.set('');
    this.contentDraft.set('');
    this.editing.set(true);
    this.deletingId.set(null);
  }

  startEdit(page: StorePage): void {
    this.selectedId.set(page.id);
    this.titleDraft.set(page.title);
    this.contentDraft.set(page.content);
    this.editing.set(true);
    this.deletingId.set(null);
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.selectedId.set(null);
    this.titleDraft.set('');
    this.contentDraft.set('');
  }

  async save(): Promise<void> {
    if (!this.canSave()) {
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const input = { title: this.titleDraft().trim(), content: this.contentDraft().trim() };
    const id = this.selectedId();

    try {
      if (id === null) {
        const created = await this.storePageService.createPage(input);
        this.pages.update((pages) => [...pages, created]);
        this.snackBar.open('Page créée.', 'Fermer', { duration: 3000 });
      } else {
        const updated = await this.storePageService.updatePage(id, input);
        this.pages.update((pages) => pages.map((page) => (page.id === id ? updated : page)));
        this.snackBar.open('Page enregistrée.', 'Fermer', { duration: 3000 });
      }
      this.cancelEdit();
    } catch {
      this.error.set("L'enregistrement a échoué. Réessayez dans un instant.");
    } finally {
      this.saving.set(false);
    }
  }

  askDelete(page: StorePage): void {
    this.deletingId.set(page.id);
  }

  cancelDelete(): void {
    this.deletingId.set(null);
  }

  async confirmDelete(page: StorePage): Promise<void> {
    this.error.set(null);
    try {
      await this.storePageService.deletePage(page.id);
      this.pages.update((pages) => pages.filter((item) => item.id !== page.id));
      if (this.selectedId() === page.id) {
        this.cancelEdit();
      }
      this.deletingId.set(null);
      this.snackBar.open(`« ${page.title} » supprimée.`, 'Fermer', { duration: 3000 });
    } catch {
      this.error.set('La suppression a échoué. Réessayez dans un instant.');
    }
  }

  /** Remet les pages livrées qui manquent, sans écraser celles qui existent. */
  async restoreDefaults(): Promise<void> {
    this.error.set(null);
    try {
      this.pages.set(await this.storePageService.restoreDefaults());
      this.snackBar.open('Pages proposées réinstallées.', 'Fermer', { duration: 3000 });
    } catch {
      this.error.set('La réinstallation a échoué. Réessayez dans un instant.');
    }
  }

  private slugify(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }
}
