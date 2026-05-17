import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { IncomeSource, IncomeSourceRequest, IncomeEntry, IncomeEntryRequest, MonthSummary } from '../../shared/models/income.model';

@Injectable({ providedIn: 'root' })
export class IncomeService {
  private readonly apiUrl = `${environment.apiUrl}/api/incomes`;
  private readonly _incomeUpdated$ = new Subject<void>();
  readonly incomeUpdated$ = this._incomeUpdated$.asObservable();

  constructor(private http: HttpClient) {}

  notifyIncomeUpdated(): void {
    this._incomeUpdated$.next();
  }

  getSources(): Observable<IncomeSource[]> {
    return this.http.get<IncomeSource[]>(`${this.apiUrl}/sources`);
  }

  createSource(request: IncomeSourceRequest): Observable<IncomeSource> {
    return this.http.post<IncomeSource>(`${this.apiUrl}/sources`, request);
  }

  updateSource(id: string, request: IncomeSourceRequest): Observable<IncomeSource> {
    return this.http.put<IncomeSource>(`${this.apiUrl}/sources/${id}`, request);
  }

  deleteSource(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/sources/${id}`);
  }

  getEntries(month: number, year: number): Observable<IncomeEntry[]> {
    const params = new HttpParams().set('month', month).set('year', year);
    return this.http.get<IncomeEntry[]>(`${this.apiUrl}/entries`, { params });
  }

  getAllEntriesForSource(sourceId: string): Observable<IncomeEntry[]> {
    const params = new HttpParams().set('sourceId', sourceId);
    return this.http.get<IncomeEntry[]>(`${this.apiUrl}/entries`, { params });
  }

  createEntry(request: IncomeEntryRequest): Observable<IncomeEntry> {
    return this.http.post<IncomeEntry>(`${this.apiUrl}/entries`, request);
  }

  updateEntry(id: string, request: IncomeEntryRequest): Observable<IncomeEntry> {
    return this.http.put<IncomeEntry>(`${this.apiUrl}/entries/${id}`, request);
  }

  getSummary(month: number, year: number): Observable<MonthSummary> {
    const params = new HttpParams().set('month', month).set('year', year);
    return this.http.get<MonthSummary>(`${this.apiUrl}/summary`, { params });
  }

  getHistory(months: number = 6): Observable<MonthSummary[]> {
    const params = new HttpParams().set('months', months);
    return this.http.get<MonthSummary[]>(`${this.apiUrl}/history`, { params });
  }
}
