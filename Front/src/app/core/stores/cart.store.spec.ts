import { TestBed } from '@angular/core/testing';
import { cartLineKey } from '../models/order.model';
import { CartStore } from './cart.store';

describe('CartStore', () => {
  let store: any;

  const product = {
    id: 1,
    slug: 'veste',
    name: 'Veste Denim',
    shortDescription: 'desc',
    longDescription: 'long',
    category: 'Homme',
    price: 50,
    rating: 4.5,
    reviewsCount: 12,
    stockQuantity: 10,
    imageUrl: 'img',
    gallery: ['img'],
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    store = TestBed.inject(CartStore);
    store.hydrate('nova');
    store.clearCart();
  });

  it('should add items and compute totals', () => {
    store.addItem(product, 1);

    expect(store.totalItems()).toBe(1);
    expect(store.subTotal()).toBe(50);
    expect(store.deliveryFee()).toBe(6.9);
    expect(store.total()).toBe(56.9);
  });

  it('charges delivery once for the whole cart, not per product', () => {
    store.addItem(product, 1);
    store.addItem({ ...product, id: 2, price: 20 }, 1);
    store.addItem({ ...product, id: 3, price: 10 }, 1);

    expect(store.subTotal()).toBe(80);
    expect(store.deliveryFee()).toBe(6.9);
    expect(store.total()).toBeCloseTo(86.9);
  });

  // Le panier annonce « Livraison offerte à partir de 100 TND » : 100 pile y a droit.
  it('offers delivery from the threshold, included', () => {
    store.addItem(product, 2);

    expect(store.subTotal()).toBe(100);
    expect(store.deliveryFee()).toBe(0);
    expect(store.total()).toBe(100);
  });

  it('should update quantity and remove item', () => {
    store.addItem(product, 1);
    const key = cartLineKey(store.items()[0]);
    store.setQuantity(key, 3);

    expect(store.totalItems()).toBe(3);

    store.removeItem(key);
    expect(store.isEmpty()).toBe(true);
  });

  /** Le vendeur prépare un 3 mois et un 12 mois : ce sont deux lignes, pas deux unités d'une seule. */
  it('keeps one line per size and color, and merges the same choice', () => {
    store.addItem(product, 1, { size: '3 mois', color: 'Bleu nuit' });
    store.addItem(product, 1, { size: '12 mois', color: 'Bleu nuit' });
    store.addItem(product, 2, { size: '3 mois', color: 'Bleu nuit' });

    expect(store.items()).toHaveLength(2);
    expect(store.items()[0]).toEqual(expect.objectContaining({ size: '3 mois', color: 'Bleu nuit', quantity: 3 }));
    expect(store.items()[1]).toEqual(expect.objectContaining({ size: '12 mois', quantity: 1 }));

    store.removeItem(cartLineKey(store.items()[0]));
    expect(store.items()).toHaveLength(1);
    expect(store.items()[0].size).toBe('12 mois');
  });

  it('should hydrate cart from local storage', () => {
    store.addItem(product, 1);

    const anotherStore = TestBed.inject(CartStore);
    anotherStore.hydrate('nova');

    expect(anotherStore.totalItems()).toBeGreaterThan(0);
  });

  /**
   * Le point du cloisonnement : deux vitrines ouvertes dans le même navigateur
   * ne doivent pas partager leur panier.
   */
  it('should keep carts separate between stores', () => {
    store.addItem(product, 2);
    expect(store.totalItems()).toBe(2);

    store.hydrate('atelier');
    expect(store.isEmpty()).toBe(true);

    store.hydrate('nova');
    expect(store.totalItems()).toBe(2);
  });
});
