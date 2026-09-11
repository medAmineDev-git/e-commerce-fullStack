import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { PublicProduct } from '../../../core/models/public-product.model';
import { PublicCatalogService } from '../../../core/services/public-catalog.service';
import { StoreContextService } from '../../../core/services/store-context.service';
import { CartStore } from '../../../core/stores/cart.store';
import { ProductDetailPage } from './product-detail-page';

describe('ProductDetailPage', () => {
  let fixture: ComponentFixture<ProductDetailPage>;
  let component: ProductDetailPage;

  const product: PublicProduct = {
    id: 7,
    slug: 'chemise',
    name: 'Chemise',
    shortDescription: 'desc',
    longDescription: 'long',
    // Sans categorie, la fiche ne charge pas d'articles apparentes.
    category: null,
    price: 59.9,
    rating: 4.5,
    reviewsCount: 0,
    stockQuantity: 10,
    imageUrl: 'img',
    gallery: ['img'],
    sizes: [],
  };

  const text = (selector: string): string =>
    (fixture.nativeElement as HTMLElement).querySelector(selector)?.textContent?.trim() ?? '';

  beforeEach(async () => {
    registerLocaleData(localeFr);

    await TestBed.configureTestingModule({
      imports: [ProductDetailPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } },
        },
        {
          provide: StoreContextService,
          useValue: { link: (...segments: (string | number)[]) => ['/boutique', 'nova', ...segments] },
        },
        {
          provide: PublicCatalogService,
          useValue: { getProductById: vi.fn().mockResolvedValue(product), listProducts: vi.fn() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetailPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('affiche a cote du bouton le meme montant que le prix de la fiche', () => {
    expect(text('.price-now')).toContain('59,90');
    expect(text('[data-testid="buy-total"]')).toBe(text('.price-now'));
  });

  it('multiplie par la quantite, sans y ajouter de frais de livraison', () => {
    component.setQuantity(2);
    fixture.detectChanges();

    expect(component.subTotal()).toBeCloseTo(119.8);
    expect(text('[data-testid="buy-total"]')).toContain('119,80');
  });
});

/**
 * Un article décliné : le client doit choisir sa taille et sa couleur, et ce
 * choix doit partir avec l'article — le vendeur en a besoin pour préparer.
 */
describe('ProductDetailPage, article décliné', () => {
  let fixture: ComponentFixture<ProductDetailPage>;
  let component: ProductDetailPage;
  const addItem = vi.fn();

  const product: PublicProduct = {
    id: 8,
    slug: 'body',
    name: 'Body',
    shortDescription: 'desc',
    longDescription: 'long',
    category: null,
    price: 19.9,
    rating: 4.5,
    reviewsCount: 0,
    stockQuantity: 10,
    imageUrl: 'img',
    gallery: ['img'],
    sizes: ['3 mois', '12 mois'],
    colors: [
      { name: 'Bleu nuit', hex: '#1b2a4a' },
      { name: 'Écru', hex: '#f3ead8' },
    ],
  };

  const buyButton = () =>
    (fixture.nativeElement as HTMLElement).querySelector('.buy-cta') as HTMLButtonElement;

  beforeEach(async () => {
    registerLocaleData(localeFr);
    addItem.mockClear();

    await TestBed.configureTestingModule({
      imports: [ProductDetailPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '8' }) } },
        },
        {
          provide: StoreContextService,
          useValue: { link: (...segments: (string | number)[]) => ['/boutique', 'nova', ...segments] },
        },
        {
          provide: PublicCatalogService,
          useValue: { getProductById: vi.fn().mockResolvedValue(product), listProducts: vi.fn() },
        },
        { provide: CartStore, useValue: { addItem, totalItems: () => 0 } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetailPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('ne choisit pas la taille à la place du client', () => {
    expect(component.selectedSize()).toBeNull();
    expect(buyButton().disabled).toBe(true);
    expect(buyButton().textContent).toContain('Choisir une taille');
  });

  it('demande ensuite la couleur', () => {
    component.selectSize('12 mois');
    fixture.detectChanges();

    expect(buyButton().disabled).toBe(true);
    expect(buyButton().textContent).toContain('Choisir une couleur');
  });

  it('met au panier la taille et la couleur choisies', () => {
    component.selectSize('12 mois');
    component.selectColor(product.colors![1]);
    fixture.detectChanges();
    buyButton().click();

    expect(addItem).toHaveBeenCalledWith(product, 1, { size: '12 mois', color: 'Écru' });
  });
});
