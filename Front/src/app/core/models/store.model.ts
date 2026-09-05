/** Ce que la vitrine connaît de sa boutique. Aucune donnée d'exploitation. */
export type PublicStore = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  logoUrl: string | null;
  bannerUrl: string | null;
  /** Visuel composé pour l'écran étroit ; absent, la bannière large sert. */
  bannerMobileUrl: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  domain: string | null;
};

/** Vue du propriétaire sur sa propre boutique. */
export type OwnedStore = PublicStore & {
  active: boolean;
  createdAt: string;
  updatedAt: string;
  ownerUsername: string | null;
};

export type StoreSettingsInput = {
  name: string;
  description: string | null;
  logoUrl: string | null;
  bannerUrl: string | null;
  /** Visuel composé pour l'écran étroit ; absent, la bannière large sert. */
  bannerMobileUrl: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  domain: string | null;
};

/** Vue plateforme : l'inventaire, sans le détail de chaque vitrine. */
export type StoreSummary = {
  id: number;
  name: string;
  slug: string;
  domain: string | null;
  active: boolean;
  ownerUsername: string | null;
  createdAt: string;
};

/** Fiche complete d'une boutique, vue par l'exploitant de la plateforme. */
export type StoreDetail = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  domain: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;

  ownerUsername: string | null;
  ownerEmail: string | null;
  ownerRole: string | null;

  phone: string | null;
  email: string | null;
  address: string | null;

  productCount: number;
  categoryCount: number;
  orderCount: number;
  storageUsedBytes: number;
};
