/**
 * Interprète un montant saisi à la main.
 *
 * La virgule est le séparateur décimal en français, et c'est elle que porte le
 * pavé numérique d'un téléphone configuré en France ou en Tunisie. La refuser
 * revenait à refuser la saisie naturelle.
 *
 * @returns null pour un champ vide ou illisible.
 */
export function parseAmount(value: string): number | null {
  const normalized = value.trim().replace(',', '.');
  if (normalized === '') {
    return null;
  }

  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : null;
}

/** Un montant tel qu'on l'écrit en français, pour pré-remplir un champ : 6.9 donne « 6,9 ». */
export function formatAmountInput(value: number | null | undefined): string {
  return value === null || value === undefined ? '' : String(value).replace('.', ',');
}

/** Le dinar compte en millimes : au-delà de trois décimales, le montant n'existe pas. */
export function hasAtMostThreeDecimals(value: number): boolean {
  return Math.abs(value * 1000 - Math.round(value * 1000)) < 1e-6;
}
