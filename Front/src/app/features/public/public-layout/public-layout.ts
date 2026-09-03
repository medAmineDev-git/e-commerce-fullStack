import { Component, effect, inject, signal } from '@angular/core';
import { StoreContextService } from '../../../core/services/store-context.service';
import { CurrencyPipe } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { CartStore } from '../../../core/stores/cart.store';
import { PublisherReferenceService } from '../../../core/services/publisher-reference';
import { SeoService } from '../../../core/seo/seo.service';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CurrencyPipe],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
})
export class PublicLayout {
  readonly storeContext = inject(StoreContextService);
  private readonly router = inject(Router);
  private readonly publisherReferenceService = inject(PublisherReferenceService);
  readonly cartStore = inject(CartStore);
  private readonly seo = inject(SeoService);
  readonly searchTerm = signal('');
  readonly cartDrawerOpen = signal(false);

  constructor() {
    // Le panier est déjà chargé par la garde de route, qui connaît la boutique
    // avant que ce composant n'existe. Ici on ne fait que titrer la vitrine :
    // sans cela chaque boutique porterait le titre du site vitrine.
    effect(() => {
      const store = this.storeContext.store();
      if (store) {
        this.seo.apply({
          title: store.name,
          description: store.description ?? `Découvrez la sélection de ${store.name}.`,
          path: `/boutique/${store.slug}`,
          imageUrl: store.bannerUrl ?? undefined,
        });
      }
    });

    this.router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd)).subscribe(() => {
      const reference = new URLSearchParams(this.router.url.split('?')[1] ?? '').get('ref');
      this.publisherReferenceService.capture(reference);
    });
    this.publisherReferenceService.capture(new URLSearchParams(this.router.url.split('?')[1] ?? '').get('ref'));
  }

  goToSearch(term: string): void {
    const cleaned = term.trim();
    void this.router.navigate(this.storeContext.link('shop'), {
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
