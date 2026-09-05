import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { StoreContextService } from '../../../core/services/store-context.service';
import { PublicCatalogService } from '../../../core/services/public-catalog.service';
import { PublicCatalogStore } from '../../../core/stores/public-catalog.store';
import { HomePage } from './home-page';

describe('HomePage', () => {
  let component: HomePage;
  let fixture: ComponentFixture<HomePage>;
  let router: Router;
  let navigateSpy: ReturnType<typeof vi.spyOn>;

  const product = {
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
  };

  const mockCatalogStore = {
    products: signal([product]),
    pagedProducts: signal([product]),
    loading: signal(false),
    error: signal(null),
    totalFiltered: signal(1),
    totalPages: signal(1),
    currentPage: signal(1),
    pageIndex: signal(0),
    categories: signal(['Tous', 'Homme']),
    subcategories: signal([]),
    availableSizes: signal(['S', 'M']),
    availableColors: signal(['Noir']),
    priceBounds: signal({ min: 10, max: 200 }),
    activeFilterCount: signal(0),
    selectedCategory: signal('Tous'),
    selectedSubcategory: signal(''),
    selectedSeason: signal(''),
    selectedSize: signal(''),
    selectedColor: signal(''),
    minPrice: signal(null),
    maxPrice: signal(null),
    sortBy: signal('id'),
    sortDirection: signal('desc'),
    applyQueryState: vi.fn().mockResolvedValue(undefined),
    loadProducts: vi.fn().mockResolvedValue(undefined),
    setCategory: vi.fn(),
    setSubcategory: vi.fn(),
    setSeason: vi.fn(),
    setSize: vi.fn(),
    setColor: vi.fn(),
    setPriceRange: vi.fn(),
    setSort: vi.fn(),
    setPage: vi.fn(),
    resetFilters: vi.fn(),
  };

  const mockCatalogService = {
    getHomeConfiguration: vi.fn().mockResolvedValue({
      title: 'Titre',
      text: 'Texte',
      welcomeEnabled: true,
    }),
  };

  beforeEach(async () => {
    registerLocaleData(localeFr);
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [
        provideRouter([]),
        // La vitrine est toujours rendue dans le contexte d'une boutique resolue.
        {
          provide: StoreContextService,
          useValue: {
            slug: () => 'nova',
            store: () => ({
              id: 1,
              name: 'NOVA',
              slug: 'nova',
              description: null,
              logoUrl: null,
              bannerUrl: null,
              phone: null,
              email: null,
              address: null,
              domain: null,
            }),
            link: (...segments: (string | number)[]) => ['/boutique', 'nova', ...segments],
          },
        },
        { provide: PublicCatalogStore, useValue: mockCatalogStore },
        { provide: PublicCatalogService, useValue: mockCatalogService },
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

  it('should show the welcome text by default', () => {
    expect(component.showWelcome()).toBe(true);
  });

  /**
   * Le vendeur peut retirer ce bloc sans effacer son texte : la configuration
   * garde le titre et l'accroche, la vitrine ne les rend plus.
   */
  it('should hide the welcome text when the seller turned it off', async () => {
    mockCatalogService.getHomeConfiguration.mockResolvedValue({
      title: 'Titre',
      text: 'Texte',
      welcomeEnabled: false,
    });

    const fresh = TestBed.createComponent(HomePage);
    await fresh.whenStable();

    expect(fresh.componentInstance.showWelcome()).toBe(false);
    expect(fresh.componentInstance.heroTitle()).toBe('Titre');
  });

  /** L'accueil porte desormais le catalogue : il applique l'etat de l'URL. */
  it('should apply the query state from the url on init', () => {
    expect(mockCatalogStore.applyQueryState).toHaveBeenCalled();
  });

  /**
   * Filtrer par categorie ne quitte plus la page : la boutique tient sur
   * l'accueil depuis la suppression de la page /shop.
   */
  it('should filter in place when a category is picked', () => {
    component.setCategory('Homme');

    expect(mockCatalogStore.setCategory).toHaveBeenCalledWith('Homme');
    expect(navigateSpy).toHaveBeenCalledWith(
      [],
      expect.objectContaining({
        queryParams: expect.objectContaining({ category: 'Homme' }),
      }),
    );
  });

  /** Les bornes inversees sont remises dans l'ordre plutot qu'ignorees. */
  it('should reorder an inverted price range', () => {
    component.draftMinPrice.set(200);
    component.draftMaxPrice.set(50);

    component.applyPriceRange();

    expect(mockCatalogStore.setPriceRange).toHaveBeenCalledWith(50, 200);
  });

  /**
   * Depuis l'accueil on ouvre la fiche produit, on n'ajoute pas au panier :
   * un vetement se choisit avec sa taille et sa couleur.
   */
  it('should open the product sheet', () => {
    component.openProduct(product);
    expect(navigateSpy).toHaveBeenCalledWith(['/boutique', 'nova', 'product', 1]);
  });
});
