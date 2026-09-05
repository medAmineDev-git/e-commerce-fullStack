import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CategoryService } from '../../../core/services/category';
import { AdminOrder, OrderService } from '../../../core/services/order';
import { ProductService } from '../../../core/services/product';
import { Product } from '../../../core/models/product.model';
import { AuthService } from '../../../core/services/auth';

/** Seuil au-delà duquel un stock cesse d'être une alerte. */
const LOW_STOCK_THRESHOLD = 10;

type StatusTally = { status: string; label: string; count: number; tone: string };

const STATUS_LABELS: Record<string, { label: string; tone: string }> = {
  EN_ATTENTE_VALIDATION_ADMIN: { label: 'À valider', tone: 'warn' },
  VALIDEE_PAR_LE_CLIENT: { label: 'Validée', tone: 'accent' },
  LIVRAISON_EN_COURS: { label: 'En livraison', tone: 'accent' },
  LIVREE_ET_PAYEE: { label: 'Livrée', tone: 'ok' },
  RETOURNEE_PAR_LE_CLIENT: { label: 'Retournée', tone: 'danger' },
  ANNULEE: { label: 'Annulée', tone: 'danger' },
};

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DecimalPipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly orderService = inject(OrderService);
  private readonly authService = inject(AuthService);

  readonly products = signal<Product[]>([]);
  readonly categoryCount = signal(0);
  readonly orders = signal<AdminOrder[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly storeSlug = this.authService.storeSlug;

  readonly inventoryUnits = computed(() =>
    this.products().reduce((total, product) => total + product.stockQuantity, 0),
  );

  readonly lowStockProducts = computed(() =>
    this.products()
      .filter((product) => product.stockQuantity <= LOW_STOCK_THRESHOLD)
      .sort((a, b) => a.stockQuantity - b.stockQuantity),
  );

  readonly outOfStockCount = computed(
    () => this.products().filter((product) => product.stockQuantity === 0).length,
  );

  readonly revenue = computed(() => this.orders().reduce((total, order) => total + order.total, 0));

  /** Panier moyen : le chiffre d'affaires seul ne dit pas si les paniers montent. */
  readonly averageOrder = computed(() => {
    const count = this.orders().length;
    return count === 0 ? 0 : this.revenue() / count;
  });

  /** Ce qui demande une action aujourd'hui. */
  readonly pendingCount = computed(
    () => this.orders().filter((order) => order.status === 'EN_ATTENTE_VALIDATION_ADMIN').length,
  );

  readonly recentOrders = computed(() => this.orders().slice(0, 6));

  /** Répartition par statut, pour voir où le flux se bloque. */
  readonly statusTallies = computed<StatusTally[]>(() => {
    const counts = new Map<string, number>();
    for (const order of this.orders()) {
      counts.set(order.status, (counts.get(order.status) ?? 0) + 1);
    }
    return [...counts.entries()]
      .map(([status, count]) => ({
        status,
        count,
        label: STATUS_LABELS[status]?.label ?? status,
        tone: STATUS_LABELS[status]?.tone ?? 'accent',
      }))
      .sort((a, b) => b.count - a.count);
  });

  /** Une boutique sans produit ni commande n'a pas besoin d'indicateurs à zéro. */
  readonly isBrandNew = computed(
    () => !this.loading() && this.products().length === 0 && this.orders().length === 0,
  );

  constructor() {
    void this.loadDashboard();
  }

  async retry(): Promise<void> {
    await this.loadDashboard();
  }

  statusLabel(status: string): string {
    return STATUS_LABELS[status]?.label ?? status;
  }

  statusTone(status: string): string {
    return STATUS_LABELS[status]?.tone ?? 'accent';
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
      this.error.set(
        'Impossible de charger les indicateurs. Vérifiez que le serveur répond, puis réessayez.',
      );
    } finally {
      this.loading.set(false);
    }
  }
}
