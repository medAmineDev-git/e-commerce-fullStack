import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { Category } from '../../../core/models/category.model';
import { ProductColor, ProductInput, ProductStatus } from '../../../core/models/product.model';
import { CategoryService } from '../../../core/services/category';
import { ProductService } from '../../../core/services/product';
import { ProductStore } from '../../../core/stores/product.store';

type ProductFormModel = {
  name: string;
  category: string;
  subcategory: string;
  description: string;
  sku: string;
  price: number;
  compareAtPrice: number | null;
  stockQuantity: number;
  status: ProductStatus;
  imageUrls: string[];
  sizes: string[];
  seasons: string[];
  colors: ProductColor[];
  seoTitle: string;
  seoDescription: string;
};

@Component({
  selector: 'app-product-form',
  imports: [MatButtonModule],
  templateUrl: './product-form.html',
  styleUrl: './product-form.scss',
})
export class ProductForm {
  private readonly store = inject(ProductStore);
  private readonly categoryService = inject(CategoryService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly productService = inject(ProductService);

  private readonly productId = this.route.snapshot.paramMap.get('id');
  readonly isEditMode = signal(this.productId !== null && this.productId !== 'new');
  readonly submitted = signal(false);
  readonly uploadingImage = signal(false);
  readonly categories = signal<Category[]>([]);
  readonly subcategories = computed(() => {
    const parent = this.categories().find((category) => category.name === this.model().category);
    return this.categories().filter((category) => category.parentId === parent?.id);
  });
  readonly imageDraft = signal('');
  readonly colorNameDraft = signal('');
  readonly colorHexDraft = signal('#000000');
  readonly availableSizes = ['XS', 'S', 'M', 'L', 'XL', 'XXL'] as const;
  readonly availableSeasons = ['Printemps', 'Été', 'Automne', 'Hiver'] as const;

  readonly model = signal<ProductFormModel>(this.emptyModel());
  readonly saving = computed(() => this.store.saving());
  readonly errors = computed(() => {
    const value = this.model();
    return {
      name: value.name.trim() ? '' : 'Le nom est obligatoire.',
      // Catalogue et description sont facultatifs : un vendeur peut publier un
      // article avant d'avoir arrêté sa taxonomie ou rédigé son texte.
      category: '',
      description: '',
      price: value.price > 0 ? '' : 'Le prix de vente doit être supérieur à 0.',
      compareAtPrice:
        value.compareAtPrice === null || value.compareAtPrice > value.price
          ? ''
          : 'Le prix avant remise doit être supérieur au prix de vente.',
      stockQuantity: value.stockQuantity >= 0 ? '' : 'Le stock ne peut pas être négatif.',
      imageUrls: value.imageUrls.length ? '' : 'Ajoutez au moins une image produit.',
    };
  });
  readonly formValid = computed(() => Object.values(this.errors()).every((error) => !error));

  constructor() {
    void this.initializeForm();
  }

  async addImage(): Promise<void> {
    const imageUrl = this.imageDraft().trim();
    if (!this.isValidImageUrl(imageUrl)) {
      this.snackBar.open("Saisissez une URL d'image valide.", 'Fermer', { duration: 3000 });
      return;
    }
    if (this.model().imageUrls.includes(imageUrl)) {
      this.snackBar.open('Cette image est déjà dans la galerie.', 'Fermer', { duration: 3000 });
      return;
    }
    this.model.update((current) => ({ ...current, imageUrls: [...current.imageUrls, imageUrl] }));
    this.imageDraft.set('');
  }

  async uploadImage(file: File | undefined): Promise<void> {
    if (!file || this.uploadingImage()) {
      return;
    }

    this.uploadingImage.set(true);
    try {
      const response = await firstValueFrom(this.productService.uploadImage(file));
      const imageUrl = response.url.startsWith('/')
        ? `${this.productService.apiOrigin}${response.url}`
        : response.url;
      this.model.update((current) => ({ ...current, imageUrls: [...current.imageUrls, imageUrl] }));
      this.snackBar.open('Image ajoutée à la galerie.', 'Fermer', { duration: 2000 });
    } catch (error) {
      this.snackBar.open(this.getUploadErrorMessage(error), 'Fermer', { duration: 3500 });
    } finally {
      this.uploadingImage.set(false);
    }
  }

  removeImage(imageUrl: string): void {
    this.model.update((current) => ({
      ...current,
      imageUrls: current.imageUrls.filter((image) => image !== imageUrl),
    }));
  }

  setPrimaryImage(imageUrl: string): void {
    this.model.update((current) => {
      const imageIndex = current.imageUrls.indexOf(imageUrl);
      if (imageIndex <= 0) {
        return current;
      }

      return {
        ...current,
        imageUrls: [imageUrl, ...current.imageUrls.filter((image) => image !== imageUrl)],
      };
    });
  }

  toggleSize(size: string): void {
    this.model.update((current) => ({
      ...current,
      sizes: current.sizes.includes(size)
        ? current.sizes.filter((item) => item !== size)
        : [...current.sizes, size],
    }));
  }

  toggleSeason(season: string): void {
    this.model.update((current) => ({
      ...current,
      seasons: current.seasons.includes(season)
        ? current.seasons.filter((item) => item !== season)
        : [...current.seasons, season],
    }));
  }

  addColor(): void {
    const name = this.colorNameDraft().trim();
    const hex = this.colorHexDraft();
    if (!name || !/^#[0-9A-Fa-f]{6}$/.test(hex)) {
      this.snackBar.open('Ajoutez un nom et une couleur valide.', 'Fermer', { duration: 3000 });
      return;
    }
    if (this.model().colors.some((color) => color.name.toLowerCase() === name.toLowerCase())) {
      this.snackBar.open('Cette couleur existe déjà.', 'Fermer', { duration: 3000 });
      return;
    }
    this.model.update((current) => ({ ...current, colors: [...current.colors, { name, hex }] }));
    this.colorNameDraft.set('');
    this.colorHexDraft.set('#000000');
  }

  removeColor(name: string): void {
    this.model.update((current) => ({
      ...current,
      colors: current.colors.filter((color) => color.name !== name),
    }));
  }

  updateTextField(field: keyof Pick<ProductFormModel, 'name' | 'category' | 'subcategory' | 'description' | 'sku' | 'seoTitle' | 'seoDescription'>, value: string): void {
    this.model.update((current) => ({
      ...current,
      [field]: value,
      ...(field === 'category' ? { subcategory: '' } : {}),
    }));
  }

  updateSubcategory(value: string): void {
    this.model.update((current) => ({ ...current, subcategory: value }));
  }

  updateNumberField(field: 'price' | 'compareAtPrice' | 'stockQuantity', value: string): void {
    const parsed = Number(value);
    this.model.update((current) => ({
      ...current,
      [field]: value === '' || !Number.isFinite(parsed) ? null : parsed,
    }));
  }

  updateStatus(value: string): void {
    this.model.update((current) => ({ ...current, status: value === 'DRAFT' ? 'DRAFT' : 'ACTIVE' }));
  }

  async save(): Promise<void> {
    this.submitted.set(true);
    if (!this.formValid()) {
      this.snackBar.open('Corrigez les champs indiqués avant de publier.', 'Fermer', { duration: 3000 });
      return;
    }
    if (this.saving()) {
      return;
    }

    const payload = this.toPayload(this.model());
    const product = this.isEditMode()
      ? await this.store.updateProduct(Number(this.productId), payload)
      : await this.store.createProduct(payload);

    if (!product) {
      this.snackBar.open(this.store.error() ?? "Erreur lors de l'enregistrement", 'Fermer', {
        duration: 3000,
      });
      return;
    }

    this.snackBar.open(
      this.isEditMode()
        ? this.model().subcategory
          ? 'Produit et sous-catégorie modifiés avec succès.'
          : 'Produit modifié avec succès.'
        : 'Produit créé avec succès.',
      'Fermer',
      { duration: 3000 },
    );
  }

  cancel(): void {
    void this.router.navigate(['/admin/products']);
  }

  private async initializeForm(): Promise<void> {
    try {
      this.categories.set(await firstValueFrom(this.categoryService.getAll()));
    } catch {
      this.snackBar.open('Impossible de charger les catalogues.', 'Fermer', { duration: 3000 });
    }

    if (!this.isEditMode()) {
      return;
    }

    const product = await this.store.loadProduct(Number(this.productId));
    if (!product) {
      this.snackBar.open('Produit introuvable', 'Fermer', { duration: 3000 });
      await this.router.navigate(['/admin/products']);
      return;
    }

    this.model.set({
      name: product.name,
      // Le formulaire travaille sur des chaines : un champ absent y est une chaine
      // vide, et redevient null au moment de l'envoi.
      category: product.category ?? '',
      subcategory: product.subcategory ?? '',
      description: product.description ?? '',
      sku: product.sku ?? '',
      price: product.price,
      compareAtPrice: product.compareAtPrice ?? null,
      stockQuantity: product.stockQuantity,
      status: product.status ?? 'ACTIVE',
      imageUrls: [...(product.imageUrls ?? [])],
      sizes: [...(product.sizes ?? [])],
      seasons: [...(product.seasons ?? [])],
      colors: [...(product.colors ?? [])],
      seoTitle: product.seoTitle ?? '',
      seoDescription: product.seoDescription ?? '',
    });
  }

  private emptyModel(): ProductFormModel {
    return {
      name: '',
      category: '',
      subcategory: '',
      description: '',
      sku: '',
      price: 0,
      compareAtPrice: null,
      stockQuantity: 0,
      status: 'DRAFT',
      imageUrls: [],
      sizes: [],
      seasons: [],
      colors: [],
      seoTitle: '',
      seoDescription: '',
    };
  }

  private toPayload(model: ProductFormModel): ProductInput {
    return {
      ...model,
      category: model.category.trim(),
      description: model.description.trim(),
      sku: model.sku.trim(),
      seoTitle: model.seoTitle.trim(),
      seoDescription: model.seoDescription.trim(),
      imageUrls: [...model.imageUrls],
      sizes: [...model.sizes],
      colors: [...model.colors],
    };
  }

  private isValidImageUrl(value: string): boolean {
    try {
      const url = new URL(value);
      return url.protocol === 'https:' || url.protocol === 'http:';
    } catch {
      return false;
    }
  }

  private getUploadErrorMessage(error: unknown): string {
    const response = error as { error?: { message?: string } };
    return response.error?.message ?? 'Impossible de téléverser cette image.';
  }
}
