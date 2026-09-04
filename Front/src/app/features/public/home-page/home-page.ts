import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { StoreContextService } from '../../../core/services/store-context.service';
import { PublicCatalogStore } from '../../../core/stores/public-catalog.store';
import { PublicProduct } from '../../../core/models/public-product.model';
import { PublicCatalogService } from '../../../core/services/public-catalog.service';

type StoreHome = { title: string; text: string; featuredProductId: number | null };

@Component({
  selector: 'app-home-page',
  imports: [CurrencyPipe],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  readonly storeContext = inject(StoreContextService);
  readonly catalogStore = inject(PublicCatalogStore);
  private readonly catalogService = inject(PublicCatalogService);
  private readonly router = inject(Router);

  readonly store = this.storeContext.store;
  readonly homeConfiguration = signal<StoreHome | null>(null);

  readonly heroTitle = computed(
    () => this.homeConfiguration()?.title ?? this.store()?.name ?? 'La sélection',
  );
  readonly heroText = computed(
    () => this.homeConfiguration()?.text ?? this.store()?.description ?? '',
  );

  /** Le visuel de tête : la bannière de la boutique, sinon le produit mis en avant. */
  readonly featured = computed(() => {
    const configuredId = this.homeConfiguration()?.featuredProductId;
    const products = this.catalogStore.products();
    return products.find((product) => product.id === configuredId) ?? products[0] ?? null;
  });

  readonly newArrivals = computed(() => this.catalogStore.products().slice(0, 8));

  /**
   * Les catégories viennent du catalogue de la boutique.
   * Elles étaient auparavant écrites en dur — ce qui affichait Homme, Femme,
   * Sneakers et Accessoires à une boutique qui n'en avait aucune.
   */
  readonly categories = computed(() =>
    [...new Set(this.catalogStore.products().map((product) => product.category))]
      .filter(Boolean)
      .slice(0, 6),
  );

  constructor() {
    void this.catalogStore.loadProducts();
    void this.loadHomeConfiguration();
  }

  private async loadHomeConfiguration(): Promise<void> {
    try {
      this.homeConfiguration.set(await this.catalogService.getHomeConfiguration());
    } catch {
      // Une boutique sans page d'accueil configurée reste parfaitement utilisable.
      this.homeConfiguration.set(null);
    }
  }

  openCategory(category: string): void {
    void this.router.navigate(this.storeContext.link('shop'), { queryParams: { category } });
  }

  openProduct(product: PublicProduct): void {
    void this.router.navigate(this.storeContext.link('product', product.id));
  }
}
