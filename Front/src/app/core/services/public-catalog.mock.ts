import { Service } from '@angular/core';
import { PublicProduct } from '../models/public-product.model';

const DEFAULT_COLORS = [
  { name: 'Sable', hex: '#dac4ab' },
  { name: 'Carbone', hex: '#1f1f1f' },
  { name: 'Ivoire', hex: '#f7efe3' },
];

const DEFAULT_SIZES = ['S', 'M', 'L'] as const;

const MOCK_PRODUCTS: PublicProduct[] = [
  {
    id: 1,
    slug: 'veste-denim-solar',
    name: 'Veste Denim Solar',
    shortDescription: 'Coupe droite, denim premium, finition vintage.',
    longDescription:
      'Une veste en denim premium avec une coupe droite moderne. Doublure legere et finitions renforcees pour un usage quotidien.',
    category: 'Homme',
    price: 74.9,
    originalPrice: 99.9,
    rating: 4.6,
    reviewsCount: 124,
    stockQuantity: 18,
    badge: 'Best Seller',
    imageUrl:
      'https://images.unsplash.com/photo-1551537482-f2075a1d41f2?auto=format&fit=crop&w=1100&q=80',
    gallery: [
      'https://images.unsplash.com/photo-1551537482-f2075a1d41f2?auto=format&fit=crop&w=1100&q=80',
      'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1100&q=80',
    ],
    colors: [
      { name: 'Stone Blue', hex: '#4b6278' },
      { name: 'Noir Delave', hex: '#2b2f36' },
      { name: 'Gris Fume', hex: '#838995' },
    ],
    sizes: ['S', 'M', 'L', 'XL'],
    sizeGuide: [
      { size: 'S', chest: '94 cm', length: '66 cm' },
      { size: 'M', chest: '100 cm', length: '68 cm' },
      { size: 'L', chest: '106 cm', length: '70 cm' },
      { size: 'XL', chest: '112 cm', length: '72 cm' },
    ],
    reviews: [
      { author: 'Nina', rating: 5, comment: 'Coupe parfaite et belle finition.', date: '12 mai 2026' },
      { author: 'Sofiane', rating: 4, comment: 'Tres beau rendu en vrai, livraison rapide.', date: '28 juin 2026' },
    ],
  },
  {
    id: 2,
    slug: 'robe-fluide-nova',
    name: 'Robe Fluide Nova',
    shortDescription: 'Tissu leger, taille ajustee, mouvement elegant.',
    longDescription:
      'Robe fluide polyvalente pour le quotidien et les sorties. Tissu respirant, taille elastique et finition soignee.',
    category: 'Femme',
    price: 59.9,
    rating: 4.7,
    reviewsCount: 89,
    stockQuantity: 25,
    badge: 'Nouveau',
    imageUrl:
      'https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=1100&q=80',
    gallery: [
      'https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=1100&q=80',
      'https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=1100&q=80',
    ],
    colors: [
      { name: 'Rouge Terre', hex: '#a6463a' },
      { name: 'Blush', hex: '#d1948c' },
      { name: 'Noir Nuit', hex: '#2f2a2a' },
    ],
    sizes: ['XS', 'S', 'M', 'L'],
    sizeGuide: [
      { size: 'XS', chest: '84 cm', length: '92 cm' },
      { size: 'S', chest: '88 cm', length: '94 cm' },
      { size: 'M', chest: '92 cm', length: '96 cm' },
      { size: 'L', chest: '98 cm', length: '98 cm' },
    ],
    reviews: [
      { author: 'Lina', rating: 5, comment: 'Tres fluide et tres flatteuse.', date: '2 juillet 2026' },
      { author: 'Amel', rating: 4, comment: 'Couleurs fideles aux photos.', date: '19 juillet 2026' },
    ],
  },
  {
    id: 3,
    slug: 'sneakers-arc-runner',
    name: 'Sneakers Arc Runner',
    shortDescription: 'Semelle confort, ligne sportive, look urbain.',
    longDescription:
      'Sneakers concues pour la marche longue duree. Semelle amortissante et empeigne maillee pour plus de respirabilite.',
    category: 'Sneakers',
    price: 84.5,
    originalPrice: 109.0,
    rating: 4.8,
    reviewsCount: 302,
    stockQuantity: 32,
    badge: 'Best Seller',
    imageUrl:
      'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1100&q=80',
    gallery: [
      'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1100&q=80',
      'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=1100&q=80',
    ],
    colors: [
      { name: 'Blanc', hex: '#f5f5f5' },
      { name: 'Graphite', hex: '#3f434b' },
      { name: 'Orange Flare', hex: '#ea7a1f' },
    ],
    sizes: ['S', 'M', 'L', 'XL'],
    sizeGuide: [
      { size: 'S', chest: '26 cm', length: '29 cm' },
      { size: 'M', chest: '27 cm', length: '30 cm' },
      { size: 'L', chest: '28 cm', length: '31 cm' },
      { size: 'XL', chest: '29 cm', length: '32 cm' },
    ],
    reviews: [
      { author: 'Marc', rating: 5, comment: 'Confort incroyable au quotidien.', date: '7 juin 2026' },
      { author: 'Yanis', rating: 5, comment: 'Look moderne et belle amorti.', date: '15 juillet 2026' },
    ],
  },
  {
    id: 4,
    slug: 'sac-crossbody-orbit',
    name: 'Sac Crossbody Orbit',
    shortDescription: 'Compact, resistant, poches intelligentes.',
    longDescription:
      'Sac crossbody compact pour une organisation simple: compartiment principal, poche frontale et sangle reglable.',
    category: 'Accessoires',
    price: 44.9,
    rating: 4.4,
    reviewsCount: 61,
    stockQuantity: 40,
    imageUrl:
      'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=1100&q=80',
    gallery: [
      'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=1100&q=80',
      'https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?auto=format&fit=crop&w=1100&q=80',
    ],
    sizes: ['S', 'M', 'L'],
    sizeGuide: [
      { size: 'S', chest: '30 cm', length: '18 cm' },
      { size: 'M', chest: '32 cm', length: '19 cm' },
      { size: 'L', chest: '34 cm', length: '20 cm' },
    ],
    reviews: [
      { author: 'Ines', rating: 4, comment: 'Tres pratique et compact.', date: '8 juillet 2026' },
      { author: 'Sara', rating: 5, comment: 'Parfait pour les sorties.', date: '1 aout 2026' },
    ],
  },
  {
    id: 5,
    slug: 'chemise-lin-pure',
    name: 'Chemise Lin Pure',
    shortDescription: 'Lin naturel, coupe relax, style estival.',
    longDescription:
      'Chemise en lin 100% naturel pour une sensation fraiche. Coupe relax et patte de boutonnage renforcee.',
    category: 'Homme',
    price: 49.9,
    rating: 4.5,
    reviewsCount: 74,
    stockQuantity: 21,
    imageUrl:
      'https://images.unsplash.com/photo-1603252109303-2751441dd157?auto=format&fit=crop&w=1100&q=80',
    gallery: [
      'https://images.unsplash.com/photo-1603252109303-2751441dd157?auto=format&fit=crop&w=1100&q=80',
      'https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?auto=format&fit=crop&w=1100&q=80',
    ],
    colors: [
      { name: 'Blanc Craie', hex: '#efefed' },
      { name: 'Sauge', hex: '#7f8c74' },
      { name: 'Marine', hex: '#2d3e61' },
    ],
    sizes: ['S', 'M', 'L', 'XL'],
    sizeGuide: [
      { size: 'S', chest: '96 cm', length: '72 cm' },
      { size: 'M', chest: '100 cm', length: '74 cm' },
      { size: 'L', chest: '104 cm', length: '76 cm' },
      { size: 'XL', chest: '108 cm', length: '78 cm' },
    ],
    reviews: [
      { author: 'Claire', rating: 4, comment: 'Super tenue pour l ete.', date: '23 juin 2026' },
      { author: 'Maya', rating: 5, comment: 'Le tissu est vraiment agreable.', date: '11 juillet 2026' },
    ],
  },
  {
    id: 6,
    slug: 'ensemble-sport-echo',
    name: 'Ensemble Sport Echo',
    shortDescription: 'Confort stretch, coupe active, seche vite.',
    longDescription:
      'Ensemble sport deux pieces avec textile stretch et sechage rapide. Ideal pour training leger ou look athleisure.',
    category: 'Femme',
    price: 69.0,
    rating: 4.3,
    reviewsCount: 55,
    stockQuantity: 14,
    badge: 'Edition Limitee',
    imageUrl:
      'https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=1100&q=80',
    gallery: [
      'https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=1100&q=80',
      'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1100&q=80',
    ],
    sizes: ['XS', 'S', 'M', 'L'],
    sizeGuide: [
      { size: 'XS', chest: '86 cm', length: '61 cm' },
      { size: 'S', chest: '90 cm', length: '63 cm' },
      { size: 'M', chest: '94 cm', length: '65 cm' },
      { size: 'L', chest: '98 cm', length: '67 cm' },
    ],
    reviews: [
      { author: 'Thomas', rating: 4, comment: 'Tres bon basique de qualite.', date: '4 juillet 2026' },
      { author: 'Julie', rating: 5, comment: 'Coupe propre et matiere agreable.', date: '22 juillet 2026' },
    ],
  },
];

function wait(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

@Service()
export class PublicCatalogMockService {
  async listProducts(): Promise<PublicProduct[]> {
    await wait(180);
    return MOCK_PRODUCTS.map((product) => ({
      ...product,
      gallery: [...product.gallery],
      colors: product.colors ? [...product.colors] : [...DEFAULT_COLORS],
      sizes: product.sizes ? [...product.sizes] : [...DEFAULT_SIZES],
    }));
  }

  async getProductById(id: number): Promise<PublicProduct | null> {
    await wait(120);
    const product = MOCK_PRODUCTS.find((item) => item.id === id);
    if (!product) {
      return null;
    }
    return {
      ...product,
      gallery: [...product.gallery],
      colors: product.colors ? [...product.colors] : [...DEFAULT_COLORS],
      sizes: product.sizes ? [...product.sizes] : [...DEFAULT_SIZES],
    };
  }
}
