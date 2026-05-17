import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from './auth.service';

// Partagé entre toutes les invocations de l'intercepteur dans la même session
let isRefreshing = false;
const refreshDone$ = new BehaviorSubject<string | null>(null);

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  const isAuthUrl = req.url.includes('/auth/');
  let authReq = req;

  if (token && !req.url.includes('/auth/login') && !req.url.includes('/auth/register')) {
    authReq = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isAuthUrl) {
        return throwError(() => error);
      }

      if (isRefreshing) {
        // Un refresh est déjà en cours : attendre son résultat
        return refreshDone$.pipe(
          filter(t => t !== null),
          take(1),
          switchMap(newToken => {
            const retried = req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } });
            return next(retried);
          })
        );
      }

      // Démarrer le refresh
      isRefreshing = true;
      refreshDone$.next(null);

      return authService.refreshToken().pipe(
        switchMap(tokenRes => {
          isRefreshing = false;
          refreshDone$.next(tokenRes.accessToken);
          const retried = req.clone({ setHeaders: { Authorization: `Bearer ${tokenRes.accessToken}` } });
          return next(retried);
        }),
        catchError(refreshErr => {
          isRefreshing = false;
          refreshDone$.next('');
          authService.logout();
          return throwError(() => refreshErr);
        })
      );
    })
  );
};
