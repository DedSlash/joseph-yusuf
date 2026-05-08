import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription, interval, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AlertDto } from '../../shared/models/alert.model';

@Injectable({ providedIn: 'root' })
export class AlertService {
  private readonly apiUrl = `${environment.apiUrl}/api/alerts`;
  private readonly POLL_INTERVAL_MS = 60_000;

  private readonly _unreadCount$ = new BehaviorSubject<number>(0);
  readonly unreadCount$ = this._unreadCount$.asObservable();

  private pollSubscription: Subscription | null = null;

  constructor(private http: HttpClient) {}

  list(unread = false): Observable<AlertDto[]> {
    return this.http.get<AlertDto[]>(`${this.apiUrl}?unread=${unread}`);
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/unread-count`);
  }

  markAsRead(id: string): Observable<AlertDto> {
    return this.http.put<AlertDto>(`${this.apiUrl}/${id}/read`, {}).pipe(
      tap(() => this.refreshUnreadCount())
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/read-all`, {}).pipe(
      tap(() => this._unreadCount$.next(0))
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.refreshUnreadCount())
    );
  }

  startPolling(): void {
    if (this.pollSubscription) {
      return;
    }
    this.refreshUnreadCount();
    this.pollSubscription = interval(this.POLL_INTERVAL_MS)
      .pipe(switchMap(() => this.getUnreadCount().pipe(catchError(() => of({ count: this._unreadCount$.value })))))
      .subscribe(({ count }) => this._unreadCount$.next(count));
  }

  stopPolling(): void {
    this.pollSubscription?.unsubscribe();
    this.pollSubscription = null;
    this._unreadCount$.next(0);
  }

  refreshUnreadCount(): void {
    this.getUnreadCount()
      .pipe(catchError(() => of({ count: this._unreadCount$.value })))
      .subscribe(({ count }) => this._unreadCount$.next(count));
  }
}
