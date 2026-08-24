import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CartItem, CheckoutPayload, OrderConfirmation } from '../models/order.model';

type BackendOrderItemRequest = {
  productId: number;
  quantity: number;
};

type BackendOrderRequest = {
  customerName: string;
  phone: string;
  city: string;
  address: string;
  note: string;
  paymentMethod: string;
  items: BackendOrderItemRequest[];
  total: number;
};

type BackendOrderItemResponse = {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
};

type BackendOrderResponse = {
  orderId: string;
  estimatedDelivery: string;
  total: number;
  status: 'confirmed';
  items: BackendOrderItemResponse[];
};

@Service()
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/orders`;

  async placeOrder(
    payload: CheckoutPayload,
    items: CartItem[],
    total: number,
  ): Promise<OrderConfirmation> {
    const body: BackendOrderRequest = {
      customerName: payload.customerName,
      phone: payload.phone,
      city: payload.city,
      address: payload.address,
      note: payload.note,
      paymentMethod: payload.paymentMethod,
      items: items.map((item) => ({ productId: item.product.id, quantity: item.quantity })),
      total,
    };

    const response = await firstValueFrom(this.http.post<BackendOrderResponse>(this.baseUrl, body));

    return {
      orderId: response.orderId,
      estimatedDelivery: response.estimatedDelivery,
      total: response.total,
      status: response.status,
      items,
    };
  }
}
