import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ProductSizeOption, PublicCategory, PublicProduct } from '../models/public-product.model';
import { Category } from '../models/category.model';
import { SortDirection } from '../stores/crud-list.helpers';
import { StoreContextService } from './store-context.service';

type BackendProduct = {
  id: number;
  name: string;
  category: PublicCategory;
  subcategory?: string;
  description: string;
  price: number;
  stockQuantity: number;
  compareAtPrice?: number | null;
  imageUrls?: string[];
  sizes?: string[];
  seasons?: string[];
  colors?: Array<{ name: string; hex: string }>;
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

/** Facettes reellement presentes dans le catalogue de la boutique. */
export type CatalogFacets = {
  categories: string[];
  sizes: string[];
  colors: string[];
  minPrice: number | null;
  maxPrice: number | null;
};

export type PublicCatalogPageQuery = {
  category: PublicCategory | "Tous";
  subcategory: string;
  season: string;
  productSize: string;
  color: string;
  minPrice: number | null;
  maxPrice: number | null;
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
  private readonly storeContext = inject(StoreContextService);
  private readonly apiOrigin = environment.apiBaseUrl.replace(/\/api\/?$/, '');

  /**
   * Toutes les lectures passent par le slug de la boutique courante.
   * Il n'existe plus de route catalogue non rattachée à une boutique.
   */
  private get baseUrl(): string {
    return `${this.storeContext.requireStoreApiUrl()}/products`;
  }

  private get categoriesUrl(): string {
    return `${this.storeContext.requireStoreApiUrl()}/categories`;
  }

  async listCategories(): Promise<Category[]> {
    return firstValueFrom(this.http.get<Category[]>(this.categoriesUrl));
  }

  async listProducts(): Promise<PublicProduct[]> {
    const products = await firstValueFrom(this.http.get<BackendProduct[]>(this.baseUrl));
    return products.map((product) => this.mapBackendProduct(product));
  }

  /** Ce sur quoi cette boutique peut etre filtree. */
  async getFacets(): Promise<CatalogFacets> {
    return firstValueFrom(this.http.get<CatalogFacets>(`${this.baseUrl}/facets`));
  }

  async listProductsPage(query: PublicCatalogPageQuery): Promise<PublicCatalogPageResponse> {
    // Un critere vide n'est pas envoye : le serveur distingue absence de critere
    // et valeur vide, qui ne correspondrait a aucun produit.
    const params: Record<string, string> = {
      page: String(query.page),
      size: String(query.size),
      sortBy: query.sortBy,
      sortDirection: query.sortDirection,
    };

    const optional: Array<[string, string | number | null]> = [
      ['category', query.category === 'Tous' ? '' : query.category],
      ['subcategory', query.subcategory],
      ['season', query.season],
      ['productSize', query.productSize],
      ['color', query.color],
      ['minPrice', query.minPrice],
      ['maxPrice', query.maxPrice],
    ];

    for (const [key, value] of optional) {
      if (value !== null && value !== undefined && String(value).trim() !== '') {
        params[key] = String(value);
      }
    }

    const backendPage = await firstValueFrom(
      this.http.get<BackendProductPageResponse>(`${this.baseUrl}/page`, { params }),
    );

    return {
      items: backendPage.items.map((product) => this.mapBackendProduct(product)),
      page: backendPage.page,
      size: backendPage.size,
      totalElements: backendPage.totalElements,
      totalPages: backendPage.totalPages,
      last: backendPage.last,
    };
  }

  async getHomeConfiguration(): Promise<{ title: string; text: string; featuredProductId: number | null }> {
    return firstValueFrom(
      this.http.get<{ title: string; text: string; featuredProductId: number | null }>(
        `${this.storeContext.requireStoreApiUrl()}/home`,
      ),
    );
  }

  async getProductById(id: number): Promise<PublicProduct> {
    const product = await firstValueFrom(this.http.get<BackendProduct>(`${this.baseUrl}/${id}`));
    return this.mapBackendProduct(product);
  }

  private mapBackendProduct(product: BackendProduct): PublicProduct {
    const gallery = product.imageUrls?.length
      ? product.imageUrls.map((image) => this.resolveImageUrl(image))
      : [];
    const imageUrl = gallery[0] ?? IMAGE_POOL[Math.abs(product.id) % IMAGE_POOL.length];
    const resolvedGallery = gallery.length ? gallery : [imageUrl];
    const sizes = (product.sizes ?? []).filter(this.isProductSize);

    return {
      id: product.id,
      slug: this.slugify(product.name),
      name: product.name,
      shortDescription: this.truncate(product.description, 96),
      longDescription: product.description,
      category: product.category,
      subcategory: product.subcategory,
      seasons: product.seasons ?? [],
      price: product.price,
      originalPrice: product.compareAtPrice ?? undefined,
      rating: 4.5,
      reviewsCount: 0,
      stockQuantity: product.stockQuantity,
      imageUrl,
      gallery: resolvedGallery,
      colors: product.colors?.length ? [...product.colors] : [...FALLBACK_COLORS],
      sizes: sizes.length ? sizes : [...FALLBACK_SIZES],
      reviews: [],
    };
  }

  private resolveImageUrl(image: string): string {
    return image.startsWith('/') ? `${this.apiOrigin}${image}` : image;
  }

  private isProductSize(value: string): value is ProductSizeOption {
    return ['XS', 'S', 'M', 'L', 'XL'].includes(value);
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
