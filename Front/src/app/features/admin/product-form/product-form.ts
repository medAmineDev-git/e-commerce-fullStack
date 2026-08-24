import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProductInput } from '../../../core/models/product.model';
import { ProductStore } from '../../../core/stores/product.store';

type ProductFormTouched = {
  name: boolean;
  category: boolean;
  description: boolean;
  price: boolean;
  stockQuantity: boolean;
};

@Component({
  selector: 'app-product-form',
  imports: [MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './product-form.html',
  styleUrl: './product-form.scss',
})
export class ProductForm {
  private readonly store = inject(ProductStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  private readonly productId = this.route.snapshot.paramMap.get('id');
  readonly isEditMode = signal(this.productId !== null && this.productId !== 'new');
  readonly submitted = signal(false);

  readonly model = signal<ProductInput>({
    name: '',
    category: '',
    description: '',
    price: 0,
    stockQuantity: 0,
  });

  readonly touched = signal<ProductFormTouched>({
    name: false,
    category: false,
    description: false,
    price: false,
    stockQuantity: false,
  });

  readonly saving = computed(() => this.store.saving());

  readonly errors = computed(() => {
    const value = this.model();
    return {
      name: value.name.trim().length === 0 ? 'Le nom est obligatoire' : '',
      category: value.category.trim().length === 0 ? 'La categorie est obligatoire' : '',
      price: value.price < 0 ? 'Le prix doit être supérieur ou égal à 0' : '',
      stockQuantity: value.stockQuantity < 0 ? 'Le stock doit être supérieur ou égal à 0' : '',
    };
  });

  readonly formValid = computed(() => {
    const errors = this.errors();
    return !errors.name && !errors.category && !errors.price && !errors.stockQuantity;
  });

  constructor() {
    void this.initializeForm();
  }

  private async initializeForm(): Promise<void> {
    if (!this.isEditMode()) {
      return;
    }

    const id = Number(this.productId);
    const product = await this.store.loadProduct(id);
    if (!product) {
      this.snackBar.open('Produit introuvable', 'Fermer', { duration: 3000 });
      void this.router.navigate(['/admin/products']);
      return;
    }

    this.model.set({
      name: product.name,
      category: product.category,
      description: product.description,
      price: product.price,
      stockQuantity: product.stockQuantity,
    });
  }

  updateTextField(field: 'name' | 'category' | 'description', value: string): void {
    this.model.update((current) => ({ ...current, [field]: value }));
  }

  updateNumberField(field: 'price' | 'stockQuantity', value: string): void {
    const parsed = Number(value);
    this.model.update((current) => ({
      ...current,
      [field]: Number.isFinite(parsed) ? parsed : 0,
    }));
  }

  touch(field: keyof ProductFormTouched): void {
    this.touched.update((current) => ({ ...current, [field]: true }));
  }

  async save(): Promise<void> {
    this.submitted.set(true);
    if (!this.formValid()) {
      return;
    }

    const payload = this.model();
    const product = this.isEditMode()
      ? await this.store.updateProduct(Number(this.productId), payload)
      : await this.store.createProduct(payload);

    if (!product) {
      this.snackBar.open(this.store.error() ?? "Erreur lors de l'enregistrement", 'Fermer', {
        duration: 3000,
      });
      return;
    }

    this.snackBar.open('Produit enregistré', 'Fermer', { duration: 2000 });
    await this.router.navigate(['/admin/products']);
  }

  cancel(): void {
    void this.router.navigate(['/admin/products']);
  }
}
