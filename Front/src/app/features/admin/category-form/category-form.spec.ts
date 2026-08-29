import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CategoryStore } from '../../../core/stores/category.store';

import { CategoryForm } from './category-form';

describe('CategoryForm', () => {
  let component: CategoryForm;
  let fixture: ComponentFixture<CategoryForm>;
  const mockNavigate = vi.fn().mockResolvedValue(true);
  const mockSnackBarOpen = vi.fn();
  let mockStore: {
    saving: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    categories: ReturnType<typeof signal<any[]>>;
    loadCategories: ReturnType<typeof vi.fn>;
    loadCategory: ReturnType<typeof vi.fn>;
    createCategory: ReturnType<typeof vi.fn>;
    updateCategory: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    mockStore = {
      saving: signal(false),
      error: signal(null),
      categories: signal([]),
      loadCategories: vi.fn().mockResolvedValue(undefined),
      loadCategory: vi.fn().mockResolvedValue(null),
      createCategory: vi.fn().mockResolvedValue({ id: 1, name: 'Homme', description: 'Collection homme' }),
      updateCategory: vi.fn().mockResolvedValue(null),
    };

    await TestBed.configureTestingModule({
      imports: [CategoryForm],
      providers: [
        provideRouter([]),
        { provide: CategoryStore, useValue: mockStore },
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

    fixture = TestBed.createComponent(CategoryForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call create on valid save', async () => {
    component.updateTextField('name', 'Accessoires');
    component.updateTextField('description', 'Collection accessoires');

    await component.save();

    expect(mockStore.createCategory).toHaveBeenCalledWith({
      name: 'Accessoires',
      description: 'Collection accessoires',
      parentId: null,
    });
    expect(mockNavigate).toHaveBeenCalledWith(['/admin/categories']);
  });
});
