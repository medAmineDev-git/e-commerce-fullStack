import { Component, computed, inject, signal } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HomeConfigurationService } from '../../../core/services/home-configuration';

/**
 * Texte d'accueil de la vitrine.
 *
 * Le choix d'un produit à mettre en avant a été retiré : la tête de page ne
 * porte plus qu'une bannière, et ce champ n'était plus lu par la vitrine. Il
 * restait pourtant obligatoire à l'enregistrement, ce qui empêchait une
 * boutique sans aucun produit de sauvegarder son texte.
 */
@Component({
  selector: 'app-home-configuration',
  templateUrl: './home-configuration.html',
  styleUrl: './home-configuration.scss',
})
export class HomeConfigurationPage {
  private readonly configurationService = inject(HomeConfigurationService);
  private readonly snackBar = inject(MatSnackBar);

  readonly title = signal('');
  readonly text = signal('');
  readonly welcomeEnabled = signal(true);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  readonly canSave = computed(
    () => !!this.title().trim() && !!this.text().trim() && !this.saving(),
  );

  constructor() {
    void this.load();
  }

  toggleWelcome(): void {
    this.welcomeEnabled.update((enabled) => !enabled);
    this.message.set(null);
  }

  async save(): Promise<void> {
    if (!this.canSave()) {
      return;
    }

    this.saving.set(true);
    this.message.set(null);
    this.error.set(null);

    try {
      const configuration = await this.configurationService.save({
        title: this.title().trim(),
        text: this.text().trim(),
        welcomeEnabled: this.welcomeEnabled(),
      });
      this.title.set(configuration.title);
      this.text.set(configuration.text);
      this.welcomeEnabled.set(configuration.welcomeEnabled);
      this.message.set("Texte d'accueil enregistré.");
      this.snackBar.open('Configuration enregistrée avec succès.', 'Fermer', { duration: 3000 });
    } catch {
      const errorMessage = "Impossible d'enregistrer le texte d'accueil.";
      this.error.set(errorMessage);
      this.snackBar.open(errorMessage, 'Fermer', { duration: 4000 });
    } finally {
      this.saving.set(false);
    }
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const configuration = await this.configurationService.get();
      this.title.set(configuration.title);
      this.text.set(configuration.text);
      this.welcomeEnabled.set(configuration.welcomeEnabled);
    } catch {
      this.error.set("Impossible de charger le texte d'accueil.");
    } finally {
      this.loading.set(false);
    }
  }
}
