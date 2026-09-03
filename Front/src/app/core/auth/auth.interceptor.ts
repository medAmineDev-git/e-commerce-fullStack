import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth';

/** Les routes d'authentification ne portent jamais de jeton. */
const PUBLIC_PATHS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/register-store'];

const isPublic = (url: string) => PUBLIC_PATHS.some((path) => url.includes(path));

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (isPublic(request.url)) {
    return next(request);
  }

  const token = authService.token();
  const authorized = token ? withToken(request, token) : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      // Le jeton d'accès ne vit que 15 minutes : un 401 en cours de session est
      // attendu, et se rattrape sans renvoyer l'utilisateur sur la page de
      // connexion. Seul l'échec du rafraîchissement termine la session.
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !token) {
        return throwError(() => error);
      }

      return from(authService.refresh()).pipe(
        switchMap((renewedToken) => {
          if (!renewedToken) {
            void router.navigate(['/connexion'], {
              queryParams: { returnUrl: router.url },
            });
            return throwError(() => error);
          }
          return next(withToken(request, renewedToken));
        }),
      );
    }),
  );
};

function withToken(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
