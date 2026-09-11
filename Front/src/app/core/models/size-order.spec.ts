import { compareSizes } from './size-order';

describe('compareSizes', () => {
  const sorted = (sizes: string[]) => [...sizes].sort(compareSizes);

  it('range les lettres sur leur échelle, pas par ordre alphabétique', () => {
    expect(sorted(['XL', 'L', 'S', 'M', 'XS', 'XXL'])).toEqual(['XS', 'S', 'M', 'L', 'XL', 'XXL']);
  });

  it('range les âges en mois : 12 mois précède 2 ans', () => {
    expect(sorted(['10 ans', '2 ans', '12 mois', '3 mois', '4-5 ans'])).toEqual([
      '3 mois',
      '12 mois',
      '2 ans',
      '4-5 ans',
      '10 ans',
    ]);
  });

  it('range les pointures par valeur', () => {
    expect(sorted(['42', '38', '40', '39,5'])).toEqual(['38', '39,5', '40', '42']);
  });

  it('place les lettres, puis les âges, puis les nombres, puis le reste', () => {
    expect(sorted(['Taille unique', '38', '4 ans', 'M'])).toEqual(['M', '4 ans', '38', 'Taille unique']);
  });
});
