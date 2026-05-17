import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthResponse, User } from '../../shared/models/admin.model';

interface JwtPayload {
  userId: string;
  plan: string;
  role: string;
  exp: number;
}

@Injectable({ providedIn: 'root' })
export class AdminAuthService {
  private readonly apiUrl = `${environment.apiUrl}/api/auth`;
  private currentUser$ = new BehaviorSubject<User | null>(null);
  user$ = this.currentUser$.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    this.loadFromStorage();
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap(res => {
        const role = this.decodeRole(res.accessToken);
        if (role !== 'ADMIN') {
          throw new Error("Accès refusé : ce compte n'a pas le rôle ADMIN");
        }
        this.persist(res);
      })
    );
  }

  logout(): void {
    const refreshToken = localStorage.getItem('admin_refreshToken');
    if (refreshToken) {
      this.http.post(`${this.apiUrl}/logout`, { refreshToken }).subscribe({
        error: () => { /* best-effort */ }
      });
    }
    localStorage.removeItem('admin_accessToken');
    localStorage.removeItem('admin_refreshToken');
    localStorage.removeItem('admin_user');
    this.currentUser$.next(null);
    this.router.navigate(['/login']);
  }

  refreshToken(): Observable<{ accessToken: string }> {
    const refreshToken = localStorage.getItem('admin_refreshToken');
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token'));
    }
    return this.http.post<{ accessToken: string }>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
      tap(res => localStorage.setItem('admin_accessToken', res.accessToken))
    );
  }

  getAccessToken(): string | null {
    return localStorage.getItem('admin_accessToken');
  }

  isLoggedIn(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;
    const payload = this.decodeToken(token);
    return payload != null && payload.role === 'ADMIN' && payload.exp * 1000 > Date.now();
  }

  isAdmin(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;
    return this.decodeRole(token) === 'ADMIN';
  }

  isTokenExpiringSoon(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;
    const payload = this.decodeToken(token);
    if (!payload) return false;
    // true si le token expire dans moins de 60 secondes
    return payload.exp * 1000 - Date.now() < 60_000;
  }

  getCurrentUser(): User | null {
    return this.currentUser$.value;
  }

  private persist(res: AuthResponse): void {
    localStorage.setItem('admin_accessToken', res.accessToken);
    localStorage.setItem('admin_refreshToken', res.refreshToken);
    localStorage.setItem('admin_user', JSON.stringify(res.user));
    this.currentUser$.next(res.user);
  }

  private loadFromStorage(): void {
    const userJson = localStorage.getItem('admin_user');
    if (userJson) {
      this.currentUser$.next(JSON.parse(userJson));
    }
  }

  private decodeRole(token: string): string | null {
    return this.decodeToken(token)?.role ?? null;
  }

  private decodeToken(token: string): JwtPayload | null {
    try {
      const payload = token.split('.')[1];
      const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(decoded) as JwtPayload;
    } catch {
      return null;
    }
  }
}
