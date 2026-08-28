export type ProductStatus = 'ACTIVE' | 'DRAFT';

export interface ProductColor {
  name: string;
  hex: string;
}

export interface Product {
  id: number;
  name: string;
  category: string;
  description: string;
  price: number;
  stockQuantity: number;
  sku?: string;
  compareAtPrice?: number | null;
  status?: ProductStatus;
  imageUrls?: string[];
  sizes?: string[];
  colors?: ProductColor[];
  seoTitle?: string;
  seoDescription?: string;
}

export type ProductInput = Omit<Product, 'id'>;
