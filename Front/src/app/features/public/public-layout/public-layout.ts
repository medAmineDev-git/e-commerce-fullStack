import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { CartStore } from '../../../core/stores/cart.store';
import { PublisherReferenceService } from '../../../core/services/publisher-reference';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CurrencyPipe],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
})
export class PublicLayout {
  private readonly router = inject(Router);
  private readonly publisherReferenceService = inject(PublisherReferenceService);
  readonly cartStore = inject(CartStore);
  readonly searchTerm = signal('');
  readonly cartDrawerOpen = signal(false);

  constructor() {
    this.cartStore.hydrate();
    this.router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd)).subscribe(() => {
      const reference = new URLSearchParams(this.router.url.split('?')[1] ?? '').get('ref');
      this.publisherReferenceService.capture(reference);
    });
    this.publisherReferenceService.capture(new URLSearchParams(this.router.url.split('?')[1] ?? '').get('ref'));
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
