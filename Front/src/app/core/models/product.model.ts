export type ProductStatus = 'ACTIVE' | 'DRAFT';

export interface ProductColor {
  name: string;
  hex: string;
}

export interface Product {
  id: number;
  name: string;
  /** Facultatifs depuis la mise en ligne sans taxonomie : le serveur renvoie null. */
  category: string | null;
  subcategory?: string;
  description: string | null;
  price: number;
  stockQuantity: number;
  sku?: string;
  compareAtPrice?: number | null;
  status?: ProductStatus;
  imageUrls?: string[];
  sizes?: string[];
  seasons?: string[];
  colors?: ProductColor[];
  seoTitle?: string;
  seoDescription?: string;
}

export type ProductInput = Omit<Product, 'id'>;
