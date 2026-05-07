import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AllocationResult, CalculateRequest, RuleAvailability, UserRuleConfig, UserRuleConfigRequest } from '../../shared/models/rule.model';

@Injectable({ providedIn: 'root' })
export class RuleService {
  private readonly apiUrl = `${environment.apiUrl}/api/rules`;

  constructor(private http: HttpClient) {}

  calculate(request: CalculateRequest): Observable<AllocationResult> {
    return this.http.post<AllocationResult>(`${this.apiUrl}/calculate`, request);
  }

  calculateCurrent(): Observable<AllocationResult> {
    return this.http.get<AllocationResult>(`${this.apiUrl}/calculate/current`);
  }

  getConfig(): Observable<UserRuleConfig> {
    return this.http.get<UserRuleConfig>(`${this.apiUrl}/config`);
  }

  updateConfig(request: UserRuleConfigRequest): Observable<UserRuleConfig> {
    return this.http.put<UserRuleConfig>(`${this.apiUrl}/config`, request);
  }

  getAvailableRules(): Observable<RuleAvailability[]> {
    return this.http.get<RuleAvailability[]>(`${this.apiUrl}/available`);
  }
}
