/**
 * Texte d'accueil de la vitrine.
 *
 * Le produit mis en avant a été retiré : la tête de page ne porte plus qu'une
 * bannière, et ce champ n'était plus lu par la vitrine.
 */
export interface HomeConfiguration {
  title: string;
  text: string;
  /** Faux : le texte reste enregistré mais n'apparaît pas sur la vitrine. */
  welcomeEnabled: boolean;
}

export type HomeConfigurationInput = HomeConfiguration;
