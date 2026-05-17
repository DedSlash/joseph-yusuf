import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TabViewModule } from 'primeng/tabview';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { TooltipModule } from 'primeng/tooltip';
import { FileUploadModule } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';
import { CheckboxModule } from 'primeng/checkbox';
import { ProgressBarModule } from 'primeng/progressbar';
import { IncomeService } from '../../core/services/income.service';
import { AuthService } from '../../core/auth/auth.service';
import { IncomeSource, IncomeSourceRequest, IncomeSourceType, IncomeEntry, IncomeEntryRequest, MonthSummary } from '../../shared/models/income.model';
import { Plan } from '../../shared/models/user.model';

interface CurrencyOption {
  code: string;
  label: string;
  rateToXOF: number; // 1 unité de cette devise = X XOF (taux indicatif)
}

const CURRENCIES: CurrencyOption[] = [
  { code: 'XOF', label: 'XOF — Franc CFA (BCEAO)',        rateToXOF: 1       },
  { code: 'XAF', label: 'XAF — Franc CFA (BEAC)',          rateToXOF: 1       },
  { code: 'EUR', label: 'EUR — Euro',                       rateToXOF: 655.96  },
  { code: 'USD', label: 'USD — Dollar américain',           rateToXOF: 600     },
  { code: 'GBP', label: 'GBP — Livre sterling',             rateToXOF: 760     },
  { code: 'CAD', label: 'CAD — Dollar canadien',            rateToXOF: 445     },
  { code: 'CHF', label: 'CHF — Franc suisse',               rateToXOF: 670     },
  { code: 'MAD', label: 'MAD — Dirham marocain',            rateToXOF: 60      },
  { code: 'DZD', label: 'DZD — Dinar algérien',             rateToXOF: 4.5     },
  { code: 'TND', label: 'TND — Dinar tunisien',             rateToXOF: 190     },
  { code: 'NGN', label: 'NGN — Naira nigérian',             rateToXOF: 0.40    },
  { code: 'GHS', label: 'GHS — Cedi ghanéen',               rateToXOF: 42      },
  { code: 'MRU', label: 'MRU — Ouguiya mauritanien',        rateToXOF: 16      },
  { code: 'GMD', label: 'GMD — Dalasi gambien',             rateToXOF: 9       },
  { code: 'SLL', label: 'SLL — Leone sierra-léonais',       rateToXOF: 0.03    },
  { code: 'LRD', label: 'LRD — Dollar libérien',            rateToXOF: 3       },
];

interface SourceEntryForm {
  sourceId: string;
  sourceName: string;
  currency: string;
  amount: number;
}

interface SourceTypeOption {
  label: string;
  value: IncomeSourceType;
}

interface ImportRow {
  month: number;
  year: number;
  source: string;
  amount: number;
  note: string;
  status: 'new' | 'existing' | 'conflict' | 'invalid';
  statusLabel: string;
  valid: boolean;
}

