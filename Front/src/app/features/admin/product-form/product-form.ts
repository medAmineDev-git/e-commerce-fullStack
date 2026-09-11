import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { parseAmount } from '../../../core/models/amount';
import { Category } from '../../../core/models/category.model';
import {
  ProductColor,
  ProductInput,
  ProductServiceTerm,
  ProductStatus,
} from '../../../core/models/product.model';
import { CategoryService } from '../../../core/services/category';
import { ProductService } from '../../../core/services/product';
import { ProductStore } from '../../../core/stores/product.store';

/** Limites du serveur (ProductRequest.java) : au-delà, il refuse l'enregistrement. */
const MAX_SIZE_LENGTH = 20;
const MAX_SIZES = 30;

/**
 * Conditions proposees a la creation, identiques a celles du serveur : le
 * vendeur voit ce qui sera enregistre plutot que des champs vides.
 */
const DEFAULT_SERVICE_TERMS: ProductServiceTerm[] = [
  { label: 'Livraison', value: '48 à 72 heures' },
  { label: 'Paiement', value: 'À la livraison ou par virement' },
  { label: 'Retour', value: 'Sous 7 jours' },
];

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
  serviceTerms: ProductServiceTerm[];
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
  readonly sizeDraft = signal('');
  readonly maxSizeLength = MAX_SIZE_LENGTH;
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

  /**
   * Tailles libres : « M », « 38 », « 4 ans », « Taille unique ». Le « + »
   * retient la saisie et vide le champ pour la suivante.
   *
   * Plusieurs tailles séparées par des virgules s'ajoutent d'un coup : taper
   * « S, M, L » évite trois allers-retours pour une gamme adulte courante.
   *
   * @returns false si la saisie a été refusée, pour que l'enregistrement s'arrête.
   */
  addSize(): boolean {
    const entries = this.sizeDraft()
      .split(',')
      .map((entry) => entry.trim())
      .filter((entry) => entry !== '');
    if (entries.length === 0) {
      return true;
    }

    const tooLong = entries.find((entry) => entry.length > MAX_SIZE_LENGTH);
    if (tooLong) {
      this.snackBar.open(`Une taille compte au plus ${MAX_SIZE_LENGTH} caractères.`, 'Fermer', {
        duration: 3000,
      });
      return false;
    }

    // « m » et « M » proposeraient deux fois la même taille au client.
    const known = new Set(this.model().sizes.map((size) => size.toLowerCase()));
    const fresh = entries.filter((entry) => {
      const key = entry.toLowerCase();
      if (known.has(key)) {
        return false;
      }
      known.add(key);
      return true;
    });

    if (this.model().sizes.length + fresh.length > MAX_SIZES) {
      this.snackBar.open(`Un produit compte au plus ${MAX_SIZES} tailles.`, 'Fermer', { duration: 3000 });
      return false;
    }
    if (fresh.length === 0) {
      this.snackBar.open('Cette taille est déjà dans la liste.', 'Fermer', { duration: 3000 });
      this.sizeDraft.set('');
      return true;
    }

    this.model.update((current) => ({ ...current, sizes: [...current.sizes, ...fresh] }));
    this.sizeDraft.set('');
    return true;
  }

  removeSize(size: string): void {
    this.model.update((current) => ({
      ...current,
      sizes: current.sizes.filter((item) => item !== size),
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

  /**
   * Conditions de la fiche : ajout, modification, suppression.
   *
   * Les lignes se modifient en place plutot que par un formulaire separe. Il
   * s'agit de deux courts champs de texte, et le vendeur veut surtout corriger
   * un delai, pas composer une structure.
   */
  addServiceTerm(): void {
    this.model.update((current) => ({
      ...current,
      serviceTerms: [...current.serviceTerms, { label: '', value: '' }],
    }));
  }

  updateServiceTerm(index: number, field: 'label' | 'value', value: string): void {
    this.model.update((current) => ({
      ...current,
      serviceTerms: current.serviceTerms.map((term, position) =>
        position === index ? { ...term, [field]: value } : term,
      ),
    }));
  }

  removeServiceTerm(index: number): void {
    this.model.update((current) => ({
      ...current,
      serviceTerms: current.serviceTerms.filter((_, position) => position !== index),
    }));
  }

  /** Remet les conditions proposees, pour revenir en arriere sans les retaper. */
  restoreServiceTerms(): void {
    this.model.update((current) => ({
      ...current,
      serviceTerms: DEFAULT_SERVICE_TERMS.map((term) => ({ ...term })),
    }));
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

  /**
   * Texte saisi pour les prix, conservé tel quel.
   *
   * Le champ était de type `number` : selon le navigateur, une virgule rend la
   * valeur invalide, qui revient vide et efface la saisie. Il est désormais
   * textuel, et c'est nous qui interprétons.
   *
   * Garder le texte à part évite que la liaison ne réécrive le champ pendant la
   * frappe : sans cela, taper « 35, » donnerait 35, que la liaison réinjecterait
   * aussitôt, faisant disparaître la virgule sous les doigts.
   */
  readonly priceText = signal('');
  readonly compareAtPriceText = signal('');

  updatePrice(field: 'price' | 'compareAtPrice', value: string): void {
    if (field === 'price') {
      this.priceText.set(value);
    } else {
      this.compareAtPriceText.set(value);
    }

    const amount = parseAmount(value);

    this.model.update((current) =>
      field === 'price'
        ? // Le prix de vente n'est pas facultatif : un champ vide vaut zero, que
          // la validation refuse avec un message explicite.
          { ...current, price: amount ?? 0 }
        : { ...current, compareAtPrice: amount },
    );
  }

  /** Le stock est un entier : ni virgule, ni valeur absente. */
  updateNumberField(field: 'stockQuantity', value: string): void {
    const parsed = Number(value);
    const quantity = value.trim() === '' || !Number.isFinite(parsed) ? 0 : Math.floor(parsed);

    this.model.update((current) => ({ ...current, [field]: quantity }));
  }

  updateStatus(value: string): void {
    this.model.update((current) => ({ ...current, status: value === 'DRAFT' ? 'DRAFT' : 'ACTIVE' }));
  }

  async save(): Promise<void> {
    // Une taille tapée sans avoir pressé « + » est une taille voulue : la
    // perdre à l'enregistrement surprendrait le vendeur.
    if (!this.addSize()) {
      return;
    }

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
      serviceTerms: [...(product.serviceTerms ?? [])],
      seoTitle: product.seoTitle ?? '',
      seoDescription: product.seoDescription ?? '',
    });

    // Les champs de prix sont textuels : ils reçoivent la valeur enregistrée,
    // qu'ils garderont ensuite telle que le vendeur la retape.
    this.priceText.set(String(product.price));
    this.compareAtPriceText.set(
      product.compareAtPrice === null || product.compareAtPrice === undefined
        ? ''
        : String(product.compareAtPrice),
    );
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
      serviceTerms: DEFAULT_SERVICE_TERMS.map((term) => ({ ...term })),
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
      // Une ligne ajoutee puis laissee vide n'est pas une condition : le serveur
      // la refuserait, et l'erreur ne dirait rien au vendeur.
      serviceTerms: model.serviceTerms
        .map((term) => ({ label: term.label.trim(), value: term.value.trim() }))
        .filter((term) => term.label && term.value),
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
