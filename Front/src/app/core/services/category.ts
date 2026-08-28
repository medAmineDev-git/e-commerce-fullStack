import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Category, CategoryInput } from '../models/category.model';

@Service()
export class CategoryService {
	private readonly http = inject(HttpClient);
	private readonly baseUrl = `${environment.apiBaseUrl}/categories`;

	getAll(): Observable<Category[]> {
		return this.http.get<Category[]>(this.baseUrl);
	}

	getById(id: number): Observable<Category> {
		return this.http.get<Category>(`${this.baseUrl}/${id}`);
	}

	create(category: CategoryInput): Observable<Category> {
		return this.http.post<Category>(this.baseUrl, category);
	}

	update(id: number, category: CategoryInput): Observable<Category> {
		return this.http.put<Category>(`${this.baseUrl}/${id}`, category);
	}

	delete(id: number): Observable<void> {
		return this.http.delete<void>(`${this.baseUrl}/${id}`);
	}
}