@Component({
  selector: 'app-incomes',
  standalone: true,
  imports: [CommonModule, FormsModule, TabViewModule, DialogModule, DropdownModule, InputNumberModule, TooltipModule, FileUploadModule, TableModule, CheckboxModule, ProgressBarModule],
  template: `
    <div class="incomes-page">
      <h2 class="page-title">Mes Revenus</h2>

      <p-tabView styleClass="dark-tabs">
        <!-- Tab 1: Sources -->
        <p-tabPanel header="Sources">
          <div class="tab-content">
            <div class="tab-header">
              <h3 class="tab-title">Mes sources de revenus</h3>
              <div class="tab-header-actions">
                <button
                  class="btn-import"
                  [disabled]="!isPremium()"
                  [pTooltip]="!isPremium() ? 'Disponible en Premium' : ''"
                  tooltipPosition="left"
                  (click)="showImportDialog = true"
                >
                  <i class="pi pi-upload"></i> Importer
                </button>
                <button
                  class="btn-add"
                  [disabled]="isAddSourceDisabled()"
                  [pTooltip]="getAddSourceTooltip()"
                  tooltipPosition="left"
                  (click)="openAddSourceDialog()"
                >
                  + Ajouter une source
                </button>
              </div>
            </div>

            <div class="sources-list" *ngIf="sources.length > 0">
              <div class="source-card" *ngFor="let source of sources">
                <div class="source-info">
                  <span class="source-name">{{ source.name }}</span>
                  <span class="source-type">{{ getSourceTypeLabel(source.type) }}</span>
                </div>
                <div class="source-meta">
                  <span class="source-currency">{{ source.currency }}</span>
                  <span class="source-status" [ngClass]="source.active ? 'active' : 'inactive'">
                    {{ source.active ? 'Active' : 'Inactive' }}
                  </span>
                  <button class="btn-icon" (click)="openEditSourceDialog(source)" pTooltip="Modifier" tooltipPosition="top">
                    <i class="pi pi-pencil"></i>
                  </button>
                  <button class="btn-icon btn-icon-danger" (click)="initiateDeleteSource(source)" pTooltip="Supprimer" tooltipPosition="top">
                    <i class="pi pi-trash"></i>
                  </button>
                </div>
              </div>
            </div>

            <div class="empty-state" *ngIf="sources.length === 0">
              <p>Aucune source de revenu configurée.</p>
              <p class="empty-hint">Ajoutez votre première source ou importez un fichier historique pour démarrer.</p>
            </div>
          </div>
        </p-tabPanel>

        <!-- Tab 2: Saisie mensuelle -->
        <p-tabPanel header="Saisie mensuelle">
          <div class="tab-content">
            <div class="tab-header">
              <h3 class="tab-title">Saisie du mois</h3>
              <div class="tab-header-actions">
                <button
                  class="btn-import"
                  [disabled]="!isPremium()"
                  [pTooltip]="!isPremium() ? 'Disponible en Premium' : ''"
                  tooltipPosition="left"
                  (click)="showImportDialog = true"
                >
                  <i class="pi pi-upload"></i> Importer des données
                </button>
                <div class="month-selector">
                  <select [(ngModel)]="selectedMonth" (ngModelChange)="loadEntries()" class="select-input">
                    <option *ngFor="let m of availableMonths" [ngValue]="m.value">{{ m.label }}</option>
                  </select>
                  <select [(ngModel)]="selectedYear" (ngModelChange)="onYearChange()" class="select-input">
                    <option *ngFor="let y of years" [ngValue]="y">{{ y }}</option>
                  </select>
                </div>
              </div>
            </div>

            <div class="entries-form" *ngIf="sources.length > 0">
              <div class="entry-row" *ngFor="let entry of entryForms">
                <div class="entry-left">
                  <label class="entry-label">{{ entry.sourceName }}</label>
                  <span class="entry-converted" *ngIf="entry.currency !== 'XOF' && entry.amount > 0">
                    ≈ {{ formatCurrency(toXOF(entry.amount, entry.currency)) }} XOF
                  </span>
                </div>
                <div class="entry-input-wrapper">
                  <p-inputNumber
                    [(ngModel)]="entry.amount"
                    [min]="0"
                    mode="decimal"
                    [useGrouping]="true"
                    locale="fr-FR"
                    placeholder="0"
                    styleClass="dark-input-number"
                  ></p-inputNumber>
                  <span class="entry-currency">{{ entry.currency }}</span>
                </div>
              </div>

              <div class="entries-footer">
                <div class="total-display">
                  <span class="total-label">Total (XOF) :</span>
                  <span class="total-amount">{{ formatCurrency(calculateTotalXOF()) }}</span>
                  <span class="status-badge" *ngIf="currentSummary" [ngClass]="getStatusClass(currentSummary.status)">
                    {{ getStatusLabel(currentSummary.status) }}
                  </span>
                </div>
                <button class="btn-save" (click)="saveEntries()" [disabled]="saving">
                  {{ saving ? 'Enregistrement...' : 'Enregistrer' }}
                </button>
              </div>
            </div>

            <div class="empty-state" *ngIf="sources.length === 0">
              <p>Ajoutez d'abord une source de revenu dans l'onglet "Sources".</p>
            </div>
          </div>
        </p-tabPanel>
      </p-tabView>

      <!-- Add/Edit Source Dialog -->
      <p-dialog
        [header]="editingSource ? 'Modifier la source' : 'Ajouter une source de revenu'"
        [(visible)]="showSourceDialog"
        [modal]="true"
        [style]="{ width: '420px' }"
        [baseZIndex]="10000"
        [draggable]="false"
        [resizable]="false"
      >
        <div class="dialog-form">
          <div class="form-group">
            <label>Nom</label>
            <input
              type="text"
              [(ngModel)]="newSource.name"
              placeholder="Ex: Salaire principal"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label>Type</label>
            <p-dropdown
              [options]="sourceTypes"
              [(ngModel)]="newSource.type"
              optionLabel="label"
              optionValue="value"
              placeholder="Selectionnez un type"
              styleClass="dark-dropdown"
            ></p-dropdown>
          </div>
          <div class="form-group">
            <label>Devise</label>
            <p-dropdown
              [options]="currencyOptions"
              [(ngModel)]="newSource.currency"
              optionLabel="label"
              optionValue="code"
              placeholder="Sélectionnez une devise"
              styleClass="dark-dropdown"
              [filter]="true"
              filterPlaceholder="Rechercher…"
            ></p-dropdown>
            <span class="currency-hint" *ngIf="newSource.currency && newSource.currency !== 'XOF'">
              Les montants seront convertis en XOF pour l'analyse (taux indicatif).
            </span>
          </div>
        </div>
        <ng-template pTemplate="footer">
          <button class="btn-cancel" (click)="showSourceDialog = false">Annuler</button>
          <button class="btn-confirm" (click)="editingSource ? updateSource() : createSource()">
            {{ editingSource ? 'Enregistrer' : 'Ajouter' }}
          </button>
        </ng-template>
      </p-dialog>

      <!-- Delete Confirmation Dialog (FREE plan) -->
      <p-dialog
        header="Confirmer la suppression"
        [(visible)]="showDeleteConfirmDialog"
        [modal]="true"
        [style]="{ width: '420px' }"
        [baseZIndex]="10000"
        [draggable]="false"
        [resizable]="false"
      >
        <p class="delete-message">
          Etes-vous sur de vouloir supprimer la source "{{ sourceToDelete?.name }}" ?
          Cette action est irreversible.
        </p>
        <ng-template pTemplate="footer">
          <button class="btn-cancel" (click)="showDeleteConfirmDialog = false">Annuler</button>
          <button class="btn-danger" (click)="confirmDeleteSource()">Supprimer</button>
        </ng-template>
      </p-dialog>

      <!-- Delete Dialog with Export Option (PREMIUM plan) -->
      <p-dialog
        header="Supprimer la source"
        [(visible)]="showDeletePremiumDialog"
        [modal]="true"
        [style]="{ width: '480px' }"
        [baseZIndex]="10000"
        [draggable]="false"
        [resizable]="false"
      >
        <p class="delete-message">
          Vous etes sur le point de supprimer la source "{{ sourceToDelete?.name }}".
        </p>
        <div class="delete-options">
          <button class="btn-option" (click)="exportAndDelete()">
            <i class="pi pi-download"></i>
            <div>
              <strong>Exporter les donnees avant suppression</strong>
              <span class="option-desc">Telecharge un fichier JSON contenant toutes les entrees</span>
            </div>
          </button>
          <button class="btn-option btn-option-danger" (click)="confirmDeleteSource()">
            <i class="pi pi-trash"></i>
            <div>
              <strong>Supprimer sans exporter</strong>
              <span class="option-desc">Les donnees seront definitivement perdues</span>
            </div>
          </button>
        </div>
        <ng-template pTemplate="footer">
          <button class="btn-cancel" (click)="showDeletePremiumDialog = false">Annuler</button>
        </ng-template>
      </p-dialog>

      <!-- Import Dialog -->
      <p-dialog
        header="Importer des donnees historiques"
        [(visible)]="showImportDialog"
        [modal]="true"
        [style]="{ width: '700px' }"
        [baseZIndex]="10000"
        [draggable]="false"
        [resizable]="false"
        (onHide)="resetImport()"
      >
        <div class="import-content" *ngIf="!importPreviewRows.length">

          <!-- Bandeau modèle -->
          <div class="import-template-banner">
            <div class="template-banner-left">
              <i class="pi pi-file-excel template-icon"></i>
              <div>
                <span class="template-title">Utilisez notre modèle pour un import sans erreur</span>
                <span class="template-sub">Colonnes attendues : Source · Montant · Date (AAAA-MM-JJ)</span>
              </div>
            </div>
            <button class="btn-template" (click)="downloadTemplate()">
              <i class="pi pi-download"></i> Télécharger le modèle
            </button>
          </div>

          <!-- Aide IA — collapsible -->
          <div class="ai-help-block">
            <button class="ai-help-toggle" (click)="aiHelpOpen = !aiHelpOpen">
              <span class="ai-help-toggle-left">
                <span class="ai-icon">✦</span>
                <span>Vous avez déjà un fichier dans un autre format ?</span>
              </span>
              <i class="pi" [ngClass]="aiHelpOpen ? 'pi-chevron-up' : 'pi-chevron-down'"></i>
            </button>

            <div class="ai-help-body" *ngIf="aiHelpOpen">
              <p class="ai-help-intro">
                Laissez un assistant IA convertir votre fichier en quelques secondes —
                sans reformater manuellement chaque colonne.
              </p>

              <ol class="ai-steps">
                <li>
                  <span class="ai-step-num">1</span>
                  <div>
                    <strong>Téléchargez le modèle</strong> ci-dessus. Il montre exactement le format attendu.
                  </div>
                </li>
                <li>
                  <span class="ai-step-num">2</span>
                  <div>
                    <strong>Ouvrez un assistant IA</strong> —
                    <span class="ai-tool">ChatGPT</span>,
                    <span class="ai-tool">Claude</span>,
                    <span class="ai-tool">Gemini</span> ou tout autre outil similaire.
                  </div>
                </li>
                <li>
                  <span class="ai-step-num">3</span>
                  <div>
                    <strong>Envoyez les deux fichiers</strong> à l'IA avec ce message :
                    <div class="ai-prompt-box">
                      <span class="ai-prompt-text">
                        « J'ai un fichier de revenus (joint). Convertis-le au format du modèle joint
                        (colonnes : Source, Description, Montant, Devise, Date au format AAAA-MM-JJ).
                        Chaque ligne doit représenter une transaction.
                        Exporte le résultat en fichier Excel (.xlsx). »
                      </span>
                      <button class="btn-copy-prompt" (click)="copyPrompt($event)">
                        <i class="pi pi-copy"></i>
                        <span>{{ promptCopied ? 'Copié !' : 'Copier' }}</span>
                      </button>
                    </div>
                  </div>
                </li>
                <li>
                  <span class="ai-step-num">4</span>
                  <div>
                    <strong>Téléchargez le fichier converti</strong> produit par l'IA,
                    puis importez-le ici.
                  </div>
                </li>
              </ol>
            </div>
          </div>

          <div class="import-upload-zone" (dragover)="onDragOver($event)" (drop)="onFileDrop($event)" (click)="fileInput.click()">
            <i class="pi pi-cloud-upload import-icon"></i>
            <p class="import-text">Glissez un fichier ici ou cliquez pour sélectionner</p>
            <p class="import-formats">Formats acceptés : .xlsx, .csv, .json</p>
            <input #fileInput type="file" accept=".xlsx,.csv,.json" (change)="onFileSelect($event)" hidden />
          </div>

          <div class="form-group import-source-select">
            <label>Source de revenu par défaut <span class="label-hint">(utilisée si la colonne Source est vide)</span></label>
            <p-dropdown
              [options]="sourceOptions"
              [(ngModel)]="importTargetSourceId"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionnez une source"
              styleClass="dark-dropdown"
            ></p-dropdown>
          </div>

          <div class="form-group import-checkbox">
            <p-checkbox [(ngModel)]="importOverwrite" [binary]="true" label="Écraser les entrées existantes pour les mêmes mois"></p-checkbox>
          </div>
        </div>

        <!-- Preview Table -->
        <div class="import-preview" *ngIf="importPreviewRows.length > 0">

          <!-- Résumé sources -->
          <div class="import-sources-summary" *ngIf="importNewSources.length > 0">
            <span class="sources-summary-icon">✦</span>
            <div>
              <strong>{{ importNewSources.length }} nouvelle(s) source(s) à créer :</strong>
              <span class="new-sources-list">{{ importNewSources.join(' · ') }}</span>
              <p class="sources-summary-hint">Elles seront créées automatiquement avant l'import des revenus.</p>
            </div>
          </div>

          <div class="import-summary">
            <span>{{ importValidCount() }} entrées valides</span>
            <span class="import-conflicts" *ngIf="getConflictCount() > 0">
              · {{ getConflictCount() }} conflits (mois déjà saisis)
            </span>
            <span class="import-invalid" *ngIf="importInvalidCount() > 0">
              · {{ importInvalidCount() }} invalides ignorées
            </span>
          </div>

          <div class="import-table-wrapper">
            <table class="import-table">
              <thead>
                <tr>
                  <th>Période</th>
                  <th>Source</th>
                  <th>Montant cumulé</th>
                  <th>Source</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let row of importAggregatedPreview()" [ngClass]="{'row-invalid': !row.valid}">
                  <td>{{ formatImportDate(row.month, row.year) }}</td>
                  <td>{{ row.source || '—' }}</td>
                  <td>{{ row.valid ? formatCurrency(row.amount) : '—' }}</td>
                  <td>
                    <span class="source-tag" [ngClass]="isNewSource(row.source) ? 'source-tag-new' : 'source-tag-exists'">
                      {{ isNewSource(row.source) ? '+ à créer' : '✓ existante' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="import-actions">
            <button class="btn-cancel" (click)="resetImport()">Retour</button>
            <button class="btn-confirm" (click)="confirmImport()" [disabled]="importing">
              <ng-container *ngIf="importing">Import en cours...</ng-container>
              <ng-container *ngIf="!importing">Confirmer l'import</ng-container>
            </button>
          </div>
        </div>
      </p-dialog>
    </div>
  `,
  styles: [`
    .incomes-page {
      padding: 2rem;
      padding-top: 5rem;
      max-width: 900px;
      margin: 0 auto;
    }

    .page-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.8rem;
      color: #F0E8D0;
      margin-bottom: 1.5rem;
    }

    .tab-content {
      padding: 1.5rem 0;
    }

    .tab-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
    }

    .tab-header-actions {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .tab-title {
      font-size: 1rem;
      color: #F0E8D0;
      margin: 0;
    }

    .btn-add, .btn-import {
      padding: 0.5rem 1rem;
      background: rgba(201, 168, 76, 0.1);
      border: 1px solid rgba(201, 168, 76, 0.3);
      border-radius: 6px;
      color: #C9A84C;
      font-size: 0.8rem;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }

    .btn-add:hover:not(:disabled), .btn-import:hover:not(:disabled) {
      background: rgba(201, 168, 76, 0.2);
    }

    .btn-add:disabled, .btn-import:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .sources-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .source-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.25rem;
      background: #1A1710;
      border: 1px solid rgba(201, 168, 76, 0.1);
      border-radius: 8px;
    }

    .source-info {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .source-name {
      font-size: 0.9rem;
      color: #F0E8D0;
      font-weight: 500;
    }

    .source-type {
      font-size: 0.75rem;
      color: #F0E8D0;
      opacity: 0.5;
    }

    .source-meta {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .source-currency {
      font-size: 0.8rem;
      color: #C9A84C;
      font-weight: 500;
    }

    .source-status {
      font-size: 0.7rem;
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
      font-weight: 600;
      text-transform: uppercase;
    }

    .source-status.active {
      background: rgba(40, 167, 69, 0.15);
      color: #5cdb6f;
    }

    .source-status.inactive {
      background: rgba(128, 128, 128, 0.15);
      color: #aaa;
    }

    .btn-icon {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(201, 168, 76, 0.08);
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 6px;
      color: #C9A84C;
      cursor: pointer;
      transition: background 0.2s;
      font-size: 0.8rem;
    }

    .btn-icon:hover {
      background: rgba(201, 168, 76, 0.2);
    }

    .btn-icon-danger {
      color: #ff6b7a;
      background: rgba(220, 53, 69, 0.08);
      border-color: rgba(220, 53, 69, 0.15);
    }

    .btn-icon-danger:hover {
      background: rgba(220, 53, 69, 0.2);
    }

    .empty-state {
      text-align: center;
      padding: 3rem 1rem;
      color: #F0E8D0;
      opacity: 0.6;
    }

    .empty-hint {
      font-size: 0.85rem;
      opacity: 0.7;
      margin-top: 0.5rem;
    }

    .month-selector {
      display: flex;
      gap: 0.5rem;
    }

    .select-input {
      padding: 0.5rem 0.75rem;
      background: rgba(13, 11, 7, 0.6);
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 6px;
      color: #F0E8D0;
      font-size: 0.85rem;
      outline: none;
    }

    .select-input:focus {
      border-color: #C9A84C;
    }

    .entries-form {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .entry-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.25rem;
      background: #1A1710;
      border: 1px solid rgba(201, 168, 76, 0.1);
      border-radius: 8px;
    }

    .entry-left {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }

    .entry-label {
      font-size: 0.9rem;
      color: #F0E8D0;
      font-weight: 500;
    }

    .entry-converted {
      font-size: 0.75rem;
      color: #C9A84C;
      opacity: 0.8;
    }

    .currency-hint {
      font-size: 0.75rem;
      color: #C9A84C;
      opacity: 0.8;
      margin-top: 0.25rem;
    }

    .entry-input-wrapper {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .entry-currency {
      font-size: 0.8rem;
      color: #C9A84C;
      font-weight: 500;
    }

    .entries-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 1.5rem;
      padding: 1rem 1.25rem;
      background: rgba(201, 168, 76, 0.05);
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 8px;
    }

    .total-display {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .total-label {
      font-size: 0.85rem;
      color: #F0E8D0;
      opacity: 0.7;
    }

    .total-amount {
      font-size: 1.2rem;
      color: #F0E8D0;
      font-weight: 600;
    }

    .status-badge {
      padding: 0.2rem 0.5rem;
      border-radius: 8px;
      font-size: 0.7rem;
      font-weight: 600;
      text-transform: uppercase;
    }

    .status-abundance {
      background: rgba(40, 167, 69, 0.15);
      color: #5cdb6f;
    }

    .status-lean {
      background: rgba(220, 53, 69, 0.15);
      color: #ff6b7a;
    }

    .status-normal {
      background: rgba(52, 152, 219, 0.15);
      color: #5dade2;
    }

    .btn-save {
      padding: 0.6rem 1.5rem;
      background: #C9A84C;
      color: #0D0B07;
      border: none;
      border-radius: 6px;
      font-size: 0.85rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
    }

    .btn-save:hover:not(:disabled) {
      background: #DAC372;
    }

    .btn-save:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .dialog-form {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .form-group label {
      font-size: 0.85rem;
      color: #F0E8D0;
      opacity: 0.8;
    }

    .form-input {
      padding: 0.65rem 0.85rem;
      background: rgba(13, 11, 7, 0.6);
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 6px;
      color: #F0E8D0;
      font-size: 0.9rem;
      outline: none;
      transition: border-color 0.2s;
    }

    .form-input:focus {
      border-color: #C9A84C;
    }

    .form-input::placeholder {
      color: rgba(240, 232, 208, 0.3);
    }

    .btn-cancel {
      padding: 0.5rem 1rem;
      background: transparent;
      border: 1px solid rgba(240, 232, 208, 0.2);
      border-radius: 6px;
      color: #F0E8D0;
      font-size: 0.85rem;
      cursor: pointer;
      margin-right: 0.5rem;
    }

    .btn-confirm {
      padding: 0.5rem 1rem;
      background: #C9A84C;
      border: none;
      border-radius: 6px;
      color: #0D0B07;
      font-size: 0.85rem;
      font-weight: 600;
      cursor: pointer;
    }

    .btn-confirm:hover:not(:disabled) {
      background: #DAC372;
    }

    .btn-confirm:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .btn-danger {
      padding: 0.5rem 1rem;
      background: #dc3545;
      border: none;
      border-radius: 6px;
      color: #fff;
      font-size: 0.85rem;
      font-weight: 600;
      cursor: pointer;
    }

    .btn-danger:hover {
      background: #c82333;
    }

    .delete-message {
      color: #F0E8D0;
      font-size: 0.9rem;
      line-height: 1.6;
    }

    .delete-options {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      margin-top: 1rem;
    }

    .btn-option {
      display: flex;
      align-items: flex-start;
      gap: 1rem;
      padding: 1rem 1.25rem;
      background: rgba(13, 11, 7, 0.5);
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 8px;
      color: #F0E8D0;
      cursor: pointer;
      text-align: left;
      transition: border-color 0.2s, background 0.2s;
      width: 100%;
    }

    .btn-option:hover {
      border-color: rgba(201, 168, 76, 0.4);
      background: rgba(201, 168, 76, 0.05);
    }

    .btn-option i {
      font-size: 1.2rem;
      color: #C9A84C;
      margin-top: 0.1rem;
    }

    .btn-option strong {
      display: block;
      font-size: 0.85rem;
      margin-bottom: 0.25rem;
    }

    .option-desc {
      font-size: 0.75rem;
      opacity: 0.6;
    }

    .btn-option-danger:hover {
      border-color: rgba(220, 53, 69, 0.4);
      background: rgba(220, 53, 69, 0.05);
    }

    .btn-option-danger i {
      color: #ff6b7a;
    }

    /* Import styles */
    .import-template-banner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.85rem 1.1rem;
      background: rgba(92, 219, 111, 0.05);
      border: 1px solid rgba(92, 219, 111, 0.2);
      border-radius: 8px;
      margin-bottom: 1.1rem;
    }

    .template-banner-left {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .template-icon {
      font-size: 1.4rem;
      color: #5cdb6f;
      flex-shrink: 0;
    }

    .template-title {
      display: block;
      font-size: 0.83rem;
      font-weight: 600;
      color: #F0E8D0;
    }

    .template-sub {
      display: block;
      font-size: 0.72rem;
      color: rgba(240, 232, 208, 0.5);
      margin-top: 0.15rem;
      font-family: monospace;
    }

    .btn-template {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.45rem 0.9rem;
      background: rgba(92, 219, 111, 0.1);
      border: 1px solid rgba(92, 219, 111, 0.35);
      border-radius: 6px;
      color: #5cdb6f;
      font-size: 0.78rem;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      transition: background 0.2s;
    }

    .btn-template:hover { background: rgba(92, 219, 111, 0.18); }

    .label-hint {
      font-size: 0.72rem;
      color: rgba(240, 232, 208, 0.45);
      font-weight: 400;
    }

    /* ── Bloc aide IA ── */
    .ai-help-block {
      border: 1px solid rgba(201, 168, 76, 0.18);
      border-radius: 8px;
      overflow: hidden;
      margin-bottom: 1.1rem;
    }

    .ai-help-toggle {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0.7rem 1rem;
      background: rgba(201, 168, 76, 0.05);
      border: none;
      cursor: pointer;
      color: #F0E8D0;
      font-size: 0.82rem;
      font-weight: 500;
      transition: background 0.2s;
    }

    .ai-help-toggle:hover { background: rgba(201, 168, 76, 0.1); }

    .ai-help-toggle-left {
      display: flex;
      align-items: center;
      gap: 0.55rem;
    }

    .ai-icon {
      color: #C9A84C;
      font-size: 0.85rem;
    }

    .ai-help-toggle .pi-chevron-up,
    .ai-help-toggle .pi-chevron-down {
      font-size: 0.75rem;
      color: rgba(240, 232, 208, 0.45);
    }

    .ai-help-body {
      padding: 1rem 1.1rem 1.1rem;
      border-top: 1px solid rgba(201, 168, 76, 0.12);
    }

    .ai-help-intro {
      font-size: 0.82rem;
      color: rgba(240, 232, 208, 0.65);
      line-height: 1.6;
      margin-bottom: 1rem;
    }

    .ai-steps {
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 0.85rem;
    }

    .ai-steps li {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
    }

    .ai-step-num {
      flex-shrink: 0;
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: rgba(201, 168, 76, 0.12);
      border: 1px solid rgba(201, 168, 76, 0.35);
      color: #C9A84C;
      font-size: 0.7rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-top: 1px;
    }

    .ai-steps li div {
      font-size: 0.82rem;
      color: rgba(240, 232, 208, 0.75);
      line-height: 1.55;
    }

    .ai-steps li strong {
      color: #F0E8D0;
      font-weight: 600;
    }

    .ai-tool {
      display: inline-block;
      font-size: 0.72rem;
      font-weight: 600;
      padding: 0.1rem 0.45rem;
      border-radius: 4px;
      background: rgba(201, 168, 76, 0.1);
      border: 1px solid rgba(201, 168, 76, 0.25);
      color: #C9A84C;
      margin: 0 1px;
    }

    .ai-prompt-box {
      margin-top: 0.6rem;
      padding: 0.75rem 1rem;
      background: rgba(13, 11, 7, 0.6);
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 6px;
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
    }

    .ai-prompt-text {
      font-size: 0.78rem;
      color: rgba(240, 232, 208, 0.6);
      line-height: 1.6;
      font-style: italic;
      flex: 1;
    }

    .btn-copy-prompt {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      gap: 0.3rem;
      padding: 0.3rem 0.65rem;
      background: rgba(201, 168, 76, 0.1);
      border: 1px solid rgba(201, 168, 76, 0.3);
      border-radius: 5px;
      color: #C9A84C;
      font-size: 0.72rem;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      transition: background 0.2s;
      align-self: flex-start;
    }

    .btn-copy-prompt:hover { background: rgba(201, 168, 76, 0.18); }

    .import-upload-zone {
      border: 2px dashed rgba(201, 168, 76, 0.3);
      border-radius: 10px;
      padding: 2.5rem;
      text-align: center;
      cursor: pointer;
      transition: border-color 0.2s, background 0.2s;
      margin-bottom: 1.25rem;
    }

    .import-upload-zone:hover {
      border-color: #C9A84C;
      background: rgba(201, 168, 76, 0.03);
    }

    .import-icon {
      font-size: 2.5rem;
      color: #C9A84C;
      opacity: 0.6;
    }

    .import-text {
      color: #F0E8D0;
      margin: 0.75rem 0 0.25rem;
      font-size: 0.9rem;
    }

    .import-formats {
      color: #F0E8D0;
      opacity: 0.5;
      font-size: 0.75rem;
    }

    .import-source-select {
      margin-bottom: 1rem;
    }

    .import-checkbox {
      margin-bottom: 0.5rem;
    }

    .import-preview {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .import-sources-summary {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
      padding: 0.85rem 1rem;
      background: rgba(92, 219, 111, 0.06);
      border: 1px solid rgba(92, 219, 111, 0.2);
      border-radius: 8px;
    }

    .sources-summary-icon { color: #C9A84C; flex-shrink: 0; margin-top: 2px; }

    .import-sources-summary strong {
      display: block;
      font-size: 0.83rem;
      color: #F0E8D0;
      margin-bottom: 0.2rem;
    }

    .new-sources-list {
      font-size: 0.78rem;
      color: #5cdb6f;
      font-weight: 600;
    }

    .sources-summary-hint {
      font-size: 0.73rem;
      color: rgba(240,232,208,0.45);
      margin: 0.2rem 0 0;
    }

    .import-summary {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      font-size: 0.85rem;
      color: #F0E8D0;
      flex-wrap: wrap;
    }

    .import-conflicts {
      color: #f5b041;
      font-size: 0.8rem;
    }

    .import-invalid {
      color: #ff6b7a;
      font-size: 0.8rem;
    }

    .source-tag {
      font-size: 0.68rem;
      font-weight: 700;
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .source-tag-new {
      background: rgba(92,219,111,0.12);
      color: #5cdb6f;
      border: 1px solid rgba(92,219,111,0.3);
    }

    .source-tag-exists {
      background: rgba(201,168,76,0.1);
      color: #C9A84C;
      border: 1px solid rgba(201,168,76,0.25);
    }

    .import-table-wrapper {
      max-height: 350px;
      overflow-y: auto;
      border-radius: 8px;
      border: 1px solid rgba(201, 168, 76, 0.1);
    }

    .import-table {
      width: 100%;
      border-collapse: collapse;
      background: #1A1710;
    }

    .import-table th {
      text-align: left;
      padding: 0.7rem 1rem;
      font-size: 0.75rem;
      font-weight: 600;
      color: #F0E8D0;
      opacity: 0.6;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      border-bottom: 1px solid rgba(201, 168, 76, 0.1);
      position: sticky;
      top: 0;
      background: #1A1710;
    }

    .import-table td {
      padding: 0.6rem 1rem;
      font-size: 0.85rem;
      color: #F0E8D0;
      border-bottom: 1px solid rgba(201, 168, 76, 0.05);
    }

    .row-invalid {
      opacity: 0.5;
    }

    .import-status {
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
      font-size: 0.7rem;
      font-weight: 600;
      text-transform: uppercase;
    }

    .import-status-new {
      background: rgba(40, 167, 69, 0.15);
      color: #5cdb6f;
    }

    .import-status-existing {
      background: rgba(52, 152, 219, 0.15);
      color: #5dade2;
    }

    .import-status-conflict {
      background: rgba(243, 156, 18, 0.15);
      color: #f5b041;
    }

    .import-status-invalid {
      background: rgba(220, 53, 69, 0.15);
      color: #ff6b7a;
    }

    .import-actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
      margin-top: 0.5rem;
    }

    @media (max-width: 768px) {
      .incomes-page {
        padding: 1rem;
        padding-top: 5rem;
      }

      .tab-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 1rem;
      }

      .tab-header-actions {
        flex-direction: column;
        align-items: flex-start;
        width: 100%;
      }

      .entry-row {
        flex-direction: column;
        align-items: flex-start;
        gap: 0.75rem;
      }
    }
  `]
})
export class IncomesComponent implements OnInit {
  sources: IncomeSource[] = [];
  entryForms: SourceEntryForm[] = [];
  currentSummary: MonthSummary | null = null;
  showSourceDialog = false;
  editingSource: IncomeSource | null = null;
  saving = false;

