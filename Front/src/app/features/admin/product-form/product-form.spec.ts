import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProductStore } from '../../../core/stores/product.store';

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

    await component.save();

    expect(mockStore.createProduct).toHaveBeenCalledWith({
      name: 'Pull',
      category: 'Homme',
      description: 'Laine',
      price: 35,
      stockQuantity: 12,
    });
    expect(mockNavigate).toHaveBeenCalledWith(['/admin/products']);
  });
});
