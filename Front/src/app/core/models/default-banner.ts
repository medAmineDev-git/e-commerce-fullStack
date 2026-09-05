/**
 * Visuels de tête servis tant qu'une boutique n'a pas les siens.
 *
 * Une vitrine qui s'ouvre sur un vide ne donne pas envie d'y rester, et le
 * vendeur qui vient de créer sa boutique n'a pas toujours un visuel prêt. Ces
 * images sont livrées avec le site plutôt que copiées en base : elles ne
 * dépendent donc d'aucun disque, d'aucune migration, et un champ vide garde
 * son sens — « pas de visuel propre », et non « pas de visuel ».
 *
 * Chaque format existe en WebP et en JPEG ; le navigateur prend le premier
 * qu'il sait lire.
 */
export const DEFAULT_BANNER = {
  desktopWebp: '/banners/defaut-desktop.webp',
  desktopJpeg: '/banners/defaut-desktop.jpg',
  mobileWebp: '/banners/defaut-mobile.webp',
  mobileJpeg: '/banners/defaut-mobile.jpg',
} as const;