  // Delete
  showDeleteConfirmDialog = false;
  showDeletePremiumDialog = false;
  sourceToDelete: IncomeSource | null = null;

  // Import
  showImportDialog = false;
  importPreviewRows: ImportRow[] = [];
  importTargetSourceId = '';
  importOverwrite = false;
  importing = false;
  sourceOptions: { label: string; value: string }[] = [];
  aiHelpOpen = false;
  promptCopied = false;
  importNewSources: string[] = [];

  selectedMonth: number;
  selectedYear: number;

  newSource: IncomeSourceRequest = {
    name: '',
    type: 'SALARY',
    currency: 'XOF'
  };

  currencyOptions = CURRENCIES;

  sourceTypes: SourceTypeOption[] = [
    { label: 'Salaire', value: 'SALARY' },
    { label: 'Freelance', value: 'FREELANCE' },
    { label: 'Location', value: 'RENTAL' },
    { label: 'Mobile Money', value: 'MOBILE_MONEY' },
    { label: 'Autre', value: 'OTHER' }
  ];

  months = [
    { label: 'Janvier', value: 1 },
    { label: 'Fevrier', value: 2 },
    { label: 'Mars', value: 3 },
    { label: 'Avril', value: 4 },
    { label: 'Mai', value: 5 },
    { label: 'Juin', value: 6 },
    { label: 'Juillet', value: 7 },
    { label: 'Aout', value: 8 },
    { label: 'Septembre', value: 9 },
    { label: 'Octobre', value: 10 },
    { label: 'Novembre', value: 11 },
    { label: 'Decembre', value: 12 }
  ];

