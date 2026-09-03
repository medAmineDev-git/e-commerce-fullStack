import { DOCUMENT, Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { environment } from '../../../environments/environment';

export type PageSeo = {
  title: string;
  description: string;
  /** Chemin absolu depuis la racine du site, par exemple `/tarifs`. */
  path: string;
  imageUrl?: string;
  type?: 'website' | 'article' | 'product';
  noIndex?: boolean;
};

const SITE_NAME = 'Boutique en quelques minutes';
const JSON_LD_ID = 'seo-structured-data';

/**
 * Titre, métadonnées et données structurées.
 *
 * Le prérendu fige ce que ce service écrit dans le HTML livré : c'est ce qui
 * rend la landing lisible par les moteurs et par les aperçus de lien. Les
 * données structurées comptent au moins autant que le rendu serveur — ce sont
 * elles qui déclenchent les résultats enrichis.
 */
@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly document = inject(DOCUMENT);

  apply(seo: PageSeo): void {
    const url = this.absoluteUrl(seo.path);

    this.title.setTitle(seo.title);
    this.meta.updateTag({ name: 'description', content: seo.description });
    this.meta.updateTag({
      name: 'robots',
      content: seo.noIndex ? 'noindex, nofollow' : 'index, follow',
    });

    this.setCanonical(url);

    this.meta.updateTag({ property: 'og:site_name', content: SITE_NAME });
    this.meta.updateTag({ property: 'og:type', content: seo.type ?? 'website' });
    this.meta.updateTag({ property: 'og:title', content: seo.title });
    this.meta.updateTag({ property: 'og:description', content: seo.description });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:locale', content: 'fr_FR' });

    this.meta.updateTag({
      name: 'twitter:card',
      content: seo.imageUrl ? 'summary_large_image' : 'summary',
    });
    this.meta.updateTag({ name: 'twitter:title', content: seo.title });
    this.meta.updateTag({ name: 'twitter:description', content: seo.description });

    if (seo.imageUrl) {
      this.meta.updateTag({ property: 'og:image', content: seo.imageUrl });
      this.meta.updateTag({ name: 'twitter:image', content: seo.imageUrl });
    } else {
      this.meta.removeTag('property="og:image"');
      this.meta.removeTag('name="twitter:image"');
    }
  }

  /** Remplace le bloc JSON-LD de la page. Un seul bloc à la fois, jamais empilé. */
  setStructuredData(data: unknown): void {
    this.removeStructuredData();
    if (!data) {
      return;
    }

    const script = this.document.createElement('script');
    script.type = 'application/ld+json';
    script.id = JSON_LD_ID;
    script.textContent = JSON.stringify(data);
    this.document.head.appendChild(script);
  }

  removeStructuredData(): void {
    this.document.getElementById(JSON_LD_ID)?.remove();
  }

  private setCanonical(url: string): void {
    let link = this.document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.document.head.appendChild(link);
    }
    link.setAttribute('href', url);
  }

  /**
   * URL publique de la page.
   *
   * `document.location.origin` n'est pas utilisable au prérendu : il y vaut une
   * origine factice interne au moteur de rendu, qui se retrouverait figée dans
   * le HTML livré. Tant que le domaine réel n'est pas configuré, on émet donc un
   * chemin relatif — valide, et interprété par rapport à la page — plutôt qu'une
   * adresse absolue fausse sur la seule page destinée à être indexée.
   */
  private absoluteUrl(path: string): string {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    const origin = environment.siteUrl?.replace(/\/$/, '') ?? '';
    return `${origin}${normalizedPath}`;
  }
}
