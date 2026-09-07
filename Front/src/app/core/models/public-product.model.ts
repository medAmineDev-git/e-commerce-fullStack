export type PublicCategory = 'Homme' | 'Femme' | 'Sneakers' | 'Accessoires';

export interface ProductColorOption {
  name: string;
  hex: string;
}

export interface ProductReview {
  author: string;
  rating: number;
  comment: string;
  date: string;
}

export type ProductSizeOption = 'XS' | 'S' | 'M' | 'L' | 'XL';

/** Une ligne du bloc « Livraison et retours » de la fiche. */
export interface ProductServiceTerm {
  label: string;
  value: string;
}

export interface PublicProduct {
  id: number;
  slug: string;
  name: string;
  shortDescription: string;
  longDescription: string;
  /**
   * Libre et facultative : chaque boutique nomme ses propres rayons, et un
   * article peut etre publie sans en avoir.
   */
  category: string | null;
  subcategory?: string;
  seasons?: string[];
  /** Reference interne, affichee sur la fiche produit quand elle existe. */
  sku?: string;
  price: number;
  originalPrice?: number;
  rating: number;
  reviewsCount: number;
  stockQuantity: number;
  badge?: 'Nouveau' | 'Best Seller' | 'Edition Limitee';
  imageUrl: string;
  gallery: string[];
  colors?: ProductColorOption[];
  sizes?: ProductSizeOption[];
  sizeGuide?: Array<{ size: ProductSizeOption; chest: string; length: string }>;
  reviews?: ProductReview[];
  /** Vide : le vendeur a retiré le bloc « Livraison et retours » de la fiche. */
  serviceTerms?: ProductServiceTerm[];
}
