import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'ecommerce.publisher-reference';
const ALLOWED_REFERENCES = new Set(['am', 'wa']);

@Injectable({ providedIn: 'root' })
export class PublisherReferenceService {
  readonly reference = signal<string | null>(this.read());

  capture(reference: string | null): void {
    const normalized = reference?.trim().toLowerCase();
    if (!normalized || !ALLOWED_REFERENCES.has(normalized)) {
      return;
    }

    localStorage.setItem(STORAGE_KEY, normalized);
    this.reference.set(normalized);
  }

  private read(): string | null {
    const reference = localStorage.getItem(STORAGE_KEY);
    return reference && ALLOWED_REFERENCES.has(reference) ? reference : null;
  }
}
