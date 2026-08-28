import {
  clampPageIndex,
  filterBySearch,
  normalizeSearchTerm,
  paginateItems,
  sortItems,
} from './crud-list.helpers';

describe('crud-list.helpers', () => {
  it('should normalize search term', () => {
    expect(normalizeSearchTerm('  TeST Valeur  ')).toBe('test valeur');
  });

  it('should return all items when search term is empty', () => {
    const items = [{ name: 'A' }, { name: 'B' }];
    const result = filterBySearch(items, '   ', (item) => item.name);
    expect(result).toEqual(items);
  });

  it('should filter items case-insensitively', () => {
    const items = [{ name: 'T-shirt' }, { name: 'Jeans' }];
    const result = filterBySearch(items, 'SHIRT', (item) => item.name);
    expect(result).toEqual([{ name: 'T-shirt' }]);
  });

  it('should sort items ascending and descending', () => {
    const items = [{ id: 2, name: 'B' }, { id: 1, name: 'A' }];

    const asc = sortItems(items, { sortBy: 'id', sortDirection: 'asc' }, (item, field) => item[field]);
    const desc = sortItems(items, { sortBy: 'name', sortDirection: 'desc' }, (item, field) => item[field]);

    expect(asc.map((i) => i.id)).toEqual([1, 2]);
    expect(desc.map((i) => i.name)).toEqual(['B', 'A']);
  });

  it('should clamp page index in valid range', () => {
    expect(clampPageIndex(100, 10, -5)).toBe(0);
    expect(clampPageIndex(100, 10, 99)).toBe(9);
    expect(clampPageIndex(0, 10, 5)).toBe(0);
    expect(clampPageIndex(10, 0, 5)).toBe(0);
  });

  it('should paginate with clamped page index', () => {
    const result = paginateItems([1, 2, 3, 4, 5], { pageIndex: 99, pageSize: 2 });
    expect(result).toEqual([5]);
  });
});
