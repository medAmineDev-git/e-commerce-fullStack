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
    component.updateNumberField('price', '35');
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
    component.updateNumberField('price', '35');
    component.updateNumberField('stockQuantity', '12');
    component.imageDraft.set('https://example.com/pull.jpg');
    await component.addImage();

    await component.save();

    expect(mockStore.createProduct).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Pull', category: '', description: '' }),
    );
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
