import { Injectable, inject, signal } from "@angular/core";
import { BROWSER_STORAGE } from "../platform/browser-storage";

const STORAGE_KEY = 'ecommerce.publisher-reference';
const ALLOWED_REFERENCES = new Set(['am', 'wa']);

@Injectable({ providedIn: 'root' })
export class PublisherReferenceService {
  private readonly storage = inject(BROWSER_STORAGE);
  readonly reference = signal<string | null>(this.read());

  capture(reference: string | null): void {
    const normalized = reference?.trim().toLowerCase();
    if (!normalized || !ALLOWED_REFERENCES.has(normalized)) {
      return;
    }

    this.storage.write("local", STORAGE_KEY, normalized);
    this.reference.set(normalized);
  }

  private read(): string | null {
    const reference = this.storage.read("local", STORAGE_KEY);
    return reference && ALLOWED_REFERENCES.has(reference) ? reference : null;
  }
}
