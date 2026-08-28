import { Service } from '@angular/core';
import { CartItem, CheckoutPayload, OrderConfirmation } from '../models/order.model';

function wait(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function formatDeliveryDate(baseDate: Date): string {
  return baseDate.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });
}

@Service()
export class OrderMockService {
  async placeOrder(
    payload: CheckoutPayload,
    items: CartItem[],
    total: number,
  ): Promise<OrderConfirmation> {
    await wait(260);

    if (!payload.customerName || !payload.phone || !payload.address || !payload.city) {
      throw new Error('Informations client incompletes');
    }

    if (items.length === 0) {
      throw new Error('Panier vide');
    }

    const expectedDate = new Date();
    expectedDate.setDate(expectedDate.getDate() + 3);

    return {
      orderId: `CMD-${Date.now().toString().slice(-6)}`,
      estimatedDelivery: formatDeliveryDate(expectedDate),
      total,
      status: 'confirmed',
      items,
    };
  }
}
