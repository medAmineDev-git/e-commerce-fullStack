import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProductStore } from '../../../core/stores/product.store';
import { ProductService } from '../../../core/services/product';

import { ProductForm } from './product-form';

describe('ProductForm', () => {
  let component: ProductForm;
  let fixture: ComponentFixture<ProductForm>;
  const mockNavigate = vi.fn().mockResolvedValue(true);
  const mockSnackBarOpen = vi.fn();
  let mockStore: {
    saving: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    loadProduct: ReturnType<typeof vi.fn>;
    createProduct: ReturnType<typeof vi.fn>;
    updateProduct: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    mockStore = {
      saving: signal(false),
      error: signal(null),
      loadProduct: vi.fn().mockResolvedValue(null),
      createProduct: vi.fn().mockResolvedValue({
        id: 1,
        name: 'T-shirt',
        category: 'Homme',
        description: '',
        price: 10,
        stockQuantity: 1,
      }),
      updateProduct: vi.fn().mockResolvedValue(null),
    };

    await TestBed.configureTestingModule({
      imports: [ProductForm],
      providers: [
        provideRouter([]),
        { provide: ProductStore, useValue: mockStore },
        { provide: ProductService, useValue: { uploadImage: vi.fn() } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => null } } },
        },
        {
          provide: Router,
          useValue: { navigate: mockNavigate },
        },
        { provide: MatSnackBar, useValue: { open: mockSnackBarOpen } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call create on valid save', async () => {
    component.updateTextField('name', 'Pull');
    component.updateTextField('category', 'Homme');
    component.updateTextField('description', 'Laine');
    component.updatePrice('price', '35');
    component.updateNumberField('stockQuantity', '12');
    component.imageDraft.set('https://example.com/pull.jpg');
    await component.addImage();

    await component.save();

    expect(mockStore.createProduct).toHaveBeenCalledWith({
      name: 'Pull',
      category: 'Homme',
      description: 'Laine',
      price: 35,
      stockQuantity: 12,
      status: 'DRAFT',
      imageUrls: ['https://example.com/pull.jpg'],
      sizes: [],
      seasons: [],
      colors: [],
      // Conditions proposees, pre-remplies a la creation : le vendeur voit ce
      // qui sera enregistre plutot que des champs vides.
      serviceTerms: [
        { label: 'Livraison', value: '48 à 72 heures' },
        { label: 'Paiement', value: 'À la livraison ou par virement' },
        { label: 'Retour', value: 'Sous 7 jours' },
      ],
      sku: '',
      compareAtPrice: null,
      seoTitle: '',
      seoDescription: '',
      subcategory: '',
    });
    expect(mockNavigate).not.toHaveBeenCalledWith(['/admin/products']);
    expect(mockSnackBarOpen).toHaveBeenCalledWith('Produit créé avec succès.', 'Fermer', { duration: 3000 });
  });

  it('should save a product without category nor description', async () => {
    component.updateTextField('name', 'Pull');
    component.updatePrice('price', '35');
    component.updateNumberField('stockQuantity', '12');
    component.imageDraft.set('https://example.com/pull.jpg');
    await component.addImage();

    await component.save();

    expect(mockStore.createProduct).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Pull', category: '', description: '' }),
    );
  });

  /**
   * Le vendeur retire toutes les lignes : le bloc « Livraison et retours »
   * disparait de la fiche. Une liste vide part au serveur, qui la distingue
   * d'un champ absent et ne reinstalle donc pas les valeurs proposees.
   */
  it('should send an empty list when every service term is removed', async () => {
    component.updateTextField('name', 'Pull');
    component.updatePrice('price', '35');
    component.updateNumberField('stockQuantity', '12');
    component.imageDraft.set('https://example.com/pull.jpg');
    await component.addImage();

    component.removeServiceTerm(2);
    component.removeServiceTerm(1);
    component.removeServiceTerm(0);
    await component.save();

    expect(mockStore.createProduct).toHaveBeenCalledWith(
      expect.objectContaining({ serviceTerms: [] }),
    );
  });

  it('should drop a service term left incomplete', async () => {
    component.updateTextField('name', 'Pull');
    component.updatePrice('price', '35');
    component.updateNumberField('stockQuantity', '12');
    component.imageDraft.set('https://example.com/pull.jpg');
    await component.addImage();

    component.addServiceTerm();
    component.updateServiceTerm(3, 'label', 'Garantie');
    // La valeur reste vide : le serveur la refuserait sur un message obscur.
    await component.save();

    expect(mockStore.createProduct).toHaveBeenCalledWith(
      expect.objectContaining({
        serviceTerms: expect.not.arrayContaining([
          expect.objectContaining({ label: 'Garantie' }),
        ]),
      }),
    );
  });

  it('should edit a service term in place', async () => {
    component.updateServiceTerm(2, 'value', 'Sous 30 jours');

    expect(component.model().serviceTerms[2]).toEqual({
      label: 'Retour',
      value: 'Sous 30 jours',
    });
  });

  it('should restore the proposed service terms', async () => {
    component.removeServiceTerm(0);
    component.restoreServiceTerms();

    expect(component.model().serviceTerms).toHaveLength(3);
    expect(component.model().serviceTerms[0].label).toBe('Livraison');
  });

  /*
   * Le champ etait de type number : selon le navigateur, une virgule rend la
   * valeur invalide, qui revient vide et efface la saisie. Le vendeur voyait
   * son prix disparaitre en le tapant.
   */
  it('should accept a comma as decimal separator in prices', () => {
    component.updatePrice('price', '35,90');
    component.updatePrice('compareAtPrice', '49,90');

    expect(component.model().price).toBe(35.9);
    expect(component.model().compareAtPrice).toBe(49.9);
  });

  it('should still accept a dot', () => {
    component.updatePrice('price', '35.90');

    expect(component.model().price).toBe(35.9);
  });

  /**
   * Le texte saisi est conserve tel quel : sans cela, la liaison reecrirait le
   * champ pendant la frappe et « 35, » perdrait sa virgule sous les doigts.
   */
  it('should keep a half typed amount visible', () => {
    component.updatePrice('price', '35,');

    expect(component.priceText()).toBe('35,');
    expect(component.model().price).toBe(35);
  });

  it('should treat an emptied price as zero, which validation refuses', () => {
    component.updatePrice('price', '35,90');
    component.updatePrice('price', '');

    expect(component.model().price).toBe(0);
    expect(component.errors().price).toBeTruthy();
  });

  it('should move a selected gallery image to the primary position', async () => {
    component.imageDraft.set('https://example.com/first.jpg');
    await component.addImage();
    component.imageDraft.set('https://example.com/second.jpg');
    await component.addImage();

    component.setPrimaryImage('https://example.com/second.jpg');

    expect(component.model().imageUrls).toEqual([
      'https://example.com/second.jpg',
      'https://example.com/first.jpg',
    ]);
  });
});

