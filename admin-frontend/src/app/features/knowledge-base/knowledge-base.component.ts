import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminSupportService } from '../../core/services/admin-support.service';
import {
  ArticleRequest,
  KnowledgeArticle,
  TICKET_CATEGORY_LABELS,
  TicketCategory
} from '../../shared/models/support.model';

@Component({
  selector: 'admin-knowledge-base',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DatePipe],
  template: `
    <div class="page-head">
      <div>
        <h1>Base de connaissances</h1>
        <p class="subtitle">Articles FAQ visibles dans le centre d'aide utilisateur.</p>
      </div>
      <button class="btn btn-primary" (click)="openCreate()">+ Nouvel article</button>
    </div>

    <div *ngIf="errorMessage()" class="alert error">{{ errorMessage() }}</div>
    <div *ngIf="successMessage()" class="alert success">{{ successMessage() }}</div>

    <div class="card">
      <table class="data-table">
        <thead>
          <tr>
            <th>Titre</th>
            <th>Catégorie</th>
            <th>Vues</th>
            <th>Statut</th>
            <th>Mis à jour</th>
            <th style="text-align: right;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let a of articles(); trackBy: trackById">
            <td><strong>{{ a.title }}</strong></td>
            <td>{{ categoryLabel(a.category) }}</td>
            <td>{{ a.views }}</td>
            <td>
              <span class="badge" [class.success]="a.active" [class.disabled]="!a.active">
                {{ a.active ? 'Actif' : 'Inactif' }}
              </span>
            </td>
            <td>{{ a.updatedAt | date:'dd/MM/yyyy HH:mm' }}</td>
            <td style="text-align: right;">
              <button class="btn btn-ghost mini" (click)="edit(a)">Modifier</button>
              <button class="btn btn-ghost mini" (click)="remove(a)" [disabled]="busyId() === a.id">
                Supprimer
              </button>
            </td>
          </tr>
          <tr *ngIf="!loading() && articles().length === 0">
            <td colspan="6" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Aucun article
            </td>
          </tr>
          <tr *ngIf="loading()">
            <td colspan="6" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Chargement…
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="modal-backdrop" *ngIf="showForm()" (click)="closeForm()">
      <div class="modal" (click)="$event.stopPropagation()">
        <header>
          <h2>{{ editing() ? 'Modifier l\\'article' : 'Nouvel article' }}</h2>
          <button class="close" (click)="closeForm()">×</button>
        </header>
        <form [formGroup]="form" (ngSubmit)="save()">
          <label>
            Titre
            <input class="input" type="text" formControlName="title" maxlength="255" />
          </label>
          <label>
            Catégorie
            <select class="input" formControlName="category">
              <option *ngFor="let c of categories" [value]="c">{{ categoryLabel(c) }}</option>
            </select>
          </label>
          <label>
            Tags (séparés par virgule)
            <input class="input" type="text" formControlName="tags" maxlength="500" />
          </label>
          <label>
            Contenu
            <textarea class="input" rows="10" formControlName="content"></textarea>
          </label>
          <label class="checkbox">
            <input type="checkbox" formControlName="active" /> Article actif (visible côté user)
          </label>
          <div class="actions">
            <button class="btn btn-ghost" type="button" (click)="closeForm()">Annuler</button>
            <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving()">
              {{ saving() ? 'Enregistrement…' : (editing() ? 'Mettre à jour' : 'Créer') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .modal-backdrop {
      position: fixed; inset: 0; background: rgba(0,0,0,0.6);
      display: flex; align-items: center; justify-content: center;
      z-index: 950; padding: 1rem;
    }
    .modal {
      background: var(--surface, #1f1f1f); color: var(--text);
      border: 1px solid var(--border-gold); border-radius: 12px;
      width: 100%; max-width: 640px; max-height: 90vh; overflow: auto;
    }
    .modal header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 1rem 1.2rem; border-bottom: 1px solid var(--border-gold);
    }
    .modal header h2 { margin: 0; font-size: 1.1rem; color: var(--gold); }
    .close { background: none; border: none; color: var(--text); font-size: 1.5rem; cursor: pointer; }
    .modal form { padding: 1.2rem; }
    .modal label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.8rem; color: var(--text-dim); margin-bottom: 0.85rem; }
    .modal label.checkbox { flex-direction: row; align-items: center; gap: 0.5rem; }
    .input {
      width: 100%; padding: 0.55rem 0.7rem;
      background: var(--bg, #111); color: var(--text);
      border: 1px solid var(--border-gold); border-radius: 6px;
      font-family: inherit;
    }
    .actions { display: flex; justify-content: flex-end; gap: 0.6rem; margin-top: 1rem; }
  `]
})
export class KnowledgeBaseComponent implements OnInit {
  private readonly api = inject(AdminSupportService);
  private readonly fb = inject(FormBuilder);

  readonly categories: TicketCategory[] = ['ACCOUNT', 'INCOME', 'SUBSCRIPTION', 'RULES', 'TECHNICAL', 'OTHER'];

  articles = signal<KnowledgeArticle[]>([]);
  loading = signal(true);
  saving = signal(false);
  busyId = signal<string | null>(null);
  showForm = signal(false);
  editing = signal<KnowledgeArticle | null>(null);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    content: ['', Validators.required],
    category: ['OTHER' as TicketCategory, Validators.required],
    tags: [''],
    active: [true]
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.api.listArticles().subscribe({
      next: items => { this.articles.set(items); this.loading.set(false); },
      error: () => { this.articles.set([]); this.loading.set(false); }
    });
  }

  openCreate(): void {
    this.editing.set(null);
    this.form.reset({ title: '', content: '', category: 'OTHER', tags: '', active: true });
    this.showForm.set(true);
  }

  edit(a: KnowledgeArticle): void {
    this.editing.set(a);
    this.form.reset({
      title: a.title, content: a.content,
      category: a.category, tags: a.tags ?? '', active: a.active
    });
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editing.set(null);
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.errorMessage.set(null);
    const payload: ArticleRequest = this.form.getRawValue();
    const editing = this.editing();
    const obs = editing
      ? this.api.updateArticle(editing.id, payload)
      : this.api.createArticle(payload);

    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.successMessage.set(editing ? 'Article mis à jour' : 'Article créé');
        setTimeout(() => this.successMessage.set(null), 3000);
        this.closeForm();
        this.reload();
      },
      error: err => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message || 'Erreur à l\'enregistrement.');
      }
    });
  }

  remove(a: KnowledgeArticle): void {
    if (!confirm(`Supprimer l'article « ${a.title} » ?`)) return;
    this.busyId.set(a.id);
    this.api.deleteArticle(a.id).subscribe({
      next: () => { this.busyId.set(null); this.successMessage.set('Article supprimé'); setTimeout(() => this.successMessage.set(null), 3000); this.reload(); },
      error: err => {
        this.busyId.set(null);
        this.errorMessage.set(err?.error?.message || 'Erreur à la suppression.');
      }
    });
  }

  trackById(_: number, a: KnowledgeArticle): string { return a.id; }
  categoryLabel(c: TicketCategory): string { return TICKET_CATEGORY_LABELS[c]; }
}
