export interface Product {
  id: number;
  name: string;
  category: string;
  description: string;
  price: number;
  stockQuantity: number;
}

export type ProductInput = Omit<Product, 'id'>;
