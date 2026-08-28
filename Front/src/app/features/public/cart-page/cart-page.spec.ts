import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { CartStore } from '../../../core/stores/cart.store';
import { CartPage } from './cart-page';

describe('CartPage', () => {
  let component: CartPage;
  let fixture: ComponentFixture<CartPage>;
  let router: Router;
  let navigateSpy: ReturnType<typeof vi.spyOn>;

  const mockItem = {
    product: {
      id: 1,
      slug: 'veste',
      name: 'Veste',
      shortDescription: 'desc',
      longDescription: 'long',
      category: 'Homme' as const,
      price: 70,
      rating: 4.8,
      reviewsCount: 20,
      stockQuantity: 10,
      imageUrl: 'img',
      gallery: ['img'],
    },
    quantity: 1,
  };

  const mockStore = {
    items: signal([mockItem]),
    totalItems: signal(1),
    subTotal: signal(70),
    deliveryFee: signal(6.9),
    total: signal(76.9),
    isEmpty: signal(false),
    setQuantity: vi.fn(),
    removeItem: vi.fn(),
  };

  beforeEach(async () => {
    registerLocaleData(localeFr);

    await TestBed.configureTestingModule({
      imports: [CartPage],
      providers: [
        provideRouter([]),
        { provide: CartStore, useValue: mockStore },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(CartPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should update item quantity', () => {
    component.setQuantity(mockItem, '3');
    expect(mockStore.setQuantity).toHaveBeenCalledWith(1, 3);
  });

  it('should remove item', () => {
    component.remove(mockItem);
    expect(mockStore.removeItem).toHaveBeenCalledWith(1);
  });

  it('should navigate to checkout', () => {
    component.proceedToCheckout();
    expect(navigateSpy).toHaveBeenCalledWith(['/checkout']);
  });
});
