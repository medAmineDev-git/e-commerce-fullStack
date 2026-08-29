import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HomeConfigurationService } from '../../../core/services/home-configuration';
import { ProductService } from '../../../core/services/product';
import { Product } from '../../../core/models/product.model';

@Component({
  selector: 'app-home-configuration',
  imports: [DecimalPipe],
  templateUrl: './home-configuration.html',
  styleUrl: './home-configuration.scss',
})
export class HomeConfigurationPage {
  private readonly configurationService = inject(HomeConfigurationService);
  private readonly productService = inject(ProductService);
  private readonly snackBar = inject(MatSnackBar);

  readonly products = signal<Product[]>([]);
  readonly title = signal('');
  readonly text = signal('');
  readonly featuredProductId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  private loadCompleted = false;
  readonly canSave = computed(() =>
    !!this.title().trim() && !!this.text().trim() && this.featuredProductId() !== null && !this.saving(),
  );

  selectedProductValue(): string {
    return this.featuredProductId() === null ? '' : String(this.featuredProductId());
  }

  selectFeaturedProduct(value: string): void {
    const productId = Number(value);
    this.featuredProductId.set(Number.isInteger(productId) && productId > 0 ? productId : null);
  }

  constructor() {
    void this.load();
  }

  async save(): Promise<void> {
    const featuredProductId = this.featuredProductId();
    if (!this.canSave() || featuredProductId === null) {
      return;
    }

    this.saving.set(true);
    this.message.set(null);
    this.error.set(null);
    try {
      const configuration = await this.configurationService.save({
        title: this.title().trim(),
        text: this.text().trim(),
        featuredProductId,
      });
      this.title.set(configuration.title);
      this.text.set(configuration.text);
      this.featuredProductId.set(configuration.featuredProductId);
      this.loadCompleted = true;
      this.message.set('Configuration de la home enregistrée.');
      this.snackBar.open('Configuration enregistrée avec succès.', 'Fermer', { duration: 3000 });
    } catch {
      const errorMessage = 'Impossible d’enregistrer la configuration de la home.';
      this.error.set(errorMessage);
      this.snackBar.open(errorMessage, 'Fermer', { duration: 4000 });
    } finally {
      this.saving.set(false);
    }
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [configuration, products] = await Promise.all([
        this.configurationService.get(),
        firstValueFrom(this.productService.getAll()),
      ]);
      if (this.loadCompleted) {
        this.products.set(products);
        return;
      }
      this.title.set(configuration.title);
      this.text.set(configuration.text);
      this.featuredProductId.set(configuration.featuredProductId);
      this.products.set(products);
      this.loadCompleted = true;
    } catch {
      this.error.set('Impossible de charger la configuration de la home.');
    } finally {
      this.loading.set(false);
    }
  }
}
