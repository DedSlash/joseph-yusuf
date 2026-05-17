import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AddResponseRequest,
  CreateTicketRequest,
  KnowledgeArticle,
  PageResponse,
  Ticket,
  TicketCategory
} from '../../shared/models/support.model';

@Injectable({ providedIn: 'root' })
export class SupportService {
  private readonly apiUrl = `${environment.apiUrl}/api/support`;

  constructor(private http: HttpClient) {}

  // Tickets
  createTicket(request: CreateTicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.apiUrl}/tickets`, request);
  }

  listMyTickets(page = 0, size = 20): Observable<PageResponse<Ticket>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<Ticket>>(`${this.apiUrl}/tickets`, { params });
  }

  getTicket(id: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.apiUrl}/tickets/${id}`);
  }

  addResponse(id: string, request: AddResponseRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.apiUrl}/tickets/${id}/responses`, request);
  }

  // Knowledge base
  searchKnowledge(query: string): Observable<KnowledgeArticle[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<KnowledgeArticle[]>(`${this.apiUrl}/knowledge/search`, { params });
  }

  listByCategory(category: TicketCategory): Observable<KnowledgeArticle[]> {
    return this.http.get<KnowledgeArticle[]>(`${this.apiUrl}/knowledge/category/${category}`);
  }

  listPublic(page = 0, size = 20): Observable<KnowledgeArticle[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<KnowledgeArticle[]>(`${this.apiUrl}/knowledge/public/list`, { params });
  }

  getArticle(id: string): Observable<KnowledgeArticle> {
    return this.http.get<KnowledgeArticle>(`${this.apiUrl}/knowledge/public/${id}`);
  }
}
