import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { PublicCategory, PublicProduct } from '../models/public-product.model';
import { Category } from '../models/category.model';
import {
  CatalogFacets,
  CatalogSortField,
  PublicCatalogService,
} from '../services/public-catalog.service';
import { SortDirection } from './crud-list.helpers';

type CatalogState = {
  products: PublicProduct[];
  loading: boolean;
  error: string | null;
  selectedCategory: PublicCategory | 'Tous';
  selectedSubcategory: string;
  selectedSeason: string;
  selectedSize: string;
  minPrice: number | null;
  maxPrice: number | null;
  availableCategories: Category[];
  facets: CatalogFacets | null;
  sortBy: CatalogSortField;
  sortDirection: SortDirection;
  pageIndex: number;
  pageSize: number;
  totalElements: number;
  pageCount: number;
  lastPage: boolean;
};

export type CatalogQueryState = {
  category: PublicCategory | 'Tous';
  subcategory?: string;
  season?: string;
  productSize?: string;
  minPrice?: number | null;
  maxPrice?: number | null;
  sortBy: CatalogSortField;
  sortDirection: SortDirection;
  page: number;
};

const initialState: CatalogState = {
  products: [],
  loading: false,
  error: null,
  selectedCategory: 'Tous',
  selectedSubcategory: '',
  selectedSeason: '',
  selectedSize: '',
  minPrice: null,
  maxPrice: null,
  availableCategories: [],
  facets: null,
  sortBy: 'id',
  sortDirection: 'desc',
  pageIndex: 0,
  pageSize: 12,
  totalElements: 0,
  pageCount: 1,
  lastPage: true,
};

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Erreur lors du chargement du catalogue';
}

export const PublicCatalogStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((state) => ({
    /*
     * Le serveur filtre, trie et pagine. Le store se contente de presenter ce
     * qu'il a recu : refiltrer ici par-dessus une page deja decoupee donnait des
     * comptes faux, et n'aurait jamais pu tenir avec les filtres prix et taille,
     * qui portent sur l'ensemble du catalogue et non sur la page affichee.
     */
    pagedProducts: computed(() => state.products()),
    totalFiltered: computed(() => state.totalElements()),
    totalPages: computed(() => Math.max(state.pageCount(), 1)),
    currentPage: computed(() => state.pageIndex() + 1),
    isLastPage: computed(() => state.lastPage()),
    hasResults: computed(() => !state.loading() && !state.error() && state.totalElements() > 0),

    categories: computed(() => {
      const fromFacets = state.facets()?.categories ?? [];
      if (fromFacets.length) {
        return ['Tous', ...fromFacets];
      }
      return [
        'Tous',
        ...state
          .availableCategories()
          .filter((category) => !category.parentId)
          .map((category) => category.name),
      ];
    }),

    subcategories: computed(() => {
      const parent = state.availableCategories().find((c) => c.name === state.selectedCategory());
      return state
        .availableCategories()
        .filter((category) => category.parentId === parent?.id)
        .map((category) => category.name);
    }),

    availableSizes: computed(() => state.facets()?.sizes ?? []),
    priceBounds: computed(() => ({
      min: state.facets()?.minPrice ?? null,
      max: state.facets()?.maxPrice ?? null,
    })),

    /** Nombre de filtres actifs, affiche sur le bouton Filtrer. */
    activeFilterCount: computed(() => {
      let count = 0;
      if (state.selectedSubcategory()) count++;
      if (state.selectedSeason()) count++;
      if (state.selectedSize()) count++;
      if (state.minPrice() !== null || state.maxPrice() !== null) count++;
      return count;
    }),
  })),
  withMethods((store, catalogService = inject(PublicCatalogService)) => {
    const fetchPage = async (): Promise<void> => {
      patchState(store, { loading: true, error: null });
      try {
        const page = await catalogService.listProductsPage({
          category: store.selectedCategory(),
          subcategory: store.selectedSubcategory(),
          season: store.selectedSeason(),
          productSize: store.selectedSize(),
          minPrice: store.minPrice(),
          maxPrice: store.maxPrice(),
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

    const loadReferenceData = async (): Promise<void> => {
      try {
        patchState(store, { facets: await catalogService.getFacets() });
      } catch {
        patchState(store, { facets: null });
      }
      try {
        patchState(store, { availableCategories: await catalogService.listCategories() });
      } catch {
        patchState(store, { availableCategories: [] });
      }
    };

    return {
      async applyQueryState(queryState: CatalogQueryState): Promise<void> {
        patchState(store, {
          selectedCategory: queryState.category,
          selectedSubcategory: queryState.subcategory ?? '',
          selectedSeason: queryState.season ?? '',
          selectedSize: queryState.productSize ?? '',
          minPrice: queryState.minPrice ?? null,
          maxPrice: queryState.maxPrice ?? null,
          sortBy: queryState.sortBy,
          sortDirection: queryState.sortDirection,
          pageIndex: Math.max(queryState.page, 0),
        });
        await loadReferenceData();
        await fetchPage();
      },

      async loadProducts(): Promise<void> {
        await loadReferenceData();
        await fetchPage();
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

      setSize(productSize: string): void {
        // Un second clic sur la meme taille la retire : c'est le comportement
        // attendu d'une pastille de filtre.
        patchState(store, {
          selectedSize: store.selectedSize() === productSize ? '' : productSize,
          pageIndex: 0,
        });
        void fetchPage();
      },

      setPriceRange(minPrice: number | null, maxPrice: number | null): void {
        patchState(store, { minPrice, maxPrice, pageIndex: 0 });
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
          selectedCategory: 'Tous',
          selectedSubcategory: '',
          selectedSeason: '',
          selectedSize: '',
          minPrice: null,
          maxPrice: null,
          sortBy: 'id',
          sortDirection: 'desc',
          pageIndex: 0,
        });
        void fetchPage();
      },
    };
  }),
);
