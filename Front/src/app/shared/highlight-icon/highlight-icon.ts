import { Component, computed, input } from '@angular/core';

/**
 * Jeu d'icônes du bandeau de réassurance.
 *
 * Les tracés sont livrés avec le site plutôt que stockés en base : le serveur
 * ne connaît que des clés, et aucun balisage envoyé par un exploitant de
 * boutique n'atteint la page. Ajouter une icône demande donc deux gestes — une
 * clé côté serveur, un tracé ici.
 *
 * Dessins au trait, `currentColor` et `stroke` : ils prennent la couleur du
 * texte qui les entoure et restent nets à toute taille.
 */
@Component({
  selector: 'app-highlight-icon',
  template: `
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.4"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      @for (segment of paths(); track $index) {
        <path [attr.d]="segment"></path>
      }
      @for (dot of circles(); track $index) {
        <circle [attr.cx]="dot.cx" [attr.cy]="dot.cy" [attr.r]="dot.r"></circle>
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
    }

    svg {
      width: 100%;
      height: 100%;
    }
  `,
})
export class HighlightIcon {
  readonly name = input.required<string>();

  readonly paths = computed(() => ICONS[this.name()]?.paths ?? ICONS['confiance'].paths);
  readonly circles = computed(() => ICONS[this.name()]?.circles ?? []);
}

type IconShape = {
  paths: string[];
  circles?: Array<{ cx: number; cy: number; r: number }>;
};

/** Les clés reprennent exactement celles du catalogue serveur. */
const ICONS: Record<string, IconShape> = {
  // Camion de livraison.
  livraison: {
    paths: ['M2 7h11v9H2z', 'M13 10h4l3 3v3h-7z', 'M2 16h1', 'M9.5 16h2'],
    circles: [
      { cx: 5.5, cy: 17.5, r: 1.6 },
      { cx: 16.5, cy: 17.5, r: 1.6 },
    ],
  },
  // Casque d'assistance.
  assistance: {
    paths: [
      'M4 13v-1a8 8 0 0 1 16 0v1',
      'M4 13h2.5v5H5a1 1 0 0 1-1-1z',
      'M20 13h-2.5v5H19a1 1 0 0 0 1-1z',
      'M17.5 18v.5a2.5 2.5 0 0 1-2.5 2.5h-2',
    ],
  },
  // Deux flèches en boucle : retour et échange.
  retours: {
    paths: ['M3 9h13a4 4 0 0 1 0 8h-3', 'M6 6 3 9l3 3', 'M21 15H8a4 4 0 0 1 0-8h3', 'M18 18l3-3-3-3'],
  },
  // Carte bancaire.
  paiement: {
    paths: ['M2 6h20v12H2z', 'M2 10h20', 'M6 14.5h3'],
  },
  // Devanture de boutique, pour le retrait sur place.
  retrait: {
    paths: ['M4 10v10h16V10', 'M3 5h18l1 5H2z', 'M10 20v-6h4v6'],
  },
  // Médaille.
  qualite: {
    paths: ['M9 3.5 7 8l2.5 1.5', 'M15 3.5 17 8l-2.5 1.5'],
    circles: [
      { cx: 12, cy: 15, r: 6 },
      { cx: 12, cy: 15, r: 2.4 },
    ],
  },
  // Paquet cadeau.
  emballage: {
    paths: ['M3 11h18v10H3z', 'M3 7h18v4H3z', 'M12 7v14', 'M12 7S9.5 3 7.5 4.5 9 7 12 7z', 'M12 7s2.5-4 4.5-2.5S15 7 12 7z'],
  },
  // Bouclier : confiance, sécurité.
  confiance: {
    paths: ['M12 3l7 3v5.5c0 4-3 7.5-7 9-4-1.5-7-5-7-9V6z', 'M9 12l2.2 2.2L15.5 10'],
  },
};

/** Clés proposées au vendeur, dans l'ordre du sélecteur d'icônes. */
export const HIGHLIGHT_ICON_KEYS = Object.keys(ICONS);
