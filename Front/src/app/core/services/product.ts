import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Product, ProductInput } from '../models/product.model';

/**
 * Catalogue vu par le propriétaire. Le périmètre vient du jeton : aucun
 * identifiant de boutique ne circule dans ces URL.
 * La vitrine passe par `PublicCatalogService`.
 */
@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/products`;
  readonly apiOrigin = environment.apiBaseUrl.replace(/\/api\/?$/, '');

  getAll(): Observable<Product[]> {
    return this.http.get<Product[]>(this.baseUrl);
  }

  getById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }

  create(product: ProductInput): Observable<Product> {
    return this.http.post<Product>(this.baseUrl, product);
  }

  update(id: number, product: ProductInput): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/${id}`, product);
  }

  uploadImage(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ url: string }>(`${this.baseUrl}/images`, formData);
  }

  deleteImage(url: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/images`, { params: { url } });
  }

  storageUsage(): Observable<{ usedBytes: number; quotaBytes: number }> {
    return this.http.get<{ usedBytes: number; quotaBytes: number }>(`${this.baseUrl}/images/usage`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
