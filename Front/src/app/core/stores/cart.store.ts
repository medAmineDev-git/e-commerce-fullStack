import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { CartItem, cartLineKey } from '../models/order.model';
import { PublicProduct } from '../models/public-product.model';
import { PublicStore } from '../models/store.model';
import { BROWSER_STORAGE } from '../platform/browser-storage';
import { StoreContextService } from '../services/store-context.service';

type CartState = {
  items: CartItem[];
  storeSlug: string | null;
};

/**
 * Une clé par boutique. Sans cela, ouvrir deux vitrines dans le même navigateur
 * mélange les paniers, et l'utilisateur commande sur une boutique des articles
 * ajoutés sur une autre — que le serveur rejettera en 404, sans explication utile.
 *
 * v3 : les lignes portent la taille et la couleur choisies. Les paniers v2 n'en
 * avaient pas, et le serveur les refuserait à la commande pour tout produit qui
 * en propose : ils sont abandonnés plutôt que de faire échouer la commande.
 */
const STORAGE_KEY_PREFIX = 'ecommerce_cart_v3';

export type CartVariant = { size: string | null; color: string | null };

const NO_VARIANT: CartVariant = { size: null, color: null };

function storageKey(storeSlug: string | null): string {
  return storeSlug ? `${STORAGE_KEY_PREFIX}.${storeSlug}` : STORAGE_KEY_PREFIX;
}

/**
 * Un seul forfait de livraison par commande, quel que soit le nombre
 * d'articles, offert à partir du seuil (inclus, comme l'annonce le panier).
 * Montant et seuil sont ceux de la boutique, réglés dans ses paramètres. Le
 * serveur applique la même règle et fait foi (DeliveryFeePolicy.java).
 */
export function deliveryFeeFor(
  subTotal: number,
  store: Pick<PublicStore, 'deliveryFee' | 'freeDeliveryFrom'> | null,
): number {
  if (!store || subTotal <= 0) {
    return 0;
  }
  const offered = store.freeDeliveryFrom !== null && subTotal >= store.freeDeliveryFrom;
  return offered ? 0 : store.deliveryFee;
}

const initialState: CartState = {
  items: [],
  storeSlug: null,
};

export const CartStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ items }, storeContext = inject(StoreContextService)) => {
    const subTotal = computed(() =>
      items().reduce((total, item) => total + item.product.price * item.quantity, 0),
    );
    const deliveryFee = computed(() => deliveryFeeFor(subTotal(), storeContext.store()));

    return {
      totalItems: computed(() => items().reduce((total, item) => total + item.quantity, 0)),
      subTotal,
      deliveryFee,
      /** Seuil de gratuité de la boutique, pour l'annoncer ; null, jamais offerte. */
      freeDeliveryFrom: computed(() => storeContext.store()?.freeDeliveryFrom ?? null),
      total: computed(() => subTotal() + deliveryFee()),
      isEmpty: computed(() => items().length === 0),
    };
  }),
  withMethods((store) => {
    const storage = inject(BROWSER_STORAGE);

    const persist = (items: CartItem[], storeSlug: string | null): void => {
      storage.writeJson('local', storageKey(storeSlug), items);
    };

    const load = (storeSlug: string | null): CartItem[] => {
      const parsed = storage.readJson<CartItem[]>('local', storageKey(storeSlug), []);
      if (!Array.isArray(parsed)) {
        return [];
      }
      return parsed
        .filter((item) => item?.product && item.quantity > 0)
        .map((item) => ({ ...item, size: item.size ?? null, color: item.color ?? null }));
    };

    return {
    /** Charge le panier de cette boutique. Appelé à chaque entrée sur une vitrine. */
    hydrate(storeSlug: string | null = null): void {
      patchState(store, { items: load(storeSlug), storeSlug });
    },

    /** Même produit, même déclinaison : la quantité s'additionne. Sinon, une nouvelle ligne. */
    addItem(product: PublicProduct, quantity = 1, variant: CartVariant = NO_VARIANT): void {
      patchState(store, (state) => {
        const added: CartItem = { product, quantity, size: variant.size, color: variant.color };
        const key = cartLineKey(added);
        const existing = state.items.some((item) => cartLineKey(item) === key);

        const nextItems = existing
          ? state.items.map((item) =>
              cartLineKey(item) === key ? { ...item, quantity: item.quantity + quantity } : item,
            )
          : [...state.items, added];
        persist(nextItems, state.storeSlug);
        return { items: nextItems };
      });
    },

    /** @param key la clé de ligne, donnée par cartLineKey. */
    removeItem(key: string): void {
      patchState(store, (state) => {
        const nextItems = state.items.filter((item) => cartLineKey(item) !== key);
        persist(nextItems, state.storeSlug);
        return { items: nextItems };
      });
    },

    /** @param key la clé de ligne, donnée par cartLineKey. */
    setQuantity(key: string, quantity: number): void {
      if (quantity <= 0) {
        this.removeItem(key);
        return;
      }

      patchState(store, (state) => {
        const nextItems = state.items.map((item) =>
          cartLineKey(item) === key ? { ...item, quantity } : item,
        );
        persist(nextItems, state.storeSlug);
        return { items: nextItems };
      });
    },

    clearCart(): void {
      persist([], store.storeSlug());
      patchState(store, { items: [] });
    },
    };
  }),
);
