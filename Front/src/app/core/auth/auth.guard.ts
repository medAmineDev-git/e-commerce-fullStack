import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

/**
 * Les gardes vérifient le rôle, pas la simple présence d'un jeton.
 * Le serveur reste l'autorité — ces gardes évitent d'afficher un écran vide,
 * elles ne protègent rien à elles seules.
 */
export const storeOwnerGuard: CanActivateFn = (_, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isStoreOwner()) {
    return true;
  }

  // Rien n'interdit à un exploitant de la plateforme de tenir aussi une
  // boutique, et le serveur l'autorise déjà. Le bloquer ici obligeait à
  // maintenir deux comptes séparés sans raison.
  if (authService.isPlatformOperator()) {
    return authService.storeSlug() ? true : router.createUrlTree(['/plateforme']);
  }

  return router.createUrlTree(['/connexion'], { queryParams: { returnUrl: state.url } });
};

export const platformGuard: CanActivateFn = (_, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isPlatformOperator()) {
    return true;
  }

  if (authService.isStoreOwner()) {
    return router.createUrlTree(['/admin']);
  }

  return router.createUrlTree(['/connexion'], { queryParams: { returnUrl: state.url } });
};

/** Empêche de revenir sur la connexion en étant déjà identifié. */
export const anonymousOnlyGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree([authService.isPlatformOperator() ? '/plateforme' : '/admin']);
};
