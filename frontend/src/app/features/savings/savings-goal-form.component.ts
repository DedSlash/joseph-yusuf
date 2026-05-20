import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { SavingsService } from '../../core/services/savings.service';
import { SavingsGoal, SavingsGoalRequest } from '../../shared/models/savings.model';

@Component({
  selector: 'app-savings-goal-form',
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
      [style]="{ width: '520px' }"
      [header]="goal ? 'Modifier l\\'objectif d\\'épargne' : 'Créer un objectif d\\'épargne'">
      <form (ngSubmit)="submit()" #f="ngForm" class="savings-form">
        <label class="field">
          <span>Nom de l'objectif</span>
          <input type="text" name="name" [(ngModel)]="model.name" required maxlength="150"
                 placeholder="Fonds d'urgence, Voyage, Investissement…" />
        </label>

        <label class="field">
          <span>Montant cible (XOF)</span>
          <input type="number" name="targetAmount" [(ngModel)]="model.targetAmount" required min="1" />
        </label>

        <div class="row">
          <label class="field">
            <span>Versement mensuel fixe (optionnel)</span>
            <input type="number" name="monthlyTarget" [(ngModel)]="model.monthlyTarget" min="0" />
          </label>
          <label class="field">
            <span>% du revenu (optionnel)</span>
            <input type="number" name="monthlyTargetPercent" [(ngModel)]="model.monthlyTargetPercent"
                   min="0" max="100" step="0.1" />
          </label>
        </div>
        <p class="hint">Si les deux sont renseignés, le montant le plus élevé est retenu.</p>

        <div class="row">
          <label class="field">
            <span>Date de début</span>
            <input type="date" name="startDate" [(ngModel)]="model.startDate" required />
          </label>
          <label class="field">
            <span>Date cible (optionnel)</span>
            <input type="date" name="targetDate" [(ngModel)]="model.targetDate" />
          </label>
        </div>

        <div *ngIf="error" class="error">{{ error }}</div>

        <div class="actions">
          <button type="button" class="btn-secondary" (click)="close()">Annuler</button>
          <button type="submit" class="btn-primary" [disabled]="!f.valid || saving">
            {{ saving ? 'Enregistrement…' : (goal ? 'Mettre à jour' : 'Créer') }}
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
    .field input:focus {
      outline: none;
      border-color: #C9A84C;
    }
    .row { display: flex; gap: 1rem; }
    .hint { font-size: 0.8rem; color: #888; margin: -0.4rem 0 0 0; }
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
export class SavingsGoalFormComponent implements OnChanges {
  @Input() visible = false;
  @Input() goal: SavingsGoal | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<SavingsGoal>();

  model: SavingsGoalRequest = this.emptyModel();
  error: string | null = null;
  saving = false;

  constructor(private savingsService: SavingsService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      this.error = null;
      this.saving = false;
      if (this.goal) {
        this.model = {
          name: this.goal.name,
          targetAmount: this.goal.targetAmount,
          monthlyTarget: this.goal.monthlyTarget,
          monthlyTargetPercent: this.goal.monthlyTargetPercent,
          startDate: this.goal.startDate,
          targetDate: this.goal.targetDate
        };
      } else {
        this.model = this.emptyModel();
      }
    }
  }

  submit(): void {
    if (this.saving) return;
    this.saving = true;
    this.error = null;
    const payload: SavingsGoalRequest = {
      ...this.model,
      monthlyTarget: this.model.monthlyTarget || null,
      monthlyTargetPercent: this.model.monthlyTargetPercent || null,
      targetDate: this.model.targetDate || null
    };
    const obs = this.goal
      ? this.savingsService.updateGoal(this.goal.id, payload)
      : this.savingsService.createGoal(payload);
    obs.subscribe({
      next: (saved) => {
        this.saving = false;
        this.savingsService.notifyUpdated();
        this.saved.emit(saved);
        this.close();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || "Échec de l'enregistrement";
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

  private emptyModel(): SavingsGoalRequest {
    const today = new Date().toISOString().substring(0, 10);
    return {
      name: '',
      targetAmount: 0,
      monthlyTarget: null,
      monthlyTargetPercent: null,
      startDate: today,
      targetDate: null
    };
  }
}
