import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { PublicCategory, PublicProduct } from '../models/public-product.model';
import { Category } from '../models/category.model';
import {
  CatalogSortField,
  PublicCatalogService,
} from '../services/public-catalog.service';
import { SortDirection } from './crud-list.helpers';

type CatalogState = {
  products: PublicProduct[];
  loading: boolean;
  error: string | null;
  searchTerm: string;
  selectedCategory: PublicCategory | 'Tous';
  selectedSubcategory: string;
  selectedSeason: string;
  availableCategories: Category[];
  sortBy: CatalogSortField;
  sortDirection: SortDirection;
  pageIndex: number;
  pageSize: number;
  totalElements: number;
  pageCount: number;
  lastPage: boolean;
};

export type CatalogQueryState = {
  q: string;
  category: PublicCategory | 'Tous';
  subcategory?: string;
  season?: string;
  sortBy: CatalogSortField;
  sortDirection: SortDirection;
  page: number;
};

const initialState: CatalogState = {
  products: [],
  loading: false,
  error: null,
  searchTerm: '',
  selectedCategory: 'Tous',
  selectedSubcategory: '',
  selectedSeason: '',
  availableCategories: [],
  sortBy: 'id',
  sortDirection: 'desc',
  pageIndex: 0,
  pageSize: 8,
  totalElements: 0,
  pageCount: 1,
  lastPage: true,
};

function filterByCategory(products: PublicProduct[], category: PublicCategory | 'Tous', subcategory = ''): PublicProduct[] {
  if (category === 'Tous') {
    return subcategory ? products.filter((product) => product.subcategory === subcategory) : products;
  }
  return products.filter((product) => product.category === category && (!subcategory || product.subcategory === subcategory));
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Erreur lors du chargement du catalogue';
}

export const PublicCatalogStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ products, loading, error, selectedCategory, selectedSubcategory, availableCategories, pageIndex, totalElements, pageCount, lastPage }) => ({
    productCount: computed(() => totalElements()),
    categories: computed(() => ['Tous', ...availableCategories().filter((category) => !category.parentId).map((category) => category.name)]),
    subcategories: computed(() => {
      const parent = availableCategories().find((category) => category.name === selectedCategory());
      return availableCategories()
        .filter((category) => category.parentId === parent?.id)
        .map((category) => category.name);
    }),
    filteredProducts: computed(() => filterByCategory(products(), selectedCategory(), selectedSubcategory())),
    pagedProducts: computed(() => filterByCategory(products(), selectedCategory(), selectedSubcategory())),
    totalFiltered: computed(() => {
      if (selectedCategory() === 'Tous') {
        return totalElements();
      }
      return filterByCategory(products(), selectedCategory(), selectedSubcategory()).length;
    }),
    totalPages: computed(() => {
      if (selectedCategory() === 'Tous') {
        return Math.max(pageCount(), 1);
      }
      return 1;
    }),
    currentPage: computed(() => pageIndex() + 1),
    isLastPage: computed(() => lastPage()),
    hasResults: computed(() => !loading() && !error() && totalElements() > 0),
  })),
  withMethods((store, catalogService = inject(PublicCatalogService)) => {
    const fetchPage = async (): Promise<void> => {
      patchState(store, { loading: true, error: null });
      try {
        const page = await catalogService.listProductsPage({
          query: store.searchTerm(),
          category: store.selectedCategory(),
          subcategory: store.selectedSubcategory(),
          season: store.selectedSeason(),
          page: store.pageIndex(),
          size: store.pageSize(),
          sortBy: store.sortBy(),
          sortDirection: store.sortDirection(),
        });

        patchState(store, {
          products: page.items,
          loading: false,
          pageIndex: page.page,
          pageSize: page.size,
          totalElements: page.totalElements,
          pageCount: Math.max(page.totalPages, 1),
          lastPage: page.last,
        });
      } catch (error) {
        patchState(store, { loading: false, error: errorMessage(error) });
      }
    };

    return {
      async applyQueryState(queryState: CatalogQueryState): Promise<void> {
        patchState(store, {
          searchTerm: queryState.q,
          selectedCategory: queryState.category,
          selectedSubcategory: queryState.subcategory ?? '',
          selectedSeason: queryState.season ?? '',
          sortBy: queryState.sortBy,
          sortDirection: queryState.sortDirection,
          pageIndex: Math.max(queryState.page, 0),
        });
        const categories = await catalogService.listCategories();
        patchState(store, { availableCategories: categories });
        await fetchPage();
      },

      async loadProducts(): Promise<void> {
        try {
          patchState(store, { availableCategories: await catalogService.listCategories() });
        } catch {
          patchState(store, { availableCategories: [] });
        }
        await fetchPage();
      },

      setSearchTerm(searchTerm: string): void {
        patchState(store, { searchTerm, pageIndex: 0 });
        void fetchPage();
      },

      setCategory(category: PublicCategory | 'Tous'): void {
        patchState(store, { selectedCategory: category, selectedSubcategory: '', pageIndex: 0 });
        void fetchPage();
      },

      setSubcategory(subcategory: string): void {
        patchState(store, { selectedSubcategory: subcategory, pageIndex: 0 });
        void fetchPage();
      },

      setSeason(season: string): void {
        patchState(store, { selectedSeason: season, pageIndex: 0 });
        void fetchPage();
      },

      setSort(sortBy: CatalogSortField): void {
        if (store.sortBy() === sortBy) {
          patchState(store, {
            sortDirection: store.sortDirection() === 'asc' ? 'desc' : 'asc',
            pageIndex: 0,
          });
          void fetchPage();
          return;
        }

        patchState(store, { sortBy, sortDirection: 'asc', pageIndex: 0 });
        void fetchPage();
      },

      setPage(pageIndex: number): void {
        const safePage = Math.max(0, Math.min(pageIndex, Math.max(store.pageCount() - 1, 0)));
        patchState(store, { pageIndex: safePage });
        void fetchPage();
      },

      resetFilters(): void {
        patchState(store, {
          searchTerm: '',
          selectedCategory: 'Tous',
          selectedSubcategory: '',
          selectedSeason: '',
          sortBy: 'id',
          sortDirection: 'desc',
          pageIndex: 0,
        });
        void fetchPage();
      },
    };
  }),
);
