import { PublicProduct } from './public-product.model';

export type CheckoutPaymentMethod = 'cash_on_delivery';

export interface CartItem {
  product: PublicProduct;
  quantity: number;
  /** Taille choisie ; null si le produit n'en propose pas. */
  size: string | null;
  /** Nom de la couleur choisie ; null si le produit n'en propose pas. */
  color: string | null;
}

/**
 * Une ligne de panier est un produit dans une déclinaison : le même body en
 * 3 mois et en 12 mois fait deux lignes, chacune avec sa quantité.
 */
export function cartLineKey(item: Pick<CartItem, 'size' | 'color'> & { product: { id: number } }): string {
  return JSON.stringify([item.product.id, item.size, item.color]);
}

/** « 12 mois · Bleu nuit », ou une chaîne vide pour un article sans déclinaison. */
export function variantLabel(item: { size?: string | null; color?: string | null }): string {
  return [item.size, item.color].filter((part) => !!part).join(' · ');
}

export interface CheckoutPayload {
  customerName: string;
  phone: string;
  city: string;
  address: string;
  note: string;
  paymentMethod: CheckoutPaymentMethod;
}

export interface OrderConfirmation {
  orderId: string;
  estimatedDelivery: string;
  total: number;
  status: 'confirmed';
  items: CartItem[];
}
