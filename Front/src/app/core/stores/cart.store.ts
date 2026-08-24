import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { CartItem } from '../models/order.model';
import { PublicProduct } from '../models/public-product.model';

type CartState = {
  items: CartItem[];
};

const STORAGE_KEY = 'ecommerce_cart_v1';

const initialState: CartState = {
  items: [],
};

function persist(items: CartItem[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // Ignore storage write errors in test or private browsing context.
  }
}

function loadFromStorage(): CartItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return [];
    }

    const parsed = JSON.parse(raw) as CartItem[];
    if (!Array.isArray(parsed)) {
      return [];
    }

    return parsed.filter((item) => item.product && item.quantity > 0);
  } catch {
    return [];
  }
}

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
  withMethods((store) => ({
    hydrate(): void {
      const stored = loadFromStorage();
      patchState(store, { items: stored });
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
          persist(nextItems);
          return { items: nextItems };
        }

        const nextItems = [...state.items, { product, quantity }];
        persist(nextItems);
        return { items: nextItems };
      });
    },

    removeItem(productId: number): void {
      patchState(store, (state) => {
        const nextItems = state.items.filter((item) => item.product.id !== productId);
        persist(nextItems);
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
        persist(nextItems);
        return { items: nextItems };
      });
    },

    clearCart(): void {
      patchState(store, { items: [] });
      persist([]);
    },
  })),
);
