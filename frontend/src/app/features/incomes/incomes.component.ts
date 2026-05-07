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

interface SourceEntryForm {
  sourceId: string;
  sourceName: string;
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
              <p>Aucune source de revenu configuree.</p>
              <p class="empty-hint">Ajoutez votre premiere source pour commencer.</p>
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
                  <i class="pi pi-upload"></i> Importer des donnees
                </button>
                <div class="month-selector">
                  <select [(ngModel)]="selectedMonth" (ngModelChange)="loadEntries()" class="select-input">
                    <option *ngFor="let m of months" [ngValue]="m.value">{{ m.label }}</option>
                  </select>
                  <select [(ngModel)]="selectedYear" (ngModelChange)="loadEntries()" class="select-input">
                    <option *ngFor="let y of years" [ngValue]="y">{{ y }}</option>
                  </select>
                </div>
              </div>
            </div>

            <div class="entries-form" *ngIf="sources.length > 0">
              <div class="entry-row" *ngFor="let entry of entryForms">
                <label class="entry-label">{{ entry.sourceName }}</label>
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
                  <span class="entry-currency">XOF</span>
                </div>
              </div>

              <div class="entries-footer">
                <div class="total-display">
                  <span class="total-label">Total:</span>
                  <span class="total-amount">{{ formatCurrency(calculateTotal()) }}</span>
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
            <input
              type="text"
              [(ngModel)]="newSource.currency"
              placeholder="XOF"
              class="form-input"
            />
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
          <div class="import-upload-zone" (dragover)="onDragOver($event)" (drop)="onFileDrop($event)" (click)="fileInput.click()">
            <i class="pi pi-cloud-upload import-icon"></i>
            <p class="import-text">Glissez un fichier ici ou cliquez pour selectionner</p>
            <p class="import-formats">Formats acceptes : .xlsx, .csv, .json</p>
            <input #fileInput type="file" accept=".xlsx,.csv,.json" (change)="onFileSelect($event)" hidden />
          </div>

          <div class="form-group import-source-select">
            <label>Source de revenu cible (pour fichiers Excel/CSV)</label>
            <p-dropdown
              [options]="sourceOptions"
              [(ngModel)]="importTargetSourceId"
              optionLabel="label"
              optionValue="value"
              placeholder="Selectionnez une source"
              styleClass="dark-dropdown"
            ></p-dropdown>
          </div>

          <div class="form-group import-checkbox">
            <p-checkbox [(ngModel)]="importOverwrite" [binary]="true" label="Ecraser les entrees existantes"></p-checkbox>
          </div>
        </div>

        <!-- Preview Table -->
        <div class="import-preview" *ngIf="importPreviewRows.length > 0">
          <div class="import-summary">
            <span>{{ importPreviewRows.length }} lignes detectees</span>
            <span class="import-conflicts" *ngIf="getConflictCount() > 0">
              ({{ getConflictCount() }} conflits)
            </span>
          </div>

