import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApiService } from '../../core/services/admin-api.service';
import { PromoCode, PromoCodeStats } from '../../shared/models/admin.model';

@Component({
  selector: 'admin-promo-codes',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DatePipe, DecimalPipe],
  template: `
    <div class="page-head">
      <div>
        <h1>Codes promo</h1>
        <p class="subtitle">Création et gestion des codes de réduction Stripe</p>
      </div>
      <button class="btn btn-primary" (click)="openCreate()">+ Nouveau code</button>
    </div>

    <div *ngIf="errorMessage()" class="alert error">{{ errorMessage() }}</div>
    <div *ngIf="successMessage()" class="alert success">{{ successMessage() }}</div>

    <div class="card">
      <div class="filters">
        <select class="select" [(ngModel)]="activeFilter" (change)="reload()" style="max-width: 200px;">
          <option [ngValue]="''">Tous</option>
          <option [ngValue]="'true'">Actifs</option>
          <option [ngValue]="'false'">Inactifs</option>
        </select>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>Code</th>
            <th>Description</th>
            <th>Réduction</th>
            <th>Utilisations</th>
            <th>Expire</th>
            <th>Statut</th>
            <th style="text-align: right;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let p of codes(); trackBy: trackById">
            <td><strong>{{ p.code }}</strong></td>
            <td>{{ p.description || '—' }}</td>
            <td>{{ p.discountPercent }} %</td>
            <td>{{ p.usedCount }} / {{ p.maxUses ?? '∞' }}</td>
            <td>{{ p.expiresAt ? (p.expiresAt | date:'dd/MM/yyyy') : '—' }}</td>
            <td>
              <span class="badge" [class.success]="p.active" [class.disabled]="!p.active">
                {{ p.active ? 'Actif' : 'Inactif' }}
              </span>
            </td>
            <td style="text-align: right;">
              <button class="btn btn-ghost mini" (click)="showStats(p)">Stats</button>
              <button class="btn btn-ghost mini" (click)="toggle(p)" [disabled]="busyId() === p.id">
                {{ p.active ? 'Désactiver' : 'Activer' }}
              </button>
            </td>
          </tr>
          <tr *ngIf="!loading() && codes().length === 0">
            <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Aucun code promo
            </td>
          </tr>
          <tr *ngIf="loading()">
            <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Chargement…
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span>{{ totalElements() }} codes</span>
        <button class="btn btn-ghost mini" (click)="prevPage()" [disabled]="page() === 0">Précédent</button>
        <span>Page {{ page() + 1 }} / {{ totalPages() || 1 }}</span>
        <button class="btn btn-ghost mini" (click)="nextPage()"
                [disabled]="page() + 1 >= totalPages()">Suivant</button>
      </div>
    </div>

    <div class="modal-backdrop" *ngIf="showCreateForm()" (click)="closeCreate()">
      <div class="modal" (click)="$event.stopPropagation()">
        <h3>Créer un code promo</h3>
        <form [formGroup]="createForm" (ngSubmit)="submitCreate()">
          <div class="form-row">
            <label>Code</label>
            <input class="input" formControlName="code" placeholder="JOSEPH20" />
            <small *ngIf="invalid('code')" class="error-text">Code requis (3 à 50 caractères)</small>
          </div>
          <div class="form-row">
            <label>Description</label>
            <input class="input" formControlName="description" placeholder="Lancement v1" />
          </div>
          <div class="form-row">
            <label>Réduction (%)</label>
            <input class="input" type="number" formControlName="discountPercent" min="1" max="100" />
            <small *ngIf="invalid('discountPercent')" class="error-text">Entre 1 et 100</small>
          </div>
          <div class="form-row">
            <label>Limite d'utilisations (vide = illimité)</label>
            <input class="input" type="number" formControlName="maxUses" min="1" />
          </div>
          <div class="form-row">
            <label>Date d'expiration</label>
            <input class="input" type="datetime-local" formControlName="expiresAt" />
          </div>

          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="closeCreate()">Annuler</button>
            <button type="submit" class="btn btn-primary"
                    [disabled]="createForm.invalid || creating()">
              {{ creating() ? 'Création…' : 'Créer' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div class="modal-backdrop" *ngIf="statsView() as s" (click)="closeStats()">
      <div class="modal" (click)="$event.stopPropagation()">
        <h3>Statistiques · {{ s.code }}</h3>
        <dl class="stats-list">
          <div><dt>Utilisations</dt><dd>{{ s.totalUsages }} / {{ s.maxUses ?? '∞' }}</dd></div>
          <div><dt>Économies générées</dt><dd>{{ s.estimatedSavings | number:'1.0-2' }} €</dd></div>
          <div><dt>Statut</dt>
            <dd>
              <span class="badge" [class.success]="s.active" [class.disabled]="!s.active">
                {{ s.active ? 'Actif' : 'Inactif' }}
              </span>
            </dd>
          </div>
        </dl>
        <div class="modal-actions">
          <button class="btn btn-primary" (click)="closeStats()">Fermer</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1.5rem;
    }
    .mini { padding: 0.3rem 0.6rem; font-size: 0.75rem; margin-left: 0.3rem; }
    h3 { font-size: 1.1rem; color: var(--gold); margin-bottom: 0.6rem; }
    .error-text { color: var(--status-error); font-size: 0.75rem; display: block; margin-top: 0.3rem; }

    .modal-backdrop {
      position: fixed; inset: 0;
      background: rgba(13, 11, 7, 0.7);
      display: flex; align-items: center; justify-content: center;
      z-index: 100;
    }
    .modal {
      background: var(--night-mid);
      border: 1px solid var(--border-gold);
      border-radius: 12px;
      padding: 1.5rem;
      width: 100%; max-width: 480px;
    }
    .modal-actions {
      display: flex; justify-content: flex-end; gap: 0.5rem;
      margin-top: 1.25rem;
    }

    .stats-list { display: flex; flex-direction: column; gap: 0.5rem; }
    .stats-list > div {
      display: grid;
      grid-template-columns: 160px 1fr;
      align-items: center;
      padding: 0.5rem 0;
      border-bottom: 1px solid rgba(201, 168, 76, 0.06);
    }
    .stats-list dt { color: var(--text-dim); font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.05em; }
    .stats-list dd { color: var(--text); font-size: 0.92rem; }
  `]
})
export class PromoCodesComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly codes = signal<PromoCode[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly busyId = signal<string | null>(null);
  protected readonly creating = signal(false);

  protected readonly page = signal(0);
  protected readonly size = signal(20);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  protected readonly showCreateForm = signal(false);
  protected readonly statsView = signal<PromoCodeStats | null>(null);

  protected activeFilter: '' | 'true' | 'false' = '';

  protected readonly createForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    description: [''],
    discountPercent: [10, [Validators.required, Validators.min(1), Validators.max(100)]],
    maxUses: [null as number | null],
    expiresAt: ['']
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const active = this.activeFilter === '' ? undefined : this.activeFilter === 'true';
    this.api.listPromoCodes(this.page(), this.size(), active).subscribe({
      next: page => {
        this.codes.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les codes promo');
        this.loading.set(false);
      }
    });
  }

  prevPage(): void {
    if (this.page() === 0) return;
    this.page.set(this.page() - 1);
    this.reload();
  }

  nextPage(): void {
    if (this.page() + 1 >= this.totalPages()) return;
    this.page.set(this.page() + 1);
    this.reload();
  }

  openCreate(): void {
    this.createForm.reset({
      code: '',
      description: '',
      discountPercent: 10,
      maxUses: null,
      expiresAt: ''
    });
    this.showCreateForm.set(true);
  }

  closeCreate(): void {
    this.showCreateForm.set(false);
  }

  invalid(field: string): boolean {
    const c = this.createForm.get(field);
    return !!(c && c.touched && c.invalid);
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.creating.set(true);
    const raw = this.createForm.getRawValue();
    const payload = {
      code: raw.code.trim().toUpperCase(),
      description: raw.description?.trim() || undefined,
      discountPercent: raw.discountPercent,
      maxUses: raw.maxUses ?? undefined,
      expiresAt: raw.expiresAt ? new Date(raw.expiresAt).toISOString() : undefined,
      active: true
    };
    this.api.createPromoCode(payload).subscribe({
      next: created => {
        this.creating.set(false);
        this.showCreateForm.set(false);
        this.codes.update(list => [created, ...list]);
        this.totalElements.update(n => n + 1);
        this.flash(`Code ${created.code} créé`);
      },
      error: err => {
        this.creating.set(false);
        const msg = err?.error?.message ?? 'Échec de la création';
        this.errorMessage.set(msg);
        setTimeout(() => this.errorMessage.set(null), 4000);
      }
    });
  }

  toggle(p: PromoCode): void {
    this.busyId.set(p.id);
    this.api.togglePromoCode(p.id).subscribe({
      next: updated => {
        this.codes.update(list => list.map(c => c.id === updated.id ? updated : c));
        this.busyId.set(null);
        this.flash(`Code ${updated.code} ${updated.active ? 'activé' : 'désactivé'}`);
      },
      error: () => {
        this.busyId.set(null);
        this.errorMessage.set('Échec du changement de statut');
        setTimeout(() => this.errorMessage.set(null), 4000);
      }
    });
  }

  showStats(p: PromoCode): void {
    this.api.promoCodeStats(p.id).subscribe({
      next: stats => this.statsView.set(stats),
      error: () => this.errorMessage.set('Impossible de charger les stats')
    });
  }

  closeStats(): void {
    this.statsView.set(null);
  }

  trackById(_: number, p: PromoCode): string { return p.id; }

  private flash(message: string): void {
    this.successMessage.set(message);
    setTimeout(() => this.successMessage.set(null), 3000);
  }
}
