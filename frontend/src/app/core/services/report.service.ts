import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ReportResponse {
  id: string;
  type: string;
  month: number | null;
  year: number;
  fileName: string;
  generatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly apiUrl = `${environment.apiUrl}/api/reports`;

  constructor(private readonly http: HttpClient) {}

  generateMonthly(month: number, year: number): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.apiUrl}/monthly`, { month, year });
  }

  generateAnnual(year: number): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.apiUrl}/annual`, { year });
  }

  list(): Observable<{ content: ReportResponse[] }> {
    return this.http.get<{ content: ReportResponse[] }>(this.apiUrl);
  }

  download(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}`, { responseType: 'blob' });
  }
}
