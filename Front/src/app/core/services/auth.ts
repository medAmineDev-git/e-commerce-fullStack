import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BROWSER_STORAGE } from '../platform/browser-storage';

const SESSION_KEY = 'ecommerce.session';

export type UserRole = 'ROLE_STORE_OWNER' | 'ROLE_SUPER_ADMIN';

type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  username: string;
  role: UserRole;
  storeId: number | null;
  storeSlug: string | null;
};

type StoredSession = Omit<LoginResponse, 'expiresInSeconds'>;

export type RegisterStoreInput = {
  username: string;
  email: string;
  password: string;
  storeName: string;
  storeSlug?: string;
  description?: string;
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(BROWSER_STORAGE);

  // Lecture différée et gardée : ce champ s'initialise aussi pendant le prérendu,
  // où sessionStorage n'existe pas.
  private readonly session = signal<StoredSession | null>(
    this.storage.readJson<StoredSession | null>('session', SESSION_KEY, null),
  );

  readonly token = computed(() => this.session()?.accessToken ?? null);
  readonly refreshToken = computed(() => this.session()?.refreshToken ?? null);
  readonly username = computed(() => this.session()?.username ?? null);
  readonly role = computed(() => this.session()?.role ?? null);
  readonly storeSlug = computed(() => this.session()?.storeSlug ?? null);
  readonly isAuthenticated = computed(() => !!this.session());
  readonly isStoreOwner = computed(() => this.role() === 'ROLE_STORE_OWNER');
  readonly isPlatformOperator = computed(() => this.role() === 'ROLE_SUPER_ADMIN');

  async login(identifier: string, password: string): Promise<void> {
    const response = await firstValueFrom(
      this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, {
        identifier,
        password,
      }),
    );
    this.persist(response);
  }

  async registerStore(input: RegisterStoreInput): Promise<void> {
    const response = await firstValueFrom(
      this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/register-store`, input),
    );
    this.persist(response);
  }

  /**
   * Échange le jeton de rafraîchissement contre un nouveau jeton d'accès.
   * Renvoie null si la session est définitivement perdue — compte supprimé,
   * boutique désactivée, ou jeton expiré.
   */
  async refresh(): Promise<string | null> {
    const refreshToken = this.refreshToken();
    if (!refreshToken) {
      return null;
    }

    try {
      const response = await firstValueFrom(
        this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/refresh`, { refreshToken }),
      );
      this.persist(response);
      return response.accessToken;
    } catch {
      this.logout();
      return null;
    }
  }

  logout(): void {
    this.storage.remove('session', SESSION_KEY);
    this.session.set(null);
  }

  private persist(response: LoginResponse): void {
    const stored: StoredSession = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      username: response.username,
      role: response.role,
      storeId: response.storeId,
      storeSlug: response.storeSlug,
    };
    this.storage.writeJson('session', SESSION_KEY, stored);
    this.session.set(stored);
  }
}
