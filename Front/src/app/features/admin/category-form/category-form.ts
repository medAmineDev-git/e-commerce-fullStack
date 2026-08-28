import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CategoryInput } from '../../../core/models/category.model';
import { CategoryStore } from '../../../core/stores/category.store';

type CategoryFormTouched = {
  name: boolean;
  description: boolean;
};

@Component({
  selector: 'app-category-form',
  imports: [MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './category-form.html',
  styleUrl: './category-form.scss',
})
export class CategoryForm {
  private readonly store = inject(CategoryStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  private readonly categoryId = this.route.snapshot.paramMap.get('id');
  readonly isEditMode = signal(this.categoryId !== null && this.categoryId !== 'new');
  readonly submitted = signal(false);

  readonly model = signal<CategoryInput>({
    name: '',
    description: '',
  });

  readonly touched = signal<CategoryFormTouched>({
    name: false,
    description: false,
  });

  readonly saving = computed(() => this.store.saving());

  readonly errors = computed(() => {
    const value = this.model();
    return {
      name: value.name.trim().length === 0 ? 'Le nom est obligatoire' : '',
    };
  });

  readonly formValid = computed(() => {
    const errors = this.errors();
    return !errors.name;
  });

  constructor() {
    void this.initializeForm();
  }

  private async initializeForm(): Promise<void> {
    if (!this.isEditMode()) {
      return;
    }

    const id = Number(this.categoryId);
    const category = await this.store.loadCategory(id);
    if (!category) {
      this.snackBar.open('Catégorie introuvable', 'Fermer', { duration: 3000 });
      await this.router.navigate(['/admin/categories']);
      return;
    }

    this.model.set({
      name: category.name,
      description: category.description,
    });
  }

  updateTextField(field: 'name' | 'description', value: string): void {
    this.model.update((current) => ({ ...current, [field]: value }));
  }

  touch(field: keyof CategoryFormTouched): void {
    this.touched.update((current) => ({ ...current, [field]: true }));
  }

  async save(): Promise<void> {
    this.submitted.set(true);
    if (!this.formValid()) {
      return;
    }

    const payload = this.model();
    const category = this.isEditMode()
      ? await this.store.updateCategory(Number(this.categoryId), payload)
      : await this.store.createCategory(payload);

    if (!category) {
      this.snackBar.open(this.store.error() ?? "Erreur lors de l'enregistrement", 'Fermer', {
        duration: 3000,
      });
      return;
    }

    this.snackBar.open('Catégorie enregistrée', 'Fermer', { duration: 2000 });
    await this.router.navigate(['/admin/categories']);
  }

  cancel(): void {
    void this.router.navigate(['/admin/categories']);
  }
}
