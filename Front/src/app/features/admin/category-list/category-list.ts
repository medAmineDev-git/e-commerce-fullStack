import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { Category } from '../../../core/models/category.model';
import { CategoryStore } from '../../../core/stores/category.store';

@Component({
  selector: 'app-category-list',
  imports: [RouterLink, MatTableModule, MatButtonModule, MatIconModule],
  templateUrl: './category-list.html',
  styleUrl: './category-list.scss',
})
export class CategoryList {
  readonly store = inject(CategoryStore);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['id', 'name', 'description', 'actions'];
  readonly sortOptions = [
    { label: 'ID', value: 'id' },
    { label: 'Nom', value: 'name' },
  ] as const;
  readonly pageSizes = [5, 10, 20, 50] as const;

  constructor() {
    void this.store.loadCategories();
  }

  onSearch(term: string): void {
    this.store.setSearchTerm(term);
  }

  onSort(sortBy: 'id' | 'name'): void {
    this.store.setSort(sortBy);
  }

  onPageSizeChange(value: string): void {
    this.store.setPageSize(Number(value));
  }

  previousPage(): void {
    this.store.setPage(this.store.pageIndex() - 1);
  }

  nextPage(): void {
    this.store.setPage(this.store.pageIndex() + 1);
  }

  async deleteCategory(category: Category): Promise<void> {
    if (!confirm(`Supprimer "${category.name}" ?`)) {
      return;
    }

    const success = await this.store.deleteCategory(category.id);
    if (success) {
      this.snackBar.open('Catégorie supprimée', 'Fermer', { duration: 2000 });
      return;
    }

    this.snackBar.open(this.store.error() ?? 'Erreur lors de la suppression', 'Fermer', {
      duration: 3000,
    });
  }
}
