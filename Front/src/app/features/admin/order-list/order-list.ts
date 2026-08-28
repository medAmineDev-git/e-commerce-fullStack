import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { OrderService, AdminOrder } from '../../../core/services/order';

@Component({
  selector: 'app-order-list',
  imports: [CurrencyPipe],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss',
})
export class OrderList {
  private readonly orderService = inject(OrderService);

  readonly orders = signal<AdminOrder[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    void this.loadOrders();
  }

  async retry(): Promise<void> {
    await this.loadOrders();
  }

  private async loadOrders(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.orders.set(await this.orderService.listOrders());
    } catch {
      this.error.set('Impossible de charger les commandes.');
    } finally {
      this.loading.set(false);
    }
  }
}
