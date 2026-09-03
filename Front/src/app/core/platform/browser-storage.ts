import { inject, InjectionToken, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * Accès au stockage navigateur, sûr pendant le prérendu.
 *
 * Ces appels s'exécutent aussi côté serveur au moment du prérendu de la landing,
 * où `localStorage` n'existe pas. Ils échouent aussi en navigation privée stricte.
 * Dans les deux cas on rend une page correcte plutôt que de planter.
 */
export type BrowserStorageKind = 'local' | 'session';

export const BROWSER_STORAGE = new InjectionToken<BrowserStorage>('BROWSER_STORAGE', {
  providedIn: 'root',
  factory: () => new BrowserStorage(isPlatformBrowser(inject(PLATFORM_ID))),
});

export class BrowserStorage {
  constructor(private readonly isBrowser: boolean) {}

  read(kind: BrowserStorageKind, key: string): string | null {
    if (!this.isBrowser) {
      return null;
    }
    try {
      return this.storage(kind).getItem(key);
    } catch {
      return null;
    }
  }

  write(kind: BrowserStorageKind, key: string, value: string): void {
    if (!this.isBrowser) {
      return;
    }
    try {
      this.storage(kind).setItem(key, value);
    } catch {
      // Quota dépassé ou stockage refusé : la page reste utilisable.
    }
  }

  remove(kind: BrowserStorageKind, key: string): void {
    if (!this.isBrowser) {
      return;
    }
    try {
      this.storage(kind).removeItem(key);
    } catch {
      // Rien à faire : la valeur n'était de toute façon pas lisible.
    }
  }

  readJson<T>(kind: BrowserStorageKind, key: string, fallback: T): T {
    const raw = this.read(kind, key);
    if (!raw) {
      return fallback;
    }
    try {
      return JSON.parse(raw) as T;
    } catch {
      return fallback;
    }
  }

  writeJson(kind: BrowserStorageKind, key: string, value: unknown): void {
    this.write(kind, key, JSON.stringify(value));
  }

  private storage(kind: BrowserStorageKind): Storage {
    return kind === 'local' ? localStorage : sessionStorage;
  }
}
