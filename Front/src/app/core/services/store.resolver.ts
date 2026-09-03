import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StoreContextService } from './store-context.service';
import { CartStore } from '../stores/cart.store';

/**
 * Résout la boutique avant le rendu des routes de la vitrine.
 *
 * Rien ne s'affiche tant que la boutique n'est pas connue : sans elle, les
 * services de catalogue n'ont pas d'URL à appeler. Une boutique inconnue ou
 * désactivée renvoie sur une page dédiée plutôt que sur un écran vide.
 */
export const storeResolverGuard: CanActivateFn = async (route) => {
  const storeContext = inject(StoreContextService);
  const cartStore = inject(CartStore);
  const router = inject(Router);

  const slug = route.paramMap.get('slug');
  if (!slug) {
    return router.createUrlTree(['/']);
  }

  const store = await storeContext.resolveBySlug(slug);
  if (!store) {
    return router.createUrlTree(['/boutique-introuvable'], {
      queryParams: { slug },
    });
  }

  // Le panier est propre à chaque boutique : on charge celui-ci en entrant.
  cartStore.hydrate(store.slug);
  return true;
};
