import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProductStore } from '../../../core/stores/product.store';

import { ProductList } from './product-list';

describe('ProductList', () => {
  let component: ProductList;
  let fixture: ComponentFixture<ProductList>;
  const mockSnackBarOpen = vi.fn();
  let mockStore: {
    loadProducts: ReturnType<typeof vi.fn>;
    deleteProduct: ReturnType<typeof vi.fn>;
    products: ReturnType<typeof signal<any[]>>;
    pagedProducts: ReturnType<typeof signal<any[]>>;
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    searchTerm: ReturnType<typeof signal<string>>;
    sortBy: ReturnType<typeof signal<'id' | 'name' | 'price' | 'stockQuantity'>>;
    sortDirection: ReturnType<typeof signal<'asc' | 'desc'>>;
    totalFiltered: ReturnType<typeof signal<number>>;
    totalPages: ReturnType<typeof signal<number>>;
    currentPage: ReturnType<typeof signal<number>>;
    pageIndex: ReturnType<typeof signal<number>>;
    pageSize: ReturnType<typeof signal<number>>;
    setSearchTerm: ReturnType<typeof vi.fn>;
    setSort: ReturnType<typeof vi.fn>;
    setPage: ReturnType<typeof vi.fn>;
    setPageSize: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    mockStore = {
      loadProducts: vi.fn().mockResolvedValue(undefined),
      deleteProduct: vi.fn().mockResolvedValue(true),
      products: signal([]),
      pagedProducts: signal([]),
      loading: signal(false),
      error: signal(null),
      searchTerm: signal(''),
      sortBy: signal('id'),
      sortDirection: signal('desc'),
      totalFiltered: signal(0),
      totalPages: signal(1),
      currentPage: signal(1),
      pageIndex: signal(0),
      pageSize: signal(10),
      setSearchTerm: vi.fn(),
      setSort: vi.fn(),
      setPage: vi.fn(),
      setPageSize: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [
        provideRouter([]),
        { provide: ProductStore, useValue: mockStore },
        { provide: MatSnackBar, useValue: { open: mockSnackBarOpen } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load products on init', () => {
    expect(mockStore.loadProducts).toHaveBeenCalled();
  });

  it('should delete product when confirmed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await component.deleteProduct({ id: 1, name: 'T-shirt', category: 'Homme', description: '', price: 10, stockQuantity: 5 });
    expect(mockStore.deleteProduct).toHaveBeenCalledWith(1);
    expect(mockSnackBarOpen).toHaveBeenCalledWith('Produit supprimé', 'Fermer', { duration: 2000 });
  });

  it('should not delete product when cancel confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    await component.deleteProduct({ id: 1, name: 'T-shirt', category: 'Homme', description: '', price: 10, stockQuantity: 5 });
    expect(mockStore.deleteProduct).not.toHaveBeenCalled();
  });
});
