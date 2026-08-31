import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { OrderService, AdminOrder } from '../../../core/services/order';
import { AdminOrderDetail, ORDER_STATUSES, OrderStatus } from '../../../core/services/order';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-order-list',
  imports: [CurrencyPipe],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss',
})
export class OrderList {
  private readonly orderService = inject(OrderService);
  private readonly snackBar = inject(MatSnackBar);

  readonly orders = signal<AdminOrder[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly selectedOrder = signal<AdminOrderDetail | null>(null);
  readonly selectedStatus = signal<OrderStatus | null>(null);
  readonly selectedNote = signal('');
  readonly saving = signal(false);
  readonly orderStatuses = ORDER_STATUSES;
  readonly selectedPublisherReference = signal('');
  readonly publisherReferences = ['am', 'wa'] as const;

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      EN_ATTENTE_VALIDATION_ADMIN: 'En attente validation admin',
      ANNULEE: 'Annulée',
      VALIDEE_PAR_LE_CLIENT: 'Validée par le client',
      LIVREE_ET_PAYEE: 'Livrée et payée',
      RETOURNEE_PAR_LE_CLIENT: 'Retournée par le client',
      LIVRAISON_EN_COURS: 'Livraison en cours',
    };
    return labels[status] ?? status;
  }

  constructor() {
    void this.loadOrders();
  }

  async retry(): Promise<void> {
    await this.loadOrders();
  }

  async filterByPublisherReference(value: string): Promise<void> {
    this.selectedPublisherReference.set(value);
    await this.loadOrders();
  }

  async openDetails(orderId: string): Promise<void> {
    try {
      const order = await this.orderService.getOrder(orderId);
      this.selectedOrder.set(order);
      this.selectedStatus.set(order.status as OrderStatus);
      this.selectedNote.set(order.note ?? '');
    } catch {
      this.snackBar.open('Impossible de charger le détail de la commande.', 'Fermer', { duration: 3500 });
    }
  }

  closeDetails(): void {
    if (!this.saving()) {
      this.selectedOrder.set(null);
    }
  }

  async saveDetails(): Promise<void> {
    const order = this.selectedOrder();
    const status = this.selectedStatus();
    if (!order || !status || this.saving()) {
      return;
    }

    this.saving.set(true);
    try {
      const updated = await this.orderService.updateOrder(order.orderId, status, this.selectedNote());
      this.selectedOrder.set(updated);
      this.orders.update((orders) => orders.map((item) => item.orderId === updated.orderId
        ? { ...item, status: updated.status, estimatedDelivery: updated.estimatedDelivery, total: updated.total }
        : item));
      this.snackBar.open('Commande mise à jour avec succès.', 'Fermer', { duration: 3000 });
    } catch {
      this.snackBar.open('Impossible de mettre à jour la commande.', 'Fermer', { duration: 4000 });
    } finally {
      this.saving.set(false);
    }
  }

  private async loadOrders(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const publisherRef = this.selectedPublisherReference();
      this.orders.set(publisherRef
        ? await this.orderService.listOrdersByPublisherReference(publisherRef)
        : await this.orderService.listOrders());
    } catch {
      this.error.set('Impossible de charger les commandes.');
    } finally {
      this.loading.set(false);
    }
  }
}
