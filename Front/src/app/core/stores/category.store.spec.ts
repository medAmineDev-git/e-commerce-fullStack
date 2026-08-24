import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CategoryService } from '../services/category';
import { CategoryStore } from './category.store';

describe('CategoryStore', () => {
  let store: any;
  let categoryService: {
    getAll: ReturnType<typeof vi.fn>;
    getById: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  const categories = [
    { id: 1, name: 'Homme', description: 'Collection homme' },
    { id: 2, name: 'Femme', description: 'Collection femme' },
  ];

  beforeEach(() => {
    categoryService = {
      getAll: vi.fn(),
      getById: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
      delete: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: CategoryService, useValue: categoryService }],
    });

    store = TestBed.inject(CategoryStore);
  });

  it('should load categories successfully', async () => {
    categoryService.getAll.mockReturnValue(of(categories));

    await store.loadCategories();

    expect(store.categories()).toEqual(categories);
    expect(store.loading()).toBe(false);
    expect(store.error()).toBeNull();
  });

  it('should set error when load fails', async () => {
    categoryService.getAll.mockReturnValue(throwError(() => new Error('load failed')));

    await store.loadCategories();

    expect(store.categories()).toEqual([]);
    expect(store.error()).toBe('load failed');
  });

  it('should create category and set selected category', async () => {
    const input = { name: 'Accessoires', description: 'Collection accessoires' };
    const created = { id: 3, ...input };
    categoryService.create.mockReturnValue(of(created));

    const result = await store.createCategory(input);

    expect(result).toEqual(created);
    expect(store.categories()).toContain(created);
    expect(store.selectedCategory()).toEqual(created);
  });

  it('should update category', async () => {
    categoryService.getAll.mockReturnValue(of(categories));
    await store.loadCategories();

    const input = { name: 'Homme Premium', description: 'Nouvelles pieces' };
    const updated = { id: 1, ...input };
    categoryService.update.mockReturnValue(of(updated));

    const result = await store.updateCategory(1, input);

    expect(result).toEqual(updated);
    expect(store.categories().find((c: any) => c.id === 1)).toEqual(updated);
  });

  it('should delete category', async () => {
    categoryService.getAll.mockReturnValue(of(categories));
    await store.loadCategories();
    categoryService.delete.mockReturnValue(of(void 0));

    const result = await store.deleteCategory(2);

    expect(result).toBe(true);
    expect(store.categories().some((c: any) => c.id === 2)).toBe(false);
  });

  it('should apply search and sort query state', async () => {
    categoryService.getAll.mockReturnValue(of(categories));
    await store.loadCategories();

    store.setSearchTerm('fem');
    expect(store.totalFiltered()).toBe(1);

    store.setSort('name');
    expect(store.sortBy()).toBe('name');
    expect(store.sortDirection()).toBe('asc');

    store.setSort('name');
    expect(store.sortDirection()).toBe('desc');
  });
});
