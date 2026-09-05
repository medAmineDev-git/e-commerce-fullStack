import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { StoreContextService } from '../../../core/services/store-context.service';
import { PublicCatalogStore } from '../../../core/stores/public-catalog.store';
import { PublicCategory, PublicProduct } from '../../../core/models/public-product.model';
import { CatalogSortField, PublicCatalogService } from '../../../core/services/public-catalog.service';
import { SortDirection } from '../../../core/stores/crud-list.helpers';
import { DEFAULT_BANNER } from '../../../core/models/default-banner';

type StoreHome = { title: string; text: string; featuredProductId: number | null };

/**
 * Page unique de la vitrine : presentation de la boutique et catalogue complet.
 *
 * La page /shop separee a ete supprimee — elle obligeait a naviguer pour voir
 * les produits alors qu'une boutique tient sur une seule page.
 */
@Component({
  selector: 'app-home-page',
  imports: [CurrencyPipe],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  readonly storeContext = inject(StoreContextService);
  readonly catalogStore = inject(PublicCatalogStore);
  private readonly catalogService = inject(PublicCatalogService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly store = this.storeContext.store;
  readonly homeConfiguration = signal<StoreHome | null>(null);
  readonly filtersOpen = signal(false);

  /** Bornes de prix en cours de saisie, appliquees a la validation du panneau. */
  readonly draftMinPrice = signal<number | null>(null);
  readonly draftMaxPrice = signal<number | null>(null);

  readonly heroTitle = computed(
    () => this.homeConfiguration()?.title ?? this.store()?.name ?? 'La sélection',
  );
  readonly heroText = computed(
    () => this.homeConfiguration()?.text ?? this.store()?.description ?? '',
  );
  /**
   * La tete de page ne porte qu'une banniere.
   *
   * Un produit y servait de visuel par defaut : une photo cadree pour une
   * fiche article, etiree en bandeau, ne disait rien de la boutique.
   */
  readonly bannerDesktop = computed(() => this.store()?.bannerUrl ?? null);
  readonly bannerMobile = computed(() => this.store()?.bannerMobileUrl ?? null);

  /**
   * Un seul visuel televerse suffit a ecarter les images livrees : melanger le
   * bandeau du vendeur et notre visuel generique donnerait deux identites a la
   * meme boutique selon la taille de l'ecran.
   */
  readonly hasOwnBanner = computed(() => !!(this.bannerDesktop() || this.bannerMobile()));
  readonly defaultBanner = DEFAULT_BANNER;

  readonly sortOptions = [
    { label: 'Nouveautés', value: 'id' as const },
    { label: 'Prix', value: 'price' as const },
    { label: 'Nom', value: 'name' as const },
  ];

  private isSyncingFromUrl = false;
  private readonly allowedSortBy: CatalogSortField[] = ['id', 'name', 'price', 'stockQuantity'];
  private readonly allowedSortDirection: SortDirection[] = ['asc', 'desc'];

  constructor() {
    void this.loadHomeConfiguration();

    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const sortByRaw = params.get('sortBy') ?? 'id';
      const sortDirectionRaw = params.get('sortDirection') ?? 'desc';
      const pageRaw = Number(params.get('page') ?? '1');

      this.isSyncingFromUrl = true;
      void this.catalogStore
        .applyQueryState({
          category: (params.get('category') as PublicCategory | 'Tous') ?? 'Tous',
          subcategory: params.get('subcategory') ?? '',
          season: params.get('season') ?? '',
          productSize: params.get('productSize') ?? '',
          minPrice: this.parseNumber(params.get('minPrice')),
          maxPrice: this.parseNumber(params.get('maxPrice')),
          sortBy: this.allowedSortBy.includes(sortByRaw as CatalogSortField)
            ? (sortByRaw as CatalogSortField)
            : 'id',
          sortDirection: this.allowedSortDirection.includes(sortDirectionRaw as SortDirection)
            ? (sortDirectionRaw as SortDirection)
            : 'desc',
          page: Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) - 1 : 0,
        })
        .finally(() => {
          this.isSyncingFromUrl = false;
          this.draftMinPrice.set(this.catalogStore.minPrice());
          this.draftMaxPrice.set(this.catalogStore.maxPrice());
        });
    });
  }

  private async loadHomeConfiguration(): Promise<void> {
    try {
      this.homeConfiguration.set(await this.catalogService.getHomeConfiguration());
    } catch {
      // Une boutique sans page d'accueil configurée reste parfaitement utilisable.
      this.homeConfiguration.set(null);
    }
  }

  toggleFilters(): void {
    this.filtersOpen.update((open) => !open);
  }

  scrollToCatalog(): void {
    document.getElementById('catalogue')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  setCategory(category: string): void {
    this.catalogStore.setCategory(category as PublicCategory | 'Tous');
    this.syncUrl({ category, subcategory: '', page: 1 });
  }

  setSubcategory(value: string): void {
    this.catalogStore.setSubcategory(value);
    this.syncUrl({ subcategory: value, page: 1 });
  }

  setSeason(value: string): void {
    this.catalogStore.setSeason(value);
    this.syncUrl({ season: value, page: 1 });
  }

  setSize(value: string): void {
    this.catalogStore.setSize(value);
    this.syncUrl({ productSize: this.catalogStore.selectedSize(), page: 1 });
  }

  applyPriceRange(): void {
    const min = this.draftMinPrice();
    const max = this.draftMaxPrice();
    // Bornes inversées : on les remet dans l'ordre plutôt que de ne rien renvoyer.
    const [safeMin, safeMax] = min !== null && max !== null && min > max ? [max, min] : [min, max];

    this.catalogStore.setPriceRange(safeMin, safeMax);
    this.draftMinPrice.set(safeMin);
    this.draftMaxPrice.set(safeMax);
    this.syncUrl({ minPrice: safeMin, maxPrice: safeMax, page: 1 });
  }

  setSort(sortBy: CatalogSortField): void {
    this.catalogStore.setSort(sortBy);
    this.syncUrl({
      sortBy,
      sortDirection: this.catalogStore.sortDirection(),
      page: 1,
    });
  }

  toggleSortDirection(): void {
    this.catalogStore.setSort(this.catalogStore.sortBy());
    this.syncUrl({ sortDirection: this.catalogStore.sortDirection(), page: 1 });
  }

  clearAll(): void {
    this.catalogStore.resetFilters();
    this.draftMinPrice.set(null);
    this.draftMaxPrice.set(null);
    this.syncUrl({
      category: 'Tous',
      subcategory: '',
      season: '',
      productSize: '',
      minPrice: null,
      maxPrice: null,
      sortBy: 'id',
      sortDirection: 'desc',
      page: 1,
    });
  }

  previousPage(): void {
    this.catalogStore.setPage(this.catalogStore.pageIndex() - 1);
    this.syncUrl({ page: Math.max(this.catalogStore.currentPage() - 1, 1) });
    this.scrollToCatalog();
  }

  nextPage(): void {
    this.catalogStore.setPage(this.catalogStore.pageIndex() + 1);
    this.syncUrl({ page: this.catalogStore.currentPage() + 1 });
    this.scrollToCatalog();
  }

  openProduct(product: PublicProduct): void {
    void this.router.navigate(this.storeContext.link('product', product.id));
  }

  private parseNumber(value: string | null): number | null {
    if (value === null || value.trim() === '') {
      return null;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private syncUrl(changes: Record<string, string | number | null>): void {
    if (this.isSyncingFromUrl) {
      return;
    }

    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: changes,
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
