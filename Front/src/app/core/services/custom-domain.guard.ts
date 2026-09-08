import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';
import { DOCUMENT } from '@angular/common';
import { StoreContextService } from './store-context.service';
import { CartStore } from '../stores/cart.store';

/**
 * Décide, à la racine, si l'on est sur le domaine d'une boutique.
 *
 * Le DNS ne connaît que des noms de serveurs : `yomna-fashion.com` arrive donc
 * sur `/`, sans le moindre slug dans l'adresse. C'est ici qu'on rattrape
 * l'information, en demandant au serveur quelle boutique porte ce nom d'hôte.
 *
 * `canMatch` plutôt que `canActivate` : refuser doit laisser la route suivante
 * — la page de présentation de la plateforme — prendre la main, ce qu'un
 * `canActivate` ne permet pas.
 */
export const customDomainGuard: CanMatchFn = async () => {
  const storeContext = inject(StoreContextService);
  const cartStore = inject(CartStore);
  const document = inject(DOCUMENT);

  const hostname = document.defaultView?.location?.hostname;
  if (!hostname) {
    // Rendu hors navigateur, au prérendu : il n'y a pas d'hôte à interroger,
    // et la page servie est celle de la plateforme.
    return false;
  }

  const store = await storeContext.resolveByDomain(hostname);
  if (!store) {
    return false;
  }

  // Le panier est propre a chaque boutique, comme sur /boutique/<slug>.
  cartStore.hydrate(store.slug);
  return true;
};
