import { Component, input } from '@angular/core';
import { StoreHighlight } from '../../core/models/store-highlight.model';
import { HighlightIcon } from '../highlight-icon/highlight-icon';

/**
 * Bandeau de réassurance : les promesses qui répondent aux questions qu'un
 * visiteur se pose avant d'ajouter au panier.
 *
 * Le composant ne charge rien lui-même : les lignes lui sont passées. Le même
 * bandeau paraît sous la bannière et au-dessus du pied de page, et deux
 * chargements pour une seule donnée seraient un gâchis visible à l'écran.
 */
@Component({
  selector: 'app-highlight-bar',
  imports: [HighlightIcon],
  template: `
    @if (items().length > 0) {
      <ul class="bar u-shell" [class.bar--quiet]="quiet()">
        @for (item of items(); track item.id) {
          <li class="item">
            <span class="item__icon">
              <app-highlight-icon [name]="item.iconKey" />
            </span>
            <span class="item__text">
              <span class="item__label">{{ item.label }}</span>
              @if (item.detail) {
                <span class="item__detail">{{ item.detail }}</span>
              }
            </span>
          </li>
        }
      </ul>
    }
  `,
  styleUrl: './highlight-bar.scss',
})
export class HighlightBar {
  readonly items = input.required<StoreHighlight[]>();

  /** Version discrète, pour la reprise juste avant le pied de page. */
  readonly quiet = input(false);
}
