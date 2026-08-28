import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CategoryService } from '../../../core/services/category';
import { AdminOrder, OrderService } from '../../../core/services/order';
import { ProductService } from '../../../core/services/product';
import { Product } from '../../../core/models/product.model';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly orderService = inject(OrderService);

  readonly products = signal<Product[]>([]);
  readonly categoryCount = signal(0);
  readonly orders = signal<AdminOrder[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly inventoryUnits = computed(() =>
    this.products().reduce((total, product) => total + product.stockQuantity, 0),
  );
  readonly lowStockProducts = computed(() =>
    this.products().filter((product) => product.stockQuantity <= 10).sort((a, b) => a.stockQuantity - b.stockQuantity),
  );
  readonly revenue = computed(() => this.orders().reduce((total, order) => total + order.total, 0));
  readonly recentOrders = computed(() => this.orders().slice(0, 5));

  constructor() {
    void this.loadDashboard();
  }

  async retry(): Promise<void> {
    await this.loadDashboard();
  }

  private async loadDashboard(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [products, categories, orders] = await Promise.all([
        firstValueFrom(this.productService.getAll()),
        firstValueFrom(this.categoryService.getAll()),
        this.orderService.listOrders(),
      ]);
      this.products.set(products);
      this.categoryCount.set(categories.length);
      this.orders.set(orders);
    } catch {
      this.error.set('Impossible de charger les données de pilotage. Vérifiez que le backend est démarré.');
    } finally {
      this.loading.set(false);
    }
  }
}
