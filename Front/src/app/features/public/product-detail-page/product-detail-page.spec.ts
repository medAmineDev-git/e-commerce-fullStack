import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, ParamMap, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { PublicProduct } from '../../../core/models/public-product.model';
import { PublicCatalogService } from '../../../core/services/public-catalog.service';
import { StoreContextService } from '../../../core/services/store-context.service';
import { CartStore } from '../../../core/stores/cart.store';
import { ProductDetailPage } from './product-detail-page';

describe('ProductDetailPage', () => {
  let fixture: ComponentFixture<ProductDetailPage>;
  let component: ProductDetailPage;
  let params: BehaviorSubject<ParamMap>;
  let getProductById: ReturnType<typeof vi.fn>;

  const base = {
    shortDescription: 'desc',
    longDescription: 'long',
    category: 'Chemises',
    rating: 4.5,
    reviewsCount: 0,
    stockQuantity: 10,
    imageUrl: 'img',
    gallery: ['img'],
    sizes: [],
  };
  const product: PublicProduct = { ...base, id: 7, slug: 'chemise', name: 'Chemise', price: 59.9 };
  const sibling: PublicProduct = { ...base, id: 8, slug: 'chemise-lin', name: 'Chemise en lin', price: 74 };
  const catalog: Record<number, PublicProduct> = { 7: product, 8: sibling };

  const text = (selector: string): string =>
    (fixture.nativeElement as HTMLElement).querySelector(selector)?.textContent?.trim() ?? '';

  /** Ce que fait le routeur quand on clique un article de la même catégorie. */
  const navigateTo = async (id: number) => {
    params.next(convertToParamMap({ id: String(id) }));
    await fixture.whenStable();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    registerLocaleData(localeFr);
    params = new BehaviorSubject(convertToParamMap({ id: '7' }));
    getProductById = vi.fn().mockImplementation(async (id: number) => catalog[id]);

    await TestBed.configureTestingModule({
      imports: [ProductDetailPage],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: params.asObservable() } },
        {
          provide: StoreContextService,
          useValue: { link: (...segments: (string | number)[]) => ['/boutique', 'nova', ...segments] },
        },
        {
          provide: PublicCatalogService,
          useValue: { getProductById, listProducts: vi.fn().mockResolvedValue([product, sibling]) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetailPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
    fixture.detectChanges();
  });

  /**
   * Le composant est réutilisé d'une fiche à l'autre : lire l'identifiant une
   * seule fois laissait l'adresse changer sous une fiche restée figée.
   */
  it('affiche le produit cliqué dans « Dans la même catégorie »', async () => {
    expect(text('.related-name')).toBe('Chemise en lin');

    component.setQuantity(3);
    await navigateTo(8);

    expect(getProductById).toHaveBeenLastCalledWith(8);
    expect(text('h1')).toBe('Chemise en lin');
    expect(text('.price-now')).toContain('74,00');
    // La quantité choisie pour l'article précédent ne le suit pas.
    expect(component.quantity()).toBe(1);
    // Et l'article quitté devient à son tour un article apparenté.
    expect(text('.related-name')).toBe('Chemise');
  });

  it("n'affiche pas une réponse arrivée après un clic plus récent", async () => {
    let releaseFirst!: (value: PublicProduct) => void;
    getProductById.mockImplementationOnce(() => new Promise((resolve) => (releaseFirst = resolve)));

    params.next(convertToParamMap({ id: '7' }));
    await navigateTo(8);
    releaseFirst(product);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(text('h1')).toBe('Chemise en lin');
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
          useValue: { paramMap: of(convertToParamMap({ id: '8' })) },
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
