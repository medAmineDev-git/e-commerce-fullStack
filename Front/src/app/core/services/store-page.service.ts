import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StorePage, StorePageInput, StorePageSummary } from '../models/store-page.model';
import { StoreContextService } from './store-context.service';

/**
 * Pages de contenu : mentions légales, livraison, retours.
 *
 * La vitrine passe par les routes publiques de la boutique courante, le
 * back-office par ses propres routes, où la boutique se déduit du jeton.
 */
@Injectable({ providedIn: 'root' })
export class StorePageService {
  private readonly http = inject(HttpClient);
  private readonly storeContext = inject(StoreContextService);
  private readonly adminUrl = `${environment.apiBaseUrl}/admin/pages`;

  /** Liens du pied de page, sans les textes. */
  listPublicPages(): Promise<StorePageSummary[]> {
    const base = this.storeContext.requireStoreApiUrl();
    return firstValueFrom(this.http.get<StorePageSummary[]>(`${base}/pages`));
  }

  getPublicPage(pageSlug: string): Promise<StorePage> {
    const base = this.storeContext.requireStoreApiUrl();
    return firstValueFrom(
      this.http.get<StorePage>(`${base}/pages/${encodeURIComponent(pageSlug)}`),
    );
  }

  listPages(): Promise<StorePage[]> {
    return firstValueFrom(this.http.get<StorePage[]>(this.adminUrl));
  }

  getPage(id: number): Promise<StorePage> {
    return firstValueFrom(this.http.get<StorePage>(`${this.adminUrl}/${id}`));
  }

  createPage(input: StorePageInput): Promise<StorePage> {
    return firstValueFrom(this.http.post<StorePage>(this.adminUrl, input));
  }

  updatePage(id: number, input: StorePageInput): Promise<StorePage> {
    return firstValueFrom(this.http.put<StorePage>(`${this.adminUrl}/${id}`, input));
  }

  deletePage(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.adminUrl}/${id}`));
  }

  /** Réinstalle les pages livrées manquantes, sans toucher aux autres. */
  restoreDefaults(): Promise<StorePage[]> {
    return firstValueFrom(this.http.post<StorePage[]>(`${this.adminUrl}/defaults`, {}));
  }
}
