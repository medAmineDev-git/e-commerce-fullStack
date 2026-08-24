import { DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Product } from '../../../core/models/product.model';
import { ProductStore } from '../../../core/stores/product.store';

@Component({
  selector: 'app-product-list',
  imports: [RouterLink, MatTableModule, MatButtonModule, MatIconModule, DecimalPipe],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList {
  readonly store = inject(ProductStore);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['id', 'name', 'price', 'stockQuantity', 'actions'];
  readonly sortOptions = [
    { label: 'ID', value: 'id' },
    { label: 'Nom', value: 'name' },
    { label: 'Prix', value: 'price' },
    { label: 'Stock', value: 'stockQuantity' },
  ] as const;
  readonly pageSizes = [5, 10, 20, 50] as const;

  constructor() {
    void this.store.loadProducts();
  }

  onSearch(term: string): void {
    this.store.setSearchTerm(term);
  }

  onSort(sortBy: 'id' | 'name' | 'price' | 'stockQuantity'): void {
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

  async deleteProduct(product: Product): Promise<void> {
    if (!confirm(`Supprimer "${product.name}" ?`)) {
      return;
    }

    const success = await this.store.deleteProduct(product.id);
    if (success) {
      this.snackBar.open('Produit supprimé', 'Fermer', { duration: 2000 });
      return;
    }

    this.snackBar.open(this.store.error() ?? 'Erreur lors de la suppression', 'Fermer', { duration: 3000 });
  }
}
