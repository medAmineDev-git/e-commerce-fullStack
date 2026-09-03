import { TestBed } from '@angular/core/testing';
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
    store.addItem(product, 2);

    expect(store.totalItems()).toBe(2);
    expect(store.subTotal()).toBe(100);
    expect(store.deliveryFee()).toBe(6.9);
    expect(store.total()).toBe(106.9);
  });

  it('should update quantity and remove item', () => {
    store.addItem(product, 1);
    store.setQuantity(product.id, 3);

    expect(store.totalItems()).toBe(3);

    store.removeItem(product.id);
    expect(store.isEmpty()).toBe(true);
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
