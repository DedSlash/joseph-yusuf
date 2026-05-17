import { Routes } from '@angular/router';
import { adminGuard } from './core/auth/admin.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/login/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    canActivate: [adminGuard],
    loadComponent: () => import('./shared/layout/admin-layout.component').then(m => m.AdminLayoutComponent),
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'users', loadComponent: () => import('./features/users/users.component').then(m => m.UsersComponent) },
      { path: 'users/:id', loadComponent: () => import('./features/users/user-detail.component').then(m => m.UserDetailComponent) },
      { path: 'transactions', loadComponent: () => import('./features/transactions/transactions.component').then(m => m.TransactionsComponent) },
      { path: 'promo-codes', loadComponent: () => import('./features/promo-codes/promo-codes.component').then(m => m.PromoCodesComponent) },
      { path: 'audit-log', loadComponent: () => import('./features/audit-log/audit-log.component').then(m => m.AuditLogComponent) },
      { path: 'support', loadComponent: () => import('./features/support/support.component').then(m => m.SupportComponent) },
      { path: 'support/:id', loadComponent: () => import('./features/support/support-detail.component').then(m => m.SupportDetailComponent) },
      { path: 'knowledge-base', loadComponent: () => import('./features/knowledge-base/knowledge-base.component').then(m => m.KnowledgeBaseComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '' }
];
