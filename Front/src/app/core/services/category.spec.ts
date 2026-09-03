import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CategoryService } from './category';

describe('CategoryService', () => {
  let service: CategoryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CategoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch categories', () => {
    const mockCategories = [{ id: 1, name: 'Homme', description: 'Collection homme' }];

    service.getAll().subscribe((categories) => {
      expect(categories).toEqual(mockCategories);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/admin/categories');
    expect(req.request.method).toBe('GET');
    req.flush(mockCategories);
  });

  it('should create category', () => {
    const payload = { name: 'Femme', description: 'Collection femme' };
    const response = { id: 2, ...payload };

    service.create(payload).subscribe((category) => {
      expect(category).toEqual(response);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/admin/categories');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(response);
  });

  it('should fetch category by id', () => {
    const response = { id: 3, name: 'Enfants', description: 'Collection enfants' };

    service.getById(3).subscribe((category) => {
      expect(category).toEqual(response);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/admin/categories/3');
    expect(req.request.method).toBe('GET');
    req.flush(response);
  });

  it('should update category', () => {
    const payload = { name: 'Enfants+ ', description: 'Collection enfants premium' };
    const response = { id: 3, ...payload };

    service.update(3, payload).subscribe((category) => {
      expect(category).toEqual(response);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/admin/categories/3');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush(response);
  });

  it('should delete category', () => {
    service.delete(3).subscribe((result) => {
      expect(result).toBeNull();
    });

    const req = httpMock.expectOne('http://localhost:8080/api/admin/categories/3');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
