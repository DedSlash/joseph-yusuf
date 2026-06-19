import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, shareReplay, tap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { IncomeService } from './income.service';
import { IncomeSource } from '../../shared/models/income.model';

/**
 * Source unique de vérité pour l'affichage des montants côté UI :
 * - Les montants sont stockés et calculés en XOF côté backend
 * - On les convertit en devise utilisateur uniquement pour l'affichage
 * - La devise d'affichage = devise de la source de revenu la plus ancienne
 *   du user (fallback : user.currency, puis XOF). Ça marche sans backfill
 *   pour les users existants qui ont déjà des sources.
 */
@Injectable({ providedIn: 'root' })
export class CurrencyDisplayService {
  private readonly ratesUrl = `${environment.apiUrl}/api/incomes/currencies/rates`;

  private readonly displayCurrencySubject = new BehaviorSubject<string>('XOF');
  readonly displayCurrency$ = this.displayCurrencySubject.asObservable();

  private rates: Record<string, number> = { XOF: 1 };
  private rates$: Observable<Record<string, number>> | null = null;
  private oldestSourceCurrency: string | null = null;
  private userCurrency: string = 'XOF';

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService,
    private readonly incomeService: IncomeService
  ) {
    this.authService.currentUser$.subscribe(user => {
      this.userCurrency = (user?.currency ?? 'XOF').toUpperCase();
      this.recomputeDisplay();
      if (user) this.refreshSources();
    });
    this.incomeService.incomeUpdated$.subscribe(() => this.refreshSources());
    this.loadRates().subscribe();
  }

  private refreshSources(): void {
    this.incomeService.getSources().subscribe({
      next: (sources) => {
        const oldest = this.pickOldestActive(sources);
        this.oldestSourceCurrency = oldest ? oldest.currency.toUpperCase() : null;
        this.recomputeDisplay();
      },
      error: () => { /* offline / unauthenticated : on garde la devise courante */ }
    });
  }

  private pickOldestActive(sources: IncomeSource[]): IncomeSource | null {
    const active = sources.filter(s => s.active && s.currency);
    if (active.length === 0) return null;
    return active.reduce<IncomeSource | null>((oldest, s) => {
      if (!oldest) return s;
      return (s.createdAt && oldest.createdAt && s.createdAt < oldest.createdAt) ? s : oldest;
    }, null);
  }

  private recomputeDisplay(): void {
    const next = this.oldestSourceCurrency ?? this.userCurrency ?? 'XOF';
    if (next !== this.displayCurrencySubject.value) {
      this.displayCurrencySubject.next(next);
    }
  }

  loadRates(): Observable<Record<string, number>> {
    if (!this.rates$) {
      this.rates$ = this.http.get<Record<string, number>>(this.ratesUrl).pipe(
        tap(rates => { this.rates = { ...this.rates, ...this.normalize(rates) }; }),
        catchError(() => of(this.rates)),
        shareReplay(1)
      );
    }
    return this.rates$;
  }

  get displayCurrency(): string {
    return this.displayCurrencySubject.value;
  }

  fromXof(amountXof: number, currency?: string): number {
    const code = (currency ?? this.displayCurrency).toUpperCase();
    const rate = this.rates[code] ?? 1;
    return amountXof / rate;
  }

  formatAmount(amountXof: number | null | undefined, currency?: string): string {
    const code = (currency ?? this.displayCurrency).toUpperCase();
    const value = this.fromXof(amountXof ?? 0, code);
    const locale = this.localeFor(code);
    const fractionDigits = this.zeroDecimal(code) ? 0 : 2;
    try {
      return new Intl.NumberFormat(locale, {
        style: 'currency',
        currency: code,
        maximumFractionDigits: fractionDigits,
        minimumFractionDigits: 0
      }).format(value);
    } catch {
      const rounded = value.toLocaleString(locale, { maximumFractionDigits: fractionDigits });
      return `${rounded} ${code}`;
    }
  }

  private normalize(raw: Record<string, unknown>): Record<string, number> {
    const out: Record<string, number> = {};
    Object.entries(raw).forEach(([k, v]) => {
      const n = typeof v === 'number' ? v : Number(v);
      if (Number.isFinite(n) && n > 0) out[k.toUpperCase()] = n;
    });
    return out;
  }

  private localeFor(code: string): string {
    switch (code) {
      case 'XOF':
      case 'XAF': return 'fr-SN';
      case 'EUR': return 'fr-FR';
      case 'USD': return 'en-US';
      case 'GBP': return 'en-GB';
      case 'CAD': return 'fr-CA';
      case 'MAD': return 'fr-MA';
      case 'CHF': return 'fr-CH';
      default:    return 'fr-FR';
    }
  }

  private zeroDecimal(code: string): boolean {
    return code === 'XOF' || code === 'XAF';
  }
}
