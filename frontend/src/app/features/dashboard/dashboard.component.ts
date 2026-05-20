import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { Subscription } from 'rxjs';
import { IncomeService } from '../../core/services/income.service';
import { RuleService } from '../../core/services/rule.service';
import { AuthService } from '../../core/auth/auth.service';
import { ReportService } from '../../core/services/report.service';
import { MonthSummary, MonthStatus, MoneyTips } from '../../shared/models/income.model';
import { AllocationResult, AllocationLine, RuleAvailability, RuleType, UserRuleConfigRequest } from '../../shared/models/rule.model';
import { Plan } from '../../shared/models/user.model';
import { SavingsWidgetComponent } from '../savings/savings-widget.component';
import { MoneyTipsModalComponent } from '../incomes/money-tips-modal/money-tips-modal.component';
import { Router } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, DialogModule, SavingsWidgetComponent, MoneyTipsModalComponent],
  template: `
    <div class="dashboard">

      <!-- Carte d'accueil — aucune donnée -->
      <section class="welcome-section" *ngIf="!summary && history.length === 0 && !loading">
        <div class="welcome-card">
          <div class="welcome-icon">✦</div>
          <h2 class="welcome-title">Bienvenue dans votre espace Joseph · Yusuf</h2>
          <p class="welcome-intro">
            Votre tableau de bord prend vie dès que vous enregistrez vos revenus.
            Le <strong>Principe de Joseph</strong> — épargner pendant l'abondance, tenir pendant la disette —
            repose sur au moins <strong>3 mois de données</strong> pour comparer votre mois actuel
            à votre moyenne et déterminer si vous traversez une période d'abondance ou de vaches maigres.
          </p>
          <div class="welcome-steps">
            <div class="step">
              <span class="step-num">1</span>
              <div>
                <strong>Saisissez vos revenus du mois</strong>
                <p>Rendez-vous dans « Mes Revenus » pour ajouter votre premier revenu.</p>
              </div>
            </div>
            <div class="step">
              <span class="step-num">2</span>
              <div>
                <strong>Importez votre historique si vous en avez</strong>
                <p>
                  Vous avez déjà des données sur Excel, CSV ou JSON ?
                  Notre système les met en forme automatiquement.
                  Importez-les depuis « Mes Revenus » → bouton <em>Importer</em> —
                  et reprenez votre aventure dans le principe de richesse divin sans stress.
                </p>
              </div>
            </div>
            <div class="step">
              <span class="step-num">3</span>
              <div>
                <strong>Laissez Joseph travailler pour vous</strong>
                <p>Après 3 mois, votre tableau de bord vous indiquera précisément où vous en êtes et comment répartir vos revenus.</p>
              </div>
            </div>
          </div>
          <a routerLink="/incomes" class="btn-start">Commencer maintenant →</a>
        </div>
      </section>

      <!-- Section 1: Summary Card -->
      <section class="summary-section" *ngIf="summary">
        <div class="summary-card">
          <div class="summary-header">
            <div>
              <span class="summary-label">{{ getSummaryLabel() }}</span>
              <h2 class="summary-amount">{{ formatCurrency(summary.totalIncome) }}</h2>
            </div>
            <span class="status-badge" [ngClass]="getStatusClass(summary.status)">
              {{ getStatusLabel(summary.status) }}
            </span>
          </div>

          <!-- Données insuffisantes : message d'accompagnement -->
          <div class="summary-meta" *ngIf="history.length < 3">
            <div class="insufficient-data">
              <span class="insufficient-icon">⏳</span>
              <div>
                <p class="insufficient-title">Encore {{ 3 - history.length }} mois de données nécessaires</p>
                <p class="insufficient-body">
                  Il faut au moins 3 mois de revenus pour déterminer si vous êtes en période
                  d'abondance ou de disette. Vous pouvez accélérer le processus en
                  <a routerLink="/incomes" class="link-import">important votre historique</a>
                  depuis un fichier Excel, CSV ou JSON.
                </p>
              </div>
            </div>
          </div>

          <!-- Comparaison normale -->
          <div class="summary-meta" *ngIf="history.length >= 3">
            <ng-container *ngIf="summary.averageLast3Months > 0; else noBaseline">
              <span class="percentage" [ngClass]="summary.percentageVsAverage >= 0 ? 'positive' : 'negative'">
                {{ summary.percentageVsAverage >= 0 ? '+' : '' }}{{ summary.percentageVsAverage | number:'1.0-1' }}%
              </span>
              <span class="vs-average">vs moyenne des 3 derniers mois</span>
            </ng-container>
            <ng-template #noBaseline>
              <span class="vs-average">Pas encore de moyenne disponible</span>
            </ng-template>
          </div>
        </div>

        <!-- Bloc conseil Joseph — jauge masquée en FREE -->
        <div class="joseph-advice" *ngIf="summary && history.length >= 1" [ngClass]="getAdviceClass()">
          <div class="advice-icon">{{ getAdviceIcon() }}</div>
          <div class="advice-body">
            <p class="advice-title">{{ getAdviceTitle() }}</p>
            <p class="advice-text">{{ getAdviceText() }}</p>

            <!-- Jauge proximité seuil — Premium uniquement -->
            <ng-container *ngIf="isPremium(); else gaugeBlocked">
              <div class="threshold-gauge" *ngIf="summary.monthsInBaseline > 0">
                <div class="gauge-labels">
                  <span class="gauge-label-lean">Disette</span>
                  <span class="gauge-label-normal">Normal</span>
                  <span class="gauge-label-abundance">Abondance</span>
                </div>
                <div class="gauge-track">
                  <div class="gauge-zone lean-zone"></div>
                  <div class="gauge-zone normal-zone"></div>
                  <div class="gauge-zone abundance-zone"></div>
                  <div class="gauge-cursor" [style.left]="getGaugeCursorPosition()"></div>
                </div>
                <p class="gauge-hint">{{ getThresholdHint() }}</p>
              </div>
            </ng-container>
            <ng-template #gaugeBlocked>
              <div class="gauge-premium-hint">
                <span>📊</span>
                <span>La jauge de positionnement et les seuils précis sont disponibles en <a routerLink="/subscription" class="gauge-upgrade-link">Premium</a>.</span>
              </div>
            </ng-template>
          </div>
        </div>
      </section>

      <!-- Section 2 : Réserve Joseph -->
      <section class="reserve-section" *ngIf="josephReserve !== null">
        <div class="reserve-card" [class.reserve-blurred]="!isPremium()">
          <div class="reserve-header">
            <span class="reserve-label">Réserve Joseph estimée</span>
            <span class="reserve-tooltip" title="Cumul des surplus d'épargne générés lors de vos mois d'abondance passés. Ce montant vous permet de tenir sereinement en période de disette.">ⓘ</span>
          </div>
          <div class="reserve-amount" [ngClass]="josephReserve > 0 ? 'positive' : 'neutral'">
            {{ formatCurrency(josephReserve) }}
          </div>
          <p class="reserve-sub" *ngIf="josephReserve === 0">
            Aucune réserve constituée. Vos mois d'abondance passés n'ont pas généré de surplus selon le Principe de Joseph.
          </p>
          <p class="reserve-sub" *ngIf="josephReserve > 0">
            Constitué sur {{ abundanceMonthsCount }} mois d'abondance. Ce coussin financier est votre protection pour les périodes difficiles.
          </p>

          <!-- Overlay Premium -->
          <div class="reserve-premium-overlay" *ngIf="!isPremium()">
            <div class="reserve-overlay-content">
              <span class="overlay-icon">✦</span>
              <p class="overlay-title">Votre réserve Joseph vous attend</p>
              <p class="overlay-sub">
                Passez en Premium pour découvrir combien vous auriez constitué grâce à vos mois d'abondance — et comment l'utiliser en période de disette.
              </p>
              <a routerLink="/subscription" class="btn-overlay-upgrade">Voir les offres Premium →</a>
            </div>
          </div>
        </div>
      </section>

      <!-- Section 3: Allocations -->
      <section class="allocations-section" *ngIf="allocations">
        <div class="section-header">
          <div class="section-header-left">
            <h3 class="section-title">Répartition du mois</h3>
            <span class="active-rule-badge">{{ getRuleLabel(allocations.rule) }}</span>
          </div>
          <div class="section-header-right">
            <button *ngIf="hasTipsAvailable" class="tips-button" (click)="openMoneyTips()">💡 Astuces répartition</button>
            <button class="btn-change-rule" (click)="showRuleDialog = true">Changer de règle</button>
          </div>
        </div>

        <div class="allocation-grid">
          <div class="allocation-card" *ngFor="let alloc of allocations.allocations">
            <div class="alloc-header">
              <span class="alloc-category">{{ alloc.category }}</span>
              <span class="alloc-percentage">{{ alloc.percentage }}%</span>
            </div>
            <div class="alloc-amount">{{ formatCurrency(alloc.amount) }}</div>
            <div class="alloc-bar-bg">
              <div class="alloc-bar" [style.width.%]="alloc.percentage" [style.background]="getAllocColor(alloc.category)"></div>
            </div>
          </div>
        </div>

        <!-- Message de la règle Joseph si active -->
        <div class="joseph-active-message" *ngIf="allocations.rule === 'RULE_JOSEPH' && allocations.message">
          <span class="suggestion-icon">✦</span>
          <p>{{ allocations.message }}</p>
        </div>

        <!-- Suggestion RULE_JOSEPH si non active -->
        <div class="joseph-suggestion" *ngIf="allocations.rule !== 'RULE_JOSEPH' && summary && josephComparison">
          <span class="suggestion-icon">✦</span>
          <div>
            <strong>Et avec le Principe de Joseph ?</strong>
            <p>
              Ce mois {{ summary.status === 'ABUNDANCE' ? "d'abondance" : summary.status === 'LEAN' ? 'de disette' : 'normal' }},
              la règle Joseph vous conseillerait :
              <span *ngFor="let a of josephComparison.allocations; let last = last">
                <strong>{{ a.percentage }}%</strong> {{ a.category }}{{ last ? '' : ', ' }}
              </span>.
              <ng-container *ngIf="summary.status === 'ABUNDANCE' && getJosephSavingsDelta() > 0"> Soit {{ formatCurrency(getJosephSavingsDelta()) }} d'épargne supplémentaire ce mois.</ng-container>
            </p>
            <ng-container *ngIf="summary && !isCurrentMonthData(); else canActivate">
              <div class="rule-change-warning">
                <span class="warning-icon">⚠</span>
                Pour activer le Principe de Joseph sur le mois en cours, saisissez d'abord votre revenu de
                <strong>{{ getCurrentMonthLabel() }}</strong> dans
                <a routerLink="/incomes" class="warning-link">Mes Revenus</a>.
              </div>
            </ng-container>
            <ng-template #canActivate>
              <button class="btn-switch-joseph" (click)="switchToJoseph()">Activer le Principe de Joseph</button>
            </ng-template>
          </div>
        </div>
      </section>

      <!-- Section 3: History -->
      <section class="history-section" *ngIf="history.length > 0">
        <div class="section-header">
          <h3 class="section-title">Historique ({{ history.length }} derniers mois saisis)</h3>
          <div class="report-actions" *ngIf="isPremium()">
            <!-- Sélecteur mensuel -->
            <div class="pdf-picker">
              <select [(ngModel)]="pdfMonth" class="pdf-select">
                <option *ngFor="let m of pdfMonths" [ngValue]="m.value">{{ m.label }}</option>
              </select>
              <select [(ngModel)]="pdfYear" class="pdf-select">
                <option *ngFor="let y of pdfYears" [ngValue]="y">{{ y }}</option>
              </select>
              <button class="btn-pdf"
                      [disabled]="generatingPdf"
                      (click)="downloadMonthlyPdf()">
                <span *ngIf="!generatingPdf">⬇ PDF mensuel</span>
                <span *ngIf="generatingPdf">Génération…</span>
              </button>
            </div>
            <!-- Sélecteur annuel -->
            <div class="pdf-picker">
              <select [(ngModel)]="pdfAnnualYear" class="pdf-select">
                <option *ngFor="let y of pdfYears" [ngValue]="y">{{ y }}</option>
              </select>
              <button class="btn-pdf btn-pdf-secondary"
                      [disabled]="generatingPdf"
                      (click)="downloadAnnualPdf()">
                <span *ngIf="!generatingPdf">⬇ PDF annuel</span>
                <span *ngIf="generatingPdf">Génération…</span>
              </button>
            </div>
          </div>
        </div>
        <div class="pdf-error" *ngIf="pdfError">{{ pdfError }}</div>
        <div class="history-table-wrapper">
          <table class="history-table">
            <thead>
              <tr>
                <th>Mois</th>
                <th>Revenu</th>
                <th>Statut</th>
                <th>
                  vs 3 mois précédents
                  <span class="th-hint">base de comparaison variable</span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let h of history">
                <td>{{ getMonthName(h.month, h.year) }}</td>
                <td>{{ formatCurrency(h.totalIncome) }}</td>
                <td>
                  <span class="status-badge small" [ngClass]="getStatusClass(h.status)">
                    {{ getStatusLabel(h.status) }}
                  </span>
                </td>
                <td>
                  <ng-container *ngIf="h.monthsInBaseline > 0; else noBase">
                    <span [ngClass]="h.percentageVsAverage >= 0 ? 'positive' : 'negative'">
                      {{ h.percentageVsAverage >= 0 ? '+' : '' }}{{ h.percentageVsAverage | number:'1.0-1' }}%
                    </span>
                    <span class="baseline-info" *ngIf="h.monthsInBaseline < 3"
                          [title]="'Basé sur ' + h.monthsInBaseline + ' mois (idéal : 3)'">
                      ⚠ {{ h.monthsInBaseline }}/3 mois
                    </span>
                  </ng-container>
                  <ng-template #noBase>
                    <span class="no-baseline">— 1er mois</span>
                  </ng-template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Section Objectifs d'Épargne -->
      <app-savings-widget></app-savings-widget>

      <!-- Bannière upgrade plan FREE -->
      <section class="upgrade-section" *ngIf="!isPremium()">
        <div class="upgrade-card">
          <div class="upgrade-left">
            <span class="upgrade-icon">✦</span>
            <div>
              <strong class="upgrade-title">Passez à Premium</strong>
              <p class="upgrade-desc">
                Sources illimitées, toutes les règles, import historique, rapports PDF mensuels.
              </p>
            </div>
          </div>
          <a routerLink="/subscription" class="btn-upgrade">Voir les plans →</a>
        </div>
      </section>

      <!-- Badge plan actif (Premium) -->
      <section class="plan-badge-section" *ngIf="isPremium()">
        <div class="plan-badge-card">
          <span class="plan-badge-icon">★</span>
          <span class="plan-badge-label">Plan {{ getPlanLabel() }}</span>
          <a routerLink="/subscription" class="btn-manage-plan">Gérer mon abonnement</a>
        </div>
      </section>

      <!-- Rule Selection Dialog -->
      <p-dialog
        header="Choisir une regle de repartition"
        [(visible)]="showRuleDialog"
        [modal]="true"
        [style]="{ width: '500px' }"
        [baseZIndex]="10000"
        [draggable]="false"
        [resizable]="false"
      >
        <div class="rules-list">
          <div class="rule-month-warning" *ngIf="summary && !isCurrentMonthData()">
            <span>⚠</span>
            <span>
              Vous consultez les données de <strong>{{ getMonthName(summary.month, summary.year) }}</strong>.
              Le changement de règle s'appliquera dès que vous aurez saisi votre revenu de
              <strong>{{ getCurrentMonthLabel() }}</strong>.
              <a routerLink="/incomes" (click)="showRuleDialog = false" class="warning-link"> Saisir maintenant →</a>
            </span>
          </div>
          <div
            class="rule-item"
            *ngFor="let rule of availableRules"
            [ngClass]="{ locked: rule.locked, active: allocations?.rule === rule.rule }"
            (click)="selectRule(rule)"
          >
            <div class="rule-info">
              <span class="rule-name">{{ rule.name }}</span>
              <span class="rule-badge premium" *ngIf="rule.locked">Premium</span>
            </div>
            <span class="rule-check" *ngIf="allocations?.rule === rule.rule">&#10003;</span>
          </div>
        </div>
      </p-dialog>

      <!-- Money Tips Modal -->
      <app-money-tips-modal
        [(visible)]="showTipsModal"
        [tips]="dashboardTips"
        [monthLabel]="dashboardTipsMonthLabel"
        (unlockRequested)="goToSubscription()"
        (dismissedForMonth)="onDashboardTipsDismiss()"
        (langChanged)="onDashboardTipsLangChanged($event)"
      ></app-money-tips-modal>
    </div>
  `,
  styles: [`
    .dashboard {
      padding: 2rem;
      padding-top: 5rem;
      max-width: 1100px;
      margin: 0 auto;
    }

    /* ── Carte d'accueil ── */
    .welcome-section {
      margin-bottom: 2.5rem;
    }

    .welcome-card {
      background: #1A1710;
      border: 1px solid rgba(201, 168, 76, 0.2);
      border-radius: 16px;
      padding: 2.5rem;
      text-align: center;
    }

    .welcome-icon {
      font-size: 2rem;
      color: #C9A84C;
      margin-bottom: 1rem;
    }

    .welcome-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.8rem;
      color: #F0E8D0;
      font-weight: 600;
      margin: 0 0 1rem;
    }

    .welcome-intro {
      color: #F0E8D0;
      opacity: 0.75;
      font-size: 0.95rem;
      line-height: 1.7;
      max-width: 680px;
      margin: 0 auto 2rem;
    }

    .welcome-intro strong {
      color: #C9A84C;
      opacity: 1;
    }

    .welcome-steps {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      text-align: left;
      max-width: 640px;
      margin: 0 auto 2rem;
    }

    .step {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
    }

    .step-num {
      flex-shrink: 0;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: rgba(201, 168, 76, 0.15);
      border: 1px solid rgba(201, 168, 76, 0.4);
      color: #C9A84C;
      font-size: 0.8rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-top: 2px;
    }

    .step strong {
      display: block;
      color: #F0E8D0;
      font-size: 0.9rem;
      margin-bottom: 0.25rem;
    }

    .step p {
      color: #F0E8D0;
      opacity: 0.6;
      font-size: 0.85rem;
      line-height: 1.6;
      margin: 0;
    }

    .step em {
      color: #C9A84C;
      font-style: normal;
      font-weight: 500;
    }

    .btn-start {
      display: inline-block;
      padding: 0.75rem 2rem;
      background: rgba(201, 168, 76, 0.15);
      border: 1px solid rgba(201, 168, 76, 0.5);
      border-radius: 8px;
      color: #C9A84C;
      font-size: 0.9rem;
      font-weight: 600;
      text-decoration: none;
      transition: background 0.2s;
    }

    .btn-start:hover {
      background: rgba(201, 168, 76, 0.25);
    }

    /* ── Données insuffisantes ── */
    .insufficient-data {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
      margin-top: 1rem;
      padding: 1rem 1.25rem;
      background: rgba(201, 168, 76, 0.05);
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 8px;
    }

    .insufficient-icon {
      font-size: 1.2rem;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .insufficient-title {
      font-size: 0.85rem;
      font-weight: 600;
      color: #C9A84C;
      margin: 0 0 0.3rem;
    }

    .insufficient-body {
      font-size: 0.82rem;
      color: #F0E8D0;
      opacity: 0.65;
      line-height: 1.6;
      margin: 0;
    }

    .link-import {
      color: #C9A84C;
      text-decoration: underline;
      text-underline-offset: 2px;
    }

    .summary-section {
      margin-bottom: 2.5rem;
    }

    .summary-card {
      background: #1A1710;
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 12px;
      padding: 1.5rem 2rem;
    }

    .summary-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .summary-label {
      font-size: 0.85rem;
      color: #F0E8D0;
      opacity: 0.6;
    }

    .summary-amount {
      font-family: 'Cormorant Garamond', serif;
      font-size: 2.2rem;
      color: #F0E8D0;
      margin: 0.25rem 0 0;
      font-weight: 600;
    }

    .status-badge {
      padding: 0.3rem 0.75rem;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .status-badge.small {
      padding: 0.2rem 0.5rem;
      font-size: 0.7rem;
    }

    .status-abundance {
      background: rgba(40, 167, 69, 0.15);
      color: #5cdb6f;
      border: 1px solid rgba(40, 167, 69, 0.3);
    }

    .status-lean {
      background: rgba(220, 53, 69, 0.15);
      color: #ff6b7a;
      border: 1px solid rgba(220, 53, 69, 0.3);
    }

    .status-normal {
      background: rgba(52, 152, 219, 0.15);
      color: #5dade2;
      border: 1px solid rgba(52, 152, 219, 0.3);
    }

    .summary-meta {
      margin-top: 1rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .percentage {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .positive {
      color: #5cdb6f;
    }

    .negative {
      color: #ff6b7a;
    }

    .vs-average {
      font-size: 0.8rem;
      color: #F0E8D0;
      opacity: 0.5;
    }

    .status-banner {
      margin-top: 1rem;
      padding: 1rem 1.25rem;
      border-radius: 8px;
      font-size: 0.85rem;
      line-height: 1.5;
    }

    .status-banner.abundance {
      background: rgba(40, 167, 69, 0.08);
      border: 1px solid rgba(40, 167, 69, 0.2);
      color: #5cdb6f;
    }

    .status-banner.lean {
      background: rgba(243, 156, 18, 0.08);
      border: 1px solid rgba(243, 156, 18, 0.2);
      color: #f5b041;
    }

    /* ── Conseil Joseph ── */
    .joseph-advice {
      margin-top: 1rem;
      padding: 1.25rem 1.5rem;
      border-radius: 10px;
      display: flex;
      gap: 1rem;
      align-items: flex-start;
    }

    .joseph-advice.advice-normal {
      background: rgba(201, 168, 76, 0.06);
      border: 1px solid rgba(201, 168, 76, 0.2);
    }

    .joseph-advice.advice-abundance {
      background: rgba(40, 167, 69, 0.06);
      border: 1px solid rgba(40, 167, 69, 0.25);
    }

    .joseph-advice.advice-lean {
      background: rgba(243, 156, 18, 0.06);
      border: 1px solid rgba(243, 156, 18, 0.25);
    }

    .advice-icon { font-size: 1.4rem; flex-shrink: 0; margin-top: 2px; }

    .advice-title {
      font-weight: 600;
      font-size: 0.9rem;
      color: #F0E8D0;
      margin: 0 0 0.35rem;
    }

    .advice-text {
      font-size: 0.85rem;
      color: #F0E8D0;
      opacity: 0.75;
      line-height: 1.6;
      margin: 0 0 0.75rem;
    }

    /* Jauge */
    .threshold-gauge { margin-top: 0.75rem; }

    .gauge-labels {
      display: flex;
      justify-content: space-between;
      font-size: 0.65rem;
      color: #F0E8D0;
      opacity: 0.5;
      margin-bottom: 0.3rem;
      text-transform: uppercase;
      letter-spacing: 0.4px;
    }

    .gauge-track {
      position: relative;
      height: 6px;
      border-radius: 3px;
      display: flex;
      overflow: visible;
    }

    .gauge-zone { height: 100%; }
    .lean-zone      { width: 30%; background: rgba(243,156,18,0.35); border-radius: 3px 0 0 3px; }
    .normal-zone    { width: 40%; background: rgba(52,152,219,0.25); }
    .abundance-zone { width: 30%; background: rgba(40,167,69,0.35); border-radius: 0 3px 3px 0; }

    .gauge-cursor {
      position: absolute;
      top: -3px;
      width: 12px;
      height: 12px;
      background: #C9A84C;
      border-radius: 50%;
      transform: translateX(-50%);
      border: 2px solid #0D0B07;
      transition: left 0.4s ease;
    }

    .gauge-hint {
      font-size: 0.72rem;
      color: #F0E8D0;
      opacity: 0.55;
      margin: 0.4rem 0 0;
    }

    /* ── Réserve Joseph ── */
    .reserve-section { margin-bottom: 2.5rem; }

    .reserve-card {
      background: #1A1710;
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 12px;
      padding: 1.25rem 1.5rem;
      position: relative;
      overflow: hidden;
    }

    .reserve-card.reserve-blurred {
      min-height: 280px;
    }

    .reserve-card.reserve-blurred .reserve-header,
    .reserve-card.reserve-blurred .reserve-amount,
    .reserve-card.reserve-blurred .reserve-sub {
      filter: blur(6px);
      user-select: none;
      pointer-events: none;
    }

    .reserve-premium-overlay {
      position: absolute;
      inset: 0;
      background: rgba(13, 11, 7, 0.55);
      backdrop-filter: blur(2px);
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 12px;
    }

    .reserve-overlay-content {
      text-align: center;
      padding: 1.5rem 2rem;
      max-width: 380px;
    }

    .overlay-icon {
      display: block;
      font-size: 1.5rem;
      color: #C9A84C;
      margin-bottom: 0.65rem;
    }

    .overlay-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.15rem;
      font-weight: 600;
      color: #F0E8D0;
      margin-bottom: 0.6rem;
    }

    .overlay-sub {
      font-size: 0.82rem;
      color: rgba(240, 232, 208, 0.65);
      line-height: 1.6;
      margin-bottom: 1.1rem;
    }

    .btn-overlay-upgrade {
      display: inline-block;
      padding: 0.55rem 1.4rem;
      background: #C9A84C;
      color: #0D0B07;
      border-radius: 8px;
      font-size: 0.82rem;
      font-weight: 700;
      text-decoration: none;
      transition: background 0.2s;
    }

    .btn-overlay-upgrade:hover { background: #DAC372; }

    .gauge-premium-hint {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-top: 0.75rem;
      padding: 0.5rem 0.75rem;
      background: rgba(201, 168, 76, 0.05);
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 6px;
      font-size: 0.77rem;
      color: rgba(240, 232, 208, 0.5);
    }

    .gauge-upgrade-link {
      color: #C9A84C;
      text-decoration: underline;
      text-underline-offset: 2px;
    }

    .reserve-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
    }

    .reserve-label {
      font-size: 0.8rem;
      color: #F0E8D0;
      opacity: 0.6;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .reserve-tooltip {
      font-size: 0.75rem;
      color: #C9A84C;
      cursor: help;
      opacity: 0.7;
    }

    .reserve-amount {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.8rem;
      font-weight: 600;
      margin-bottom: 0.4rem;
    }

    .reserve-amount.positive { color: #5cdb6f; }
    .reserve-amount.neutral  { color: #F0E8D0; opacity: 0.5; }

    .reserve-sub {
      font-size: 0.8rem;
      color: #F0E8D0;
      opacity: 0.55;
      line-height: 1.6;
      margin: 0;
    }

    /* ── Suggestion Joseph ── */
    .joseph-suggestion, .joseph-active-message {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
      margin-top: 1.25rem;
      padding: 1rem 1.25rem;
      background: rgba(201, 168, 76, 0.05);
      border: 1px solid rgba(201, 168, 76, 0.2);
      border-radius: 8px;
    }

    .suggestion-icon {
      color: #C9A84C;
      font-size: 1rem;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .joseph-suggestion strong {
      display: block;
      font-size: 0.85rem;
      color: #C9A84C;
      margin-bottom: 0.3rem;
    }

    .joseph-suggestion p, .joseph-active-message p {
      font-size: 0.82rem;
      color: #F0E8D0;
      opacity: 0.75;
      line-height: 1.6;
      margin: 0 0 0.75rem;
    }

    .joseph-active-message p { margin: 0; opacity: 0.85; }

    .btn-switch-joseph {
      padding: 0.4rem 0.85rem;
      background: rgba(201, 168, 76, 0.15);
      border: 1px solid rgba(201, 168, 76, 0.4);
      border-radius: 6px;
      color: #C9A84C;
      font-size: 0.78rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
    }

    .btn-switch-joseph:hover { background: rgba(201, 168, 76, 0.25); }

    .rule-change-warning {
      display: flex;
      align-items: flex-start;
      gap: 0.5rem;
      padding: 0.65rem 0.85rem;
      background: rgba(243, 156, 18, 0.08);
      border: 1px solid rgba(243, 156, 18, 0.25);
      border-radius: 6px;
      font-size: 0.8rem;
      color: rgba(240, 232, 208, 0.75);
      line-height: 1.5;
      margin-top: 0.25rem;
    }

    .warning-icon { color: #f5b041; flex-shrink: 0; }

    .warning-link {
      color: #C9A84C;
      text-decoration: underline;
      text-underline-offset: 2px;
    }

    .rule-month-warning {
      display: flex;
      gap: 0.6rem;
      align-items: flex-start;
      padding: 0.75rem 1rem;
      background: rgba(243, 156, 18, 0.07);
      border: 1px solid rgba(243, 156, 18, 0.2);
      border-radius: 8px;
      font-size: 0.82rem;
      color: rgba(240, 232, 208, 0.7);
      line-height: 1.55;
      margin-bottom: 0.75rem;
    }

    .rule-month-warning span:first-child { color: #f5b041; flex-shrink: 0; margin-top: 1px; }

    .allocations-section {
      margin-bottom: 2.5rem;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.25rem;
    }

    .section-header-left {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .active-rule-badge {
      font-size: 0.72rem;
      font-weight: 600;
      color: #C9A84C;
      background: rgba(201, 168, 76, 0.1);
      border: 1px solid rgba(201, 168, 76, 0.3);
      padding: 0.2rem 0.6rem;
      border-radius: 20px;
      letter-spacing: 0.02em;
      white-space: nowrap;
    }

    .section-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.3rem;
      color: #F0E8D0;
      margin: 0 0 1.25rem;
    }

    .section-header .section-title {
      margin: 0;
    }

    .section-header-right {
      display: flex;
      align-items: center;
      gap: 0.6rem;
    }

    .tips-button {
      display: flex;
      align-items: center;
      gap: 0.3rem;
      padding: 0.35rem 0.8rem;
      background: rgba(201,168,76,0.12);
      border: 1px solid rgba(201,168,76,0.4);
      border-radius: 20px;
      color: #C9A84C;
      font-size: 0.75rem;
      font-weight: 600;
      cursor: pointer;
      animation: tipsPulse 2s ease-in-out infinite;
      transition: background 0.2s;
      white-space: nowrap;
    }
    .tips-button:hover { background: rgba(201,168,76,0.22); }
    @keyframes tipsPulse {
      0%, 100% { box-shadow: 0 0 0 0 rgba(201,168,76,0.3); }
      50% { box-shadow: 0 0 0 4px rgba(201,168,76,0); }
    }

    .btn-change-rule {
      padding: 0.5rem 1rem;
      background: rgba(201, 168, 76, 0.1);
      border: 1px solid rgba(201, 168, 76, 0.3);
      border-radius: 6px;
      color: #C9A84C;
      font-size: 0.8rem;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }

    .btn-change-rule:hover {
      background: rgba(201, 168, 76, 0.2);
    }

    .allocation-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 1rem;
    }

    .allocation-card {
      background: #1A1710;
      border: 1px solid rgba(201, 168, 76, 0.1);
      border-radius: 10px;
      padding: 1.25rem;
    }

    .alloc-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
    }

    .alloc-category {
      font-size: 0.85rem;
      color: #F0E8D0;
      font-weight: 500;
    }

    .alloc-percentage {
      font-size: 0.8rem;
      color: #C9A84C;
      font-weight: 600;
    }

    .alloc-amount {
      font-size: 1.1rem;
      color: #F0E8D0;
      font-weight: 600;
      margin-bottom: 0.75rem;
    }

    .alloc-bar-bg {
      height: 4px;
      background: rgba(201, 168, 76, 0.1);
      border-radius: 2px;
      overflow: hidden;
    }

    .alloc-bar {
      height: 100%;
      border-radius: 2px;
      transition: width 0.4s ease;
    }

    .history-section {
      margin-bottom: 2rem;
    }

    .report-actions {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      align-items: flex-end;
    }

    .pdf-picker {
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }

    .pdf-select {
      padding: 0.3rem 0.5rem;
      background: rgba(13, 11, 7, 0.7);
      border: 1px solid rgba(201, 168, 76, 0.2);
      border-radius: 5px;
      color: #F0E8D0;
      font-size: 0.75rem;
      outline: none;
      cursor: pointer;
    }

    .pdf-select:focus {
      border-color: rgba(201, 168, 76, 0.5);
    }

    .btn-pdf {
      padding: 0.4rem 0.9rem;
      background: rgba(201, 168, 76, 0.1);
      border: 1px solid rgba(201, 168, 76, 0.35);
      border-radius: 6px;
      color: #C9A84C;
      font-size: 0.78rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
      white-space: nowrap;
    }

    .btn-pdf:hover:not(:disabled) { background: rgba(201, 168, 76, 0.2); }
    .btn-pdf:disabled { opacity: 0.45; cursor: not-allowed; }

    .btn-pdf-secondary {
      background: transparent;
      border-color: rgba(201, 168, 76, 0.2);
      color: #F0E8D0;
      opacity: 0.7;
    }

    .btn-pdf-secondary:hover:not(:disabled) {
      background: rgba(201, 168, 76, 0.08);
      opacity: 1;
    }

    .pdf-error {
      font-size: 0.8rem;
      color: #ff6b7a;
      margin-bottom: 0.75rem;
    }

    .history-table-wrapper {
      overflow-x: auto;
    }

    .history-table {
      width: 100%;
      border-collapse: collapse;
      background: #1A1710;
      border-radius: 10px;
      overflow: hidden;
    }

    .th-hint {
      display: block;
      font-size: 0.65rem;
      font-weight: 400;
      opacity: 0.5;
      text-transform: none;
      letter-spacing: 0;
      margin-top: 2px;
    }

    .baseline-info {
      display: inline-block;
      margin-left: 0.4rem;
      font-size: 0.7rem;
      color: #f5b041;
      cursor: help;
    }

    .no-baseline {
      color: #F0E8D0;
      opacity: 0.35;
      font-size: 0.85rem;
    }

    .history-table th {
      text-align: left;
      padding: 0.85rem 1.25rem;
      font-size: 0.8rem;
      font-weight: 600;
      color: #F0E8D0;
      opacity: 0.6;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      border-bottom: 1px solid rgba(201, 168, 76, 0.1);
    }

    .history-table td {
      padding: 0.85rem 1.25rem;
      font-size: 0.9rem;
      color: #F0E8D0;
      border-bottom: 1px solid rgba(201, 168, 76, 0.05);
    }

    .history-table tbody tr:last-child td {
      border-bottom: none;
    }

    .rules-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .rule-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.25rem;
      background: rgba(13, 11, 7, 0.5);
      border: 1px solid rgba(201, 168, 76, 0.1);
      border-radius: 8px;
      cursor: pointer;
      transition: border-color 0.2s, background 0.2s;
    }

    .rule-item:hover:not(.locked) {
      border-color: rgba(201, 168, 76, 0.3);
      background: rgba(201, 168, 76, 0.05);
    }

    .rule-item.active {
      border-color: #C9A84C;
      background: rgba(201, 168, 76, 0.08);
    }

    /* ── Upgrade / Plan badge ── */
    .upgrade-section { margin: 2rem 0; }

    .upgrade-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1.25rem 1.5rem;
      background: linear-gradient(135deg, rgba(201, 168, 76, 0.08) 0%, rgba(201, 168, 76, 0.04) 100%);
      border: 1px solid rgba(201, 168, 76, 0.25);
      border-radius: 12px;
      gap: 1rem;
    }

    .upgrade-left {
      display: flex;
      align-items: flex-start;
      gap: 1rem;
    }

    .upgrade-icon {
      font-size: 1.4rem;
      color: #C9A84C;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .upgrade-title {
      display: block;
      font-size: 0.95rem;
      color: #F0E8D0;
      font-weight: 600;
      margin-bottom: 0.25rem;
    }

    .upgrade-desc {
      font-size: 0.82rem;
      color: #F0E8D0;
      opacity: 0.6;
      margin: 0;
      line-height: 1.4;
    }

    .btn-upgrade {
      flex-shrink: 0;
      padding: 0.6rem 1.25rem;
      background: #C9A84C;
      color: #0D0B07;
      border-radius: 8px;
      font-size: 0.85rem;
      font-weight: 700;
      text-decoration: none;
      transition: background 0.2s;
      white-space: nowrap;
    }

    .btn-upgrade:hover { background: #DAC372; }

    .plan-badge-section { margin: 1.5rem 0; }

    .plan-badge-card {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 1.25rem;
      background: rgba(92, 219, 111, 0.06);
      border: 1px solid rgba(92, 219, 111, 0.2);
      border-radius: 10px;
    }

    .plan-badge-icon { color: #5cdb6f; font-size: 1rem; }

    .plan-badge-label {
      font-size: 0.85rem;
      color: #5cdb6f;
      font-weight: 600;
      flex: 1;
    }

    .btn-manage-plan {
      font-size: 0.78rem;
      color: #C9A84C;
      text-decoration: underline;
      cursor: pointer;
    }

    .rule-item.locked {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .rule-info {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .rule-name {
      font-size: 0.9rem;
      color: #F0E8D0;
      font-weight: 500;
    }

    .rule-badge.premium {
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
      font-size: 0.65rem;
      font-weight: 600;
      background: rgba(201, 168, 76, 0.2);
      color: #C9A84C;
      text-transform: uppercase;
    }

    .rule-check {
      color: #C9A84C;
      font-size: 1.1rem;
    }

    @media (max-width: 768px) {
      .dashboard {
        padding: 1rem;
        padding-top: 5rem;
      }

      .allocation-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {
  summary: MonthSummary | null = null;
  allocations: AllocationResult | null = null;
  josephComparison: AllocationResult | null = null;
  history: MonthSummary[] = [];
  availableRules: RuleAvailability[] = [];
  showRuleDialog = false;
  loading = true;
  josephReserve: number | null = null;
  abundanceMonthsCount = 0;
  generatingPdf = false;
  pdfError = '';

  // Money Tips
  hasTipsAvailable = false;
  showTipsModal = false;
  dashboardTips: MoneyTips | null = null;
  dashboardTipsMonthLabel = '';
  private dashboardTipsMonth = 0;
  private dashboardTipsYear = 0;

  // Sélecteurs PDF
  pdfMonth: number;
  pdfYear: number;
  pdfAnnualYear: number;
  pdfMonths = [
    { label: 'Janvier',   value: 1  },
    { label: 'Février',   value: 2  },
    { label: 'Mars',      value: 3  },
    { label: 'Avril',     value: 4  },
    { label: 'Mai',       value: 5  },
    { label: 'Juin',      value: 6  },
    { label: 'Juillet',   value: 7  },
    { label: 'Août',      value: 8  },
    { label: 'Septembre', value: 9  },
    { label: 'Octobre',   value: 10 },
    { label: 'Novembre',  value: 11 },
    { label: 'Décembre',  value: 12 }
  ];
  pdfYears: number[] = [];

  private updateSub!: Subscription;

  private readonly currencyFormatter = new Intl.NumberFormat('fr-SN', {
    style: 'currency',
    currency: 'XOF',
    maximumFractionDigits: 0
  });

  private readonly allocColors: Record<string, string> = {
    'Besoins': '#C9A84C',
    'Envies': '#DAC372',
    'Epargne': '#5cdb6f',
    'Investissement': '#5dade2',
    'Don': '#bb8fce',
    'Epargne Joseph': '#5cdb6f',
    'Depenses courantes': '#C9A84C'
  };

  constructor(
    private readonly incomeService: IncomeService,
    private readonly ruleService: RuleService,
    private readonly authService: AuthService,
    private readonly reportService: ReportService,
    private readonly router: Router
  ) {
    const now = new Date();
    this.pdfMonth = now.getMonth() + 1;
    this.pdfYear = now.getFullYear();
    this.pdfAnnualYear = now.getFullYear();
    // Années disponibles : de 2020 à l'année courante
    for (let y = now.getFullYear(); y >= 2020; y--) {
      this.pdfYears.push(y);
    }
  }

  ngOnInit(): void {
    this.loadDashboardData();

    this.updateSub = this.incomeService.incomeUpdated$.subscribe(() => {
      this.loadDashboardData();
    });
  }

  ngOnDestroy(): void {
    this.updateSub?.unsubscribe();
  }

  private loadDashboardData(): void {
    this.ruleService.getAvailableRules().subscribe({
      next: (rules) => this.availableRules = rules
    });

    // Charger l'historique en premier pour déterminer le mois de référence
    this.incomeService.getHistory(12).subscribe({
      next: (history) => {
        this.history = history;
        this.loading = false;
        this.computeJosephReserve(history);

        // Mois de référence = dernier mois saisi, ou mois courant si rien
        const now = new Date();
        const refMonth = history.length > 0 ? history[0].month : now.getMonth() + 1;
        const refYear  = history.length > 0 ? history[0].year  : now.getFullYear();

        this.incomeService.getSummary(refMonth, refYear).subscribe({
          next: (summary) => {
            this.summary = summary;
            this.loadAllocations(summary, refMonth, refYear);
            this.loadJosephComparison(summary, refMonth, refYear);
            if (summary.totalIncome > 0) {
              this.checkDashboardTips(refMonth, refYear);
            }
          },
          error: () => {
            this.summary = null;
          }
        });
      },
      error: () => { this.loading = false; }
    });
  }

  private loadAllocations(summary: MonthSummary, month: number, year: number): void {
    if (!summary || summary.totalIncome === 0) return;
    this.ruleService.getConfig().subscribe({
      next: (config) => {
        this.ruleService.calculate({
          rule: config.activeRule,
          totalIncome: summary.totalIncome,
          month,
          year
        }).subscribe({
          next: (result) => this.allocations = result,
          error: () => this.allocations = null
        });
      }
    });
  }

  private loadJosephComparison(summary: MonthSummary, month: number, year: number): void {
    if (!summary || summary.totalIncome === 0) return;
    this.ruleService.calculate({
      rule: 'RULE_JOSEPH',
      totalIncome: summary.totalIncome,
      month,
      year
    }).subscribe({
      next: (result) => this.josephComparison = result,
      error: () => this.josephComparison = null
    });
  }

  private computeJosephReserve(history: MonthSummary[]): void {
    // Estime la réserve : pour chaque mois d'abondance, le surplus est
    // (revenu - seuil d'abondance) × 20% (part épargne standard)
    let reserve = 0;
    let count = 0;
    for (const h of history) {
      if (h.status === 'ABUNDANCE' && h.averageLast3Months > 0) {
        const surplus = h.totalIncome - h.abundanceThreshold;
        reserve += surplus * 0.20;
        count++;
      }
    }
    this.josephReserve = Math.round(reserve);
    this.abundanceMonthsCount = count;
  }

  // ── Conseil Joseph ──

  getAdviceClass(): string {
    if (!this.summary || this.summary.monthsInBaseline === 0) return 'advice-normal';
    switch (this.summary.status) {
      case 'ABUNDANCE': return 'advice-abundance';
      case 'LEAN':      return 'advice-lean';
      default:          return 'advice-normal';
    }
  }

  getAdviceIcon(): string {
    if (!this.summary || this.summary.monthsInBaseline === 0) return '📊';
    switch (this.summary.status) {
      case 'ABUNDANCE': return '🌿';
      case 'LEAN':      return '⚠️';
      default:          return '⚖️';
    }
  }

  getAdviceTitle(): string {
    if (!this.summary) return '';
    if (this.summary.monthsInBaseline === 0) return 'Début de votre historique';
    switch (this.summary.status) {
      case 'ABUNDANCE': return 'Mois d\'abondance — constituez votre réserve';
      case 'LEAN':      return 'Mois difficile — puisez dans votre réserve';
      default:          return 'Mois dans la normale — continuez sur votre lancée';
    }
  }

  getAdviceText(): string {
    if (!this.summary) return '';
    if (this.summary.monthsInBaseline === 0)
      return 'Ce mois est votre point de départ. Il servira de référence pour les prochains mois.';

    const pct = Math.abs(this.summary.percentageVsAverage).toFixed(1);
    switch (this.summary.status) {
      case 'ABUNDANCE':
        return `Votre revenu dépasse votre moyenne de ${pct}%. C'est le moment d'épargner le surplus plutôt que de l'augmenter vos dépenses — Joseph mettait de côté pendant les 7 années grasses pour traverser les 7 années maigres.`;
      case 'LEAN':
        return `Votre revenu est inférieur de ${pct}% à votre moyenne. Réduisez les dépenses non-essentielles et utilisez l'épargne constituée pendant vos mois d'abondance. Ne vous endettez pas.`;
      default:
        return `Votre revenu est dans votre fourchette normale (±${pct}% vs moyenne). Continuez à appliquer votre règle de répartition sans changement.`;
    }
  }

  getGaugeCursorPosition(): string {
    if (!this.summary || this.summary.averageLast3Months === 0) return '50%';
    // Lean < 85% | Normal 85-115% | Abundance > 115%
    // On mappe [50% — 200%] du ratio sur la jauge [0% — 100%]
    const ratio = this.summary.totalIncome / this.summary.averageLast3Months;
    const clamped = Math.min(Math.max(ratio, 0.5), 2.0);
    const pos = ((clamped - 0.5) / 1.5) * 100;
    return `${pos.toFixed(1)}%`;
  }

  getThresholdHint(): string {
    if (!this.summary || this.summary.averageLast3Months === 0) return '';
    const pct = this.summary.percentageVsAverage;
    if (this.summary.status === 'ABUNDANCE') {
      const aboveThreshold = pct - 15;
      return `Vous êtes ${aboveThreshold.toFixed(1)}% au-dessus du seuil d'abondance (15%).`;
    }
    if (this.summary.status === 'LEAN') {
      const belowThreshold = Math.abs(pct) - 15;
      return `Vous êtes ${belowThreshold.toFixed(1)}% en dessous du seuil de disette (-15%).`;
    }
    if (pct >= 0) {
      const toAbundance = 15 - pct;
      return `À ${toAbundance.toFixed(1)}% du seuil d'abondance (+15%).`;
    } else {
      const toLean = 15 - Math.abs(pct);
      return `À ${toLean.toFixed(1)}% du seuil de disette (-15%).`;
    }
  }

  getJosephSavingsDelta(): number {
    if (!this.josephComparison || !this.allocations) return 0;
    const josephSavings = this.josephComparison.allocations[1]?.amount ?? 0;
    const currentSavings = this.allocations.allocations[2]?.amount ?? 0;
    return Math.max(0, josephSavings - currentSavings);
  }

  switchToJoseph(): void {
    if (!this.isCurrentMonthData()) return;
    const request: UserRuleConfigRequest = {
      activeRule: 'RULE_JOSEPH',
      josephAbundanceSavingsPercent: 40,
      josephLeanSavingsPercent: 10
    };
    this.ruleService.updateConfig(request).subscribe({
      next: () => {
        this.showRuleDialog = false;
        if (this.summary) this.loadAllocations(this.summary, this.summary.month, this.summary.year);
      }
    });
  }

  formatCurrency(amount: number): string {
    return this.currencyFormatter.format(amount);
  }

  getStatusClass(status: MonthStatus): string {
    switch (status) {
      case 'ABUNDANCE': return 'status-abundance';
      case 'LEAN': return 'status-lean';
      case 'NORMAL': return 'status-normal';
    }
  }

  getStatusLabel(status: MonthStatus): string {
    switch (status) {
      case 'ABUNDANCE': return 'Abondance';
      case 'LEAN': return 'Vaches maigres';
      case 'NORMAL': return 'Normal';
    }
  }

  isCurrentMonthData(): boolean {
    if (!this.summary) return false;
    const now = new Date();
    return this.summary.month === now.getMonth() + 1 && this.summary.year === now.getFullYear();
  }

  getCurrentMonthLabel(): string {
    const now = new Date();
    return this.getMonthName(now.getMonth() + 1, now.getFullYear());
  }

  getSummaryLabel(): string {
    if (!this.summary) return 'Revenu total du mois';
    const now = new Date();
    const isCurrentMonth = this.summary.month === now.getMonth() + 1
      && this.summary.year === now.getFullYear();
    if (isCurrentMonth) return 'Revenu total du mois';
    return `Revenu — ${this.getMonthName(this.summary.month, this.summary.year)}`;
  }

  getMonthName(month: number, year: number): string {
    const date = new Date(year, month - 1);
    const name = date.toLocaleDateString('fr-FR', { month: 'long' });
    return name.charAt(0).toUpperCase() + name.slice(1) + ' ' + year;
  }

  getAllocColor(category: string): string {
    return this.allocColors[category] ?? '#C9A84C';
  }

  getRuleLabel(rule: string): string {
    switch (rule) {
      case 'RULE_50_30_20': return '50 / 30 / 20';
      case 'RULE_80_20':    return '80 / 20 — Pareto';
      case 'RULE_70_20_10': return '70 / 20 / 10';
      case 'RULE_JOSEPH':   return 'Principe de Joseph';
      default:              return rule;
    }
  }

  selectRule(rule: RuleAvailability): void {
    if (rule.locked) return;

    const request: UserRuleConfigRequest = {
      activeRule: rule.rule,
      josephAbundanceSavingsPercent: 40,
      josephLeanSavingsPercent: 10
    };

    this.ruleService.updateConfig(request).subscribe({
      next: () => {
        this.showRuleDialog = false;
        if (this.summary) this.loadAllocations(this.summary, this.summary.month, this.summary.year);
      }
    });
  }

  isPremium(): boolean {
    const plan = this.authService.getPlan();
    return plan === 'PREMIUM' || plan === 'PREMIUM_PLUS';
  }

  getPlanLabel(): string {
    const plan = this.authService.getPlan();
    switch (plan) {
      case 'PREMIUM': return 'Premium';
      case 'PREMIUM_PLUS': return 'Premium +';
      default: return 'Free';
    }
  }

  downloadMonthlyPdf(): void {
    this.triggerPdfDownload(
      this.reportService.generateMonthly(this.pdfMonth, this.pdfYear),
      `rapport-mensuel-${this.pdfYear}-${String(this.pdfMonth).padStart(2, '0')}.pdf`
    );
  }

  downloadAnnualPdf(): void {
    this.triggerPdfDownload(
      this.reportService.generateAnnual(this.pdfAnnualYear),
      `rapport-annuel-${this.pdfAnnualYear}.pdf`
    );
  }

  private triggerPdfDownload(generate$: any, fallbackName: string): void {
    this.generatingPdf = true;
    this.pdfError = '';
    generate$.subscribe({
      next: (report: any) => {
        // Le backend génère le PDF et retourne la référence — on télécharge ensuite
        this.reportService.download(report.id).subscribe({
          next: (blob: Blob) => {
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = report.fileName || fallbackName;
            a.click();
            URL.revokeObjectURL(url);
            this.generatingPdf = false;
          },
          error: () => {
            this.generatingPdf = false;
            this.pdfError = 'Téléchargement impossible. Réessayez.';
          }
        });
      },
      error: (err: any) => {
        this.generatingPdf = false;
        if (err.status === 403) {
          this.pdfError = 'Les rapports PDF sont réservés aux plans Premium.';
        } else {
          this.pdfError = err.error?.message ?? 'Erreur lors de la génération du rapport.';
        }
      }
    });
  }

  // --- Money Tips ---

  private checkDashboardTips(month: number, year: number): void {
    const lang = this.getDashboardTipsLang();
    this.incomeService.getMoneyTips(month, year, lang).subscribe({
      next: (tips) => {
        this.hasTipsAvailable = !!(tips && tips.tips && tips.tips.length > 0);
        if (this.hasTipsAvailable) {
          this.dashboardTips = tips;
          this.dashboardTipsMonth = month;
          this.dashboardTipsYear = year;
          this.dashboardTipsMonthLabel = this.pdfMonths.find(m => m.value === month)?.label ?? '';
        }
      },
      error: () => this.hasTipsAvailable = false
    });
  }

  openMoneyTips(): void {
    this.showTipsModal = true;
  }

  goToSubscription(): void {
    this.router.navigate(['/subscription']);
  }

  onDashboardTipsDismiss(): void {}

  onDashboardTipsLangChanged(lang: string): void {
    this.incomeService.getMoneyTips(this.dashboardTipsMonth, this.dashboardTipsYear, lang).subscribe({
      next: (tips) => {
        if (tips && tips.tips && tips.tips.length > 0) {
          this.dashboardTips = tips;
        }
      }
    });
  }

  private getDashboardTipsLang(): string {
    try { return localStorage.getItem('joseph_tips_lang') || 'fr'; } catch { return 'fr'; }
  }
}