  years: number[] = [];

  private readonly currencyFormatter = new Intl.NumberFormat('fr-SN', {
    style: 'currency',
    currency: 'XOF',
    maximumFractionDigits: 0
  });

  readonly currentMonth: number;
  readonly currentYear: number;

  constructor(
    private incomeService: IncomeService,
    private authService: AuthService
  ) {
    const now = new Date();
    this.currentMonth = now.getMonth() + 1;
    this.currentYear  = now.getFullYear();
    this.selectedMonth = this.currentMonth;
    this.selectedYear  = this.currentYear;
    this.years = Array.from({ length: 5 }, (_, i) => this.currentYear - 4 + i);
  }

  get availableMonths(): { label: string; value: number }[] {
    if (this.selectedYear < this.currentYear) return this.months;
    return this.months.filter(m => m.value <= this.currentMonth);
  }

  onYearChange(): void {
    if (this.selectedYear === this.currentYear && this.selectedMonth > this.currentMonth) {
      this.selectedMonth = this.currentMonth;
    }
    this.loadEntries();
  }

  ngOnInit(): void {
    this.loadSources();
  }

  loadSources(): void {
    this.incomeService.getSources().subscribe({
      next: (sources) => {
        this.sources = sources;
        this.sourceOptions = sources.filter(s => s.active).map(s => ({ label: s.name, value: s.id }));
        this.buildEntryForms();
        this.loadEntries();
      }
    });
  }

