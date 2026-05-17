import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError, of } from 'rxjs';
import { AdminAuthService } from './admin-auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AdminAuthService);
  const isAuthUrl = req.url.includes('/auth/login') || req.url.includes('/auth/refresh');

  if (isAuthUrl) {
    return next(req);
  }

  const token = auth.getAccessToken();
  const needsRefresh = auth.isTokenExpiringSoon();

  const attachAndSend = (t: string) => next(req.clone({
    setHeaders: { Authorization: `Bearer ${t}` }
  })).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        return auth.refreshToken().pipe(
          switchMap(res => next(req.clone({
            setHeaders: { Authorization: `Bearer ${res.accessToken}` }
          }))),
          catchError(() => {
            auth.logout();
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );

  if (needsRefresh) {
    return auth.refreshToken().pipe(
      switchMap(res => attachAndSend(res.accessToken)),
      catchError(() => {
        if (token) return attachAndSend(token);
        auth.logout();
        return throwError(() => new Error('Session expirée'));
      })
    );
  }

  if (token) {
    return attachAndSend(token);
  }

  return next(req);
};
