/** Une promesse du bandeau de réassurance : une icône, un libellé, un détail. */
export type StoreHighlight = {
  id: number;
  iconKey: string;
  label: string;
  detail: string | null;
  enabled: boolean;
  position: number;
};

/**
 * Le bandeau complet : où il s'affiche, et ce qu'il contient. La vitrine reçoit
 * les deux d'un coup, sinon la page clignoterait entre les deux réponses.
 */
export type StoreHighlights = {
  topEnabled: boolean;
  bottomEnabled: boolean;
  items: StoreHighlight[];
};

/** Ce que le formulaire envoie pour une ligne. */
export type StoreHighlightInput = {
  iconKey: string;
  label: string;
  detail?: string | null;
  enabled?: boolean;
  position?: number | null;
};