  loadEntries(): void {
    if (this.sources.length === 0) {
      return;
    }

    this.incomeService.getEntries(this.selectedMonth, this.selectedYear).subscribe({
      next: (entries) => {
        this.entryForms.forEach(form => {
          const existing = entries.find(e => e.incomeSourceId === form.sourceId);
          form.amount = existing ? existing.amount : 0;
        });
      }
    });

    this.incomeService.getSummary(this.selectedMonth, this.selectedYear).subscribe({
      next: (summary) => this.currentSummary = summary,
      error: () => this.currentSummary = null
    });
  }

  buildEntryForms(): void {
    this.entryForms = this.sources
      .filter(s => s.active)
      .map(s => ({
        sourceId: s.id,
        sourceName: s.name,
        currency: s.currency || 'XOF',
        amount: 0
      }));
  }

  // --- Source CRUD ---

  openAddSourceDialog(): void {
    this.editingSource = null;
    this.newSource = { name: '', type: 'SALARY', currency: 'XOF' };
    this.showSourceDialog = true;
  }

  openEditSourceDialog(source: IncomeSource): void {
    this.editingSource = source;
    this.newSource = { name: source.name, type: source.type, currency: source.currency };
    this.showSourceDialog = true;
  }

  createSource(): void {
    if (!this.newSource.name) return;

    this.incomeService.createSource(this.newSource).subscribe({
      next: () => {
        this.showSourceDialog = false;
        this.newSource = { name: '', type: 'SALARY', currency: 'XOF' };
        this.loadSources();
      }
    });
  }

