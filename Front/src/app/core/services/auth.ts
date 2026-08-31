import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'ecommerce.admin-token';
const USERNAME_KEY = 'ecommerce.admin-username';

type LoginResponse = {
  accessToken: string;
  username: string;
  role: string;
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http: HttpClient;
  readonly token = signal(sessionStorage.getItem(TOKEN_KEY));
  readonly username = signal(sessionStorage.getItem(USERNAME_KEY));
  readonly isAuthenticated = computed(() => !!this.token());

  constructor(http: HttpClient) {
    this.http = http;
  }

  async login(identifier: string, password: string): Promise<void> {
    const response = await firstValueFrom(
      this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, { identifier, password }),
    );
    sessionStorage.setItem(TOKEN_KEY, response.accessToken);
    sessionStorage.setItem(USERNAME_KEY, response.username);
    this.token.set(response.accessToken);
    this.username.set(response.username);
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USERNAME_KEY);
    this.token.set(null);
    this.username.set(null);
  }
}
