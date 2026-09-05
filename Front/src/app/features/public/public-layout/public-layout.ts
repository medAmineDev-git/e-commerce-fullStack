import { Component, computed, effect, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { StoreContextService } from '../../../core/services/store-context.service';
import { CartStore } from '../../../core/stores/cart.store';
import { PublisherReferenceService } from '../../../core/services/publisher-reference';
import { SeoService } from '../../../core/seo/seo.service';
import { StorePageService } from '../../../core/services/store-page.service';
import { StorePageSummary } from '../../../core/models/store-page.model';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, CurrencyPipe],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
})
export class PublicLayout {
  readonly storeContext = inject(StoreContextService);
  readonly cartStore = inject(CartStore);
  private readonly router = inject(Router);
  private readonly publisherReferenceService = inject(PublisherReferenceService);
  private readonly seo = inject(SeoService);
  private readonly storePageService = inject(StorePageService);

  /** L'identité affichée vient de la boutique, jamais d'un nom écrit en dur. */
  readonly store = this.storeContext.store;
  readonly cartDrawerOpen = signal(false);
  readonly currentYear = new Date().getFullYear();

  /** La section Informations ne s'affiche que si le proprietaire l'a renseignee. */
  readonly hasContactDetails = computed(() => {
    const store = this.store();
    return !!(store?.address || store?.phone || store?.email);
  });

  /** Liens de pied de page, vides tant que la boutique n'a aucune page. */
  readonly pages = signal<StorePageSummary[]>([]);

  constructor() {
    effect(() => {
      const store = this.store();
      if (store) {
        void this.loadPages();
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

  /** Un pied de page sans liens vaut mieux qu'un pied de page en erreur. */
  private async loadPages(): Promise<void> {
    try {
      this.pages.set(await this.storePageService.listPublicPages());
    } catch {
      this.pages.set([]);
    }
  }

  private captureReference(): void {
    const reference = new URLSearchParams(this.router.url.split('?')[1] ?? '').get('ref');
    this.publisherReferenceService.capture(reference);
  }
}
