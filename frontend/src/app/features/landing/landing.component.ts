import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

interface PlanCard {
  id: string;
  name: string;
  tagline: string;
  priceEur: number;
  priceXof: number;
  features: string[];
  highlight: boolean;
  cta: string;
}

const PLANS: PlanCard[] = [
  {
    id: 'FREE',
    name: 'Free',
    tagline: 'Pour débuter',
    priceEur: 0,
    priceXof: 0,
    features: [
      '1 source de revenu',
      'Règle 50/30/20',
      'Dashboard de base',
      'Alertes automatiques',
    ],
    highlight: false,
    cta: 'Commencer gratuitement',
  },
  {
    id: 'PREMIUM',
    name: 'Premium',
    tagline: 'Pour les actifs',
    priceEur: 4.99,
    priceXof: 3000,
    features: [
      'Sources illimitées',
      'Toutes les règles financières',
      'Import Excel / CSV / JSON',
      'Export de données',
      'Rapports PDF mensuels',
    ],
    highlight: true,
    cta: 'Essayer Premium',
  },
  {
    id: 'PREMIUM_PLUS',
    name: 'Premium +',
    tagline: 'Pour aller plus loin',
    priceEur: 9.99,
    priceXof: 6000,
    features: [
      'Tout ce qu\'inclut Premium',
      'Dashboard avancé',
      'Support prioritaire',
      'Accès anticipé aux nouvelles fonctionnalités',
    ],
    highlight: false,
    cta: 'Choisir Premium +',
  },
];

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <!-- ── Navbar ─────────────────────────────────────────────────────── -->
    <header class="lp-nav" [class.scrolled]="scrolled">
      <a class="lp-logo" routerLink="/">Joseph&nbsp;·&nbsp;Yusuf</a>
      <nav class="lp-nav-links">
        <a href="#principe" class="lp-nav-link" (click)="scrollTo($event,'principe')">Le Principe</a>
        <a href="#fonctionnalites" class="lp-nav-link" (click)="scrollTo($event,'fonctionnalites')">Fonctionnalités</a>
        <a href="#tarifs" class="lp-nav-link" (click)="scrollTo($event,'tarifs')">Tarifs</a>
      </nav>
      <div class="lp-nav-actions">
        <a routerLink="/login"    class="btn-ghost">Se connecter</a>
        <a routerLink="/register" class="btn-gold">Créer un compte</a>
      </div>
    </header>

    <!-- ── Hero ───────────────────────────────────────────────────────── -->
    <section class="hero">
      <div class="hero-glow hero-glow-1"></div>
      <div class="hero-glow hero-glow-2"></div>
      <div class="hero-content">
        <span class="hero-eyebrow">Gestion des revenus variables</span>
        <h1 class="hero-title">
          Épargner pendant<br>
          <span class="hero-accent">l'abondance.</span><br>
          Tenir pendant<br>
          <span class="hero-accent">la disette.</span>
        </h1>
        <p class="hero-sub">
          Joseph · Yusuf applique un principe millénaire à votre situation financière.
          Saisissez vos revenus, l'outil fait le reste — alertes, répartitions, réserves.
        </p>
        <div class="hero-cta">
          <a routerLink="/register" class="btn-hero-primary">Commencer gratuitement</a>
          <a href="#principe"       class="btn-hero-ghost"   (click)="scrollTo($event,'principe')">Voir comment ça marche ↓</a>
        </div>
        <p class="hero-caption">Gratuit · Sans carte bancaire · Idéal pour l'Afrique francophone &amp; la diaspora</p>
      </div>

      <!-- Visualisation animée -->
      <div class="hero-visual" aria-hidden="true">
        <div class="vis-card vis-card-main">
          <div class="vis-label">Mois en cours</div>
          <div class="vis-amount">425 000 <span>XOF</span></div>
          <div class="vis-badge abundance">Abondance +18%</div>
          <div class="vis-bar-row">
            <div class="vis-bar-item">
              <div class="vis-bar-fill" style="height:70%;background:#C9A84C"></div>
              <span>Besoins</span>
            </div>
            <div class="vis-bar-item">
              <div class="vis-bar-fill" style="height:30%;background:#5cdb6f"></div>
              <span>Épargne</span>
            </div>
            <div class="vis-bar-item">
              <div class="vis-bar-fill" style="height:20%;background:#5dade2"></div>
              <span>Invest.</span>
            </div>
          </div>
        </div>
        <div class="vis-card vis-card-secondary">
          <div class="vis-mini-label">Réserve Joseph</div>
          <div class="vis-mini-amount">92 000 XOF</div>
          <div class="vis-mini-sub">Constituée sur 4 mois d'abondance</div>
        </div>
        <div class="vis-card vis-card-alert">
          <span class="vis-alert-icon">✦</span>
          <span class="vis-alert-text">Mois d'abondance détecté — épargnez davantage</span>
        </div>
      </div>
    </section>

    <!-- ── Le Principe ─────────────────────────────────────────────────── -->
    <section class="section section-principe" id="principe">
      <div class="container">
        <span class="section-eyebrow">Le Principe de Joseph</span>
        <h2 class="section-title">Un principe vieux de 3 000 ans,<br>adapté à votre réalité d'aujourd'hui</h2>
        <p class="section-sub">
          Dans la Bible comme dans le Coran, Joseph (Yusuf) a géré 7 années d'abondance
          pour traverser 7 années de famine sans dettes ni privations. La même logique
          s'applique à vos revenus variables.
        </p>

        <div class="principle-grid">
          <div class="principle-card abundance-card">
            <div class="principle-icon">🌿</div>
            <h3>Période d'abondance</h3>
            <p>Votre revenu dépasse votre moyenne de 15% ou plus. C'est le moment de constituer une réserve plutôt que d'augmenter vos dépenses.</p>
          </div>
          <div class="principle-card normal-card">
            <div class="principle-icon">⚖️</div>
            <h3>Période normale</h3>
            <p>Votre revenu reste dans une fourchette de ±15% par rapport à votre moyenne. Continuez à appliquer votre règle de répartition sans changement.</p>
          </div>
          <div class="principle-card lean-card">
            <div class="principle-icon">⚠️</div>
            <h3>Période de disette</h3>
            <p>Votre revenu est inférieur de 15% ou plus à votre moyenne. Puisez dans la réserve constituée. Ne vous endettez pas.</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Comment ça marche ───────────────────────────────────────────── -->
    <section class="section section-steps" id="fonctionnalites">
      <div class="container">
        <span class="section-eyebrow">Fonctionnalités</span>
        <h2 class="section-title">Tout ce dont vous avez besoin,<br>rien de superflu</h2>

        <div class="steps-timeline">
          <div class="step-item" *ngFor="let step of steps; let i = index">
            <div class="step-num-wrap">
              <div class="step-num">{{ i + 1 }}</div>
              <div class="step-line" *ngIf="i < steps.length - 1"></div>
            </div>
            <div class="step-body">
              <div class="step-icon">{{ step.icon }}</div>
              <h3 class="step-title">{{ step.title }}</h3>
              <p class="step-desc">{{ step.desc }}</p>
            </div>
          </div>
        </div>

        <!-- Feature cards -->
        <div class="features-grid">
          <div class="feature-card" *ngFor="let f of features">
            <div class="feature-icon">{{ f.icon }}</div>
            <h4 class="feature-title">{{ f.title }}</h4>
            <p class="feature-desc">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Tarifs ──────────────────────────────────────────────────────── -->
    <section class="section section-pricing" id="tarifs">
      <div class="container">
        <span class="section-eyebrow">Tarifs</span>
        <h2 class="section-title">Un prix accessible,<br>une valeur durable</h2>
        <p class="section-sub">Pensé pour l'Afrique francophone et la diaspora européenne.</p>

        <div class="currency-toggle">
          <button [class.active]="currency === 'XOF'" (click)="currency='XOF'">XOF</button>
          <button [class.active]="currency === 'EUR'" (click)="currency='EUR'">EUR</button>
        </div>

        <div class="pricing-grid">
          <div
            class="pricing-card"
            *ngFor="let plan of plans"
            [class.highlighted]="plan.highlight"
          >
            <div class="pricing-badge" *ngIf="plan.highlight">Le plus populaire</div>
            <h3 class="pricing-name">{{ plan.name }}</h3>
            <p class="pricing-tagline">{{ plan.tagline }}</p>
            <div class="pricing-price">
              <ng-container *ngIf="plan.priceEur === 0">
                <span class="price-amount">Gratuit</span>
              </ng-container>
              <ng-container *ngIf="plan.priceEur > 0">
                <span class="price-amount">
                  {{ currency === 'XOF' ? (plan.priceXof | number) + ' XOF' : plan.priceEur + ' €' }}
                </span>
                <span class="price-period">/ mois</span>
              </ng-container>
            </div>
            <ul class="pricing-features">
              <li *ngFor="let f of plan.features">
                <span class="check">✓</span> {{ f }}
              </li>
            </ul>
            <a routerLink="/register" class="btn-plan" [class.btn-plan-gold]="plan.highlight">
              {{ plan.cta }}
            </a>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Cible ───────────────────────────────────────────────────────── -->
    <section class="section section-audience">
      <div class="container">
        <h2 class="section-title">Fait pour vous si…</h2>
        <div class="audience-grid">
          <div class="audience-card" *ngFor="let a of audiences">
            <div class="audience-icon">{{ a.icon }}</div>
            <p>{{ a.text }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ── CTA final ───────────────────────────────────────────────────── -->
    <section class="section section-final-cta">
      <div class="container">
        <div class="final-cta-card">
          <div class="final-cta-glow"></div>
          <div class="final-cta-icon">✦</div>
          <h2 class="final-cta-title">Commencez dès aujourd'hui</h2>
          <p class="final-cta-sub">
            Gratuit. Sans engagement. Aucune carte bancaire requise.
          </p>
          <div class="final-cta-actions">
            <a routerLink="/register" class="btn-hero-primary">Créer mon compte →</a>
            <a routerLink="/login"    class="btn-hero-ghost">J'ai déjà un compte</a>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Footer ─────────────────────────────────────────────────────── -->
    <footer class="lp-footer">
      <div class="footer-inner">
        <span class="footer-logo">Joseph · Yusuf</span>
        <span class="footer-copy">© {{ year }} Rey Dedy Pangou — Tous droits réservés</span>
        <div class="footer-links">
          <a routerLink="/login">Connexion</a>
          <a routerLink="/register">Inscription</a>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    /* ── Reset & tokens ── */
    :host {
      display: block;
      background: #0D0B07;
      color: #F0E8D0;
      font-family: 'DM Sans', sans-serif;
    }

    * { box-sizing: border-box; margin: 0; padding: 0; }

    /* ── Navbar ── */
    .lp-nav {
      position: fixed;
      top: 0; left: 0; right: 0;
      height: 64px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 2.5rem;
      z-index: 1000;
      transition: background 0.3s, border-color 0.3s;
      border-bottom: 1px solid transparent;
    }

    .lp-nav.scrolled {
      background: rgba(13, 11, 7, 0.95);
      backdrop-filter: blur(12px);
      border-color: rgba(201, 168, 76, 0.15);
    }

    .lp-logo {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.4rem;
      font-weight: 600;
      color: #C9A84C;
      text-decoration: none;
      letter-spacing: 0.5px;
      flex-shrink: 0;
    }

    .lp-nav-links {
      display: flex;
      gap: 2rem;
    }

    .lp-nav-link {
      color: rgba(240, 232, 208, 0.65);
      text-decoration: none;
      font-size: 0.88rem;
      font-weight: 500;
      transition: color 0.2s;
    }

    .lp-nav-link:hover { color: #F0E8D0; }

    .lp-nav-actions {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .btn-ghost {
      padding: 0.4rem 1rem;
      color: #F0E8D0;
      border: 1px solid rgba(240, 232, 208, 0.2);
      border-radius: 8px;
      font-size: 0.85rem;
      text-decoration: none;
      transition: border-color 0.2s, background 0.2s;
    }

    .btn-ghost:hover {
      border-color: rgba(240, 232, 208, 0.4);
      background: rgba(240, 232, 208, 0.05);
    }

    .btn-gold {
      padding: 0.4rem 1rem;
      background: #C9A84C;
      color: #0D0B07;
      border: none;
      border-radius: 8px;
      font-size: 0.85rem;
      font-weight: 700;
      text-decoration: none;
      transition: background 0.2s;
    }

    .btn-gold:hover { background: #DAC372; }

    /* ── Hero ── */
    .hero {
      min-height: 100vh;
      display: grid;
      grid-template-columns: 1fr 1fr;
      align-items: center;
      gap: 4rem;
      padding: 7rem 2.5rem 5rem;
      max-width: 1200px;
      margin: 0 auto;
      position: relative;
      overflow: hidden;
    }

    .hero-glow {
      position: absolute;
      border-radius: 50%;
      filter: blur(120px);
      pointer-events: none;
    }

    .hero-glow-1 {
      width: 500px; height: 500px;
      background: radial-gradient(circle, rgba(201,168,76,0.12) 0%, transparent 70%);
      top: -100px; left: -100px;
    }

    .hero-glow-2 {
      width: 400px; height: 400px;
      background: radial-gradient(circle, rgba(92,219,111,0.06) 0%, transparent 70%);
      bottom: 0; right: 100px;
    }

    .hero-eyebrow {
      display: inline-block;
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      color: #C9A84C;
      border: 1px solid rgba(201, 168, 76, 0.35);
      padding: 0.25rem 0.75rem;
      border-radius: 20px;
      margin-bottom: 1.5rem;
    }

    .hero-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: clamp(2.6rem, 5vw, 4rem);
      font-weight: 600;
      line-height: 1.15;
      color: #F0E8D0;
      margin-bottom: 1.5rem;
    }

    .hero-accent {
      color: #C9A84C;
      position: relative;
    }

    .hero-sub {
      font-size: 1rem;
      color: rgba(240, 232, 208, 0.65);
      line-height: 1.75;
      max-width: 480px;
      margin-bottom: 2.5rem;
    }

    .hero-cta {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
      margin-bottom: 1.25rem;
    }

    .btn-hero-primary {
      padding: 0.85rem 2rem;
      background: #C9A84C;
      color: #0D0B07;
      border: none;
      border-radius: 10px;
      font-size: 0.95rem;
      font-weight: 700;
      text-decoration: none;
      transition: background 0.2s, transform 0.15s;
    }

    .btn-hero-primary:hover {
      background: #DAC372;
      transform: translateY(-1px);
    }

    .btn-hero-ghost {
      padding: 0.85rem 1.75rem;
      color: rgba(240, 232, 208, 0.75);
      border: 1px solid rgba(240, 232, 208, 0.2);
      border-radius: 10px;
      font-size: 0.95rem;
      text-decoration: none;
      transition: border-color 0.2s, color 0.2s;
    }

    .btn-hero-ghost:hover {
      border-color: rgba(240, 232, 208, 0.4);
      color: #F0E8D0;
    }

    .hero-caption {
      font-size: 0.75rem;
      color: rgba(240, 232, 208, 0.35);
    }

    /* Hero visual */
    .hero-visual {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      position: relative;
      z-index: 1;
    }

    .vis-card {
      background: #1A1710;
      border-radius: 14px;
      padding: 1.25rem 1.5rem;
      border: 1px solid rgba(201, 168, 76, 0.15);
    }

    .vis-card-main {
      border-color: rgba(201, 168, 76, 0.3);
      box-shadow: 0 8px 32px rgba(0,0,0,0.3);
    }

    .vis-label {
      font-size: 0.7rem;
      text-transform: uppercase;
      letter-spacing: 0.1em;
      color: rgba(240,232,208,0.45);
      margin-bottom: 0.5rem;
    }

    .vis-amount {
      font-family: 'Cormorant Garamond', serif;
      font-size: 2rem;
      font-weight: 600;
      color: #F0E8D0;
      margin-bottom: 0.5rem;
    }

    .vis-amount span { font-size: 1rem; opacity: 0.55; }

    .vis-badge {
      display: inline-block;
      font-size: 0.72rem;
      font-weight: 700;
      padding: 0.2rem 0.65rem;
      border-radius: 20px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: 1rem;
    }

    .vis-badge.abundance {
      background: rgba(40,167,69,0.15);
      color: #5cdb6f;
      border: 1px solid rgba(40,167,69,0.3);
    }

    .vis-bar-row {
      display: flex;
      align-items: flex-end;
      gap: 0.75rem;
      height: 64px;
    }

    .vis-bar-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.35rem;
      flex: 1;
    }

    .vis-bar-fill {
      width: 100%;
      border-radius: 4px 4px 0 0;
      min-height: 6px;
    }

    .vis-bar-item span {
      font-size: 0.62rem;
      color: rgba(240,232,208,0.45);
    }

    .vis-card-secondary {
      align-self: flex-end;
      width: 75%;
    }

    .vis-mini-label {
      font-size: 0.65rem;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: rgba(240,232,208,0.4);
      margin-bottom: 0.3rem;
    }

    .vis-mini-amount {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.35rem;
      color: #5cdb6f;
      font-weight: 600;
    }

    .vis-mini-sub {
      font-size: 0.68rem;
      color: rgba(240,232,208,0.4);
      margin-top: 0.2rem;
    }

    .vis-card-alert {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      padding: 0.85rem 1.1rem;
      background: rgba(201,168,76,0.07);
      border-color: rgba(201,168,76,0.3);
    }

    .vis-alert-icon { color: #C9A84C; font-size: 0.9rem; flex-shrink: 0; }
    .vis-alert-text { font-size: 0.78rem; color: rgba(240,232,208,0.75); line-height: 1.4; }

    /* ── Sections communes ── */
    .section {
      padding: 6rem 2.5rem;
    }

    .container {
      max-width: 1100px;
      margin: 0 auto;
    }

    .section-eyebrow {
      display: inline-block;
      font-size: 0.7rem;
      font-weight: 700;
      letter-spacing: 0.14em;
      text-transform: uppercase;
      color: #C9A84C;
      margin-bottom: 1rem;
    }

    .section-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: clamp(1.8rem, 3.5vw, 2.6rem);
      font-weight: 600;
      line-height: 1.25;
      color: #F0E8D0;
      margin-bottom: 1.25rem;
    }

    .section-sub {
      font-size: 0.95rem;
      color: rgba(240,232,208,0.6);
      line-height: 1.75;
      max-width: 600px;
      margin-bottom: 3.5rem;
    }

    /* ── Section Principe ── */
    .section-principe {
      background: linear-gradient(180deg, #0D0B07 0%, #121007 100%);
    }

    .principle-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1.5rem;
      margin-top: 3rem;
    }

    .principle-card {
      padding: 2rem;
      border-radius: 14px;
      border: 1px solid;
      transition: transform 0.2s;
    }

    .principle-card:hover { transform: translateY(-3px); }

    .abundance-card {
      background: rgba(40,167,69,0.05);
      border-color: rgba(40,167,69,0.2);
    }

    .normal-card {
      background: rgba(52,152,219,0.05);
      border-color: rgba(52,152,219,0.2);
    }

    .lean-card {
      background: rgba(243,156,18,0.05);
      border-color: rgba(243,156,18,0.2);
    }

    .principle-icon { font-size: 2rem; margin-bottom: 1rem; }

    .principle-card h3 {
      font-size: 1rem;
      font-weight: 600;
      color: #F0E8D0;
      margin-bottom: 0.75rem;
    }

    .principle-card p {
      font-size: 0.85rem;
      color: rgba(240,232,208,0.6);
      line-height: 1.65;
      margin-bottom: 1.25rem;
    }


    /* ── Steps ── */
    .section-steps {
      border-top: 1px solid rgba(201,168,76,0.08);
      border-bottom: 1px solid rgba(201,168,76,0.08);
    }

    .steps-timeline {
      display: flex;
      flex-direction: column;
      gap: 0;
      max-width: 700px;
      margin: 3rem auto;
    }

    .step-item {
      display: grid;
      grid-template-columns: 48px 1fr;
      gap: 1.5rem;
      align-items: flex-start;
    }

    .step-num-wrap {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .step-num {
      width: 42px;
      height: 42px;
      border-radius: 50%;
      background: rgba(201,168,76,0.1);
      border: 1px solid rgba(201,168,76,0.4);
      color: #C9A84C;
      font-weight: 700;
      font-size: 0.9rem;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .step-line {
      width: 1px;
      flex: 1;
      min-height: 32px;
      background: rgba(201,168,76,0.15);
      margin: 6px 0;
    }

    .step-body {
      padding-bottom: 2rem;
    }

    .step-icon { font-size: 1.6rem; margin-bottom: 0.5rem; }

    .step-title {
      font-size: 1rem;
      font-weight: 600;
      color: #F0E8D0;
      margin-bottom: 0.35rem;
    }

    .step-desc {
      font-size: 0.875rem;
      color: rgba(240,232,208,0.6);
      line-height: 1.65;
    }

    /* Feature cards */
    .features-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 1.25rem;
      margin-top: 2rem;
    }

    .feature-card {
      padding: 1.5rem;
      background: #1A1710;
      border: 1px solid rgba(201,168,76,0.1);
      border-radius: 12px;
      transition: border-color 0.2s, transform 0.2s;
    }

    .feature-card:hover {
      border-color: rgba(201,168,76,0.3);
      transform: translateY(-2px);
    }

    .feature-icon { font-size: 1.5rem; margin-bottom: 0.75rem; }

    .feature-title {
      font-size: 0.9rem;
      font-weight: 600;
      color: #F0E8D0;
      margin-bottom: 0.4rem;
    }

    .feature-desc {
      font-size: 0.8rem;
      color: rgba(240,232,208,0.55);
      line-height: 1.6;
    }

    /* ── Pricing ── */
    .section-pricing { background: #0D0B07; }

    .currency-toggle {
      display: inline-flex;
      gap: 0;
      border: 1px solid rgba(201,168,76,0.25);
      border-radius: 8px;
      overflow: hidden;
      margin-bottom: 3rem;
    }

    .currency-toggle button {
      padding: 0.45rem 1.25rem;
      background: transparent;
      border: none;
      color: rgba(240,232,208,0.5);
      font-size: 0.82rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s, color 0.2s;
    }

    .currency-toggle button.active {
      background: rgba(201,168,76,0.15);
      color: #C9A84C;
    }

    .pricing-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1.5rem;
      align-items: stretch;
    }

    .pricing-card {
      position: relative;
      padding: 2rem;
      background: #1A1710;
      border: 1px solid rgba(201,168,76,0.12);
      border-radius: 16px;
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      transition: transform 0.2s;
    }

    .pricing-card:hover { transform: translateY(-4px); }

    .pricing-card.highlighted {
      border-color: #C9A84C;
      background: linear-gradient(160deg, rgba(201,168,76,0.08) 0%, #1A1710 100%);
      box-shadow: 0 0 0 1px rgba(201,168,76,0.3), 0 16px 48px rgba(201,168,76,0.08);
    }

    .pricing-badge {
      position: absolute;
      top: -12px;
      left: 50%;
      transform: translateX(-50%);
      background: #C9A84C;
      color: #0D0B07;
      font-size: 0.65rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      padding: 0.25rem 0.85rem;
      border-radius: 20px;
      white-space: nowrap;
    }

    .pricing-name {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.5rem;
      font-weight: 600;
      color: #F0E8D0;
    }

    .pricing-tagline {
      font-size: 0.8rem;
      color: rgba(240,232,208,0.5);
      margin-top: -1rem;
    }

    .pricing-price {
      display: flex;
      align-items: baseline;
      gap: 0.4rem;
    }

    .price-amount {
      font-family: 'Cormorant Garamond', serif;
      font-size: 2rem;
      font-weight: 600;
      color: #F0E8D0;
    }

    .price-period {
      font-size: 0.8rem;
      color: rgba(240,232,208,0.4);
    }

    .pricing-features {
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
      flex: 1;
    }

    .pricing-features li {
      font-size: 0.83rem;
      color: rgba(240,232,208,0.7);
      display: flex;
      gap: 0.5rem;
      align-items: flex-start;
    }

    .check {
      color: #5cdb6f;
      font-weight: 700;
      flex-shrink: 0;
    }

    .btn-plan {
      display: block;
      text-align: center;
      padding: 0.75rem;
      border: 1px solid rgba(201,168,76,0.3);
      border-radius: 8px;
      color: #C9A84C;
      text-decoration: none;
      font-size: 0.85rem;
      font-weight: 600;
      transition: background 0.2s, border-color 0.2s;
    }

    .btn-plan:hover { background: rgba(201,168,76,0.1); border-color: #C9A84C; }

    .btn-plan-gold {
      background: #C9A84C;
      color: #0D0B07;
      border-color: #C9A84C;
    }

    .btn-plan-gold:hover { background: #DAC372; border-color: #DAC372; }

    /* ── Audience ── */
    .section-audience {
      border-top: 1px solid rgba(201,168,76,0.08);
    }

    .audience-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 1.25rem;
      margin-top: 2.5rem;
    }

    .audience-card {
      padding: 1.5rem;
      background: #1A1710;
      border: 1px solid rgba(201,168,76,0.1);
      border-radius: 12px;
      text-align: center;
    }

    .audience-icon { font-size: 1.75rem; margin-bottom: 0.75rem; }

    .audience-card p {
      font-size: 0.85rem;
      color: rgba(240,232,208,0.65);
      line-height: 1.6;
    }

    /* ── Final CTA ── */
    .section-final-cta {
      padding-bottom: 5rem;
    }

    .final-cta-card {
      position: relative;
      overflow: hidden;
      text-align: center;
      padding: 5rem 2rem;
      background: #1A1710;
      border: 1px solid rgba(201,168,76,0.25);
      border-radius: 20px;
    }

    .final-cta-glow {
      position: absolute;
      width: 600px; height: 600px;
      background: radial-gradient(circle, rgba(201,168,76,0.1) 0%, transparent 65%);
      top: 50%; left: 50%;
      transform: translate(-50%, -50%);
      pointer-events: none;
    }

    .final-cta-icon {
      font-size: 2.5rem;
      color: #C9A84C;
      margin-bottom: 1.25rem;
      position: relative;
    }

    .final-cta-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: clamp(1.8rem, 3.5vw, 2.8rem);
      font-weight: 600;
      color: #F0E8D0;
      margin-bottom: 1rem;
      position: relative;
    }

    .final-cta-sub {
      font-size: 0.95rem;
      color: rgba(240,232,208,0.55);
      margin-bottom: 2.5rem;
      position: relative;
    }

    .final-cta-actions {
      display: flex;
      gap: 1rem;
      justify-content: center;
      flex-wrap: wrap;
      position: relative;
    }

    /* ── Footer ── */
    .lp-footer {
      border-top: 1px solid rgba(201,168,76,0.1);
      padding: 1.5rem 2.5rem;
    }

    .footer-inner {
      max-width: 1100px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      flex-wrap: wrap;
    }

    .footer-logo {
      font-family: 'Cormorant Garamond', serif;
      color: rgba(201,168,76,0.7);
      font-size: 1rem;
    }

    .footer-copy {
      font-size: 0.78rem;
      color: rgba(240,232,208,0.3);
    }

    .footer-links {
      display: flex;
      gap: 1.5rem;
    }

    .footer-links a {
      font-size: 0.8rem;
      color: rgba(240,232,208,0.4);
      text-decoration: none;
      transition: color 0.2s;
    }

    .footer-links a:hover { color: #C9A84C; }

    /* ── Responsive ── */
    @media (max-width: 900px) {
      .hero { grid-template-columns: 1fr; padding-top: 6rem; }
      .hero-visual { display: none; }
      .principle-grid { grid-template-columns: 1fr; }
      .pricing-grid { grid-template-columns: 1fr; }
      .lp-nav-links { display: none; }
    }

    @media (max-width: 600px) {
      .section { padding: 4rem 1.25rem; }
      .lp-nav { padding: 0 1.25rem; }
      .hero { padding: 5rem 1.25rem 3rem; }
    }
  `]
})
export class LandingComponent implements OnInit, OnDestroy {
  scrolled = false;
  currency: 'XOF' | 'EUR' = 'XOF';
  year = new Date().getFullYear();
  plans = PLANS;

  steps = [
    {
      icon: '📥',
      title: 'Ajoutez vos sources de revenus',
      desc: 'Salaire, freelance, location, mobile money… Configurez une ou plusieurs sources selon votre plan.'
    },
    {
      icon: '📊',
      title: 'Saisissez vos revenus chaque mois',
      desc: 'En quelques secondes. Vous avez déjà un historique sur Excel ou CSV ? Importez-le en un clic.'
    },
    {
      icon: '🧠',
      title: 'Joseph analyse et classe votre mois',
      desc: 'L\'outil compare votre revenu à votre moyenne des 3 derniers mois et détermine si vous êtes en abondance, en période normale ou en disette.'
    },
    {
      icon: '✦',
      title: 'Recevez vos conseils de répartition',
      desc: 'Choisissez la règle financière adaptée — 50/30/20, 70/20/10, Pareto, ou le Principe de Joseph — et suivez vos allocations précisément.'
    }
  ];

  features = [
    {
      icon: '🔔',
      title: 'Alertes automatiques',
      desc: 'Notifié dès qu\'un mois d\'abondance ou de disette est détecté, en temps réel.'
    },
    {
      icon: '📈',
      title: 'Réserve Joseph estimée',
      desc: 'Visualisez le coussin financier constitué grâce à vos mois d\'abondance passés.'
    },
    {
      icon: '📋',
      title: 'Règles financières multiples',
      desc: '50/30/20, 70/20/10, 80/20 (Pareto), Principe de Joseph — choisissez ou changez à tout moment.'
    },
    {
      icon: '📄',
      title: 'Rapports PDF',
      desc: 'Générez des rapports mensuels et annuels détaillés, téléchargeables en un clic (Premium).'
    },
    {
      icon: '💱',
      title: 'Multi-devises',
      desc: 'XOF, EUR, USD, MAD, NGN… Gérez vos revenus dans la devise de votre choix.'
    },
    {
      icon: '🔒',
      title: 'Sécurisé & privé',
      desc: 'Vos données financières restent les vôtres. JWT sécurisé, aucune donnée revendue.'
    }
  ];

  audiences = [
    { icon: '💼', text: 'Vous êtes freelance ou consultant avec des revenus irréguliers' },
    { icon: '🌍', text: 'Vous vivez en Afrique francophone ou dans la diaspora européenne' },
    { icon: '📉', text: 'Vos revenus varient d\'un mois à l\'autre et vous voulez anticiper' },
    { icon: '🤲', text: 'Vous cherchez une approche éthique et culturellement ancrée' },
    { icon: '📱', text: 'Vous gérez plusieurs sources de revenus simultanément' },
    { icon: '🎯', text: 'Vous voulez épargner sans effort grâce à des règles automatiques' },
  ];

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    if (this.authService.isTokenValid()) {
      this.router.navigate(['/dashboard']);
    }
  }

  ngOnDestroy(): void {}

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled = window.scrollY > 20;
  }

  scrollTo(event: Event, id: string): void {
    event.preventDefault();
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  }
}
