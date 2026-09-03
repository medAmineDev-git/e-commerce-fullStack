import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PublicStore } from '../models/store.model';

/**
 * La boutique courante de la vitrine.
 *
 * Résolue une seule fois, avant le rendu des routes publiques, puis diffusée
 * aux services de catalogue et de commande. C'est le pendant côté client de
 * `StoreContext` sur le serveur : rien ne devine sa boutique tout seul.
 */
@Injectable({ providedIn: 'root' })
export class StoreContextService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/public/stores`;

  private readonly currentStore = signal<PublicStore | null>(null);

  readonly store = this.currentStore.asReadonly();
  readonly slug = computed(() => this.currentStore()?.slug ?? null);
  readonly isResolved = computed(() => this.currentStore() !== null);

  /** URL de base des routes publiques de la boutique courante. */
  readonly storeApiUrl = computed(() => {
    const slug = this.slug();
    return slug ? `${this.baseUrl}/${encodeURIComponent(slug)}` : null;
  });

  async resolveBySlug(slug: string): Promise<PublicStore | null> {
    const alreadyLoaded = this.currentStore();
    if (alreadyLoaded && alreadyLoaded.slug.toLowerCase() === slug.toLowerCase()) {
      return alreadyLoaded;
    }

    try {
      const store = await firstValueFrom(
        this.http.get<PublicStore>(`${this.baseUrl}/${encodeURIComponent(slug)}`),
      );
      this.currentStore.set(store);
      return store;
    } catch {
      // Boutique inconnue ou désactivée : le serveur répond 404 dans les deux cas,
      // volontairement, pour ne pas révéler qu'elle existe mais est fermée.
      this.currentStore.set(null);
      return null;
    }
  }

  clear(): void {
    this.currentStore.set(null);
  }

  /**
   * Chemin d'une page de la boutique courante.
   *
   * Les liens de la vitrine passent tous par ici : écrire `/shop` en dur
   * enverrait le visiteur hors de sa boutique, sur une route qui n'existe plus.
   */
  link(...segments: (string | number)[]): unknown[] {
    const slug = this.slug();
    return slug ? ['/boutique', slug, ...segments] : ['/'];
  }

  /**
   * URL de la boutique courante, ou erreur explicite.
   * Un appel catalogue sans boutique résolue est un bug, pas un cas limite.
   */
  requireStoreApiUrl(): string {
    const url = this.storeApiUrl();
    if (!url) {
      throw new Error('Aucune boutique résolue pour cette requête');
    }
    return url;
  }
}
