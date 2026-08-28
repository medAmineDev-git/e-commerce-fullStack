import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Location } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CartStore } from '../../../core/stores/cart.store';
import {
  ProductColorOption,
  PublicProduct,
  ProductSizeOption,
} from '../../../core/models/public-product.model';
import { PublicCatalogService } from '../../../core/services/public-catalog.service';

@Component({
  selector: 'app-product-detail-page',
  imports: [CurrencyPipe, DecimalPipe, RouterLink],
  templateUrl: './product-detail-page.html',
  styleUrl: './product-detail-page.scss',
})
export class ProductDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly catalogService = inject(PublicCatalogService);

  readonly cartStore = inject(CartStore);
  readonly product = signal<PublicProduct | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly quantity = signal(1);
  readonly currentImage = signal('');
  readonly selectedColor = signal<ProductColorOption | null>(null);
  readonly selectedSize = signal<ProductSizeOption | null>(null);
  readonly addedToCart = signal(false);
  readonly relatedProducts = signal<PublicProduct[]>([]);
  readonly fallbackImage =
    'https://placehold.co/900x1200/f4ede4/5d4c3c?text=Image+Produit';
  readonly unitPrice = computed(() => this.product()?.price ?? 0);
  readonly subTotal = computed(() => this.unitPrice() * this.quantity());
  readonly deliveryFee = computed(() => (this.subTotal() === 0 || this.subTotal() > 100 ? 0 : 6.9));
  readonly orderTotal = computed(() => this.subTotal() + this.deliveryFee());
  readonly cartCount = computed(() => this.cartStore.totalItems());

  constructor() {
    void this.loadProduct();
  }

  private async loadProduct(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const id = Number(this.route.snapshot.paramMap.get('id'));
      const product = await this.catalogService.getProductById(id);
      this.product.set(product);
      this.currentImage.set(product.gallery[0] ?? product.imageUrl ?? this.fallbackImage);
      this.selectedColor.set(product.colors?.[0] ?? null);
      this.selectedSize.set(product.sizes?.[0] ?? null);
      this.addedToCart.set(false);
      await this.loadRelatedProducts(product);
    } catch {
      this.product.set(null);
      this.relatedProducts.set([]);
      this.error.set('Impossible de charger ce produit depuis le serveur.');
    } finally {
      this.loading.set(false);
    }
  }

  private async loadRelatedProducts(product: PublicProduct): Promise<void> {
    const all = await this.catalogService.listProducts();
    const related = all
      .filter((candidate) => candidate.id !== product.id && candidate.category === product.category)
      .slice(0, 3);
    this.relatedProducts.set(related);
  }

  setQuantity(value: string): void {
    const parsed = Number(value);
    if (!Number.isFinite(parsed) || parsed <= 0) {
      this.quantity.set(1);
      return;
    }

    this.quantity.set(Math.floor(parsed));
  }

  selectImage(imageUrl: string): void {
    this.currentImage.set(imageUrl);
  }

  selectColor(color: ProductColorOption): void {
    this.selectedColor.set(color);
    this.addedToCart.set(false);
  }

  selectSize(size: ProductSizeOption): void {
    this.selectedSize.set(size);
    this.addedToCart.set(false);
  }

  onMainImageError(): void {
    const product = this.product();
    if (!product || !product.gallery.length) {
      this.currentImage.set(this.fallbackImage);
      return;
    }

    const current = this.currentImage();
    const currentIndex = Math.max(product.gallery.indexOf(current), 0);
    const nextImage = product.gallery[currentIndex + 1] ?? this.fallbackImage;
    this.currentImage.set(nextImage);
  }

  goBack(): void {
    this.location.back();
  }

  imageHasSource(): boolean {
    return !!this.currentImage();
  }

  addToCart(): void {
    const product = this.product();
    if (!product || this.requiresSizeSelection()) {
      return;
    }

    this.cartStore.addItem(product, this.quantity());
    this.addedToCart.set(true);
  }

  goToCheckout(): void {
    void this.router.navigate(['/checkout']);
  }

  openProduct(productId: number): void {
    void this.router.navigate(['/product', productId]);
  }

  canAddToCart(): boolean {
    return !!this.product() && !this.addedToCart() && !this.requiresSizeSelection();
  }

  hasAddedToCart(): boolean {
    return this.addedToCart();
  }

  formatPrice(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 2,
    }).format(amount);
  }

  hasSizes(): boolean {
    return (this.product()?.sizes?.length ?? 0) > 0;
  }

  requiresSizeSelection(): boolean {
    return this.hasSizes() && !this.selectedSize();
  }
}
