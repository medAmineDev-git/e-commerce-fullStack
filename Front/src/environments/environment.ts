export const environment = {
  production: true,

  // Chemin relatif, et non une URL absolue : le site et l'API sont servis par
  // la meme origine (Caddy relaie /api vers le jar). Le bundle fonctionne donc
  // a l'identique derriere une IP aujourd'hui et derriere un domaine demain,
  // sans recompilation.
  apiBaseUrl: '/api',

  // Laisse vide : les URL canoniques et Open Graph restent relatives, ce qui
  // est valide. A renseigner le jour ou le domaine existe, pour que le sitemap
  // et les apercus de lien portent des adresses absolues.
  siteUrl: '',
};
