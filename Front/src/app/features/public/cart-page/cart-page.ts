import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { StoreContextService } from '../../../core/services/store-context.service';
import { Router, RouterLink } from '@angular/router';
import { CartItem } from '../../../core/models/order.model';
import { CartStore } from '../../../core/stores/cart.store';

@Component({
  selector: 'app-cart-page',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './cart-page.html',
  styleUrl: './cart-page.scss',
})
export class CartPage {
  readonly storeContext = inject(StoreContextService);
  private readonly router = inject(Router);
  readonly cartStore = inject(CartStore);

  setQuantity(item: CartItem, value: string): void {
    const quantity = Number(value);
    if (!Number.isFinite(quantity)) {
      return;
    }
    this.cartStore.setQuantity(item.product.id, Math.floor(quantity));
  }

  remove(item: CartItem): void {
    this.cartStore.removeItem(item.product.id);
  }

  proceedToCheckout(): void {
    if (this.cartStore.isEmpty()) {
      return;
    }

    void this.router.navigate(this.storeContext.link('checkout'));
  }
}
