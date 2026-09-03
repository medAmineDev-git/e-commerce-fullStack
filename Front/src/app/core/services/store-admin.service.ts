import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OwnedStore, StoreSettingsInput, StoreSummary } from '../models/store.model';

export type SlugCheck = { slug: string; available: boolean };

/** Réglages de sa propre boutique, et console plateforme. */
@Injectable({ providedIn: 'root' })
export class StoreAdminService {
  private readonly http = inject(HttpClient);
  private readonly storeUrl = `${environment.apiBaseUrl}/admin/store`;
  private readonly platformUrl = `${environment.apiBaseUrl}/platform/stores`;
  private readonly publicUrl = `${environment.apiBaseUrl}/public/stores`;

  /** Appelé pendant la saisie du nom, donc sur une route anonyme. */
  checkSlug(name: string): Promise<SlugCheck> {
    return firstValueFrom(
      this.http.get<SlugCheck>(`${this.publicUrl}/slug-check`, { params: { name } }),
    );
  }

  getMyStore(): Promise<OwnedStore> {
    return firstValueFrom(this.http.get<OwnedStore>(this.storeUrl));
  }

  updateMyStore(input: StoreSettingsInput): Promise<OwnedStore> {
    return firstValueFrom(this.http.put<OwnedStore>(this.storeUrl, input));
  }

  listAllStores(): Promise<StoreSummary[]> {
    return firstValueFrom(this.http.get<StoreSummary[]>(this.platformUrl));
  }

  toggleStoreActive(id: number): Promise<StoreSummary> {
    return firstValueFrom(this.http.patch<StoreSummary>(`${this.platformUrl}/${id}/toggle-active`, {}));
  }

  attachDomain(id: number, domain: string | null): Promise<StoreSummary> {
    return firstValueFrom(this.http.put<StoreSummary>(`${this.platformUrl}/${id}/domain`, { domain }));
  }
}
