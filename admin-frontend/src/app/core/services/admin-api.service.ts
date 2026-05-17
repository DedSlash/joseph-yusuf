import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuditLog, KpiOverview, PageResponse, PaymentMethodConfig, PlanStats,
  PromoCode, PromoCodeStats, Role, Plan, Transaction, User
} from '../../shared/models/admin.model';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly apiUrl = `${environment.apiUrl}/api/admin`;

  constructor(private http: HttpClient) {}

  // Users
  listUsers(page = 0, size = 20, plan?: Plan, enabled?: boolean, search?: string): Observable<PageResponse<User>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (plan) params = params.set('plan', plan);
    if (enabled !== undefined) params = params.set('enabled', enabled);
    if (search) params = params.set('search', search);
    return this.http.get<PageResponse<User>>(`${this.apiUrl}/users`, { params });
  }

  getUser(id: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/${id}`);
  }

  updatePlan(id: string, plan: Plan): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/users/${id}/plan`, { plan });
  }

  setEnabled(id: string, enabled: boolean): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/users/${id}/block`, { enabled });
  }

  updateRole(id: string, role: Role): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/users/${id}/role`, { role });
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/users/${id}`);
  }

  // Transactions
  listTransactions(page = 0, size = 20, status?: string, userId?: string): Observable<PageResponse<Transaction>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (userId) params = params.set('userId', userId);
    return this.http.get<PageResponse<Transaction>>(`${this.apiUrl}/transactions`, { params });
  }

  refundTransaction(id: string): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.apiUrl}/transactions/${id}/refund`, {});
  }

  cancelTransaction(id: string): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.apiUrl}/transactions/${id}/cancel`, {});
  }

  forceActivateTransaction(id: string): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.apiUrl}/transactions/${id}/force-activate`, {});
  }

  // Promo codes
  listPromoCodes(page = 0, size = 20, active?: boolean): Observable<PageResponse<PromoCode>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (active !== undefined) params = params.set('active', active);
    return this.http.get<PageResponse<PromoCode>>(`${this.apiUrl}/promo-codes`, { params });
  }

  createPromoCode(payload: Partial<PromoCode>): Observable<PromoCode> {
    return this.http.post<PromoCode>(`${this.apiUrl}/promo-codes`, payload);
  }

  togglePromoCode(id: string): Observable<PromoCode> {
    return this.http.put<PromoCode>(`${this.apiUrl}/promo-codes/${id}/toggle`, {});
  }

  promoCodeStats(id: string): Observable<PromoCodeStats> {
    return this.http.get<PromoCodeStats>(`${this.apiUrl}/promo-codes/${id}/stats`);
  }

  // Payment methods
  getPaymentMethods(): Observable<PaymentMethodConfig[]> {
    return this.http.get<PaymentMethodConfig[]>(`${this.apiUrl}/payment-methods`);
  }

  togglePaymentMethod(provider: string): Observable<PaymentMethodConfig> {
    return this.http.put<PaymentMethodConfig>(`${this.apiUrl}/payment-methods/${provider}/toggle`, {});
  }

  // KPIs
  kpiOverview(): Observable<KpiOverview> {
    return this.http.get<KpiOverview>(`${this.apiUrl}/kpis/overview`);
  }

  planStats(): Observable<PlanStats> {
    return this.http.get<PlanStats>(`${this.apiUrl}/kpis/plans`);
  }

  // Audit log
  listAuditLog(page = 0, size = 20, adminId?: string): Observable<PageResponse<AuditLog>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (adminId) params = params.set('adminId', adminId);
    return this.http.get<PageResponse<AuditLog>>(`${this.apiUrl}/audit-log`, { params });
  }
}
