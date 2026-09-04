import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OwnedStore, StoreDetail, StoreSettingsInput, StoreSummary } from '../models/store.model';

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

  /** Dépose un visuel et renvoie son URL. Le stockage est déjà cloisonné par boutique. */
  uploadImage(file: File): Promise<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return firstValueFrom(this.http.post<{ url: string }>(`${this.storeUrl}/images`, formData));
  }

  deleteImage(url: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.storeUrl}/images`, { params: { url } }));
  }

  listAllStores(): Promise<StoreSummary[]> {
    return firstValueFrom(this.http.get<StoreSummary[]>(this.platformUrl));
  }

  getStoreDetail(id: number): Promise<StoreDetail> {
    return firstValueFrom(this.http.get<StoreDetail>(`${this.platformUrl}/${id}`));
  }

  /** Suppression definitive : la boutique, son contenu et son compte proprietaire. */
  deleteStore(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.platformUrl}/${id}`));
  }

  toggleStoreActive(id: number): Promise<StoreSummary> {
    return firstValueFrom(this.http.patch<StoreSummary>(`${this.platformUrl}/${id}/toggle-active`, {}));
  }

  attachDomain(id: number, domain: string | null): Promise<StoreSummary> {
    return firstValueFrom(this.http.put<StoreSummary>(`${this.platformUrl}/${id}/domain`, { domain }));
  }
}
