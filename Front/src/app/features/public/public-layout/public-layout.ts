import { Component, effect, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { StoreContextService } from '../../../core/services/store-context.service';
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
  readonly cartStore = inject(CartStore);
  private readonly router = inject(Router);
  private readonly publisherReferenceService = inject(PublisherReferenceService);
  private readonly seo = inject(SeoService);

  /** L'identité affichée vient de la boutique, jamais d'un nom écrit en dur. */
  readonly store = this.storeContext.store;
  readonly cartDrawerOpen = signal(false);

  constructor() {
    effect(() => {
      const store = this.store();
      if (store) {
        this.seo.apply({
          title: store.name,
          description: store.description ?? `Découvrez la sélection de ${store.name}.`,
          path: `/boutique/${store.slug}`,
          imageUrl: store.bannerUrl ?? undefined,
        });
      }
    });

    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => {
        this.captureReference();
        // Un tiroir ouvert doit se refermer quand on change de page,
        // sinon il masque la vue sur laquelle on vient d'arriver.
        this.cartDrawerOpen.set(false);
      });

    this.captureReference();
  }

  toggleCartDrawer(): void {
    this.cartDrawerOpen.update((open) => !open);
  }

  closeCartDrawer(): void {
    this.cartDrawerOpen.set(false);
  }

  private captureReference(): void {
    const reference = new URLSearchParams(this.router.url.split('?')[1] ?? '').get('ref');
    this.publisherReferenceService.capture(reference);
  }
}
