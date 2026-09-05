import { bootstrapApplication } from '@angular/platform-browser';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { appConfig } from './app/app.config';
import { App } from './app/app';

registerLocaleData(localeFr);

/*
 * La supervision part dans son propre morceau, charge en parallele.
 *
 * Incluse dans le bundle initial, elle lui ajoutait 76 ko a analyser avant le
 * premier affichage — payes par chaque visiteur, sur une page dont la vitesse
 * est justement ce qu'on cherche a mesurer. Le chargement demarre ici sans
 * attendre, et n'entre dans aucun chemin dont depend le rendu.
 */
void import('./app/core/observability/datadog').then((module) => module.initDatadogRum());

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
