import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CartItem, CheckoutPayload, OrderConfirmation } from '../models/order.model';
import { PublisherReferenceService } from './publisher-reference';

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
  publisherRef: string | null;
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

export type AdminOrder = {
  orderId: string;
  customerName: string;
  city: string;
  paymentMethod: string;
  publisherRef: string | null;
  status: string;
  estimatedDelivery: string;
  total: number;
};

export type AdminOrderItem = {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
};

export type AdminOrderDetail = AdminOrder & {
  phone: string;
  address: string;
  note: string | null;
  items: AdminOrderItem[];
};

export const ORDER_STATUSES = [
  'EN_ATTENTE_VALIDATION_ADMIN',
  'ANNULEE',
  'VALIDEE_PAR_LE_CLIENT',
  'LIVREE_ET_PAYEE',
  'RETOURNEE_PAR_LE_CLIENT',
  'LIVRAISON_EN_COURS',
] as const;

export type OrderStatus = (typeof ORDER_STATUSES)[number];

@Service()
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly publisherReferenceService = inject(PublisherReferenceService);
  private readonly baseUrl = `${environment.apiBaseUrl}/orders`;

  async listOrders(): Promise<AdminOrder[]> {
    return firstValueFrom(this.http.get<AdminOrder[]>(this.baseUrl));
  }

  async listOrdersByPublisherReference(publisherRef: string): Promise<AdminOrder[]> {
    return firstValueFrom(this.http.get<AdminOrder[]>(this.baseUrl, { params: { ref: publisherRef } }));
  }

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
      publisherRef: this.publisherReferenceService.reference(),
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

  async getOrder(orderId: string): Promise<AdminOrderDetail> {
    return firstValueFrom(this.http.get<AdminOrderDetail>(`${this.baseUrl}/${orderId}`));
  }

  async updateOrder(orderId: string, status: OrderStatus, note: string): Promise<AdminOrderDetail> {
    return firstValueFrom(
      this.http.put<AdminOrderDetail>(`${this.baseUrl}/${orderId}`, { status, note }),
    );
  }
}
