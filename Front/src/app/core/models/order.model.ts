import { PublicProduct } from './public-product.model';

export type CheckoutPaymentMethod = 'cash_on_delivery';

export interface CartItem {
  product: PublicProduct;
  quantity: number;
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
