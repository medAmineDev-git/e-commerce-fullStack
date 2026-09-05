/** Lien affiché dans le pied de page : de quoi nommer et atteindre la page. */
export type StorePageSummary = {
  slug: string;
  title: string;
};

/** Page complète, telle que la vitrine l'affiche et que l'admin l'édite. */
export type StorePage = StorePageSummary & {
  id: number;
  content: string;
  position: number;
  updatedAt: string;
};

/** Ce que le formulaire envoie. Le slug se déduit du titre s'il est absent. */
export type StorePageInput = {
  title: string;
  slug?: string | null;
  content: string;
  position?: number | null;
};
