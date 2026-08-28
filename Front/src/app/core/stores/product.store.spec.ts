import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ProductService } from '../services/product';
import { ProductStore } from './product.store';

describe('ProductStore', () => {
  let store: any;
  let productService: {
    getAll: ReturnType<typeof vi.fn>;
    getById: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  const products = [
    { id: 1, name: 'T-shirt', category: 'Homme', description: 'Coton', price: 19.99, stockQuantity: 10 },
    { id: 2, name: 'Pull', category: 'Homme', description: 'Laine', price: 39.99, stockQuantity: 5 },
  ];

  beforeEach(() => {
    productService = {
      getAll: vi.fn(),
      getById: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
      delete: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: ProductService, useValue: productService }],
    });

    store = TestBed.inject(ProductStore);
  });

  it('should load products successfully', async () => {
    productService.getAll.mockReturnValue(of(products));

    await store.loadProducts();

    expect(store.products()).toEqual(products);
    expect(store.loading()).toBe(false);
    expect(store.error()).toBeNull();
  });

  it('should set error when load fails', async () => {
    productService.getAll.mockReturnValue(throwError(() => new Error('load failed')));

    await store.loadProducts();

    expect(store.products()).toEqual([]);
    expect(store.loading()).toBe(false);
    expect(store.error()).toBe('load failed');
  });

  it('should create product and update selection', async () => {
    const input = { name: 'Jean', category: 'Homme', description: 'Slim', price: 49.99, stockQuantity: 8 };
    const created = { id: 3, ...input };
    productService.create.mockReturnValue(of(created));

    const result = await store.createProduct(input);

    expect(result).toEqual(created);
    expect(store.products()).toContain(created);
    expect(store.selectedProduct()).toEqual(created);
    expect(store.saving()).toBe(false);
  });

  it('should update existing product', async () => {
    productService.getAll.mockReturnValue(of(products));
    await store.loadProducts();

    const input = { name: 'Pull Premium', category: 'Homme', description: 'Laine', price: 49.99, stockQuantity: 4 };
    const updated = { id: 2, ...input };
    productService.update.mockReturnValue(of(updated));

    const result = await store.updateProduct(2, input);

    expect(result).toEqual(updated);
    expect(store.products().find((p: any) => p.id === 2)).toEqual(updated);
  });

  it('should delete product', async () => {
    productService.getAll.mockReturnValue(of(products));
    await store.loadProducts();
    productService.delete.mockReturnValue(of(void 0));

    const result = await store.deleteProduct(1);

    expect(result).toBe(true);
    expect(store.products().some((p: any) => p.id === 1)).toBe(false);
  });

  it('should update query state and pagination', async () => {
    productService.getAll.mockReturnValue(of(products));
    await store.loadProducts();

    store.setSearchTerm('pull');
    expect(store.totalFiltered()).toBe(1);

    store.setSort('price');
    expect(store.sortBy()).toBe('price');
    expect(store.sortDirection()).toBe('asc');

    store.setSort('price');
    expect(store.sortDirection()).toBe('desc');

    store.setSearchTerm('');
    store.setPageSize(1);
    store.setPage(10);
    expect(store.pageIndex()).toBe(1);
  });
});
