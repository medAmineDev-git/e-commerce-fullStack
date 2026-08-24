import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { ProductService } from './product';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch products', () => {
    const mockProducts = [{ id: 1, name: 'T-shirt', category: 'Homme', description: '', price: 20, stockQuantity: 10 }];

    service.getAll().subscribe((products) => {
      expect(products).toEqual(mockProducts);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/products');
    expect(req.request.method).toBe('GET');
    req.flush(mockProducts);
  });

  it('should create product', () => {
    const payload = { name: 'Jean', category: 'Homme', description: 'Slim', price: 50, stockQuantity: 8 };
    const response = { id: 2, ...payload };

    service.create(payload).subscribe((product) => {
      expect(product).toEqual(response);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/products');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(response);
  });

  it('should fetch product by id', () => {
    const response = { id: 3, name: 'Veste', category: 'Homme', description: 'Noire', price: 80, stockQuantity: 4 };

    service.getById(3).subscribe((product) => {
      expect(product).toEqual(response);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/products/3');
    expect(req.request.method).toBe('GET');
    req.flush(response);
  });

  it('should update product', () => {
    const payload = { name: 'Veste maj', category: 'Homme', description: 'Noire', price: 90, stockQuantity: 3 };
    const response = { id: 3, ...payload };

    service.update(3, payload).subscribe((product) => {
      expect(product).toEqual(response);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/products/3');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush(response);
  });

  it('should delete product', () => {
    service.delete(3).subscribe((result) => {
      expect(result).toBeNull();
    });

    const req = httpMock.expectOne('http://localhost:8080/api/products/3');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
