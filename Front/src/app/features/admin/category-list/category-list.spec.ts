import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CategoryStore } from '../../../core/stores/category.store';

import { CategoryList } from './category-list';

describe('CategoryList', () => {
  let component: CategoryList;
  let fixture: ComponentFixture<CategoryList>;
  const mockSnackBarOpen = vi.fn();
  let mockStore: {
    loadCategories: ReturnType<typeof vi.fn>;
    deleteCategory: ReturnType<typeof vi.fn>;
    pagedCategories: ReturnType<typeof signal<any[]>>;
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    searchTerm: ReturnType<typeof signal<string>>;
    sortBy: ReturnType<typeof signal<'id' | 'name'>>;
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
      loadCategories: vi.fn().mockResolvedValue(undefined),
      deleteCategory: vi.fn().mockResolvedValue(true),
      pagedCategories: signal([]),
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
      imports: [CategoryList],
      providers: [
        provideRouter([]),
        { provide: CategoryStore, useValue: mockStore },
        { provide: MatSnackBar, useValue: { open: mockSnackBarOpen } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load categories on init', () => {
    expect(mockStore.loadCategories).toHaveBeenCalled();
  });

  it('should delete category when confirmed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await component.deleteCategory({ id: 1, name: 'Homme', description: '' });
    expect(mockStore.deleteCategory).toHaveBeenCalledWith(1);
    expect(mockSnackBarOpen).toHaveBeenCalledWith('Catégorie supprimée', 'Fermer', { duration: 2000 });
  });

  it('should not delete category when cancel confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    await component.deleteCategory({ id: 1, name: 'Homme', description: '' });
    expect(mockStore.deleteCategory).not.toHaveBeenCalled();
  });
});