          <div class="import-table-wrapper">
            <table class="import-table">
              <thead>
                <tr>
                  <th>Mois</th>
                  <th>Annee</th>
                  <th>Source</th>
                  <th>Montant</th>
                  <th>Statut</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let row of importPreviewRows" [ngClass]="{'row-invalid': !row.valid}">
                  <td>{{ row.month }}</td>
                  <td>{{ row.year }}</td>
                  <td>{{ row.source }}</td>
                  <td>{{ formatCurrency(row.amount) }}</td>
                  <td>
                    <span class="import-status" [ngClass]="'import-status-' + row.status">
                      {{ row.statusLabel }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="import-actions">
            <button class="btn-cancel" (click)="resetImport()">Retour</button>
            <button class="btn-confirm" (click)="confirmImport()" [disabled]="importing">
              {{ importing ? 'Import en cours...' : 'Confirmer l\\'import' }}
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

    .entry-label {
      font-size: 0.9rem;
      color: #F0E8D0;
      font-weight: 500;
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

    .import-summary {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      font-size: 0.9rem;
      color: #F0E8D0;
    }

    .import-conflicts {
      color: #ff6b7a;
      font-size: 0.8rem;
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

  selectedMonth: number;
  selectedYear: number;

  newSource: IncomeSourceRequest = {
    name: '',
    type: 'SALARY',
    currency: 'XOF'
  };

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

  constructor(
    private incomeService: IncomeService,
    private authService: AuthService
  ) {
    const now = new Date();
    this.selectedMonth = now.getMonth() + 1;
    this.selectedYear = now.getFullYear();
    this.years = [now.getFullYear() - 1, now.getFullYear(), now.getFullYear() + 1];
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

  private parseJsonImport(text: string): void {
    try {
      const data = JSON.parse(text);
      const rows: ImportRow[] = [];

      if (data.entries && Array.isArray(data.entries)) {
        const sourceName = data.source?.nom || 'Import';
        for (const entry of data.entries) {
          rows.push({
            month: entry.month,
            year: entry.year,
            source: sourceName,
            amount: entry.amount,
            note: entry.note || '',
            status: 'new',
            statusLabel: 'Nouveau',
            valid: true
          });
        }
      } else if (Array.isArray(data)) {
        for (const entry of data) {
          rows.push({
            month: entry.month || entry.Mois,
            year: entry.year || entry.Année || entry.Annee,
            source: entry.source || entry.Source || '',
            amount: entry.amount || entry.Montant || 0,
            note: entry.note || entry.Note || '',
            status: 'new',
            statusLabel: 'Nouveau',
            valid: true
          });
        }
      }

      this.validateAndPreview(rows);
    } catch {
      // invalid json
    }
  }

  private parseCsvImport(text: string): void {
    const separator = text.includes(';') ? ';' : ',';
    const lines = text.trim().split('\n');
    if (lines.length < 2) return;

    const rows: ImportRow[] = [];
    for (let i = 1; i < lines.length; i++) {
      const cols = lines[i].split(separator).map(c => c.trim().replace(/^"|"$/g, ''));
      if (cols.length >= 4) {
        rows.push({
          month: parseInt(cols[0], 10),
          year: parseInt(cols[1], 10),
          source: cols[2],
          amount: parseFloat(cols[3].replace(/\s/g, '')) || 0,
          note: cols[4] || '',
          status: 'new',
          statusLabel: 'Nouveau',
          valid: true
        });
      }
    }

    this.validateAndPreview(rows);
  }

  private async parseXlsxImport(buffer: ArrayBuffer): Promise<void> {
    try {
      const XLSX = await import('xlsx');
      const workbook = XLSX.read(buffer, { type: 'array' });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      const data: any[] = XLSX.utils.sheet_to_json(sheet);

      const rows: ImportRow[] = data.map(row => ({
        month: row['Mois'] || row['mois'] || row['month'] || 0,
        year: row['Année'] || row['Annee'] || row['annee'] || row['year'] || 0,
        source: row['Source'] || row['source'] || '',
        amount: row['Montant'] || row['montant'] || row['amount'] || 0,
        note: row['Note'] || row['note'] || '',
        status: 'new' as const,
        statusLabel: 'Nouveau',
        valid: true
      }));

      this.validateAndPreview(rows);
    } catch {
      // xlsx parsing failed
    }
  }

  private validateAndPreview(rows: ImportRow[]): void {
    if (rows.length > 500) {
      rows = rows.slice(0, 500);
    }

    for (const row of rows) {
      const errors: string[] = [];
      if (!row.amount || row.amount <= 0) errors.push('montant invalide');
      if (!row.year || row.year < 2000 || row.year > 2030) errors.push('annee invalide');
      if (!row.month || row.month < 1 || row.month > 12) errors.push('mois invalide');

      if (errors.length > 0) {
        row.valid = false;
        row.status = 'invalid';
        row.statusLabel = errors.join(', ');
      }
    }

    this.importPreviewRows = rows;
  }

  getConflictCount(): number {
    return this.importPreviewRows.filter(r => r.status === 'conflict' || r.status === 'existing').length;
  }

  resetImport(): void {
    this.importPreviewRows = [];
    this.importTargetSourceId = '';
    this.importOverwrite = false;
    this.importing = false;
  }

  confirmImport(): void {
    const validRows = this.importPreviewRows.filter(r => r.valid);
    if (validRows.length === 0) return;

    this.importing = true;
    let completed = 0;
    let imported = 0;
    let skipped = 0;
    const total = validRows.length;

    const targetSourceId = this.importTargetSourceId || (this.sources.length > 0 ? this.sources[0].id : '');

    for (const row of validRows) {
      const sourceId = this.resolveSourceId(row.source) || targetSourceId;
      if (!sourceId) {
        completed++;
        skipped++;
        if (completed === total) this.finishImport(imported, skipped);
        continue;
      }

      const request: IncomeEntryRequest = {
        incomeSourceId: sourceId,
        amount: row.amount,
        month: row.month,
        year: row.year,
        note: row.note || undefined
      };

      this.incomeService.createEntry(request).subscribe({
        next: () => {
          completed++;
          imported++;
          if (completed === total) this.finishImport(imported, skipped);
        },
        error: () => {
          completed++;
          skipped++;
          if (completed === total) this.finishImport(imported, skipped);
        }
      });
    }
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

  calculateTotal(): number {
    return this.entryForms.reduce((sum, entry) => sum + (entry.amount || 0), 0);
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
