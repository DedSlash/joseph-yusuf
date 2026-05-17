import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AddResponseRequest,
  ArticleRequest,
  KnowledgeArticle,
  PageResponse,
  Ticket,
  TicketCategory,
  TicketStatus
} from '../../shared/models/support.model';

@Injectable({ providedIn: 'root' })
export class AdminSupportService {
  private readonly apiUrl = `${environment.apiUrl}/api/support/admin`;
  private readonly publicUrl = `${environment.apiUrl}/api/support`;

  constructor(private http: HttpClient) {}

  // Tickets
  listTickets(page = 0, size = 20, status?: TicketStatus, category?: TicketCategory): Observable<PageResponse<Ticket>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (category) params = params.set('category', category);
    return this.http.get<PageResponse<Ticket>>(`${this.apiUrl}/tickets`, { params });
  }

  getTicket(id: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.apiUrl}/tickets/${id}`);
  }

  respond(id: string, message: string, userEmail?: string): Observable<Ticket> {
    let params = new HttpParams();
    if (userEmail) params = params.set('userEmail', userEmail);
    const body: AddResponseRequest = { message };
    return this.http.post<Ticket>(`${this.apiUrl}/tickets/${id}/responses`, body, { params });
  }

  updateStatus(id: string, status: TicketStatus): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.apiUrl}/tickets/${id}/status`, { status });
  }

  countOpen(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/tickets/stats/open-count`);
  }

  // Knowledge base (CRUD)
  listArticles(): Observable<KnowledgeArticle[]> {
    return this.http.get<KnowledgeArticle[]>(`${this.publicUrl}/knowledge/public/list?page=0&size=200`);
  }

  createArticle(req: ArticleRequest): Observable<KnowledgeArticle> {
    return this.http.post<KnowledgeArticle>(`${this.apiUrl}/knowledge`, req);
  }

  updateArticle(id: string, req: ArticleRequest): Observable<KnowledgeArticle> {
    return this.http.put<KnowledgeArticle>(`${this.apiUrl}/knowledge/${id}`, req);
  }

  deleteArticle(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/knowledge/${id}`);
  }
}
