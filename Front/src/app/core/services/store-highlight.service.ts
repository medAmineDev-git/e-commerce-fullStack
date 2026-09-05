import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  StoreHighlight,
  StoreHighlightInput,
  StoreHighlights,
} from '../models/store-highlight.model';
import { StoreContextService } from './store-context.service';

/**
 * Bandeau de réassurance : livraison, assistance, retours, paiement.
 *
 * La vitrine n'en reçoit que les lignes actives ; le back-office les voit
 * toutes, y compris celles que le vendeur a momentanément coupées.
 */
@Injectable({ providedIn: 'root' })
export class StoreHighlightService {
  private readonly http = inject(HttpClient);
  private readonly storeContext = inject(StoreContextService);
  private readonly adminUrl = `${environment.apiBaseUrl}/admin/highlights`;

  /**
   * Le bandeau paraît à deux endroits, portés par deux composants différents.
   * Il est donc chargé une fois et partagé, plutôt que demandé deux fois pour
   * la même page.
   */
  private readonly cache = signal<StoreHighlights | null>(null);
  readonly highlights = this.cache.asReadonly();

  async loadPublicHighlights(): Promise<void> {
    try {
      this.cache.set(await this.getPublicHighlights());
    } catch {
      // Un bandeau absent vaut mieux qu'une vitrine en erreur.
      this.cache.set(null);
    }
  }

  getPublicHighlights(): Promise<StoreHighlights> {
    const base = this.storeContext.requireStoreApiUrl();
    return firstValueFrom(this.http.get<StoreHighlights>(`${base}/highlights`));
  }

  getHighlights(): Promise<StoreHighlights> {
    return firstValueFrom(this.http.get<StoreHighlights>(this.adminUrl));
  }

  create(input: StoreHighlightInput): Promise<StoreHighlight> {
    return firstValueFrom(this.http.post<StoreHighlight>(this.adminUrl, input));
  }

  update(id: number, input: StoreHighlightInput): Promise<StoreHighlight> {
    return firstValueFrom(this.http.put<StoreHighlight>(`${this.adminUrl}/${id}`, input));
  }

  remove(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.adminUrl}/${id}`));
  }

  /** Les deux emplacements se règlent ensemble, en un seul appel. */
  updateSettings(topEnabled: boolean, bottomEnabled: boolean): Promise<StoreHighlights> {
    return firstValueFrom(
      this.http.put<StoreHighlights>(`${this.adminUrl}/settings`, { topEnabled, bottomEnabled }),
    );
  }
}
