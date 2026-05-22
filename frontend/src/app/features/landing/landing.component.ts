import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { AnimateOnScrollDirective } from '../../shared/directives/animate-on-scroll.directive';

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
  imports: [CommonModule, RouterModule, AnimateOnScrollDirective],
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
      <button class="lp-burger" (click)="mobileMenuOpen = !mobileMenuOpen" [class.open]="mobileMenuOpen" aria-label="Menu">
        <span></span><span></span><span></span>
      </button>
    </header>

    <!-- ── Drawer mobile ─────────────────────────────────────────────── -->
    <div class="lp-drawer-overlay" *ngIf="mobileMenuOpen" (click)="mobileMenuOpen = false"></div>
    <aside class="lp-drawer" [class.open]="mobileMenuOpen">
      <a href="#principe" class="lp-drawer-link" (click)="scrollTo($event,'principe'); mobileMenuOpen = false">Le Principe</a>
      <a href="#fonctionnalites" class="lp-drawer-link" (click)="scrollTo($event,'fonctionnalites'); mobileMenuOpen = false">Fonctionnalités</a>
      <a href="#tarifs" class="lp-drawer-link" (click)="scrollTo($event,'tarifs'); mobileMenuOpen = false">Tarifs</a>
      <div class="lp-drawer-divider"></div>
      <a routerLink="/login" class="lp-drawer-link" (click)="mobileMenuOpen = false">Se connecter</a>
      <a routerLink="/register" class="lp-drawer-cta" (click)="mobileMenuOpen = false">Créer un compte</a>
    </aside>

    <!-- ── Hero ───────────────────────────────────────────────────────── -->
    <section class="hero">
      <div class="hero-glow hero-glow-1"></div>
      <div class="hero-glow hero-glow-2"></div>
      <div class="hero-content">
        <span class="hero-eyebrow fade-in-up" style="animation-delay: 0ms">Gestion des revenus variables</span>
        <h1 class="hero-title fade-in-up" style="animation-delay: 100ms">
          Épargner pendant<br>
          <span class="hero-accent">l'abondance.</span><br>
          Tenir pendant<br>
          <span class="hero-accent">la disette.</span>
        </h1>
        <p class="hero-sub fade-in-up" style="animation-delay: 200ms">
          Joseph · Yusuf applique un principe millénaire à votre situation financière.
          Saisissez vos revenus, l'outil fait le reste — alertes, répartitions, réserves.
        </p>
        <div class="hero-cta fade-in-up" style="animation-delay: 300ms">
          <a routerLink="/register" class="btn-hero-primary">Commencer gratuitement</a>
          <a href="#principe"       class="btn-hero-ghost"   (click)="scrollTo($event,'principe')">Voir comment ça marche ↓</a>
        </div>
        <p class="hero-caption fade-in-up" style="animation-delay: 400ms">Gratuit · Sans carte bancaire · Idéal pour l'Afrique francophone &amp; la diaspora</p>
      </div>

      <!-- Visualisation animée -->
      <div class="hero-visual" aria-hidden="true">
        <div class="vis-card vis-card-main vis-float-1">
          <div class="vis-label">Mois en cours</div>
          <div class="vis-amount">{{ heroAmountDisplay }} <span>XOF</span></div>
          <div class="vis-badge abundance pulse-gold">Abondance +18%</div>
          <div class="vis-bar-row">
            <div class="vis-bar-item">
              <div class="vis-bar-fill vis-bar-animate"
                   style="--target-h: 70%; background: #C9A84C; animation-delay: 600ms"></div>
              <span>Besoins</span>
            </div>
            <div class="vis-bar-item">
              <div class="vis-bar-fill vis-bar-animate"
                   style="--target-h: 30%; background: #5cdb6f; animation-delay: 800ms"></div>
              <span>Épargne</span>
            </div>
            <div class="vis-bar-item">
              <div class="vis-bar-fill vis-bar-animate"
                   style="--target-h: 20%; background: #5dade2; animation-delay: 1000ms"></div>
              <span>Invest.</span>
            </div>
          </div>
        </div>
        <div class="vis-card vis-card-secondary vis-float-2">
          <div class="vis-mini-label">Réserve Joseph</div>
          <div class="vis-mini-amount">92 000 XOF</div>
          <div class="vis-mini-sub">Constituée sur 4 mois d'abondance</div>
        </div>
        <div class="vis-card vis-card-alert vis-float-3">
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
          <div class="principle-card abundance-card" appAnimateOnScroll [animationDelay]="0">
            <div class="principle-icon">🌿</div>
            <h3>Période d'abondance</h3>
            <p>Votre revenu dépasse votre moyenne de 15% ou plus. C'est le moment de constituer une réserve plutôt que d'augmenter vos dépenses.</p>
          </div>
          <div class="principle-card normal-card" appAnimateOnScroll [animationDelay]="100">
            <div class="principle-icon">⚖️</div>
            <h3>Période normale</h3>
            <p>Votre revenu reste dans une fourchette de ±15% par rapport à votre moyenne. Continuez à appliquer votre règle de répartition sans changement.</p>
          </div>
          <div class="principle-card lean-card" appAnimateOnScroll [animationDelay]="200">
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
          <div class="feature-card"
               *ngFor="let f of features; let i = index"
               appAnimateOnScroll
               [animationDelay]="i * 100">
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
            *ngFor="let plan of plans; let i = index"
            [class.highlighted]="plan.highlight"
            appAnimateOnScroll
            [animationDelay]="i * 100"
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
          <div class="audience-card"
               *ngFor="let a of audiences; let i = index"
               appAnimateOnScroll
               [animationDelay]="i * 80">
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
        <div class="footer-brand">
          <span class="footer-logo">Joseph · Yusuf</span>
          <span class="footer-copy">© {{ year }} Rey Dedy Pangou — Tous droits réservés</span>
        </div>
        <div class="footer-columns">
          <div class="footer-col">
            <span class="footer-col-title">Produit</span>
            <a routerLink="/register">Créer un compte</a>
            <a routerLink="/login">Se connecter</a>
            <a href="#tarifs" (click)="scrollTo($event,'tarifs')">Tarifs</a>
          </div>
          <div class="footer-col">
            <span class="footer-col-title">Légal</span>
            <a routerLink="/cgu">CGU</a>
            <a routerLink="/privacy">Confidentialité</a>
            <a routerLink="/legal">Mentions légales</a>
          </div>
          <div class="footer-col">
            <span class="footer-col-title">Support</span>
            <a routerLink="/contact">Contact</a>
            <a href="mailto:support&#64;josephyusuf.com">support&#64;josephyusuf.com</a>
          </div>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    /* ── Reset & tokens ── */
    :host {
      display: block;
      background: var(--night-1, #0d0e1c);
      color: var(--text-0, #F5F5F5);
      font-family: var(--font-sans, 'DM Sans', sans-serif);
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
      background: rgba(8, 8, 15, 0.55);
      backdrop-filter: blur(20px) saturate(160%);
      -webkit-backdrop-filter: blur(20px) saturate(160%);
      border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    }

    .lp-nav.scrolled {
      background: rgba(8, 8, 15, 0.85);
      backdrop-filter: blur(20px) saturate(160%);
      -webkit-backdrop-filter: blur(20px) saturate(160%);
      border-color: rgba(255, 255, 255, 0.06);
    }

    .lp-logo {
      font-family: var(--font-serif, 'Cormorant Garamond', serif);
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
      color: var(--text-2, rgba(245, 245, 245, 0.65));
      text-decoration: none;
      font-size: 0.88rem;
      font-weight: 500;
      transition: color 0.2s;
    }

    .lp-nav-link:hover { color: var(--text-0, #F5F5F5); }

    .lp-nav-actions {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .btn-ghost {
      padding: 0.4rem 1rem;
      color: var(--text-0, #F5F5F5);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 8px;
      font-size: 0.85rem;
      text-decoration: none;
      transition: border-color 0.2s, background 0.2s;
    }

    .btn-ghost:hover {
      border-color: rgba(255, 255, 255, 0.18);
      background: rgba(255, 255, 255, 0.05);
    }

    .btn-gold {
      padding: 0.4rem 1rem;
      background: linear-gradient(180deg, #E8C876, #C9A84C);
      box-shadow: 0 8px 24px -8px rgba(201,168,76,0.45);
      color: #0D0B07;
      border: none;
      border-radius: 8px;
      font-size: 0.85rem;
      font-weight: 700;
      text-decoration: none;
      transition: background 0.2s, box-shadow 0.2s;
    }

    .btn-gold:hover { background: linear-gradient(180deg, #F0D88A, #DAC372); box-shadow: 0 10px 28px -8px rgba(201,168,76,0.55); }

    /* ── Burger button (hidden on desktop) ── */
    .lp-burger {
      display: none;
      flex-direction: column;
      gap: 5px;
      width: 32px;
      height: 32px;
      padding: 6px;
      background: transparent;
      border: none;
      cursor: pointer;
      align-items: center;
      justify-content: center;
    }
    .lp-burger span {
      display: block;
      width: 22px;
      height: 2px;
      background: var(--text-0, #F5F5F5);
      border-radius: 2px;
      transition: transform 0.25s, opacity 0.25s;
      transform-origin: center;
    }
    .lp-burger.open span:nth-child(1) { transform: translateY(7px) rotate(45deg); }
    .lp-burger.open span:nth-child(2) { opacity: 0; }
    .lp-burger.open span:nth-child(3) { transform: translateY(-7px) rotate(-45deg); }

    /* ── Drawer mobile ── */
    .lp-drawer-overlay {
      position: fixed; inset: 0;
      background: rgba(0, 0, 0, 0.5);
      backdrop-filter: blur(4px);
      -webkit-backdrop-filter: blur(4px);
      z-index: 998;
      animation: fade-in 0.2s ease-out;
    }
    .lp-drawer {
      position: fixed;
      top: 0; right: 0;
      width: 280px;
      max-width: 85vw;
      height: 100vh;
      background: rgba(13, 14, 28, 0.98);
      backdrop-filter: blur(20px) saturate(160%);
      -webkit-backdrop-filter: blur(20px) saturate(160%);
      border-left: 1px solid rgba(201, 168, 76, 0.2);
      padding: 80px 1.5rem 2rem;
      z-index: 999;
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
      transform: translateX(100%);
      transition: transform 0.3s cubic-bezier(0.2, 0.7, 0.2, 1);
    }
    .lp-drawer.open { transform: translateX(0); }
    .lp-drawer-link {
      padding: 0.85rem 1rem;
      color: var(--text-1, #D9D9DE);
      text-decoration: none;
      font-size: 0.95rem;
      border-radius: 8px;
      transition: background 0.15s, color 0.15s;
    }
    .lp-drawer-link:hover { background: rgba(201,168,76,0.08); color: #C9A84C; }
    .lp-drawer-divider {
      height: 1px;
      background: rgba(255, 255, 255, 0.08);
      margin: 0.5rem 0;
    }
    .lp-drawer-cta {
      margin-top: 0.5rem;
      padding: 0.85rem 1rem;
      background: linear-gradient(180deg, #E8C876, #C9A84C);
      color: #0D0B07;
      text-align: center;
      text-decoration: none;
      font-size: 0.95rem;
      font-weight: 700;
      border-radius: 8px;
      box-shadow: 0 8px 24px -8px rgba(201,168,76,0.45);
    }

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
      background: radial-gradient(circle, rgba(201,168,76,0.1) 0%, rgba(30,60,120,0.08) 40%, transparent 70%);
      top: -100px; left: -100px;
    }

    .hero-glow-2 {
      width: 400px; height: 400px;
      background: radial-gradient(circle, rgba(40,80,160,0.1) 0%, rgba(92,219,111,0.04) 50%, transparent 70%);
      bottom: 0; right: 100px;
    }

    .hero-eyebrow {
      display: inline-block;
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      color: #C9A84C;
      border: 1px solid rgba(201, 168, 76, 0.3);
      padding: 0.25rem 0.75rem;
      border-radius: 20px;
      margin-bottom: 1.5rem;
    }

    .hero-title {
      font-family: var(--font-serif, 'Cormorant Garamond', serif);
      font-size: clamp(2.6rem, 5vw, 4rem);
      font-weight: 600;
      line-height: 1.15;
      letter-spacing: -0.025em;
      color: var(--text-0, #F5F5F5);
      margin-bottom: 1.5rem;
    }

    .hero-accent {
      color: #C9A84C;
      position: relative;
    }

    .hero-sub {
      font-size: 1rem;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
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
      background: linear-gradient(180deg, #E8C876, #C9A84C);
      box-shadow: 0 8px 24px -8px rgba(201,168,76,0.45);
      color: #0D0B07;
      border: none;
      border-radius: 10px;
      font-size: 0.95rem;
      font-weight: 700;
      text-decoration: none;
      transition: background 0.2s, transform 0.15s, box-shadow 0.2s;
    }

    .btn-hero-primary:hover {
      background: linear-gradient(180deg, #F0D88A, #DAC372);
      box-shadow: 0 10px 28px -8px rgba(201,168,76,0.55);
      transform: translateY(-1px);
    }

    .btn-hero-ghost {
      padding: 0.85rem 1.75rem;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 10px;
      font-size: 0.95rem;
      text-decoration: none;
      transition: border-color 0.2s, color 0.2s;
    }

    .btn-hero-ghost:hover {
      border-color: rgba(255, 255, 255, 0.18);
      color: var(--text-0, #F5F5F5);
    }

    .hero-caption {
      font-size: 0.75rem;
      color: var(--text-3, rgba(245, 245, 245, 0.4));
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
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border-radius: 14px;
      padding: 1.25rem 1.5rem;
      border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .vis-card-main {
      border-color: rgba(255, 255, 255, 0.12);
      box-shadow: 0 8px 32px rgba(0,0,0,0.3);
    }

    .vis-label {
      font-size: 0.7rem;
      text-transform: uppercase;
      letter-spacing: 0.1em;
      color: var(--text-3, rgba(245, 245, 245, 0.4));
      margin-bottom: 0.5rem;
    }

    .vis-amount {
      font-family: var(--font-serif, 'Cormorant Garamond', serif);
      font-size: 2rem;
      font-weight: 600;
      color: var(--text-0, #F5F5F5);
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

    /* Bar fill vertical : 0 → --target-h */
    .vis-bar-animate {
      height: 0;
      animation: visBarFill 900ms cubic-bezier(0.2, 0.7, 0.2, 1) forwards;
    }
    @keyframes visBarFill {
      from { height: 0; }
      to   { height: var(--target-h, 50%); }
    }

    /* Float subtil sur les cartes du hero-visual (3 phases décalées) */
    .vis-float-1 { animation: visFloat 6s ease-in-out infinite; }
    .vis-float-2 { animation: visFloat 7s ease-in-out infinite; animation-delay: -2s; }
    .vis-float-3 { animation: visFloat 8s ease-in-out infinite; animation-delay: -4s; }
    @keyframes visFloat {
      0%, 100% { transform: translateY(0); }
      50%      { transform: translateY(-8px); }
    }

    @media (prefers-reduced-motion: reduce) {
      .vis-bar-animate { height: var(--target-h, 50%); animation: none; }
      .vis-float-1, .vis-float-2, .vis-float-3 { animation: none; }
    }

    .vis-bar-item span {
      font-size: 0.62rem;
      color: var(--text-3, rgba(245, 245, 245, 0.4));
    }

    .vis-card-secondary {
      align-self: flex-end;
      width: 75%;
    }

    .vis-mini-label {
      font-size: 0.65rem;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: var(--text-3, rgba(245, 245, 245, 0.4));
      margin-bottom: 0.3rem;
    }

    .vis-mini-amount {
      font-family: var(--font-serif, 'Cormorant Garamond', serif);
      font-size: 1.35rem;
      color: #5cdb6f;
      font-weight: 600;
    }

    .vis-mini-sub {
      font-size: 0.68rem;
      color: var(--text-3, rgba(245, 245, 245, 0.4));
      margin-top: 0.2rem;
    }

    .vis-card-alert {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      padding: 0.85rem 1.1rem;
      background: rgba(201,168,76,0.07);
      border-color: rgba(201,168,76,0.25);
    }

    .vis-alert-icon { color: #C9A84C; font-size: 0.9rem; flex-shrink: 0; }
    .vis-alert-text { font-size: 0.78rem; color: var(--text-2, rgba(245, 245, 245, 0.65)); line-height: 1.4; }

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
      font-family: var(--font-serif, 'Cormorant Garamond', serif);
      font-size: clamp(1.8rem, 3.5vw, 2.6rem);
      font-weight: 600;
      line-height: 1.25;
      color: var(--text-0, #F5F5F5);
      margin-bottom: 1.25rem;
    }

    .section-sub {
      font-size: 0.95rem;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
      line-height: 1.75;
      max-width: 600px;
      margin-bottom: 3.5rem;
    }

    /* ── Section Principe ── */
    .section-principe {
      background: linear-gradient(180deg, #0d0e1c 0%, #101325 100%);
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
      color: var(--text-0, #F5F5F5);
      margin-bottom: 0.75rem;
    }

    .principle-card p {
      font-size: 0.85rem;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
      line-height: 1.65;
      margin-bottom: 1.25rem;
    }


    /* ── Steps ── */
    .section-steps {
      border-top: 1px solid rgba(255, 255, 255, 0.08);
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
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
      background: rgba(255, 255, 255, 0.08);
      margin: 6px 0;
    }

    .step-body {
      padding-bottom: 2rem;
    }

    .step-icon { font-size: 1.6rem; margin-bottom: 0.5rem; }

    .step-title {
      font-size: 1rem;
      font-weight: 600;
      color: var(--text-0, #F5F5F5);
      margin-bottom: 0.35rem;
    }

    .step-desc {
      font-size: 0.875rem;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
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
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 12px;
      transition: border-color 0.2s, transform 0.2s;
    }

    .feature-card:hover {
      border-color: rgba(255, 255, 255, 0.18);
      transform: translateY(-2px);
    }

    .feature-icon { font-size: 1.5rem; margin-bottom: 0.75rem; }

    .feature-title {
      font-size: 0.9rem;
      font-weight: 600;
      color: var(--text-0, #F5F5F5);
      margin-bottom: 0.4rem;
    }

    .feature-desc {
      font-size: 0.8rem;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
      line-height: 1.6;
    }

    /* ── Pricing ── */
    .section-pricing { background: #0d0e1c; }

    .currency-toggle {
      display: inline-flex;
      gap: 0;
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 8px;
      overflow: hidden;
      margin-bottom: 3rem;
    }

    .currency-toggle button {
      padding: 0.45rem 1.25rem;
      background: transparent;
      border: none;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
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
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 16px;
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      transition: transform 0.2s;
    }

    .pricing-card { transform: scale(0.98); transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s; }
    .pricing-card:hover { transform: scale(1); border-color: rgba(201, 168, 76, 0.4); }
    .pricing-card.highlighted { transform: scale(1); }
    .pricing-card.highlighted:hover { transform: scale(1.02); }

    .pricing-card.highlighted {
      border-color: var(--gold, #C9A84C);
      background: linear-gradient(160deg, rgba(201,168,76,0.06) 0%, rgba(19, 22, 42, 0.7) 100%);
      box-shadow: 0 0 0 1px rgba(201,168,76,0.25) inset, 0 30px 60px -20px rgba(201,168,76,0.18);
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
      font-family: var(--font-serif, 'Cormorant Garamond', serif);
      font-size: 1.5rem;
      font-weight: 600;
      color: var(--text-0, #F5F5F5);
    }

    .pricing-tagline {
      font-size: 0.8rem;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
      margin-top: -1rem;
    }

    .pricing-price {
      display: flex;
      align-items: baseline;
      gap: 0.4rem;
    }

    .price-amount {
      font-family: var(--font-serif, 'Cormorant Garamond', serif);
      font-size: 2rem;
      font-weight: 600;
      color: var(--text-0, #F5F5F5);
    }

    .price-period {
      font-size: 0.8rem;
      color: var(--text-3, rgba(245, 245, 245, 0.4));
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
      color: var(--text-2, rgba(245, 245, 245, 0.65));
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
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 8px;
      color: #C9A84C;
      text-decoration: none;
      font-size: 0.85rem;
      font-weight: 600;
      transition: background 0.2s, border-color 0.2s;
    }

    .btn-plan:hover { background: rgba(201,168,76,0.1); border-color: rgba(201,168,76,0.4); }

    .btn-plan-gold {
      background: linear-gradient(180deg, #E8C876, #C9A84C);
      box-shadow: 0 8px 24px -8px rgba(201,168,76,0.45);
      color: #0D0B07;
      border-color: #C9A84C;
    }

    .btn-plan-gold:hover { background: linear-gradient(180deg, #F0D88A, #DAC372); border-color: #DAC372; }

    /* ── Audience ── */
    .section-audience {
      border-top: 1px solid rgba(255, 255, 255, 0.08);
    }

    .audience-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 1.25rem;
      margin-top: 2.5rem;
    }

    .audience-card {
      padding: 1.5rem;
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 12px;
      text-align: center;
    }

    .audience-icon { font-size: 1.75rem; margin-bottom: 0.75rem; }

    .audience-card p {
      font-size: 0.85rem;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
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
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border: 1px solid rgba(255, 255, 255, 0.08);
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
      font-family: var(--font-serif, 'Cormorant Garamond', serif);
      font-size: clamp(1.8rem, 3.5vw, 2.8rem);
      font-weight: 600;
      color: var(--text-0, #F5F5F5);
      margin-bottom: 1rem;
      position: relative;
    }

    .final-cta-sub {
      font-size: 0.95rem;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
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
      border-top: 1px solid rgba(255, 255, 255, 0.08);
      padding: 2.5rem 2.5rem 2rem;
    }

    .footer-inner {
      max-width: 1100px;
      margin: 0 auto;
      display: grid;
      grid-template-columns: 1fr 2fr;
      gap: 2.5rem;
      align-items: start;
    }

    .footer-brand {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .footer-logo {
      font-family: var(--font-serif, 'Cormorant Garamond', serif);
      color: rgba(201,168,76,0.85);
      font-size: 1.1rem;
    }

    .footer-copy {
      font-size: 0.78rem;
      color: var(--text-3, rgba(245, 245, 245, 0.4));
    }

    .footer-columns {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1.5rem;
    }

    .footer-col {
      display: flex;
      flex-direction: column;
      gap: 0.45rem;
    }

    .footer-col-title {
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      color: #C9A84C;
      margin-bottom: 0.35rem;
    }

    .footer-col a {
      font-size: 0.83rem;
      color: var(--text-2, rgba(245, 245, 245, 0.65));
      text-decoration: none;
      transition: color 0.2s;
    }

    .footer-col a:hover { color: #C9A84C; }

    /* ── Responsive ── */
    /* Tablet : 768px – 1023px */
    @media (min-width: 768px) and (max-width: 1023px) {
      .hero { grid-template-columns: 1fr; padding: 6rem 2rem 4rem; }
      .hero-title { font-size: 2.5rem; }
      .hero-visual { max-width: 520px; margin: 0 auto; }
      .principle-grid { grid-template-columns: repeat(2, 1fr); }
      .principle-card:last-child { grid-column: 1 / -1; max-width: 50%; margin: 0 auto; }
      .features-grid { grid-template-columns: repeat(2, 1fr); }
      .pricing-grid { grid-template-columns: repeat(2, 1fr); }
      .pricing-card.highlighted { grid-column: 1 / -1; max-width: 55%; margin: 0 auto; }
      .footer-inner { grid-template-columns: 1fr; gap: 2rem; }
    }

    /* Mobile : ≤ 767px */
    @media (max-width: 767px) {
      .lp-nav { padding: 0 1.25rem; }
      .lp-nav-links { display: none; }
      .lp-nav-actions { display: none; }
      .lp-burger { display: flex; }

      .hero {
        grid-template-columns: 1fr;
        padding: 5rem 1.25rem 3rem;
        gap: 2.5rem;
        min-height: auto;
      }
      .hero-title { font-size: 2rem; line-height: 1.2; }
      .hero-sub { font-size: 0.92rem; }
      .hero-cta { flex-direction: column; gap: 0.65rem; }
      .hero-cta a { width: 100%; text-align: center; }
      .hero-visual { display: none; }

      .section { padding: 4rem 1.25rem; }
      .section-title { font-size: 1.7rem; }

      .principle-grid { grid-template-columns: 1fr; gap: 1rem; }
      .features-grid { grid-template-columns: 1fr; gap: 1rem; }

      /* Pricing : scroll horizontal pour permettre la comparaison */
      .pricing-grid {
        display: flex;
        gap: 1rem;
        overflow-x: auto;
        scroll-snap-type: x mandatory;
        padding: 1rem 0.5rem;
        margin: 0 -1.25rem;
        padding-left: 1.25rem;
        padding-right: 1.25rem;
        -webkit-overflow-scrolling: touch;
      }
      .pricing-card {
        flex: 0 0 85%;
        scroll-snap-align: center;
      }

      .audience-grid { grid-template-columns: 1fr 1fr; gap: 0.75rem; }
      .audience-card { padding: 1rem; }
      .audience-card p { font-size: 0.8rem; }

      .final-cta-card { padding: 3rem 1.25rem; }
      .final-cta-actions { flex-direction: column; gap: 0.65rem; width: 100%; }
      .final-cta-actions a { width: 100%; text-align: center; }

      .lp-footer { padding: 2rem 1.25rem 1.5rem; }
      .footer-inner { grid-template-columns: 1fr; gap: 2rem; }
      .footer-columns { grid-template-columns: 1fr; gap: 1.5rem; }
    }
  `]
})
export class LandingComponent implements OnInit, OnDestroy {
  scrolled = false;
  mobileMenuOpen = false;
  currency: 'XOF' | 'EUR' = 'XOF';
  year = new Date().getFullYear();
  plans = PLANS;

  // Count-up hero visual
  private static readonly HERO_TARGET_AMOUNT = 425000;
  private heroCountRaf = 0;
  heroAmount = 0;

  get heroAmountDisplay(): string {
    return this.heroAmount.toLocaleString('fr-FR').replace(/,/g, ' ');
  }

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

  constructor(private readonly authService: AuthService, private readonly router: Router) {}

  ngOnInit(): void {
    if (this.authService.isTokenValid()) {
      this.router.navigate(['/dashboard']);
      return;
    }
    // Lance le count-up après un court délai pour synchroniser avec les animations CSS
    setTimeout(() => this.animateHeroAmount(), 250);
  }

  ngOnDestroy(): void {
    if (this.heroCountRaf) cancelAnimationFrame(this.heroCountRaf);
  }

  private animateHeroAmount(): void {
    const target = LandingComponent.HERO_TARGET_AMOUNT;
    const duration = 1100;
    const startTs = performance.now();
    const step = (now: number) => {
      const t = Math.min(1, (now - startTs) / duration);
      const eased = 1 - Math.pow(1 - t, 3); // easeOutCubic
      this.heroAmount = Math.round(target * eased);
      if (t < 1) {
        this.heroCountRaf = requestAnimationFrame(step);
      } else {
        this.heroAmount = target;
        this.heroCountRaf = 0;
      }
    };
    this.heroCountRaf = requestAnimationFrame(step);
  }

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled = window.scrollY > 20;
  }

  scrollTo(event: Event, id: string): void {
    event.preventDefault();
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  }
}
