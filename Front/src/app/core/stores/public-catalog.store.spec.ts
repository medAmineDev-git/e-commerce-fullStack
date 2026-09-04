import { TestBed } from '@angular/core/testing';
import { PublicCatalogService } from '../services/public-catalog.service';
import { PublicCatalogStore } from './public-catalog.store';

describe('PublicCatalogStore', () => {
  let store: any;
  let service: {
    listProductsPage: ReturnType<typeof vi.fn>;
    listCategories: ReturnType<typeof vi.fn>;
    getProductById: ReturnType<typeof vi.fn>;
  };

  const mockProducts = [
    {
      id: 1,
      slug: 'a',
      name: 'Veste',
      shortDescription: 'desc',
      longDescription: 'long',
      category: 'Homme',
      subcategory: 'T-Shirt',
      price: 70,
      rating: 4.8,
      reviewsCount: 20,
      stockQuantity: 10,
      imageUrl: 'img',
      gallery: ['img'],
    },
    {
      id: 2,
      slug: 'b',
      name: 'Sneaker',
      shortDescription: 'desc',
      longDescription: 'long',
      category: 'Sneakers',
      subcategory: '',
      price: 90,
      rating: 4.5,
      reviewsCount: 40,
      stockQuantity: 10,
      imageUrl: 'img',
      gallery: ['img'],
    },
  ];

  beforeEach(() => {
    service = {
      listCategories: vi.fn().mockResolvedValue([
        { id: 1, name: 'Homme', description: '', parentId: null },
        { id: 2, name: 'T-Shirt', description: '', parentId: 1 },
      ]),
      listProductsPage: vi.fn().mockImplementation((params?: { query?: string; category?: string; subcategory?: string }) => {
        const normalizedQuery = (params?.query ?? '').trim().toLowerCase();
        const filteredByQuery = normalizedQuery
          ? mockProducts.filter((product) => product.name.toLowerCase().includes(normalizedQuery))
          : mockProducts;

        const items =
          params?.category && params.category !== 'Tous'
            ? filteredByQuery.filter((product) => product.category === params.category)
            : filteredByQuery;

        const filteredItems = params?.subcategory
          ? items.filter((product) => product.subcategory === params.subcategory)
          : items;

        return Promise.resolve({
          items: filteredItems,
          page: 0,
          size: 8,
          totalElements: filteredItems.length,
          totalPages: 1,
          last: true,
        });
      }),
      getProductById: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: PublicCatalogService, useValue: service }],
    });

    store = TestBed.inject(PublicCatalogStore);
  });

  it('should load products', async () => {
    await store.loadProducts();

    expect(store.products().length).toBe(2);
    expect(store.totalElements()).toBe(2);
    expect(store.error()).toBeNull();
    expect(store.loading()).toBe(false);
  });

  it('should filter by category', async () => {
    await store.loadProducts();

    await Promise.resolve();
    // La recherche textuelle a ete retiree : seuls les filtres partent au serveur.
    expect(service.listProductsPage).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 0, category: 'Tous' }),
    );

    store.setCategory('Homme');
    await store.applyQueryState({
      category: 'Homme',
      sortBy: 'id',
      sortDirection: 'desc',
      page: 0,
    });
    await Promise.resolve();
    expect(store.totalFiltered()).toBe(1);
    expect(store.selectedCategory()).toBe('Homme');
  });

  it('should toggle sort direction and reset filters', async () => {
    await store.loadProducts();

    store.setSort('price');
    expect(store.sortBy()).toBe('price');
    expect(store.sortDirection()).toBe('asc');

    store.setSort('price');
    expect(store.sortDirection()).toBe('desc');

    store.setCategory('Sneakers');
    store.resetFilters();

    expect(store.selectedCategory()).toBe('Tous');
    expect(store.sortBy()).toBe('id');
    expect(store.sortDirection()).toBe('desc');
  });
});
