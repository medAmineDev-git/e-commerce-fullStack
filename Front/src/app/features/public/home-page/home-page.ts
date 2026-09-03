import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { StoreContextService } from '../../../core/services/store-context.service';
import { Router, RouterLink } from '@angular/router';
import { CartStore } from '../../../core/stores/cart.store';
import { PublicCatalogStore } from '../../../core/stores/public-catalog.store';
import { PublicCategory, PublicProduct } from '../../../core/models/public-product.model';
import { HomeConfigurationService } from '../../../core/services/home-configuration';
import { HomeConfiguration } from '../../../core/models/home-configuration.model';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  readonly storeContext = inject(StoreContextService);
  private readonly router = inject(Router);
  private readonly homeConfigurationService = inject(HomeConfigurationService);
  readonly catalogStore = inject(PublicCatalogStore);
  readonly cartStore = inject(CartStore);

  readonly topProducts = computed(() => this.catalogStore.products().slice(0, 4));
  readonly spotlight = computed(() => this.catalogStore.products().slice(0, 1)[0] ?? null);
  readonly homeConfiguration = signal<HomeConfiguration | null>(null);
  readonly heroTitle = computed(
    () => this.homeConfiguration()?.title ?? 'Style urbain, livraison rapide, paiement a la livraison.',
  );
  readonly heroText = computed(
    () => this.homeConfiguration()?.text ?? 'Decouvre une selection orientee streetwear premium avec une experience mobile ultra simple: recherche, panier et checkout en moins de 2 minutes.',
  );
  readonly configuredSpotlight = computed(() => {
    const configuredId = this.homeConfiguration()?.featuredProductId;
    return this.catalogStore.products().find((product) => product.id === configuredId) ?? this.spotlight();
  });

  constructor() {
    void this.catalogStore.loadProducts();
    void this.loadHomeConfiguration();
  }

  private async loadHomeConfiguration(): Promise<void> {
    try {
      this.homeConfiguration.set(await this.homeConfigurationService.get());
    } catch {
      this.homeConfiguration.set(null);
    }
  }

  openCategory(category: PublicCategory): void {
    this.catalogStore.setCategory(category);
    void this.router.navigate(this.storeContext.link('shop'), { queryParams: { category } });
  }

  openProduct(product: PublicProduct): void {
    void this.router.navigate(this.storeContext.link('product', product.id));
  }

  addToCart(product: PublicProduct): void {
    this.cartStore.addItem(product, 1);
  }
}
