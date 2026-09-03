import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { CartItem } from '../models/order.model';
import { PublicProduct } from '../models/public-product.model';
import { BROWSER_STORAGE } from '../platform/browser-storage';

type CartState = {
  items: CartItem[];
  storeSlug: string | null;
};

/**
 * Une clé par boutique. Sans cela, ouvrir deux vitrines dans le même navigateur
 * mélange les paniers, et l'utilisateur commande sur une boutique des articles
 * ajoutés sur une autre — que le serveur rejettera en 404, sans explication utile.
 */
const STORAGE_KEY_PREFIX = 'ecommerce_cart_v2';

function storageKey(storeSlug: string | null): string {
  return storeSlug ? `${STORAGE_KEY_PREFIX}.${storeSlug}` : STORAGE_KEY_PREFIX;
}

const initialState: CartState = {
  items: [],
  storeSlug: null,
};

export const CartStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ items }) => ({
    totalItems: computed(() => items().reduce((total, item) => total + item.quantity, 0)),
    subTotal: computed(() =>
      items().reduce((total, item) => total + item.product.price * item.quantity, 0),
    ),
    deliveryFee: computed(() => {
      const subTotal = items().reduce((total, item) => total + item.product.price * item.quantity, 0);
      if (subTotal === 0 || subTotal > 100) {
        return 0;
      }
      return 6.9;
    }),
    total: computed(() => {
      const subTotal = items().reduce((total, item) => total + item.product.price * item.quantity, 0);
      const deliveryFee = subTotal === 0 || subTotal > 100 ? 0 : 6.9;
      return subTotal + deliveryFee;
    }),
    isEmpty: computed(() => items().length === 0),
  })),
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
      return parsed.filter((item) => item?.product && item.quantity > 0);
    };

    return {
    /** Charge le panier de cette boutique. Appelé à chaque entrée sur une vitrine. */
    hydrate(storeSlug: string | null = null): void {
      patchState(store, { items: load(storeSlug), storeSlug });
    },

    addItem(product: PublicProduct, quantity = 1): void {
      patchState(store, (state) => {
        const existing = state.items.find((item) => item.product.id === product.id);

        if (existing) {
          const nextItems = state.items.map((item) => {
            if (item.product.id !== product.id) {
              return item;
            }
            return { ...item, quantity: item.quantity + quantity };
          });
          persist(nextItems, state.storeSlug);
          return { items: nextItems };
        }

        const nextItems = [...state.items, { product, quantity }];
        persist(nextItems, state.storeSlug);
        return { items: nextItems };
      });
    },

    removeItem(productId: number): void {
      patchState(store, (state) => {
        const nextItems = state.items.filter((item) => item.product.id !== productId);
        persist(nextItems, state.storeSlug);
        return { items: nextItems };
      });
    },

    setQuantity(productId: number, quantity: number): void {
      if (quantity <= 0) {
        this.removeItem(productId);
        return;
      }

      patchState(store, (state) => {
        const nextItems = state.items.map((item) => {
          if (item.product.id !== productId) {
            return item;
          }
          return { ...item, quantity };
        });
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