  updateSource(): void {
    if (!this.editingSource || !this.newSource.name) return;

    this.incomeService.updateSource(this.editingSource.id, this.newSource).subscribe({
      next: () => {
        this.showSourceDialog = false;
        this.editingSource = null;
        this.newSource = { name: '', type: 'SALARY', currency: 'XOF' };
        this.loadSources();
      }
    });
  }

  // --- Source Delete ---

  initiateDeleteSource(source: IncomeSource): void {
    this.sourceToDelete = source;
    if (this.isPremium()) {
      this.showDeletePremiumDialog = true;
    } else {
      this.showDeleteConfirmDialog = true;
    }
  }

  confirmDeleteSource(): void {
    if (!this.sourceToDelete) return;

    this.incomeService.deleteSource(this.sourceToDelete.id).subscribe({
      next: () => {
        this.showDeleteConfirmDialog = false;
        this.showDeletePremiumDialog = false;
        this.sourceToDelete = null;
        this.loadSources();
        this.incomeService.notifyIncomeUpdated();
      }
    });
  }

  exportAndDelete(): void {
    if (!this.sourceToDelete) return;

    this.incomeService.getAllEntriesForSource(this.sourceToDelete.id).subscribe({
      next: (entries) => {
        const exportData = {
          source: {
            nom: this.sourceToDelete!.name,
            type: this.sourceToDelete!.type,
            devise: this.sourceToDelete!.currency
          },
          entries: entries.map(e => ({
            month: e.month,
            year: e.year,
            amount: e.amount,
            note: e.note
          })),
          exportedAt: new Date().toISOString()
        };

        const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `export-${this.sourceToDelete!.name.replace(/\s+/g, '-').toLowerCase()}.json`;
        a.click();
        URL.revokeObjectURL(url);

        this.confirmDeleteSource();
      },
      error: () => {
        this.showDeletePremiumDialog = false;
        this.sourceToDelete = null;
      }
    });
  }

  // --- Save Entries (Fix 1) ---

  saveEntries(): void {
    this.saving = true;
    let completed = 0;
    const total = this.entryForms.length;

    this.entryForms.forEach(entry => {
      const request: IncomeEntryRequest = {
        incomeSourceId: entry.sourceId,
        amount: entry.amount,
        month: this.selectedMonth,
        year: this.selectedYear
      };

      this.incomeService.createEntry(request).subscribe({
        next: () => {
          completed++;
          if (completed === total) {
            this.saving = false;
            this.loadEntries();
            this.incomeService.notifyIncomeUpdated();
          }
        },
        error: () => {
          completed++;
          if (completed === total) {
            this.saving = false;
            this.incomeService.notifyIncomeUpdated();
          }
        }
      });
    });
  }

