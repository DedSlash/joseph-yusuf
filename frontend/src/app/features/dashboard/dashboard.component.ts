import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { Subscription } from 'rxjs';
import { Chart, registerables } from 'chart.js';
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

Chart.register(...registerables);

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
      <section class="summary-section fade-in-up" *ngIf="summary" style="animation-delay: 0ms">
        <div class="summary-card">
          <div class="summary-header">
            <div>
              <span class="summary-label">{{ getSummaryLabel() }}</span>
              <h2 class="summary-amount count-up">{{ formatCurrency(animatedTotalIncome) }}</h2>
            </div>
            <span class="status-badge" [ngClass]="getStatusClass(summary.status)" [class.pulse-gold]="summary.status === 'ABUNDANCE'">
              {{ getStatusLabel(summary.status) }}
            </span>
          </div>

          <!-- Progression — encouragement premiers mois -->
          <div class="summary-meta" *ngIf="history.length >= 1 && history.length < 3">
            <div class="insufficient-data">
              <span class="insufficient-icon">{{ history.length === 1 ? '✓' : '📈' }}</span>
              <div>
                <p class="insufficient-title">{{ history.length === 1 ? 'Premier mois enregistré' : 'Déjà 2 mois — votre suivi se précise' }}</p>
                <p class="insufficient-body" *ngIf="history.length === 1">
                  Votre tableau de bord est actif : découvrez ci-dessous votre
                  première répartition et le conseil de Joseph pour ce mois. Plus vous ajoutez
                  de mois, plus votre suivi devient précis — vous pouvez aussi importer votre
                  <a routerLink="/incomes" class="link-import">historique</a> pour aller plus vite.
                </p>
                <p class="insufficient-body" *ngIf="history.length === 2">
                  Votre classification commence à se calibrer. Encore un mois et elle
                  sera pleinement fiable.
                </p>
              </div>
            </div>
          </div>

          <!-- Comparaison vs moyenne -->
          <div class="summary-meta" *ngIf="history.length >= 2">
            <ng-container *ngIf="summary.averageLast3Months > 0; else noBaseline">
              <span class="percentage" [ngClass]="summary.percentageVsAverage >= 0 ? 'positive' : 'negative'">
                {{ summary.percentageVsAverage >= 0 ? '+' : '' }}{{ summary.percentageVsAverage | number:'1.0-1' }}%
              </span>
              <span class="vs-average">vs moyenne des 3 derniers mois</span>
              <span class="reliability-badge" *ngIf="summary.monthsInBaseline < 3">Provisoire — s'affine avec le temps</span>
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
                <span>La jauge de positionnement et les seuils précis seront disponibles en <strong>Premium</strong> dès l'ouverture des paiements.</span>
              </div>
            </ng-template>
          </div>
        </div>
      </section>

      <!-- Section 2 : Réserve Joseph -->
      <section class="reserve-section fade-in-up" *ngIf="josephReserve !== null" style="animation-delay: 100ms">
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
                Le calcul détaillé de votre réserve sera disponible en <strong>Premium</strong>
                dès l'ouverture des paiements.
              </p>
            </div>
          </div>
        </div>
      </section>

      <!-- Section 3: Allocations -->
      <section class="allocations-section fade-in-up" *ngIf="allocations" style="animation-delay: 200ms">
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
          <div class="allocation-card fade-in-up"
               *ngFor="let alloc of allocations.allocations; let i = index"
               [style.animation-delay.ms]="i * 80">
            <div class="alloc-header">
              <span class="alloc-category">{{ alloc.category }}</span>
              <span class="alloc-percentage">{{ alloc.percentage }}%</span>
            </div>
            <div class="alloc-amount">{{ formatCurrency(alloc.amount) }}</div>
            <div class="alloc-bar-bg">
              <div class="alloc-bar bar-fill"
                   [style.--target-width.%]="alloc.percentage"
                   [style.width.%]="alloc.percentage"
                   [style.background]="getAllocColor(alloc.category)"></div>
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

      <!-- Section Objectifs d'Épargne -->
      <app-savings-widget></app-savings-widget>

      <!-- Section 4: History -->
      <section class="history-section fade-in-up" *ngIf="history.length > 0" style="animation-delay: 300ms">
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

        <!-- Bar chart : 5 derniers mois -->
        <div class="history-chart-wrapper" *ngIf="history.length >= 1">
          <canvas #historyChart></canvas>
        </div>

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

      <!-- Carte trial PREMIUM_PLUS -->
      <section class="trial-dashboard-section" *ngIf="isInTrial">
        <div class="trial-dashboard-card">
          <ng-container *ngIf="paymentsActive; else giftView">
            <div class="trial-dashboard-header">
              <span class="badge-premium-plus-dash">PREMIUM+</span>
              <span class="trial-dashboard-badge">Essai gratuit</span>
            </div>
            <p class="trial-dashboard-text">
              Vous profitez de toutes les fonctionnalités.
              Votre essai se termine dans
              <strong>{{ trialDaysRemaining }} {{ trialDaysRemaining === 1 ? 'jour' : 'jours' }}</strong>.
            </p>
            <p class="trial-dashboard-promo">
              🎁 Code <strong>EARLY50</strong> réservé aux 100 premiers inscrits — souscription bientôt disponible.
            </p>
          </ng-container>
          <ng-template #giftView>
            <div class="trial-dashboard-header">
              <span class="badge-premium-plus-dash">PREMIUM+</span>
            </div>
            <p class="trial-dashboard-text">
              <strong>Accès Premium+ offert.</strong><br>
              Les moyens de paiement arrivent bientôt.
            </p>
          </ng-template>
        </div>
      </section>

      <!-- Badge plan actif (Premium, hors trial) -->
      <section class="plan-badge-section" *ngIf="isPremium() && !isInTrial">
        <div class="plan-badge-card">
          <span class="plan-badge-icon">★</span>
          <span class="plan-badge-label">Plan {{ getPlanLabel() }}</span>
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
        [userPlan]="getDashboardPlan()"
        (unlockRequested)="goToSubscription()"
        (dismissedForMonth)="onDashboardTipsDismiss()"
        (langChanged)="onDashboardTipsLangChanged($event)"
      ></app-money-tips-modal>
    </div>
  `,
  styles: [`
    .dashboard {
      padding: 32px 28px 80px;
      padding-top: 92px;
      max-width: 1280px;
      margin: 0 auto;
      position: relative;
      z-index: 1;
    }

    /* ── Carte d'accueil ── */
    .welcome-section {
      margin-bottom: 2.5rem;
    }

    .welcome-card {
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(20px) saturate(140%);
      border-radius: 18px;
      padding: 2.5rem;
      text-align: center;
      box-shadow: 0 1px 0 rgba(255, 255, 255, 0.05) inset, 0 6px 24px -10px rgba(0, 0, 0, 0.5);
    }

    .welcome-icon {
      font-size: 2rem;
      color: var(--gold-light);
      margin-bottom: 1rem;
    }

    .welcome-title {
      font-family: var(--font-serif);
      font-size: 1.8rem;
      color: var(--text-0);
      font-weight: 600;
      margin: 0 0 1rem;
    }

    .welcome-intro {
      color: var(--text-1);
      font-size: 0.95rem;
      line-height: 1.7;
      max-width: 680px;
      margin: 0 auto 2rem;
    }

    .welcome-intro strong {
      color: var(--gold-light);
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
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      color: var(--gold-light);
      font-size: 0.8rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-top: 2px;
    }

    .step strong {
      display: block;
      color: var(--text-0);
      font-size: 0.9rem;
      margin-bottom: 0.25rem;
    }

    .step p {
      color: var(--text-0);
      opacity: 0.6;
      font-size: 0.85rem;
      line-height: 1.6;
      margin: 0;
    }

    .step em {
      color: var(--gold-light);
      font-style: normal;
      font-weight: 500;
    }

    .btn-start {
      display: inline-block;
      padding: 0.75rem 2rem;
      background: var(--gold-tint);
      border: 1px solid var(--gold);
      border-radius: 8px;
      color: var(--gold-light);
      font-size: 0.9rem;
      font-weight: 600;
      text-decoration: none;
      transition: background 0.2s;
    }

    .btn-start:hover {
      background: var(--line-strong);
    }

    /* ── Données insuffisantes ── */
    .insufficient-data {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
      margin-top: 1rem;
      padding: 1rem 1.25rem;
      background: var(--gold-tint);
      border: 1px solid rgba(255, 255, 255, 0.08);
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
      color: var(--gold-light);
      margin: 0 0 0.3rem;
    }

    .insufficient-body {
      font-size: 0.82rem;
      color: var(--text-0);
      opacity: 0.65;
      line-height: 1.6;
      margin: 0;
    }

    .link-import {
      color: var(--gold-light);
      text-decoration: underline;
      text-underline-offset: 2px;
    }

    .summary-section {
      margin-bottom: 2.5rem;
    }

    .summary-card {
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 18px;
      padding: 1.5rem 2rem;
    }

    .summary-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .summary-label {
      font-size: 0.85rem;
      color: var(--text-0);
      opacity: 0.6;
    }

    .summary-amount {
      font-family: var(--font-serif);
      font-size: 2.2rem;
      color: var(--text-0);
      margin: 0.25rem 0 0;
      font-weight: 600;
    }

    .status-badge {
      padding: 0.3rem 0.75rem;
      border-radius: 18px;
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
      color: #5cdb83;
      border: 1px solid rgba(40, 167, 69, 0.3);
    }

    .status-lean {
      background: rgba(220, 53, 69, 0.15);
      color: #ff7a6c;
      border: 1px solid rgba(220, 53, 69, 0.3);
    }

    .status-normal {
      background: rgba(52, 152, 219, 0.15);
      color: #7fc1ea;
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
      color: #5cdb83;
    }

    .negative {
      color: #ff7a6c;
    }

    .vs-average {
      font-size: 0.8rem;
      color: var(--text-0);
      opacity: 0.5;
    }

    .reliability-badge {
      display: inline-block;
      margin-left: 0.5rem;
      padding: 2px 8px;
      font-size: 0.7rem;
      border-radius: 4px;
      background: rgba(201, 168, 76, 0.15);
      color: var(--gold, #C9A84C);
      border: 1px solid rgba(201, 168, 76, 0.3);
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
      color: #5cdb83;
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
      border-radius: 14px;
      display: flex;
      gap: 1rem;
      align-items: flex-start;
    }

    .joseph-advice.advice-normal {
      background: rgba(201, 168, 76, 0.06);
      border: 1px solid var(--line);
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
      color: var(--text-0);
      margin: 0 0 0.35rem;
    }

    .advice-text {
      font-size: 0.85rem;
      color: var(--text-0);
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
      color: var(--text-0);
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
      border: 2px solid var(--night-1);
      transition: left 0.4s ease;
    }

    .gauge-hint {
      font-size: 0.72rem;
      color: var(--text-0);
      opacity: 0.55;
      margin: 0.4rem 0 0;
    }

    /* ── Réserve Joseph ── */
    .reserve-section { margin-bottom: 2.5rem; }

    .reserve-card {
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 18px;
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
      border-radius: 18px;
    }

    .reserve-overlay-content {
      text-align: center;
      padding: 1.5rem 2rem;
      max-width: 380px;
    }

    .overlay-icon {
      display: block;
      font-size: 1.5rem;
      color: var(--gold-light);
      margin-bottom: 0.65rem;
    }

    .overlay-title {
      font-family: var(--font-serif);
      font-size: 1.15rem;
      font-weight: 600;
      color: var(--text-0);
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
      background: var(--gold-tint);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 6px;
      font-size: 0.77rem;
      color: rgba(240, 232, 208, 0.5);
    }

    .gauge-upgrade-link {
      color: var(--gold-light);
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
      color: var(--text-0);
      opacity: 0.6;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .reserve-tooltip {
      font-size: 0.75rem;
      color: var(--gold-light);
      cursor: help;
      opacity: 0.7;
    }

    .reserve-amount {
      font-family: var(--font-serif);
      font-size: 1.8rem;
      font-weight: 600;
      margin-bottom: 0.4rem;
    }

    .reserve-amount.positive { color: #5cdb83; }
    .reserve-amount.neutral  { color: var(--text-0); opacity: 0.5; }

    .reserve-sub {
      font-size: 0.8rem;
      color: var(--text-0);
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
      background: var(--gold-tint);
      border: 1px solid var(--line);
      border-radius: 8px;
    }

    .suggestion-icon {
      color: var(--gold-light);
      font-size: 1rem;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .joseph-suggestion strong {
      display: block;
      font-size: 0.85rem;
      color: var(--gold-light);
      margin-bottom: 0.3rem;
    }

    .joseph-suggestion p, .joseph-active-message p {
      font-size: 0.82rem;
      color: var(--text-0);
      opacity: 0.75;
      line-height: 1.6;
      margin: 0 0 0.75rem;
    }

    .joseph-active-message p { margin: 0; opacity: 0.85; }

    .btn-switch-joseph {
      padding: 0.4rem 0.85rem;
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      border-radius: 6px;
      color: var(--gold-light);
      font-size: 0.78rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
    }

    .btn-switch-joseph:hover { background: var(--line-strong); }

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
      color: var(--gold-light);
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
      color: var(--gold-light);
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      padding: 0.2rem 0.6rem;
      border-radius: 20px;
      letter-spacing: 0.02em;
      white-space: nowrap;
    }

    .section-title {
      font-family: var(--font-serif);
      font-size: 1.3rem;
      color: var(--text-0);
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
      color: var(--gold-light);
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
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      border-radius: 6px;
      color: var(--gold-light);
      font-size: 0.8rem;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }

    .btn-change-rule:hover {
      background: var(--line);
    }

    .allocation-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 1rem;
    }

    .allocation-card {
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 14px;
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
      color: var(--text-0);
      font-weight: 500;
    }

    .alloc-percentage {
      font-size: 0.8rem;
      color: var(--gold-light);
      font-weight: 600;
    }

    .alloc-amount {
      font-size: 1.1rem;
      color: var(--text-0);
      font-weight: 600;
      margin-bottom: 0.75rem;
    }

    .alloc-bar-bg {
      height: 4px;
      background: var(--gold-tint);
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
      background: rgba(8, 8, 15, 0.5);
      border: 1px solid var(--line);
      border-radius: 5px;
      color: var(--text-0);
      font-size: 0.75rem;
      outline: none;
      cursor: pointer;
    }

    .pdf-select:focus {
      border-color: var(--gold);
    }

    .btn-pdf {
      padding: 0.4rem 0.9rem;
      background: var(--gold-tint);
      border: 1px solid rgba(201, 168, 76, 0.35);
      border-radius: 6px;
      color: var(--gold-light);
      font-size: 0.78rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
      white-space: nowrap;
    }

    .btn-pdf:hover:not(:disabled) { background: var(--line); }
    .btn-pdf:disabled { opacity: 0.45; cursor: not-allowed; }

    .btn-pdf-secondary {
      background: transparent;
      border-color: var(--line);
      color: var(--text-0);
      opacity: 0.7;
    }

    .btn-pdf-secondary:hover:not(:disabled) {
      background: var(--gold-tint);
      opacity: 1;
    }

    .pdf-error {
      font-size: 0.8rem;
      color: #ff7a6c;
      margin-bottom: 0.75rem;
    }

    .history-chart-wrapper {
      position: relative;
      height: 260px;
      padding: 1rem 1.25rem;
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 14px;
      margin-bottom: 1.25rem;
    }

    .history-table-wrapper {
      overflow-x: auto;
    }

    .history-table {
      width: 100%;
      border-collapse: collapse;
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border-radius: 14px;
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
      color: var(--text-0);
      opacity: 0.35;
      font-size: 0.85rem;
    }

    .history-table th {
      text-align: left;
      padding: 0.85rem 1.25rem;
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--text-0);
      opacity: 0.6;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      border-bottom: 1px solid var(--line-soft);
    }

    .history-table td {
      padding: 0.85rem 1.25rem;
      font-size: 0.9rem;
      color: var(--text-0);
      border-bottom: 1px solid var(--line-soft);
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
      background: rgba(8, 8, 15, 0.5);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 8px;
      cursor: pointer;
      transition: border-color 0.2s, background 0.2s;
    }

    .rule-item:hover:not(.locked) {
      border-color: var(--line-strong);
      background: var(--gold-tint);
    }

    .rule-item.active {
      border-color: var(--gold-light);
      background: var(--gold-tint);
    }

    /* ── Upgrade / Plan badge ── */
    .upgrade-section { margin: 2rem 0; }

    .upgrade-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1.25rem 1.5rem;
      background: linear-gradient(135deg, var(--gold-tint) 0%, var(--gold-tint) 100%);
      border: 1px solid var(--line-strong);
      border-radius: 18px;
      gap: 1rem;
    }

    .upgrade-left {
      display: flex;
      align-items: flex-start;
      gap: 1rem;
    }

    .upgrade-icon {
      font-size: 1.4rem;
      color: var(--gold-light);
      flex-shrink: 0;
      margin-top: 2px;
    }

    .upgrade-title {
      display: block;
      font-size: 0.95rem;
      color: var(--text-0);
      font-weight: 600;
      margin-bottom: 0.25rem;
    }

    .upgrade-desc {
      font-size: 0.82rem;
      color: var(--text-0);
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

    /* Trial dashboard card */
    .trial-dashboard-section { margin: 1.5rem 0; }

    .trial-dashboard-card {
      padding: 1.25rem 1.5rem;
      background: linear-gradient(135deg, rgba(232, 200, 118, 0.12), rgba(157, 130, 53, 0.06));
      border: 1px solid var(--gold);
      border-radius: 14px;
    }

    .trial-dashboard-header {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      margin-bottom: 0.6rem;
    }

    .badge-premium-plus-dash {
      padding: 4px 10px;
      background: linear-gradient(180deg, var(--gold-light), var(--gold));
      color: #1b1500;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.06em;
    }

    .trial-dashboard-badge {
      padding: 3px 9px;
      background: rgba(0, 0, 0, 0.35);
      color: var(--gold-light);
      border-radius: 999px;
      font-size: 11px;
      font-weight: 600;
    }

    .trial-dashboard-text {
      font-size: 0.9rem;
      color: var(--text-1);
      margin: 0 0 0.75rem;
      line-height: 1.5;
    }

    .trial-dashboard-text strong { color: var(--gold-light); }

    .btn-gold-small {
      display: inline-block;
      padding: 0.5rem 1rem;
      background: linear-gradient(180deg, var(--gold-light), var(--gold));
      color: #1b1500;
      border-radius: 8px;
      font-size: 0.82rem;
      font-weight: 700;
      text-decoration: none;
      margin-bottom: 0.6rem;
    }

    .trial-dashboard-promo {
      font-size: 0.78rem;
      color: var(--text-2);
      margin: 0;
    }

    .trial-dashboard-promo strong { color: var(--gold-light); font-family: monospace; }

    .plan-badge-section { margin: 1.5rem 0; }

    .plan-badge-card {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 1.25rem;
      background: rgba(92, 219, 111, 0.06);
      border: 1px solid rgba(92, 219, 111, 0.2);
      border-radius: 14px;
    }

    .plan-badge-icon { color: #5cdb83; font-size: 1rem; }

    .plan-badge-label {
      font-size: 0.85rem;
      color: #5cdb83;
      font-weight: 600;
      flex: 1;
    }

    .btn-manage-plan {
      font-size: 0.78rem;
      color: var(--gold-light);
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
      color: var(--text-0);
      font-weight: 500;
    }

    .rule-badge.premium {
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
      font-size: 0.65rem;
      font-weight: 600;
      background: var(--line);
      color: var(--gold-light);
      text-transform: uppercase;
    }

    .rule-check {
      color: var(--gold-light);
      font-size: 1.1rem;
    }

    /* Tablet : 768px – 1023px */
    @media (min-width: 768px) and (max-width: 1023px) {
      .dashboard { padding: 1.5rem; padding-top: 5rem; }
      .allocation-grid { grid-template-columns: repeat(2, 1fr); }
      .summary-amount { font-size: 1.9rem; }
      .history-chart-wrapper { height: 240px; }
      .report-actions { align-items: stretch; }
    }

    /* Mobile : ≤ 767px */
    @media (max-width: 767px) {
      .dashboard { padding: 1rem; padding-top: 5rem; }
      .allocation-grid { grid-template-columns: 1fr 1fr; gap: 0.6rem; }
      .allocation-card { padding: 0.85rem; }
      .alloc-amount { font-size: 0.95rem; }
      .alloc-category { font-size: 0.75rem; }
      .alloc-percentage { font-size: 0.7rem; }

      .summary-card { padding: 1rem 1.1rem; }
      .summary-amount { font-size: 1.6rem; }
      .summary-header { flex-direction: column; gap: 0.6rem; align-items: flex-start; }

      .joseph-advice { padding: 1rem; flex-direction: column; gap: 0.65rem; }
      .threshold-gauge { width: 100%; }

      .reserve-card { padding: 1rem 1.1rem; }
      .reserve-amount { font-size: 1.5rem; }

      .section-header { flex-direction: column; align-items: flex-start; gap: 0.75rem; }
      .section-header-right { width: 100%; justify-content: space-between; flex-wrap: wrap; gap: 0.5rem; }
      .section-title { font-size: 1.1rem; }

      .history-chart-wrapper { height: 200px; padding: 0.75rem; }

      .report-actions { flex-direction: column; gap: 0.5rem; align-items: stretch; width: 100%; }
      .pdf-picker { flex-wrap: wrap; }

      .upgrade-card { flex-direction: column; align-items: flex-start; gap: 1rem; }
      .btn-upgrade { width: 100%; text-align: center; }

      .welcome-card { padding: 1.5rem 1rem; }
      .welcome-title { font-size: 1.3rem; }
    }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('historyChart') historyChartRef?: ElementRef<HTMLCanvasElement>;
  private historyChart?: Chart;
  private countUpRaf = 0;
  animatedTotalIncome = 0;
  private viewReady = false;

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

  // Trial
  isInTrial = false;
  trialDaysRemaining: number | null = null;
  paymentsActive = false;

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
    this.loadTrialStatus();

    this.updateSub = this.incomeService.incomeUpdated$.subscribe(() => {
      this.loadDashboardData();
    });
  }

  private loadTrialStatus(): void {
    const user = this.authService.getCurrentUser();
    if (!user?.inTrial || !user.trialEndsAt) {
      this.isInTrial = false;
      this.trialDaysRemaining = null;
      this.paymentsActive = false;
      return;
    }
    this.authService.getTrialStatus().subscribe({
      next: status => {
        this.isInTrial = status.isInTrial;
        this.trialDaysRemaining = status.isInTrial ? status.daysRemaining : null;
        this.paymentsActive = status.paymentsActive;
      },
      error: () => {
        const diffMs = new Date(user.trialEndsAt!).getTime() - Date.now();
        this.isInTrial = diffMs > 0;
        this.trialDaysRemaining = this.isInTrial ? Math.max(0, Math.ceil(diffMs / (1000 * 60 * 60 * 24))) : null;
        this.paymentsActive = false;
      }
    });
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    if (this.history.length > 0) {
      // History déjà chargée avant la vue
      setTimeout(() => this.renderHistoryChart(), 0);
    }
  }

  ngOnDestroy(): void {
    this.updateSub?.unsubscribe();
    if (this.countUpRaf) { cancelAnimationFrame(this.countUpRaf); }
    this.historyChart?.destroy();
  }

  private animateCountUp(target: number, duration = 700): void {
    if (this.countUpRaf) { cancelAnimationFrame(this.countUpRaf); }
    const start = this.animatedTotalIncome;
    const delta = target - start;
    if (Math.abs(delta) < 1) {
      this.animatedTotalIncome = target;
      return;
    }
    const startTs = performance.now();
    const step = (now: number) => {
      const t = Math.min(1, (now - startTs) / duration);
      // easeOutCubic
      const eased = 1 - Math.pow(1 - t, 3);
      this.animatedTotalIncome = Math.round(start + delta * eased);
      if (t < 1) {
        this.countUpRaf = requestAnimationFrame(step);
      } else {
        this.animatedTotalIncome = target;
        this.countUpRaf = 0;
      }
    };
    this.countUpRaf = requestAnimationFrame(step);
  }

  private renderHistoryChart(): void {
    if (!this.viewReady || !this.historyChartRef || this.history.length === 0) return;
    const canvas = this.historyChartRef.nativeElement;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // 5 derniers mois — history est ordonnée DESC, on inverse
    const recent = this.history.slice(0, 5).reverse();
    const labels = recent.map(h => this.shortMonthLabel(h.month, h.year));
    const data = recent.map(h => h.totalIncome);
    const colors = recent.map(h => this.statusColor(h.status));
    const borderColors = recent.map(h => this.statusColor(h.status, 1));

    if (this.historyChart) {
      this.historyChart.data.labels = labels;
      this.historyChart.data.datasets[0].data = data;
      this.historyChart.data.datasets[0].backgroundColor = colors;
      this.historyChart.data.datasets[0].borderColor = borderColors;
      this.historyChart.update();
      return;
    }

    this.historyChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Revenu mensuel',
          data,
          backgroundColor: colors,
          borderColor: borderColors,
          borderWidth: 1,
          borderRadius: 6,
          maxBarThickness: 56
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 500, easing: 'easeOutQuart' },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#13162a',
            titleColor: '#F5F5F5',
            bodyColor: '#D9D9DE',
            borderColor: 'rgba(201, 168, 76, 0.32)',
            borderWidth: 1,
            padding: 10,
            cornerRadius: 8,
            displayColors: false,
            callbacks: {
              label: (ctx) => this.formatCurrency(ctx.parsed.y ?? 0)
            }
          }
        },
        scales: {
          x: {
            grid: { display: false, color: 'rgba(255,255,255,0.05)' },
            ticks: { color: '#9CA3AF', font: { size: 11 } }
          },
          y: {
            grid: { color: 'rgba(255,255,255,0.05)' },
            ticks: {
              color: '#9CA3AF',
              font: { size: 11 },
              callback: (v) => this.compactCurrency(Number(v))
            }
          }
        }
      }
    });
  }

  private statusColor(status: MonthStatus, alpha = 0.85): string {
    const a = alpha;
    switch (status) {
      case 'ABUNDANCE': return `rgba(201, 168, 76, ${a})`;
      case 'LEAN':      return `rgba(231, 76, 60, ${a})`;
      case 'NORMAL':    return `rgba(74, 144, 217, ${a})`;
    }
  }

  private shortMonthLabel(month: number, year: number): string {
    const d = new Date(year, month - 1);
    const m = d.toLocaleDateString('fr-FR', { month: 'short' }).replace('.', '');
    return `${m.charAt(0).toUpperCase()}${m.slice(1)} ${String(year).slice(-2)}`;
  }

  private compactCurrency(v: number): string {
    if (Math.abs(v) >= 1_000_000) return (v / 1_000_000).toFixed(1).replace('.0', '') + 'M';
    if (Math.abs(v) >= 1_000)     return Math.round(v / 1_000) + 'k';
    return String(v);
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
        setTimeout(() => this.renderHistoryChart(), 0);

        // Mois de référence = dernier mois saisi, ou mois courant si rien
        const now = new Date();
        const refMonth = history.length > 0 ? history[0].month : now.getMonth() + 1;
        const refYear  = history.length > 0 ? history[0].year  : now.getFullYear();

        this.incomeService.getSummary(refMonth, refYear).subscribe({
          next: (summary) => {
            this.summary = summary;
            this.animateCountUp(summary.totalIncome);
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
    // Souscription désactivée tant que les moyens de paiement ne sont pas activés.
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

  getDashboardPlan(): 'FREE' | 'PREMIUM' | 'PREMIUM_PLUS' {
    return this.authService.getPlan();
  }
}
