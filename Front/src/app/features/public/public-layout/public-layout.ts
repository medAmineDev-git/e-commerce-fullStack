import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CartStore } from '../../../core/stores/cart.store';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CurrencyPipe],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
})
export class PublicLayout {
  private readonly router = inject(Router);
  readonly cartStore = inject(CartStore);
  readonly searchTerm = signal('');
  readonly cartDrawerOpen = signal(false);

  constructor() {
    this.cartStore.hydrate();
  }

  goToSearch(term: string): void {
    const cleaned = term.trim();
    void this.router.navigate(['/shop'], {
      queryParams: cleaned ? { q: cleaned } : {},
    });
  }

  toggleCartDrawer(): void {
    this.cartDrawerOpen.update((value) => !value);
  }

  closeCartDrawer(): void {
    this.cartDrawerOpen.set(false);
  }
}
