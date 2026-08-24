import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PublicCategory, PublicProduct } from '../models/public-product.model';
import { PublicCatalogMockService } from './public-catalog.mock';
import { SortDirection } from '../stores/crud-list.helpers';

type BackendProduct = {
  id: number;
  name: string;
  category: PublicCategory;
  description: string;
  price: number;
  stockQuantity: number;
};

type BackendProductPageResponse = {
  items: BackendProduct[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  sortBy: string;
  sortDirection: SortDirection;
  query: string;
  category: string;
};

export type CatalogSortField = 'id' | 'name' | 'price' | 'stockQuantity';

export type PublicCatalogPageQuery = {
  query: string;
  category: PublicCategory | 'Tous';
  page: number;
  size: number;
  sortBy: CatalogSortField;
  sortDirection: SortDirection;
};

export type PublicCatalogPageResponse = {
  items: PublicProduct[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};

const IMAGE_POOL = [
  'https://images.unsplash.com/photo-1551537482-f2075a1d41f2?auto=format&fit=crop&w=1100&q=80',
  'https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=1100&q=80',
  'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1100&q=80',
  'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=1100&q=80',
];

const FALLBACK_COLORS = [
  { name: 'Sable', hex: '#dac4ab' },
  { name: 'Carbone', hex: '#1f1f1f' },
  { name: 'Ivoire', hex: '#f7efe3' },
];

const FALLBACK_SIZES = ['S', 'M', 'L'] as const;

@Service()
export class PublicCatalogService {
  private readonly http = inject(HttpClient);
  private readonly mockService = inject(PublicCatalogMockService);
  private readonly baseUrl = `${environment.apiBaseUrl}/products`;

  async listProducts(): Promise<PublicProduct[]> {
    if (environment.useMockPublicCatalog) {
      return this.mockService.listProducts();
    }

    try {
      const products = await firstValueFrom(this.http.get<BackendProduct[]>(this.baseUrl));
      return products.map((product) => this.mapBackendProduct(product));
    } catch {
      // Fallback resilient: the public shop remains usable if backend is offline.
      return this.mockService.listProducts();
    }
  }

  async listProductsPage(query: PublicCatalogPageQuery): Promise<PublicCatalogPageResponse> {
    if (environment.useMockPublicCatalog) {
      const mockProducts = await this.mockService.listProducts();
      return this.pageMockProducts(mockProducts, query);
    }

    try {
      const backendPage = await firstValueFrom(
        this.http.get<BackendProductPageResponse>(`${this.baseUrl}/page`, {
          params: {
            q: query.query,
            category: query.category === 'Tous' ? '' : query.category,
            page: String(query.page),
            size: String(query.size),
            sortBy: query.sortBy,
            sortDirection: query.sortDirection,
          },
        }),
      );

      return {
        items: backendPage.items.map((product) => this.mapBackendProduct(product)),
        page: backendPage.page,
        size: backendPage.size,
        totalElements: backendPage.totalElements,
        totalPages: backendPage.totalPages,
        last: backendPage.last,
      };
    } catch {
      const mockProducts = await this.mockService.listProducts();
      return this.pageMockProducts(mockProducts, query);
    }
  }

  async getProductById(id: number): Promise<PublicProduct | null> {
    if (environment.useMockPublicCatalog) {
      return this.mockService.getProductById(id);
    }

    try {
      const product = await firstValueFrom(this.http.get<BackendProduct>(`${this.baseUrl}/${id}`));
      return this.mapBackendProduct(product);
    } catch {
      return this.mockService.getProductById(id);
    }
  }

  private mapBackendProduct(product: BackendProduct): PublicProduct {
    const imageUrl = IMAGE_POOL[Math.abs(product.id) % IMAGE_POOL.length];

    return {
      id: product.id,
      slug: this.slugify(product.name),
      name: product.name,
      shortDescription: this.truncate(product.description, 96),
      longDescription: product.description,
      category: product.category,
      price: product.price,
      rating: 4.5,
      reviewsCount: 0,
      stockQuantity: product.stockQuantity,
      imageUrl,
      gallery: [imageUrl],
      colors: [...FALLBACK_COLORS],
      sizes: [...FALLBACK_SIZES],
      reviews: [],
    };
  }

  private pageMockProducts(
    products: PublicProduct[],
    query: PublicCatalogPageQuery,
  ): PublicCatalogPageResponse {
    const normalizedQuery = query.query.trim().toLowerCase();
    const searched = normalizedQuery
      ? products.filter((product) => {
          const index = `${product.name} ${product.shortDescription} ${product.longDescription}`.toLowerCase();
          return index.includes(normalizedQuery);
        })
      : products;

    const filteredByCategory =
      query.category === 'Tous'
        ? searched
        : searched.filter((product) => product.category === query.category);

    const sorted = [...filteredByCategory].sort((left, right) => {
      const leftValue = this.sortValue(left, query.sortBy);
      const rightValue = this.sortValue(right, query.sortBy);

      if (leftValue < rightValue) {
        return query.sortDirection === 'asc' ? -1 : 1;
      }
      if (leftValue > rightValue) {
        return query.sortDirection === 'asc' ? 1 : -1;
      }
      return 0;
    });

    const page = Math.max(query.page, 0);
    const size = Math.max(query.size, 1);
    const start = page * size;
    const items = sorted.slice(start, start + size);
    const totalElements = sorted.length;
    const totalPages = Math.max(Math.ceil(totalElements / size), 1);

    return {
      items,
      page,
      size,
      totalElements,
      totalPages,
      last: page >= totalPages - 1,
    };
  }

  private sortValue(product: PublicProduct, sortBy: CatalogSortField): string | number {
    if (sortBy === 'id') {
      return product.id;
    }
    if (sortBy === 'stockQuantity') {
      return product.stockQuantity;
    }
    return product[sortBy];
  }

  private slugify(value: string): string {
    return value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }

  private truncate(value: string, limit: number): string {
    if (value.length <= limit) {
      return value;
    }
    return `${value.slice(0, limit - 3)}...`;
  }
}