  // --- Import (Fix 3) ---

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.processFile(files[0]);
    }
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.processFile(input.files[0]);
    }
  }

  private async processFile(file: File): Promise<void> {
    const ext = file.name.split('.').pop()?.toLowerCase();

    if (ext === 'json') {
      const text = await file.text();
      this.parseJsonImport(text);
    } else if (ext === 'csv') {
      const text = await file.text();
      this.parseCsvImport(text);
    } else if (ext === 'xlsx') {
      const arrayBuffer = await file.arrayBuffer();
      this.parseXlsxImport(arrayBuffer);
    }
  }

  // ── Extraction universelle depuis un objet ligne ──────────────────────────

  private extractRow(raw: Record<string, any>): Omit<ImportRow, 'status' | 'statusLabel' | 'valid'> {
    const amount = this.extractAmount(raw);
    const source = String(raw['Source'] ?? raw['source'] ?? raw['Source de revenu'] ?? '').trim();
    const note = String(raw['Note'] ?? raw['note'] ?? raw['Description'] ?? raw['description'] ?? '').trim();
    const { month, year } = this.extractMonthYear(raw);
    return { month, year, source, amount, note };
  }

  private extractAmount(raw: Record<string, any>): number {
    const value = String(raw['Montant'] ?? raw['montant'] ?? raw['amount'] ?? raw['Amount'] ?? 0)
      .replace(/\s/g, '').replace(',', '.');
    return parseFloat(value) || 0;
  }

  private extractMonthYear(raw: Record<string, any>): { month: number; year: number } {
    const dateRaw = raw['Date'] ?? raw['date'];
    if (dateRaw) return this.parseDateValue(dateRaw);

    const moisRaw = raw['Mois'] ?? raw['mois'] ?? raw['month'] ?? raw['Month'];
    const anneeRaw = raw['Année'] ?? raw['Annee'] ?? raw['annee'] ?? raw['year'] ?? raw['Year'] ?? raw['ANNEE'];
    if (moisRaw != null && anneeRaw != null) {
      return { month: parseInt(String(moisRaw), 10), year: parseInt(String(anneeRaw), 10) };
    }
    return { month: 0, year: 0 };
  }

  private parseDateValue(dateRaw: unknown): { month: number; year: number } {
    if (typeof dateRaw === 'number') {
      // Serial Excel : epoch 1900
      const d = new Date(Math.round((dateRaw - 25569) * 86400 * 1000));
      return { month: d.getUTCMonth() + 1, year: d.getUTCFullYear() };
    }
    const str = String(dateRaw).trim();
    const isoRegex = /^(\d{4})-(\d{2})/;
    const frRegex = /^(\d{1,2})[/-](\d{1,2})[/-](\d{4})/;
    const isoMatch = isoRegex.exec(str);
    if (isoMatch) {
      return { year: parseInt(isoMatch[1], 10), month: parseInt(isoMatch[2], 10) };
    }
    const frMatch = frRegex.exec(str);
    if (frMatch) {
      return { month: parseInt(frMatch[1], 10), year: parseInt(frMatch[3], 10) };
    }
    return { month: 0, year: 0 };
  }

  private extractJsonEntries(data: any): any[] {
    if (data?.entries && Array.isArray(data.entries)) return data.entries;
    if (Array.isArray(data)) return data;
    return [];
  }

  private parseJsonImport(text: string): void {
    try {
      const data = JSON.parse(text);
      const rows: ImportRow[] = [];

      const entries: any[] = this.extractJsonEntries(data);

      const sourceFallback = data.source?.nom ?? '';

      for (const entry of entries) {
        const base = this.extractRow(entry);
        rows.push({
          ...base,
          source: base.source || sourceFallback,
          status: 'new',
          statusLabel: 'Nouveau',
          valid: true
        });
      }

      this.validateAndPreview(rows);
    } catch {
      // json invalide
    }
  }

  private parseCsvImport(text: string): void {
    const sep = text.includes(';') ? ';' : ',';
    const lines = text.trim().split(/\r?\n/);
    if (lines.length < 2) return;

    // Détection de l'en-tête : on cherche la première ligne contenant
    // au moins une clé reconnue
    const knownKeys = ['source','montant','amount','date','mois','annee','année'];
    let headerIdx = 0;
    for (let i = 0; i < Math.min(lines.length, 5); i++) {
      const lower = lines[i].toLowerCase();
      if (knownKeys.some(k => lower.includes(k))) { headerIdx = i; break; }
    }

    const headers = lines[headerIdx]
      .split(sep)
      .map(h => h.trim().replace(/(^")|("$)/g, ''));

    const rows: ImportRow[] = [];
    for (let i = headerIdx + 1; i < lines.length; i++) {
      const line = lines[i].trim();
      if (!line) continue;
      const cols = line.split(sep).map(c => c.trim().replace(/(^")|("$)/g, ''));
      const obj: Record<string, any> = {};
      headers.forEach((h, idx) => { obj[h] = cols[idx] ?? ''; });
      const base = this.extractRow(obj);
      rows.push({ ...base, status: 'new', statusLabel: 'Nouveau', valid: true });
    }

    this.validateAndPreview(rows);
  }

  private async parseXlsxImport(buffer: ArrayBuffer): Promise<void> {
    try {
      const XLSX = await import('xlsx');
      const workbook = XLSX.read(buffer, { type: 'array', cellDates: false });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];

      // Lire brut pour détecter les lignes d'en-tête multiples
      const raw: any[][] = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '' });

      // Trouver la ligne d'en-tête : première ligne contenant une clé reconnue
      const knownKeys = ['source','montant','amount','date','mois','annee','année','description'];
      let headerIdx = 0;
      for (let i = 0; i < Math.min(raw.length, 5); i++) {
        const lower = raw[i].map((c: any) => String(c).toLowerCase());
        if (knownKeys.some(k => lower.includes(k))) { headerIdx = i; break; }
      }

      const headers: string[] = raw[headerIdx].map((h: any) => String(h).trim());
      const rows: ImportRow[] = [];

      for (let i = headerIdx + 1; i < raw.length; i++) {
        const cols = raw[i];
        if (!cols || cols.every((c: any) => c === '' || c == null)) continue;
        const obj: Record<string, any> = {};
        headers.forEach((h, idx) => { obj[h] = cols[idx] ?? ''; });
        const base = this.extractRow(obj);
        rows.push({ ...base, status: 'new', statusLabel: 'Nouveau', valid: true });
      }

      this.validateAndPreview(rows);
    } catch {
      // xlsx invalide
    }
  }

  private validateAndPreview(rows: ImportRow[]): void {
    const truncated = rows.length > 500 ? rows.slice(0, 500) : rows;
    const currentYear = new Date().getFullYear();
    for (const row of truncated) {
      this.markRowValidity(row, currentYear);
    }
    this.importPreviewRows = truncated;
    this.importNewSources = this.collectNewSources(truncated);
  }

  private markRowValidity(row: ImportRow, currentYear: number): void {
    const errors: string[] = [];
    if (!row.amount || row.amount <= 0) errors.push('montant invalide');
    if (!row.year || row.year < 2000 || row.year > currentYear) errors.push('année invalide');
    if (!row.month || row.month < 1 || row.month > 12) errors.push('mois invalide');

    if (errors.length > 0) {
      row.valid = false;
      row.status = 'invalid';
      row.statusLabel = errors.join(', ');
    }
  }

  private collectNewSources(rows: ImportRow[]): string[] {
    const existingNames = new Set(this.sources.map(s => s.name.toLowerCase()));
    const newNames = new Set<string>();
    for (const row of rows) {
      if (row.valid && row.source && !existingNames.has(row.source.toLowerCase())) {
        newNames.add(row.source);
      }
    }
    return [...newNames];
  }

  isNewSource(sourceName: string): boolean {
    if (!sourceName) return false;
    return !this.sources.some(s => s.name.toLowerCase() === sourceName.toLowerCase());
  }

  importAggregatedPreview(): { month: number; year: number; source: string; amount: number; valid: boolean }[] {
    const map = new Map<string, { month: number; year: number; source: string; amount: number; valid: boolean }>();
    for (const row of this.importPreviewRows) {
      const key = `${row.source}|${row.month}|${row.year}`;
      const prev = map.get(key);
      if (prev) {
        prev.amount += row.amount;
      } else {
        map.set(key, { month: row.month, year: row.year, source: row.source, amount: row.amount, valid: row.valid });
      }
    }
    return [...map.values()];
  }

  importValidCount(): number {
    return this.importPreviewRows.filter(r => r.valid).length;
  }

  importInvalidCount(): number {
    return this.importPreviewRows.filter(r => !r.valid).length;
  }

  getConflictCount(): number {
    return this.importPreviewRows.filter(r => r.status === 'conflict' || r.status === 'existing').length;
  }

  resetImport(): void {
    this.importPreviewRows = [];
    this.importTargetSourceId = '';
    this.importOverwrite = false;
    this.importing = false;
    this.aiHelpOpen = false;
    this.promptCopied = false;
    this.importNewSources = [];
  }

  copyPrompt(event: Event): void {
    event.stopPropagation();
    const text = `J'ai un fichier de revenus (joint). Convertis-le au format du modèle joint (colonnes : Source, Description, Montant, Devise, Date au format AAAA-MM-JJ). Chaque ligne doit représenter une transaction. Exporte le résultat en fichier Excel (.xlsx).`;
    navigator.clipboard.writeText(text).then(() => {
      this.promptCopied = true;
      setTimeout(() => this.promptCopied = false, 2000);
    });
  }

  confirmImport(): void {
    const validRows = this.importPreviewRows.filter(r => r.valid);
    if (validRows.length === 0) return;

    this.importing = true;

    // ── Phase 1 : créer les sources manquantes ────────────────────────────
    const sourcesToCreate = [...this.importNewSources];
    let createdCount = 0;

    const afterSourcesReady = () => {
      // Re-charger les sources pour avoir les IDs des nouvelles
      this.incomeService.getSources().subscribe({
        next: sources => {
          this.sources = sources;
          this.runAggregatedImport(validRows);
        },
        error: () => this.runAggregatedImport(validRows)
      });
    };

    if (sourcesToCreate.length === 0) {
      this.runAggregatedImport(validRows);
      return;
    }

    for (const name of sourcesToCreate) {
      this.incomeService.createSource({ name, type: 'OTHER', currency: 'XOF' }).subscribe({
        next: () => {
          createdCount++;
          if (createdCount === sourcesToCreate.length) afterSourcesReady();
        },
        error: () => {
          createdCount++;
          if (createdCount === sourcesToCreate.length) afterSourcesReady();
        }
      });
    }
  }

  private runAggregatedImport(validRows: ImportRow[]): void {
    // ── Phase 2 : agréger par sourceId|month|year ─────────────────────────
    const aggMap = new Map<string, { sourceId: string; month: number; year: number; amount: number; note: string }>();

    for (const row of validRows) {
      const sourceId = this.resolveSourceId(row.source);
      if (!sourceId) continue;
      const key = `${sourceId}|${row.month}|${row.year}`;
      const prev = aggMap.get(key);
      if (prev) {
        prev.amount += row.amount;
      } else {
        aggMap.set(key, { sourceId, month: row.month, year: row.year, amount: row.amount, note: row.note || '' });
      }
    }

    const aggregated = [...aggMap.values()];
    if (aggregated.length === 0) { this.importing = false; return; }

    // ── Phase 3 : précharger les entrées existantes ───────────────────────
    const existingMap = new Map<string, string>();
    const periods = [...new Set(aggregated.map(r => `${r.month}|${r.year}`))];
    let loadedCount = 0;

    const doImport = () => {
      let done = 0;
      let ok = 0;
      const total = aggregated.length;

      const tick = (success: boolean) => {
        done++;
        if (success) ok++;
        if (done === total) this.finishImport(ok, total - ok);
      };

      for (const agg of aggregated) {
        const req: IncomeEntryRequest = {
          incomeSourceId: agg.sourceId,
          amount: agg.amount,
          month: agg.month,
          year: agg.year,
          note: agg.note || undefined
        };

        const existingId = existingMap.get(`${agg.sourceId}|${agg.month}|${agg.year}`);

        if (existingId && !this.importOverwrite) { tick(false); continue; }

        const op$ = existingId
          ? this.incomeService.updateEntry(existingId, req)
          : this.incomeService.createEntry(req);

        op$.subscribe({ next: () => tick(true), error: () => tick(false) });
      }
    };

    for (const period of periods) {
      const [m, y] = period.split('|').map(Number);
      this.incomeService.getEntries(m, y).subscribe({
        next: entries => {
          entries.forEach(e => existingMap.set(`${e.incomeSourceId}|${e.month}|${e.year}`, e.id));
          loadedCount++;
          if (loadedCount === periods.length) doImport();
        },
        error: () => { loadedCount++; if (loadedCount === periods.length) doImport(); }
      });
    }
  }

  async downloadTemplate(): Promise<void> {
    const XLSX = await import('xlsx');

    const headers = ['Source', 'Description', 'Montant', 'Devise', 'Date'];
    const examples = [
      ['Salaire', 'Salaire mensuel', 350000, 'XOF', '2024-01-31'],
      ['Freelance', 'Mission développement web', 180000, 'XOF', '2024-02-15'],
      ['Location', 'Loyer appartement', 75000, 'XOF', '2024-02-01'],
      ['Salaire', 'Salaire mensuel', 350000, 'XOF', '2024-02-28'],
      ['Freelance', 'Consulting UI/UX', 95000, 'XOF', '2024-03-10'],
    ];

    const ws = XLSX.utils.aoa_to_sheet([headers, ...examples]);

    // Largeurs de colonnes
    ws['!cols'] = [
      { wch: 18 }, { wch: 38 }, { wch: 14 }, { wch: 8 }, { wch: 14 }
    ];

    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Revenus');
    XLSX.writeFile(wb, 'joseph-yusuf-modele-import.xlsx');
  }

  formatImportDate(month: number, year: number): string {
    if (!month || !year) return '—';
    const d = new Date(year, month - 1);
    return d.toLocaleDateString('fr-FR', { month: 'short', year: 'numeric' });
  }

  private resolveSourceId(sourceName: string): string | null {
    if (!sourceName) return null;
    const match = this.sources.find(s => s.name.toLowerCase() === sourceName.toLowerCase());
    return match ? match.id : null;
  }

  private finishImport(imported: number, skipped: number): void {
    this.importing = false;
    this.showImportDialog = false;
    this.resetImport();
    this.loadEntries();
    this.incomeService.notifyIncomeUpdated();
  }

  // --- Utilities ---

  toXOF(amount: number, currencyCode: string): number {
    const c = CURRENCIES.find(c => c.code === currencyCode);
    return Math.round(amount * (c?.rateToXOF ?? 1));
  }

  calculateTotalXOF(): number {
    return this.entryForms.reduce((sum, entry) =>
      sum + this.toXOF(entry.amount || 0, entry.currency), 0);
  }

  calculateTotal(): number {
    return this.calculateTotalXOF();
  }

  formatCurrency(amount: number): string {
    return this.currencyFormatter.format(amount);
  }

  isPremium(): boolean {
    const plan = this.authService.getPlan();
    return plan === 'PREMIUM' || plan === 'PREMIUM_PLUS';
  }

  isAddSourceDisabled(): boolean {
    const plan = this.authService.getPlan();
    return plan === 'FREE' && this.sources.length >= 1;
  }

  getAddSourceTooltip(): string {
    if (this.isAddSourceDisabled()) {
      return 'Passez en Premium pour ajouter plusieurs sources';
    }
    return '';
  }

  getSourceTypeLabel(type: IncomeSourceType): string {
    const found = this.sourceTypes.find(t => t.value === type);
    return found ? found.label : type;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ABUNDANCE': return 'status-abundance';
      case 'LEAN': return 'status-lean';
      case 'NORMAL': return 'status-normal';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ABUNDANCE': return 'Abondance';
      case 'LEAN': return 'Vaches maigres';
      case 'NORMAL': return 'Normal';
      default: return status;
    }
  }
}
