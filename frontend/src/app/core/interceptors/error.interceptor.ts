import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!navigator.onLine) {
        router.navigate(['/offline']);
      } else if (error.status === 0) {
        router.navigate(['/maintenance']);
      } else if (error.status === 503) {
        router.navigate(['/maintenance']);
      } else if (error.status === 500) {
        router.navigate(['/error/500']);
      }
      return throwError(() => error);
    })
  );
};
