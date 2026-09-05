import { datadogRum } from '@datadog/browser-rum';

/**
 * Supervision des vraies visites (Datadog RUM).
 *
 * Mesure ce que vit le visiteur : temps d'affichage, erreurs JavaScript, appels
 * API lents. C'est le seul angle qui aurait montré une panne comme celle du
 * formulaire produit, où l'enregistrement échouait avant tout appel réseau —
 * invisible dans les journaux du serveur, puisque rien n'y arrivait.
 *
 * L'identifiant d'application et le jeton client sont publics par conception :
 * ils partent dans le bundle et n'ouvrent aucun accès en lecture. Ils n'ont
 * donc rien à faire dans une variable d'environnement.
 */

const APPLICATION_ID = 'e728b136-bebb-4975-9b7b-faca8b800d9f';
const CLIENT_TOKEN = 'pub96b22d9ced1e20a9e78d48fb3d3489fb';
const SITE = 'datadoghq.eu';
const SERVICE = 'ecommerce-boutiques-web';

/**
 * L'environnement se déduit du domaine plutôt que d'une variable de compilation.
 *
 * La production et la pré-production partagent le même build : les distinguer à
 * la compilation aurait demandé deux images pour un unique caractère de
 * différence. Ajustez `PREPROD_MARKERS` si votre service de pré-production
 * porte un autre nom sur Render.
 */
const PREPROD_MARKERS = ['preprod', 'pre-prod', 'staging'];

function resolveEnvironment(hostname: string): string {
  if (hostname === 'localhost' || hostname === '127.0.0.1' || hostname.endsWith('.local')) {
    return 'development';
  }
  const lower = hostname.toLowerCase();
  return PREPROD_MARKERS.some((marker) => lower.includes(marker)) ? 'preprod' : 'production';
}

export function initDatadogRum(): void {
  const environmentName = resolveEnvironment(window.location.hostname);

  // Rien à superviser en développement : les sessions locales feraient du bruit
  // dans les tableaux de bord et consommeraient le quota.
  if (environmentName === 'development') {
    return;
  }

  datadogRum.init({
    applicationId: APPLICATION_ID,
    clientToken: CLIENT_TOKEN,
    site: SITE,
    service: SERVICE,
    env: environmentName,

    sessionSampleRate: 100,

    /*
     * Rejeu de session desactive.
     *
     * Le tunnel de commande recueille un nom, un telephone et une adresse de
     * livraison. Un rejeu filme la saisie de ces champs : ce n'est plus de la
     * mesure de performance mais de la collecte de donnees personnelles, qui
     * demande une base legale et une mention dans la politique de
     * confidentialite de chaque boutique.
     *
     * Pour l'activer un jour : passer ce taux a 20, garder le masquage
     * ci-dessous, et completer la page Confidentialite des boutiques.
     */
    sessionReplaySampleRate: 0,

    /*
     * Meme sans rejeu, ce reglage vaut : il empeche le contenu des champs de
     * partir dans les evenements d'interaction. Sans lui, un nom saisi au
     * clavier pourrait se retrouver dans le libelle d'une action.
     */
    defaultPrivacyLevel: 'mask-user-input',

    trackResources: true,
    trackUserInteractions: true,
    trackLongTasks: true,
  });
}
