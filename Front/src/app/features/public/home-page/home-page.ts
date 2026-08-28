import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartStore } from '../../../core/stores/cart.store';
import { PublicCatalogStore } from '../../../core/stores/public-catalog.store';
import { PublicCategory, PublicProduct } from '../../../core/models/public-product.model';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  private readonly router = inject(Router);
  readonly catalogStore = inject(PublicCatalogStore);
  readonly cartStore = inject(CartStore);

  readonly topProducts = computed(() => this.catalogStore.products().slice(0, 4));
  readonly spotlight = computed(() => this.catalogStore.products().slice(0, 1)[0] ?? null);

  constructor() {
    void this.catalogStore.loadProducts();
  }

  openCategory(category: PublicCategory): void {
    this.catalogStore.setCategory(category);
    void this.router.navigate(['/shop']);
  }

  openProduct(product: PublicProduct): void {
    void this.router.navigate(['/product', product.id]);
  }

  addToCart(product: PublicProduct): void {
    this.cartStore.addItem(product, 1);
  }
}
