import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SavingsGoal,
  SavingsGoalRequest,
  SavingsContribution,
  SavingsContributionRequest,
  SavingsRecommendation,
  SavingsDashboard
} from '../../shared/models/savings.model';

interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class SavingsService {
  private readonly apiUrl = `${environment.apiUrl}/api/incomes/savings`;
  private readonly _savingsUpdated$ = new Subject<void>();
  readonly savingsUpdated$ = this._savingsUpdated$.asObservable();

  constructor(private http: HttpClient) {}

  notifyUpdated(): void {
    this._savingsUpdated$.next();
  }

  getGoals(): Observable<SavingsGoal[]> {
    return this.http.get<SavingsGoal[]>(`${this.apiUrl}/goals`);
  }

  getGoalById(id: string): Observable<SavingsGoal> {
    return this.http.get<SavingsGoal>(`${this.apiUrl}/goals/${id}`);
  }

  createGoal(request: SavingsGoalRequest): Observable<SavingsGoal> {
    return this.http.post<SavingsGoal>(`${this.apiUrl}/goals`, request);
  }

  updateGoal(id: string, request: SavingsGoalRequest): Observable<SavingsGoal> {
    return this.http.put<SavingsGoal>(`${this.apiUrl}/goals/${id}`, request);
  }

  deleteGoal(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/goals/${id}`);
  }

  addContribution(goalId: string, request: SavingsContributionRequest): Observable<SavingsContribution> {
    return this.http.post<SavingsContribution>(`${this.apiUrl}/goals/${goalId}/contributions`, request);
  }

  getContributions(goalId: string, page = 0, size = 20): Observable<Page<SavingsContribution>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<SavingsContribution>>(`${this.apiUrl}/goals/${goalId}/contributions`, { params });
  }

  getRecommendations(month?: number, year?: number): Observable<SavingsRecommendation[]> {
    let params = new HttpParams();
    if (month !== undefined) params = params.set('month', month);
    if (year !== undefined) params = params.set('year', year);
    return this.http.get<SavingsRecommendation[]>(`${this.apiUrl}/recommendations`, { params });
  }

  getDashboard(): Observable<SavingsDashboard> {
    return this.http.get<SavingsDashboard>(`${this.apiUrl}/dashboard`);
  }
}
