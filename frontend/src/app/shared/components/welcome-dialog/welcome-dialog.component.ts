import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { DialogModule } from 'primeng/dialog';

@Component({
  selector: 'app-welcome-dialog',
  standalone: true,
  imports: [CommonModule, RouterModule, DialogModule],
  template: `
    <p-dialog
      header=" "
      [(visible)]="visible"
      (visibleChange)="onVisibleChange($event)"
      [modal]="true"
      [style]="{ width: '640px', maxWidth: '95vw' }"
      [baseZIndex]="2000"
      [draggable]="false"
      [resizable]="false"
      styleClass="help-dialog"
    >
      <div class="help-body">
        <div class="help-icon">✦</div>
        <h2 class="help-title">{{ titleLabel }}</h2>
        <p class="help-intro">Trois petits gestes suffisent pour démarrer.</p>

        <div class="help-steps">
          <div class="help-step">
            <span class="help-step-num">1</span>
            <div>
              <strong>Ajoutez une source de revenu</strong>
              <p>Menu <em>« Mes Revenus »</em> → onglet <em>« Sources »</em> → bouton <em>« Ajouter une source »</em>.</p>
            </div>
          </div>
          <div class="help-step">
            <span class="help-step-num">2</span>
            <div>
              <strong>Indiquez ce que vous avez gagné ce mois</strong>
              <p>Onglet <em>« Saisie mensuelle »</em> → saisissez le montant → <em>« Enregistrer »</em>.</p>
            </div>
          </div>
          <div class="help-step">
            <span class="help-step-num">3</span>
            <div>
              <strong>Revenez au tableau de bord</strong>
              <p>Votre première répartition et le conseil de Joseph vous attendent.</p>
            </div>
          </div>
        </div>

        <div class="video-block">
          <p class="video-label">
            Préfères voir en vidéo ?
            <span class="video-duration">(2 min)</span>
          </p>
          <div class="video-frame">
            <iframe
              *ngIf="visible"
              [src]="videoUrl"
              title="Démo Joseph · Yusuf"
              allow="accelerometer; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              referrerpolicy="strict-origin-when-cross-origin"
              loading="lazy"
              allowfullscreen
            ></iframe>
          </div>
        </div>

        <div class="help-footer">
          <a routerLink="/incomes" (click)="onCtaClick()" class="btn-help-start">Commencer maintenant →</a>
        </div>
      </div>
    </p-dialog>
  `,
  styles: [`
    .help-body {
      text-align: center;
      padding: 0.5rem 0.5rem 1rem;
    }

    .help-icon {
      font-size: 2rem;
      color: var(--gold);
      margin-bottom: 0.75rem;
    }

    .help-title {
      font-family: var(--font-serif);
      font-size: 1.7rem;
      font-weight: 600;
      color: var(--text-0);
      margin: 0 0 1rem;
    }

    .help-intro {
      color: var(--text-1);
      font-size: 0.92rem;
      line-height: 1.7;
      max-width: 500px;
      margin: 0 auto;
    }

    .help-intro strong {
      color: var(--gold-light);
    }

    .help-steps {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      text-align: left;
      max-width: 500px;
      margin: 1.75rem auto 0;
    }

    .help-step {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      padding: 12px 14px;
      border-radius: 10px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--line-soft);
    }

    .help-step-num {
      flex-shrink: 0;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      color: var(--gold-light);
      font-size: 0.75rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-top: 2px;
    }

    .help-step strong {
      display: block;
      font-size: 0.88rem;
      color: var(--text-0);
      margin-bottom: 0.2rem;
    }

    .help-step p {
      font-size: 0.82rem;
      color: var(--text-2);
      line-height: 1.55;
      margin: 0;
    }

    .help-step em {
      color: var(--gold-light);
      font-style: normal;
      font-weight: 500;
    }

    /* ── Encadré vidéo ── */
    .video-block {
      max-width: 500px;
      margin: 1.75rem auto 0;
      text-align: left;
    }

    .video-label {
      font-size: 0.82rem;
      color: var(--text-2);
      margin: 0 0 0.6rem;
      letter-spacing: 0.01em;
    }

    .video-duration {
      color: var(--gold-light);
      font-weight: 500;
    }

    .video-frame {
      position: relative;
      width: 100%;
      aspect-ratio: 16 / 9;
      border-radius: 12px;
      overflow: hidden;
      border: 1px solid var(--line-strong);
      background: #000;
      box-shadow: 0 8px 24px -12px rgba(0, 0, 0, 0.5);
    }

    .video-frame iframe {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      border: 0;
    }

    .help-footer {
      margin-top: 2rem;
    }

    .btn-help-start {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 11px 24px;
      background: linear-gradient(180deg, var(--gold-light), var(--gold));
      border-radius: 10px;
      color: #1b1500;
      font-size: 0.88rem;
      font-weight: 600;
      text-decoration: none;
      transition: box-shadow 0.2s;
      box-shadow: 0 8px 24px -8px var(--gold-glow);
    }

    .btn-help-start:hover {
      box-shadow: 0 12px 32px -8px var(--gold-glow);
    }

    @media (max-width: 480px) {
      .help-title { font-size: 1.4rem; }
      .help-intro { font-size: 0.88rem; }
      .help-step { padding: 10px 12px; }
      .help-step strong { font-size: 0.85rem; }
      .help-step p { font-size: 0.78rem; }
      .video-label { font-size: 0.78rem; }
      .btn-help-start { padding: 10px 20px; font-size: 0.85rem; }
    }
  `]
})
export class WelcomeDialogComponent {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Input() titleLabel = 'Bienvenue dans votre espace Joseph · Yusuf';

  readonly videoUrl: SafeResourceUrl;

  constructor(private readonly sanitizer: DomSanitizer) {
    this.videoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
      'https://www.youtube-nocookie.com/embed/48Iin6rJBL8?rel=0'
    );
  }

  onVisibleChange(value: boolean): void {
    this.visible = value;
    this.visibleChange.emit(value);
  }

  onCtaClick(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }
}
