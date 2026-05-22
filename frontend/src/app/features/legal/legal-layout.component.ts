import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-legal-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="legal-page">
      <header class="legal-header">
        <a routerLink="/" class="legal-logo">Joseph &middot; Yusuf</a>
        <a routerLink="/" class="legal-back">&larr; Retour &agrave; l'accueil</a>
      </header>

      <main class="legal-content fade-in-up">
        <span class="legal-eyebrow">{{ eyebrow }}</span>
        <h1>{{ title }}</h1>
        <p class="legal-meta" *ngIf="updatedAt">Derni&egrave;re mise &agrave; jour : {{ updatedAt }}</p>

        <ng-content></ng-content>
      </main>

      <footer class="legal-footer">
        <span class="legal-footer-copy">&copy; {{ year }} Rey Dedy Pangou &mdash; Tous droits r&eacute;serv&eacute;s</span>
        <div class="legal-footer-links">
          <a routerLink="/cgu">CGU</a>
          <a routerLink="/privacy">Confidentialit&eacute;</a>
          <a routerLink="/legal">Mentions l&eacute;gales</a>
          <a routerLink="/contact">Contact</a>
        </div>
      </footer>
    </div>
  `,
  styleUrls: ['./legal-page.scss']
})
export class LegalLayoutComponent {
  @Input() eyebrow = '';
  @Input() title = '';
  @Input() updatedAt = '';
  year = new Date().getFullYear();
}
