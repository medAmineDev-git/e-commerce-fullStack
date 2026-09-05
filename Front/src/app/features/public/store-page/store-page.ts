import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef } from '@angular/core';
import { StorePageService } from '../../../core/services/store-page.service';
import { StoreContextService } from '../../../core/services/store-context.service';
import { StorePage } from '../../../core/models/store-page.model';
import { SeoService } from '../../../core/seo/seo.service';

/**
 * Page de contenu d'une boutique : mentions légales, livraison, retours.
 *
 * Le texte est stocké en clair et rendu paragraphe par paragraphe. Le passer
 * en HTML ouvrirait une injection dans une page que n'importe quel exploitant
 * de boutique peut éditer.
 */
@Component({
  selector: 'app-store-page',
  imports: [RouterLink],
  templateUrl: './store-page.html',
  styleUrl: './store-page.scss',
})
export class StorePageView {
  private readonly route = inject(ActivatedRoute);
  private readonly storePageService = inject(StorePageService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly seo = inject(SeoService);

  readonly storeContext = inject(StoreContextService);

  readonly page = signal<StorePage | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);

  /** Un paragraphe par bloc séparé d'une ligne vide, dans l'ordre du texte. */
  readonly paragraphs = computed(() =>
    (this.page()?.content ?? '')
      .split(/\n\s*\n/)
      .map((block) => block.trim())
      .filter((block) => block.length > 0),
  );

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      void this.load(params.get('pageSlug'));
    });
  }

  private async load(pageSlug: string | null): Promise<void> {
    if (!pageSlug) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.notFound.set(false);

    try {
      const page = await this.storePageService.getPublicPage(pageSlug);
      this.page.set(page);
      this.seo.apply({
        title: `${page.title} — ${this.storeContext.store()?.name ?? ''}`.trim(),
        description: page.content.slice(0, 160),
        path: `/boutique/${this.storeContext.slug()}/page/${page.slug}`,
      });
    } catch {
      this.page.set(null);
      this.notFound.set(true);
    } finally {
      this.loading.set(false);
    }
  }
}
