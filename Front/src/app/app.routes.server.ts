import { RenderMode, ServerRoute } from '@angular/ssr';

/**
 * Mode de rendu par route.
 *
 * Seul le site vitrine du service est prérendu : son contenu est fixe, connu à
 * la compilation, et c'est la seule page dont le référencement compte.
 *
 * Tout le reste reste en rendu client. Les vitrines ne peuvent pas être
 * prérendues : il faudrait énumérer toutes les boutiques et tous leurs produits
 * au moment de la compilation, alors qu'une boutique naît après le déploiement.
 * Le back-office et la console plateforme n'ont, eux, rien à faire dans un index.
 */
export const serverRoutes: ServerRoute[] = [
  { path: '', renderMode: RenderMode.Prerender },
  { path: '**', renderMode: RenderMode.Client },
];
