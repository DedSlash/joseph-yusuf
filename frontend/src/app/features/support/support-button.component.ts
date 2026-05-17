import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { SupportService } from '../../core/services/support.service';
import {
  KnowledgeArticle,
  TICKET_CATEGORY_LABELS,
  TicketCategory,
  TicketPriority
} from '../../shared/models/support.model';

type Step = 'search' | 'article' | 'form' | 'success';

@Component({
  selector: 'app-support-button',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <button class="support-fab"
            type="button"
            (click)="open()"
            aria-label="Centre d'aide">
      <span aria-hidden="true">?</span>
    </button>

    <div class="support-overlay" *ngIf="isOpen()" (click)="close()">
      <div class="support-modal" (click)="$event.stopPropagation()">
        <header class="support-modal-head">
          <h2>Centre d'aide</h2>
          <button class="support-close" type="button" (click)="close()" aria-label="Fermer">×</button>
        </header>

        <ng-container [ngSwitch]="step()">
          <!-- FAQ search -->
          <section *ngSwitchCase="'search'" class="support-modal-body">
            <p class="support-hint">Recherchez d'abord dans notre base de connaissances.</p>
            <input class="support-input"
                   type="text"
                   placeholder="Décrivez votre problème en quelques mots…"
                   [(ngModel)]="query"
                   (ngModelChange)="onSearch($event)"
                   autofocus />

            <div class="support-results" *ngIf="results().length > 0">
              <button *ngFor="let a of results()"
                      type="button"
                      class="support-result"
                      (click)="openArticle(a)">
                <strong>{{ a.title }}</strong>
                <small>{{ categoryLabel(a.category) }} · {{ a.views }} vues</small>
              </button>
            </div>

            <p class="support-empty" *ngIf="searched() && results().length === 0">
              Aucun article trouvé pour « {{ query }} ».
            </p>

            <div class="support-actions">
              <button class="support-btn support-btn-primary"
                      type="button"
                      (click)="goToForm()">
                Mon problème n'est pas résolu
              </button>
            </div>
          </section>

          <!-- Article view -->
          <section *ngSwitchCase="'article'" class="support-modal-body">
            <button class="support-link" type="button" (click)="back()">← Retour</button>
            <h3>{{ selectedArticle()?.title }}</h3>
            <div class="support-article-meta">
              {{ categoryLabel(selectedArticle()?.category) }}
            </div>
            <article class="support-article">{{ selectedArticle()?.content }}</article>
            <div class="support-actions">
              <button class="support-btn support-btn-ghost" type="button" (click)="back()">Retour aux résultats</button>
              <button class="support-btn support-btn-primary" type="button" (click)="goToForm()">
                Ça ne résout pas mon problème
              </button>
            </div>
          </section>

          <!-- Ticket form -->
          <section *ngSwitchCase="'form'" class="support-modal-body">
            <button class="support-link" type="button" (click)="back()">← Retour</button>
            <h3>Ouvrir un ticket</h3>
            <form [formGroup]="form" (ngSubmit)="submitTicket()">
              <label class="support-label">
                Sujet
                <input class="support-input" type="text" formControlName="subject" maxlength="255" />
              </label>
              <label class="support-label">
                Catégorie
                <select class="support-input" formControlName="category">
                  <option *ngFor="let c of categories" [value]="c">{{ categoryLabel(c) }}</option>
                </select>
              </label>
              <label class="support-label">
                Priorité
                <select class="support-input" formControlName="priority">
                  <option value="LOW">Basse</option>
                  <option value="NORMAL">Normale</option>
                  <option value="HIGH">Haute</option>
                  <option value="URGENT">Urgente</option>
                </select>
              </label>
              <label class="support-label">
                Message
                <textarea class="support-input" rows="6" formControlName="message"></textarea>
              </label>

              <div class="support-error" *ngIf="errorMessage()">{{ errorMessage() }}</div>

              <div class="support-actions">
                <button class="support-btn support-btn-ghost" type="button" (click)="back()">Annuler</button>
                <button class="support-btn support-btn-primary"
                        type="submit"
                        [disabled]="form.invalid || submitting()">
                  {{ submitting() ? 'Envoi…' : 'Envoyer' }}
                </button>
              </div>
            </form>
          </section>

          <!-- Success -->
          <section *ngSwitchCase="'success'" class="support-modal-body">
            <div class="support-success">
              <div class="support-success-icon">✓</div>
              <h3>Ticket envoyé</h3>
              <p>Notre équipe vous répondra par email. Vous pouvez aussi suivre ce ticket depuis votre espace support.</p>
              <div class="support-actions">
                <button class="support-btn support-btn-ghost" type="button" (click)="close()">Fermer</button>
                <button class="support-btn support-btn-primary" type="button" (click)="viewMyTickets()">
                  Voir mes tickets
                </button>
              </div>
            </div>
          </section>
        </ng-container>
      </div>
    </div>
  `,
  styles: [`
    .support-fab {
      position: fixed;
      right: 1.5rem;
      bottom: 1.5rem;
      width: 56px;
      height: 56px;
      border-radius: 50%;
      border: none;
      background: var(--gold, #C9A84C);
      color: #1a1a1a;
      font-size: 1.8rem;
      font-weight: 700;
      cursor: pointer;
      box-shadow: 0 6px 18px rgba(0,0,0,0.35);
      z-index: 980;
      transition: transform 0.15s ease;
    }
    .support-fab:hover { transform: scale(1.05); }
    .support-overlay {
      position: fixed; inset: 0;
      background: rgba(0,0,0,0.6);
      display: flex; align-items: center; justify-content: center;
      z-index: 990; padding: 1rem;
    }
    .support-modal {
      background: var(--surface, #1f1f1f);
      color: var(--text, #e8e8e8);
      border: 1px solid var(--border-gold, rgba(201,168,76,0.3));
      border-radius: 12px;
      width: 100%; max-width: 560px;
      max-height: 85vh; overflow: hidden;
      display: flex; flex-direction: column;
    }
    .support-modal-head {
      display: flex; justify-content: space-between; align-items: center;
      padding: 1rem 1.25rem;
      border-bottom: 1px solid var(--border-gold, rgba(201,168,76,0.2));
    }
    .support-modal-head h2 { margin: 0; font-size: 1.15rem; color: var(--gold, #C9A84C); }
    .support-close {
      background: none; border: none; color: var(--text, #e8e8e8);
      font-size: 1.6rem; cursor: pointer; line-height: 1;
    }
    .support-modal-body { padding: 1.25rem; overflow-y: auto; }
    .support-hint { color: var(--text-dim, #999); margin: 0 0 0.75rem 0; font-size: 0.9rem; }
    .support-input {
      width: 100%; padding: 0.6rem 0.75rem;
      background: var(--bg, #111); color: var(--text, #e8e8e8);
      border: 1px solid var(--border-gold, rgba(201,168,76,0.3));
      border-radius: 6px; font-size: 0.95rem;
    }
    .support-label { display: flex; flex-direction: column; gap: 0.3rem; margin-bottom: 0.85rem; font-size: 0.85rem; color: var(--text-dim, #999); }
    .support-results { display: flex; flex-direction: column; gap: 0.5rem; margin-top: 0.9rem; }
    .support-result {
      text-align: left; background: var(--bg, #111);
      border: 1px solid var(--border-gold, rgba(201,168,76,0.2));
      padding: 0.7rem 0.9rem; border-radius: 6px;
      color: var(--text, #e8e8e8); cursor: pointer;
      display: flex; flex-direction: column; gap: 0.2rem;
    }
    .support-result strong { color: var(--gold, #C9A84C); }
    .support-result small { color: var(--text-dim, #999); font-size: 0.75rem; }
    .support-empty { margin-top: 1rem; color: var(--text-dim, #999); font-size: 0.85rem; }
    .support-actions { display: flex; justify-content: flex-end; gap: 0.6rem; margin-top: 1.25rem; flex-wrap: wrap; }
    .support-btn {
      padding: 0.55rem 1rem; border-radius: 6px; cursor: pointer;
      border: 1px solid var(--gold, #C9A84C); font-size: 0.9rem; font-weight: 500;
    }
    .support-btn-primary { background: var(--gold, #C9A84C); color: #1a1a1a; }
    .support-btn-ghost { background: transparent; color: var(--gold, #C9A84C); }
    .support-btn:disabled { opacity: 0.55; cursor: not-allowed; }
    .support-link { background: none; border: none; color: var(--gold, #C9A84C); cursor: pointer; padding: 0; margin-bottom: 0.8rem; }
    .support-article-meta { color: var(--text-dim, #999); font-size: 0.8rem; margin-bottom: 0.6rem; }
    .support-article { white-space: pre-wrap; line-height: 1.5; font-size: 0.95rem; }
    .support-error { color: #e57373; margin-top: 0.5rem; font-size: 0.85rem; }
    .support-success { text-align: center; padding: 1.5rem 0.5rem; }
    .support-success-icon {
      width: 56px; height: 56px; border-radius: 50%;
      background: var(--gold, #C9A84C); color: #1a1a1a;
      font-size: 2rem; display: inline-flex; align-items: center; justify-content: center;
      margin-bottom: 0.8rem;
    }
  `]
})
export class SupportButtonComponent {
  private readonly support = inject(SupportService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly categories: TicketCategory[] = ['ACCOUNT', 'INCOME', 'SUBSCRIPTION', 'RULES', 'TECHNICAL', 'OTHER'];

  isOpen = signal(false);
  step = signal<Step>('search');
  query = '';
  results = signal<KnowledgeArticle[]>([]);
  selectedArticle = signal<KnowledgeArticle | null>(null);
  searched = signal(false);
  submitting = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    subject: ['', [Validators.required, Validators.maxLength(255)]],
    category: ['OTHER' as TicketCategory, Validators.required],
    priority: ['NORMAL' as TicketPriority, Validators.required],
    message: ['', Validators.required]
  });

  private searchTimer: ReturnType<typeof setTimeout> | null = null;

  open(): void {
    this.isOpen.set(true);
    this.step.set('search');
    this.query = '';
    this.results.set([]);
    this.searched.set(false);
    this.errorMessage.set(null);
  }

  close(): void {
    this.isOpen.set(false);
  }

  back(): void {
    this.step.set('search');
    this.errorMessage.set(null);
  }

  onSearch(q: string): void {
    if (this.searchTimer) clearTimeout(this.searchTimer);
    if (!q || q.trim().length < 2) {
      this.results.set([]);
      this.searched.set(false);
      return;
    }
    this.searchTimer = setTimeout(() => {
      this.support.searchKnowledge(q.trim()).subscribe({
        next: items => { this.results.set(items); this.searched.set(true); },
        error: () => { this.results.set([]); this.searched.set(true); }
      });
    }, 280);
  }

  openArticle(a: KnowledgeArticle): void {
    this.selectedArticle.set(a);
    this.step.set('article');
    this.support.getArticle(a.id).subscribe({ next: full => this.selectedArticle.set(full), error: () => {} });
  }

  goToForm(): void {
    this.step.set('form');
    if (this.query) {
      this.form.patchValue({ subject: this.query.slice(0, 120) });
    }
  }

  submitTicket(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.support.createTicket(this.form.getRawValue()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.step.set('success');
        this.form.reset({ subject: '', category: 'OTHER', priority: 'NORMAL', message: '' });
      },
      error: err => {
        this.submitting.set(false);
        this.errorMessage.set(err?.error?.message || 'Erreur à l\'envoi du ticket. Réessayez.');
      }
    });
  }

  viewMyTickets(): void {
    this.close();
    this.router.navigate(['/support']);
  }

  categoryLabel(c: TicketCategory | undefined): string {
    return c ? TICKET_CATEGORY_LABELS[c] : '';
  }
}
