import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { Category, CategoryInput } from '../models/category.model';
import { CategoryService } from '../services/category';
import { clampPageIndex, filterBySearch, paginateItems, sortItems, SortDirection } from './crud-list.helpers';

type CategorySortField = 'id' | 'name';

type CategoryState = {
  categories: Category[];
  selectedCategory: Category | null;
  loading: boolean;
  saving: boolean;
  error: string | null;
  searchTerm: string;
  sortBy: CategorySortField;
  sortDirection: SortDirection;
  pageIndex: number;
  pageSize: number;
};

const initialState: CategoryState = {
  categories: [],
  selectedCategory: null,
  loading: false,
  saving: false,
  error: null,
  searchTerm: '',
  sortBy: 'id',
  sortDirection: 'desc',
  pageIndex: 0,
  pageSize: 10,
};

function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return 'Une erreur est survenue';
}

function categoryFilterSort(
  categories: Category[],
  searchTerm: string,
  sortBy: CategorySortField,
  sortDirection: SortDirection,
): Category[] {
  const searched = filterBySearch(categories, searchTerm, (category) => {
    return `${category.id} ${category.name} ${category.description ?? ''}`;
  });
  return sortItems(searched, { sortBy, sortDirection }, (category, field) => category[field]);
}

export const CategoryStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ categories, searchTerm, sortBy, sortDirection, pageIndex, pageSize }) => ({
    filteredCategories: computed(() =>
      categoryFilterSort(categories(), searchTerm(), sortBy(), sortDirection()),
    ),
    pagedCategories: computed(() =>
      paginateItems(categoryFilterSort(categories(), searchTerm(), sortBy(), sortDirection()), {
        pageIndex: pageIndex(),
        pageSize: pageSize(),
      }),
    ),
    totalFiltered: computed(
      () => categoryFilterSort(categories(), searchTerm(), sortBy(), sortDirection()).length,
    ),
    totalPages: computed(() => {
      const total = categoryFilterSort(categories(), searchTerm(), sortBy(), sortDirection()).length;
      return Math.max(Math.ceil(total / pageSize()), 1);
    }),
    currentPage: computed(() => pageIndex() + 1),
  })),
  withMethods((store, categoryService = inject(CategoryService)) => ({
    async loadCategories(): Promise<void> {
      patchState(store, { loading: true, error: null });
      try {
        const categories = await firstValueFrom(categoryService.getAll());
        patchState(store, {
          categories,
          pageIndex: clampPageIndex(categories.length, store.pageSize(), store.pageIndex()),
          loading: false,
        });
      } catch (error) {
        patchState(store, { loading: false, error: getErrorMessage(error) });
      }
    },

    async loadCategory(id: number): Promise<Category | null> {
      patchState(store, { loading: true, error: null });
      try {
        const category = await firstValueFrom(categoryService.getById(id));
        patchState(store, { selectedCategory: category, loading: false });
        return category;
      } catch (error) {
        patchState(store, { selectedCategory: null, loading: false, error: getErrorMessage(error) });
        return null;
      }
    },

    async createCategory(input: CategoryInput): Promise<Category | null> {
      patchState(store, { saving: true, error: null });
      try {
        const created = await firstValueFrom(categoryService.create(input));
        patchState(store, (state) => ({
          saving: false,
          categories: [...state.categories, created],
          selectedCategory: created,
          pageIndex: clampPageIndex(state.categories.length + 1, state.pageSize, state.pageIndex),
        }));
        return created;
      } catch (error) {
        patchState(store, { saving: false, error: getErrorMessage(error) });
        return null;
      }
    },

    async updateCategory(id: number, input: CategoryInput): Promise<Category | null> {
      patchState(store, { saving: true, error: null });
      try {
        const updated = await firstValueFrom(categoryService.update(id, input));
        patchState(store, (state) => ({
          saving: false,
          categories: state.categories.map((category) => (category.id === id ? updated : category)),
          selectedCategory: updated,
        }));
        return updated;
      } catch (error) {
        patchState(store, { saving: false, error: getErrorMessage(error) });
        return null;
      }
    },

    async deleteCategory(id: number): Promise<boolean> {
      patchState(store, { saving: true, error: null });
      try {
        await firstValueFrom(categoryService.delete(id));
        patchState(store, (state) => ({
          saving: false,
          categories: state.categories.filter((category) => category.id !== id),
          pageIndex: clampPageIndex(state.categories.length - 1, state.pageSize, state.pageIndex),
        }));
        return true;
      } catch (error) {
        patchState(store, { saving: false, error: getErrorMessage(error) });
        return false;
      }
    },

    setSearchTerm(searchTerm: string): void {
      patchState(store, { searchTerm, pageIndex: 0 });
    },

    setSort(sortBy: CategorySortField): void {
      if (store.sortBy() === sortBy) {
        patchState(store, {
          sortDirection: store.sortDirection() === 'asc' ? 'desc' : 'asc',
          pageIndex: 0,
        });
        return;
      }
      patchState(store, { sortBy, sortDirection: 'asc', pageIndex: 0 });
    },

    setPage(pageIndex: number): void {
      const total = categoryFilterSort(
        store.categories(),
        store.searchTerm(),
        store.sortBy(),
        store.sortDirection(),
      ).length;
      patchState(store, { pageIndex: clampPageIndex(total, store.pageSize(), pageIndex) });
    },

    setPageSize(pageSize: number): void {
      const total = categoryFilterSort(
        store.categories(),
        store.searchTerm(),
        store.sortBy(),
        store.sortDirection(),
      ).length;
      patchState(store, {
        pageSize,
        pageIndex: clampPageIndex(total, pageSize, 0),
      });
    },
  })),
);
