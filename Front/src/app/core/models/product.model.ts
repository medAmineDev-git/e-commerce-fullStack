export type ProductStatus = 'ACTIVE' | 'DRAFT';

/** Une ligne du bloc « Livraison et retours » de la fiche produit. */
export interface ProductServiceTerm {
  label: string;
  value: string;
}

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
  /** Prix de gros, facultatif. Renvoyé au back-office seulement, jamais à la vitrine. */
  wholesalePrice?: number | null;
  status?: ProductStatus;
  imageUrls?: string[];
  sizes?: string[];
  seasons?: string[];
  colors?: ProductColor[];
  /** Liste vide : le bloc « Livraison et retours » ne paraît pas sur la fiche. */
  serviceTerms?: ProductServiceTerm[];
  seoTitle?: string;
  seoDescription?: string;
}

export type ProductInput = Omit<Product, 'id'>;
