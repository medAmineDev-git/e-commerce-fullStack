/**
 * Ordre d'affichage des tailles libres dans les filtres de la vitrine.
 *
 * Le serveur les rend par ordre alphabétique, qui range « 10 ans » avant
 * « 2 ans » et « L » avant « M » avant « S ». On range donc par famille, puis
 * dans chaque famille selon sa propre échelle :
 *   1. lettres : XXS, XS, S, M, L, XL, XXL, 3XL…
 *   2. âges : 3 mois, 12 mois, 2 ans, 4-5 ans — en mois, pour que 12 mois
 *      précède 2 ans ;
 *   3. autres nombres : pointures, tours de taille ;
 *   4. le reste (« Taille unique ») par ordre alphabétique.
 */
const LETTER_SCALE = ['XXXS', 'XXS', 'XS', 'S', 'M', 'L', 'XL', 'XXL', 'XXXL', '4XL', '5XL'];
const LETTER_ALIASES: Record<string, string> = { '2XS': 'XXS', '2XL': 'XXL', '3XL': 'XXXL' };

const AGE_PATTERN = /^(\d+(?:[.,]\d+)?)(?:\s*-\s*\d+(?:[.,]\d+)?)?\s*(mois|ans?)\b/i;
const NUMBER_PATTERN = /^(\d+(?:[.,]\d+)?)/;

const collator = new Intl.Collator('fr', { numeric: true, sensitivity: 'base' });

type SizeKey = { family: number; rank: number };

function keyOf(size: string): SizeKey {
  const value = size.trim();
  const upper = value.toUpperCase();

  const letter = LETTER_SCALE.indexOf(LETTER_ALIASES[upper] ?? upper);
  if (letter >= 0) {
    return { family: 0, rank: letter };
  }

  const age = AGE_PATTERN.exec(value);
  if (age) {
    const amount = Number(age[1].replace(',', '.'));
    const inMonths = age[2].toLowerCase() === 'mois' ? amount : amount * 12;
    return { family: 1, rank: inMonths };
  }

  const number = NUMBER_PATTERN.exec(value);
  if (number) {
    return { family: 2, rank: Number(number[1].replace(',', '.')) };
  }

  return { family: 3, rank: 0 };
}

export function compareSizes(a: string, b: string): number {
  const left = keyOf(a);
  const right = keyOf(b);
  return left.family - right.family || left.rank - right.rank || collator.compare(a, b);
}
