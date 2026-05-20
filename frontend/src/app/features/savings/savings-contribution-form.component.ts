import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { SavingsService } from '../../core/services/savings.service';
import { SavingsContribution, SavingsContributionRequest, SavingsGoal } from '../../shared/models/savings.model';

@Component({
  selector: 'app-savings-contribution-form',
  standalone: true,
  imports: [CommonModule, FormsModule, DialogModule],
  template: `
    <p-dialog
      [visible]="visible"
      (visibleChange)="onVisibleChange($event)"
      [modal]="true"
      [closable]="true"
      [draggable]="false"
      [resizable]="false"
      [style]="{ width: '480px' }"
      [header]="'Ajouter un versement — ' + (goal?.name || '')">
      <form (ngSubmit)="submit()" #f="ngForm" class="savings-form">
        <label class="field">
          <span>Montant (XOF)</span>
          <input type="number" name="amount" [(ngModel)]="model.amount" required min="1" />
        </label>

        <div class="row">
          <label class="field">
            <span>Mois</span>
            <input type="number" name="month" [(ngModel)]="model.month" min="1" max="12" />
          </label>
          <label class="field">
            <span>Année</span>
            <input type="number" name="year" [(ngModel)]="model.year" min="2020" />
          </label>
        </div>

        <label class="field">
          <span>Note (optionnel)</span>
          <input type="text" name="note" [(ngModel)]="model.note" maxlength="255" />
        </label>

        <div *ngIf="error" class="error">{{ error }}</div>

        <div class="actions">
          <button type="button" class="btn-secondary" (click)="close()">Annuler</button>
          <button type="submit" class="btn-primary" [disabled]="!f.valid || saving">
            {{ saving ? 'Enregistrement…' : 'Verser' }}
          </button>
        </div>
      </form>
    </p-dialog>
  `,
  styles: [`
    .savings-form { display: flex; flex-direction: column; gap: 0.9rem; }
    .field { display: flex; flex-direction: column; gap: 0.35rem; flex: 1; }
    .field span { font-size: 0.85rem; color: #b8b8b8; font-weight: 500; }
    .field input {
      padding: 0.6rem 0.75rem;
      background: #1a1a1a;
      border: 1px solid #333;
      border-radius: 6px;
      color: #f0f0f0;
      font-size: 0.95rem;
    }
    .field input:focus { outline: none; border-color: #C9A84C; }
    .row { display: flex; gap: 1rem; }
    .actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 0.6rem; }
    .btn-primary, .btn-secondary {
      padding: 0.6rem 1.2rem;
      border-radius: 6px;
      border: none;
      cursor: pointer;
      font-weight: 600;
    }
    .btn-primary { background: #C9A84C; color: #1a1a1a; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-secondary { background: transparent; color: #b8b8b8; border: 1px solid #444; }
    .error { color: #e57373; font-size: 0.85rem; }
  `]
})
export class SavingsContributionFormComponent implements OnChanges {
  @Input() visible = false;
  @Input() goal: SavingsGoal | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<SavingsContribution>();

  model: SavingsContributionRequest = this.emptyModel();
  error: string | null = null;
  saving = false;

  constructor(private savingsService: SavingsService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      this.model = this.emptyModel();
      this.error = null;
      this.saving = false;
    }
  }

  submit(): void {
    if (!this.goal || this.saving) return;
    this.saving = true;
    this.error = null;
    this.savingsService.addContribution(this.goal.id, { ...this.model, type: 'MANUAL' }).subscribe({
      next: (c) => {
        this.saving = false;
        this.savingsService.notifyUpdated();
        this.saved.emit(c);
        this.close();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || "Échec du versement";
      }
    });
  }

  close(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  onVisibleChange(v: boolean): void {
    this.visible = v;
    this.visibleChange.emit(v);
  }

  private emptyModel(): SavingsContributionRequest {
    const now = new Date();
    return {
      amount: 0,
      month: now.getMonth() + 1,
      year: now.getFullYear(),
      note: ''
    };
  }
}
