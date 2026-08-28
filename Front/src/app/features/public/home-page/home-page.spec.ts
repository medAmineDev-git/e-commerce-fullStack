import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { CartStore } from '../../../core/stores/cart.store';
import { PublicCatalogStore } from '../../../core/stores/public-catalog.store';
import { HomePage } from './home-page';

describe('HomePage', () => {
  let component: HomePage;
  let fixture: ComponentFixture<HomePage>;
  let router: Router;
  let navigateSpy: ReturnType<typeof vi.spyOn>;

  const mockCatalogStore = {
    products: signal([
      {
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
    ]),
    loading: signal(false),
    loadProducts: vi.fn().mockResolvedValue(undefined),
    setCategory: vi.fn(),
  };

  const mockCartStore = {
    addItem: vi.fn(),
  };

  beforeEach(async () => {
    registerLocaleData(localeFr);

    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [
        provideRouter([]),
        { provide: PublicCatalogStore, useValue: mockCatalogStore },
        { provide: CartStore, useValue: mockCartStore },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(HomePage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load products on init', () => {
    expect(mockCatalogStore.loadProducts).toHaveBeenCalled();
  });

  it('should open category and navigate to shop', () => {
    component.openCategory('Homme');

    expect(mockCatalogStore.setCategory).toHaveBeenCalledWith('Homme');
    expect(navigateSpy).toHaveBeenCalledWith(['/shop']);
  });

  it('should add product to cart', () => {
    component.addToCart(mockCatalogStore.products()[0]);
    expect(mockCartStore.addItem).toHaveBeenCalledWith(mockCatalogStore.products()[0], 1);
  });
});