/**
 * Le mode edition n'etait couvert par aucun test : le formulaire y chargeait un
 * produit dont la categorie et la description peuvent desormais valoir null, et
 * la sauvegarde echouait avant meme d'atteindre le serveur.
 */
describe('ProductForm en edition', () => {
  let component: ProductForm;
  let fixture: ComponentFixture<ProductForm>;
  const mockSnackBarOpen = vi.fn();
  let mockStore: {
    saving: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    loadProduct: ReturnType<typeof vi.fn>;
    createProduct: ReturnType<typeof vi.fn>;
    updateProduct: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    const stored = {
      id: 7,
      name: 'Pull sans rayon',
      category: null,
      description: null,
      price: 35,
      stockQuantity: 12,
      imageUrls: ['https://example.com/pull.jpg'],
    };

    mockStore = {
      saving: signal(false),
      error: signal(null),
      loadProduct: vi.fn().mockResolvedValue(stored),
      createProduct: vi.fn().mockResolvedValue(null),
      updateProduct: vi.fn().mockResolvedValue({ ...stored, name: 'Pull renomme' }),
    };

    await TestBed.configureTestingModule({
      imports: [ProductForm],
      providers: [
        provideRouter([]),
        { provide: ProductStore, useValue: mockStore },
        { provide: ProductService, useValue: { uploadImage: vi.fn() } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '7' } } },
        },
        { provide: Router, useValue: { navigate: vi.fn().mockResolvedValue(true) } },
        { provide: MatSnackBar, useValue: { open: mockSnackBarOpen } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should turn absent category and description into empty fields', () => {
    expect(component.model().category).toBe('');
    expect(component.model().description).toBe('');
    expect(component.model().name).toBe('Pull sans rayon');
  });

  it('should update a product whose category and description are absent', async () => {
    component.updateTextField('name', 'Pull renomme');

    await component.save();

    expect(mockStore.updateProduct).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ name: 'Pull renomme', category: '', description: '' }),
    );
    expect(mockSnackBarOpen).toHaveBeenCalledWith(
      'Produit modifié avec succès.',
      'Fermer',
      { duration: 3000 },
    );
  });
});
