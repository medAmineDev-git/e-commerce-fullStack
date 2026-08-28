import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { Product, ProductInput } from '../models/product.model';
import { ProductService } from '../services/product';
import {
  clampPageIndex,
  filterBySearch,
  paginateItems,
  sortItems,
  SortDirection,
} from './crud-list.helpers';

type ProductSortField = 'id' | 'name' | 'price' | 'stockQuantity';

type ProductState = {
  products: Product[];
  selectedProduct: Product | null;
  loading: boolean;
  saving: boolean;
  error: string | null;
  searchTerm: string;
  sortBy: ProductSortField;
  sortDirection: SortDirection;
  pageIndex: number;
  pageSize: number;
};

const initialState: ProductState = {
  products: [],
  selectedProduct: null,
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

export const ProductStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ products, searchTerm, sortBy, sortDirection, pageIndex, pageSize }) => ({
    filteredProducts: computed(() => {
      const searched = filterBySearch(products(), searchTerm(), (product) => {
        return `${product.id} ${product.name} ${product.category} ${product.description} ${product.price} ${product.stockQuantity}`;
      });
      return sortItems(
        searched,
        { sortBy: sortBy(), sortDirection: sortDirection() },
        (product, field) => product[field],
      );
    }),
    pagedProducts: computed(() => {
      return paginateItems(productsFilterSort(products(), searchTerm(), sortBy(), sortDirection()), {
        pageIndex: pageIndex(),
        pageSize: pageSize(),
      });
    }),
    hasProducts: computed(() => products().length > 0),
    totalFiltered: computed(() => productsFilterSort(products(), searchTerm(), sortBy(), sortDirection()).length),
    totalPages: computed(() => Math.max(Math.ceil(productsFilterSort(products(), searchTerm(), sortBy(), sortDirection()).length / pageSize()), 1)),
    currentPage: computed(() => pageIndex() + 1),
  })),
  withMethods((store, productService = inject(ProductService)) => ({
    async loadProducts(): Promise<void> {
      patchState(store, { loading: true, error: null });
      try {
        const products = await firstValueFrom(productService.getAll());
        patchState(store, {
          products,
          pageIndex: clampPageIndex(products.length, store.pageSize(), store.pageIndex()),
          loading: false,
        });
      } catch (error) {
        patchState(store, { loading: false, error: getErrorMessage(error) });
      }
    },

    async loadProduct(id: number): Promise<Product | null> {
      patchState(store, { loading: true, error: null });
      try {
        const product = await firstValueFrom(productService.getById(id));
        patchState(store, { selectedProduct: product, loading: false });
        return product;
      } catch (error) {
        patchState(store, { selectedProduct: null, loading: false, error: getErrorMessage(error) });
        return null;
      }
    },

    async createProduct(input: ProductInput): Promise<Product | null> {
      patchState(store, { saving: true, error: null });
      try {
        const created = await firstValueFrom(productService.create(input));
        patchState(store, (state) => ({
          saving: false,
          products: [...state.products, created],
          selectedProduct: created,
          pageIndex: clampPageIndex(state.products.length + 1, state.pageSize, state.pageIndex),
        }));
        return created;
      } catch (error) {
        patchState(store, { saving: false, error: getErrorMessage(error) });
        return null;
      }
    },

    async updateProduct(id: number, input: ProductInput): Promise<Product | null> {
      patchState(store, { saving: true, error: null });
      try {
        const updated = await firstValueFrom(productService.update(id, input));
        patchState(store, (state) => ({
          saving: false,
          products: state.products.map((product) => (product.id === id ? updated : product)),
          selectedProduct: updated,
        }));
        return updated;
      } catch (error) {
        patchState(store, { saving: false, error: getErrorMessage(error) });
        return null;
      }
    },

    async deleteProduct(id: number): Promise<boolean> {
      patchState(store, { saving: true, error: null });
      try {
        await firstValueFrom(productService.delete(id));
        patchState(store, (state) => ({
          saving: false,
          products: state.products.filter((product) => product.id !== id),
          pageIndex: clampPageIndex(state.products.length - 1, state.pageSize, state.pageIndex),
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

    setSort(sortBy: ProductSortField): void {
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
      const total = productsFilterSort(
        store.products(),
        store.searchTerm(),
        store.sortBy(),
        store.sortDirection(),
      ).length;
      patchState(store, { pageIndex: clampPageIndex(total, store.pageSize(), pageIndex) });
    },

    setPageSize(pageSize: number): void {
      const total = productsFilterSort(
        store.products(),
        store.searchTerm(),
        store.sortBy(),
        store.sortDirection(),
      ).length;
      patchState(store, {
        pageSize,
        pageIndex: clampPageIndex(total, pageSize, 0),
      });
    },

    resetQueryState(): void {
      patchState(store, {
        searchTerm: '',
        sortBy: 'id',
        sortDirection: 'desc',
        pageIndex: 0,
        pageSize: 10,
      });
    },
  })),
);

function productsFilterSort(
  products: Product[],
  searchTerm: string,
  sortBy: ProductSortField,
  sortDirection: SortDirection,
): Product[] {
  const searched = filterBySearch(products, searchTerm, (product) => {
    return `${product.id} ${product.name} ${product.category} ${product.description} ${product.price} ${product.stockQuantity}`;
  });
  return sortItems(searched, { sortBy, sortDirection }, (product, field) => product[field]);
}
