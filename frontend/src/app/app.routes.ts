import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'incomes',
    loadComponent: () => import('./features/incomes/incomes.component').then(m => m.IncomesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'subscription',
    loadComponent: () => import('./features/subscription/subscription.component').then(m => m.SubscriptionComponent),
    canActivate: [authGuard]
  },
  {
    path: 'subscription/success',
    loadComponent: () => import('./features/subscription/success/success.component').then(m => m.SuccessComponent),
    canActivate: [authGuard]
  },
  {
    path: 'support',
    loadComponent: () => import('./features/support/support.component').then(m => m.SupportComponent),
    canActivate: [authGuard]
  },
  {
    path: 'support/:id',
    loadComponent: () => import('./features/support/support-detail.component').then(m => m.SupportDetailComponent),
    canActivate: [authGuard]
  },
  {
    path: '404',
    loadComponent: () => import('./shared/components/error-pages/not-found/not-found.component').then(m => m.NotFoundComponent)
  },
  {
    path: 'error/500',
    loadComponent: () => import('./shared/components/error-pages/server-error/server-error.component').then(m => m.ServerErrorComponent)
  },
  {
    path: 'maintenance',
    loadComponent: () => import('./shared/components/error-pages/maintenance/maintenance.component').then(m => m.MaintenanceComponent)
  },
  {
    path: 'offline',
    loadComponent: () => import('./shared/components/error-pages/offline/offline.component').then(m => m.OfflineComponent)
  },
  { path: '**', redirectTo: '/404' }
];
