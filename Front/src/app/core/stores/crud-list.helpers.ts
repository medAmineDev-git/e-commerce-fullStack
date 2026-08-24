export type SortDirection = 'asc' | 'desc';

export type SortConfig<TSort extends string> = {
  sortBy: TSort;
  sortDirection: SortDirection;
};

export type PaginationConfig = {
  pageIndex: number;
  pageSize: number;
};

export function normalizeSearchTerm(value: string): string {
  return value.trim().toLowerCase();
}

export function filterBySearch<T>(items: T[], term: string, indexer: (item: T) => string): T[] {
  const normalized = normalizeSearchTerm(term);
  if (!normalized) {
    return items;
  }
  return items.filter((item) => indexer(item).toLowerCase().includes(normalized));
}

export function sortItems<T, TSort extends string>(
  items: T[],
  config: SortConfig<TSort>,
  getter: (item: T, sortBy: TSort) => string | number,
): T[] {
  const sorted = [...items];
  sorted.sort((a, b) => {
    const left = getter(a, config.sortBy);
    const right = getter(b, config.sortBy);

    if (left < right) {
      return config.sortDirection === 'asc' ? -1 : 1;
    }
    if (left > right) {
      return config.sortDirection === 'asc' ? 1 : -1;
    }
    return 0;
  });
  return sorted;
}

export function clampPageIndex(totalItems: number, pageSize: number, pageIndex: number): number {
  if (pageSize <= 0) {
    return 0;
  }
  const maxPageIndex = Math.max(Math.ceil(totalItems / pageSize) - 1, 0);
  return Math.min(Math.max(pageIndex, 0), maxPageIndex);
}

export function paginateItems<T>(items: T[], config: PaginationConfig): T[] {
  const pageIndex = clampPageIndex(items.length, config.pageSize, config.pageIndex);
  const start = pageIndex * config.pageSize;
  return items.slice(start, start + config.pageSize);
}
