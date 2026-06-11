import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  router.navigate(['/']);
  return false;
};

/**
 * Garde l'accès aux pages d'abonnement.
 * - Admin : accès direct (preview QA), décodé du claim role=ADMIN du JWT.
 * - Public : accès uniquement si payments.active = true côté serveur
 *   (lu depuis /api/auth/trial/status pour rester source unique).
 */
export const subscriptionDisabledGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAdmin()) {
    return true;
  }

  return authService.getTrialStatus().pipe(
    map(status => {
      if (status.paymentsActive) {
        return true;
      }
      router.navigate(['/dashboard']);
      return false;
    }),
    catchError(() => {
      router.navigate(['/dashboard']);
      return of(false);
    })
  );
};
