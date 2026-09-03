import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SeoService } from '../../../core/seo/seo.service';

/**
 * Boutique inconnue ou fermée.
 *
 * Le serveur répond 404 dans les deux cas, volontairement : il ne dit pas
 * qu'une boutique existe mais a été désactivée. Cette page reprend la même
 * réserve, et n'est pas indexée.
 */
@Component({
  selector: 'app-store-not-found',
  imports: [RouterLink],
  templateUrl: './store-not-found.html',
  styleUrl: './store-not-found.scss',
})
export class StoreNotFound {
  private readonly route = inject(ActivatedRoute);
  private readonly seo = inject(SeoService);

  readonly slug = signal(this.route.snapshot.queryParamMap.get('slug'));

  constructor() {
    this.seo.apply({
      title: 'Boutique introuvable',
      description: 'Cette boutique n existe pas ou n est plus accessible.',
      path: '/boutique-introuvable',
      noIndex: true,
    });
    this.seo.removeStructuredData();
  }
}
