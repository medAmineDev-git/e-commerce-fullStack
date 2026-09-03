import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { StoreContextService } from '../../../core/services/store-context.service';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PublicCatalogStore } from '../../../core/stores/public-catalog.store';
import { PublicCategory, PublicProduct } from '../../../core/models/public-product.model';
import { CatalogSortField } from '../../../core/services/public-catalog.service';
import { SortDirection } from '../../../core/stores/crud-list.helpers';

@Component({
  selector: 'app-shop-page',
  imports: [CurrencyPipe],
  templateUrl: './shop-page.html',
  styleUrl: './shop-page.scss',
})
export class ShopPage {
  readonly storeContext = inject(StoreContextService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly catalogStore = inject(PublicCatalogStore);
  readonly draftSearch = signal('');
  readonly isCategoryPage =
    this.route.snapshot.paramMap.has('category') || this.route.snapshot.queryParamMap.has('category');

  readonly sortOptions = [
    { label: 'Nouveautes', value: 'id' as const },
    { label: 'Prix', value: 'price' as const },
    { label: 'Nom', value: 'name' as const },
  ];

  private isSyncingFromUrl = false;
  private readonly allowedCategories: Array<PublicCategory | 'Tous'> = [
    'Tous',
    'Homme',
    'Femme',
    'Sneakers',
    'Accessoires',
  ];
  private readonly allowedSortBy: CatalogSortField[] = ['id', 'name', 'price', 'stockQuantity'];
  private readonly allowedSortDirection: SortDirection[] = ['asc', 'desc'];

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const q = params.get('q') ?? '';
      const categoryRaw = params.get('category') ?? this.route.snapshot.paramMap.get('category') ?? 'Tous';
      const subcategory = params.get('subcategory') ?? '';
      const season = params.get('season') ?? '';
      const sortByRaw = params.get('sortBy') ?? 'id';
      const sortDirectionRaw = params.get('sortDirection') ?? 'desc';
      const pageRaw = Number(params.get('page') ?? '1');

      const category = this.allowedCategories.includes(categoryRaw as PublicCategory | 'Tous')
        ? (categoryRaw as PublicCategory | 'Tous')
        : 'Tous';
      const sortBy = this.allowedSortBy.includes(sortByRaw as CatalogSortField)
        ? (sortByRaw as CatalogSortField)
        : 'id';
      const sortDirection = this.allowedSortDirection.includes(sortDirectionRaw as SortDirection)
        ? (sortDirectionRaw as SortDirection)
        : 'desc';
      const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;

      this.draftSearch.set(q);
      this.isSyncingFromUrl = true;
      void this.catalogStore
        .applyQueryState({
          q,
          category,
          subcategory,
          season,
          sortBy,
          sortDirection,
          page: page - 1,
        })
        .finally(() => {
          this.isSyncingFromUrl = false;
        });
    });
  }

  applySearch(): void {
    this.catalogStore.setSearchTerm(this.draftSearch());
    this.syncUrl({ q: this.draftSearch(), page: 1 });
  }

  setCategory(value: string): void {
    this.catalogStore.setCategory(value as PublicCategory | 'Tous');
    this.syncUrl({ category: value, subcategory: '', page: 1 });
  }

  setSubcategory(value: string): void {
    this.catalogStore.setSubcategory(value);
    this.syncUrl({ subcategory: value, page: 1 });
  }

  setSeason(value: string): void {
    this.catalogStore.setSeason(value);
    this.syncUrl({ season: value, page: 1 });
  }

  toggleSort(): void {
    this.catalogStore.setSort(this.catalogStore.sortBy());
    this.syncUrl({
      sortDirection: this.catalogStore.sortDirection() === 'asc' ? 'desc' : 'asc',
      page: 1,
    });
  }

  setSort(sortBy: CatalogSortField): void {
    this.catalogStore.setSort(sortBy);
    const currentSortBy = this.catalogStore.sortBy();
    const currentDirection = this.catalogStore.sortDirection();
    const nextDirection = currentSortBy === sortBy ? 'asc' : currentDirection;

    this.syncUrl({ sortBy, sortDirection: nextDirection, page: 1 });
  }

  openProduct(product: PublicProduct): void {
    void this.router.navigate(this.storeContext.link('product', product.id));
  }

  previousPage(): void {
    this.catalogStore.setPage(this.catalogStore.pageIndex() - 1);
    this.syncUrl({ page: Math.max(this.catalogStore.currentPage() - 1, 1) });
  }

  nextPage(): void {
    this.catalogStore.setPage(this.catalogStore.pageIndex() + 1);
    this.syncUrl({ page: this.catalogStore.currentPage() + 1 });
  }

  clearAll(): void {
    this.catalogStore.resetFilters();
    this.draftSearch.set('');
    this.syncUrl({ q: '', category: 'Tous', sortBy: 'id', sortDirection: 'desc', page: 1 });
  }

  private syncUrl(changes: {
    q?: string;
    category?: string;
    subcategory?: string;
    season?: string;
    sortBy?: CatalogSortField;
    sortDirection?: SortDirection;
    page?: number;
  }): void {
    if (this.isSyncingFromUrl) {
      return;
    }

    const queryParams = {
      q: changes.q ?? this.catalogStore.searchTerm(),
      category: changes.category ?? this.catalogStore.selectedCategory(),
      subcategory: changes.subcategory ?? this.catalogStore.selectedSubcategory(),
      season: changes.season ?? this.catalogStore.selectedSeason(),
      sortBy: changes.sortBy ?? this.catalogStore.sortBy(),
      sortDirection: changes.sortDirection ?? this.catalogStore.sortDirection(),
      page: changes.page ?? this.catalogStore.currentPage(),
    };

    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
