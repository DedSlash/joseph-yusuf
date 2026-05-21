import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-maintenance',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="error-page">
      <div class="error-brand">Joseph &middot; Yusuf</div>
      <div class="error-icon" aria-hidden="true">&#x1F527;</div>
      <h1 class="error-title">Maintenance en cours</h1>
      <p class="error-subtitle">
        Nous am&eacute;liorons Joseph&middot;Yusuf pour vous offrir une meilleure exp&eacute;rience.
        Nous revenons tr&egrave;s vite !
      </p>
      <blockquote class="error-quote">
        &laquo; Comme Joseph pr&eacute;parait l'&Eacute;gypte pendant les ann&eacute;es d'abondance,
        nous pr&eacute;parons votre outil pour les ann&eacute;es &agrave; venir. &#x1F31F; &raquo;
      </blockquote>
      <p class="retry-counter">
        Nouvelle tentative dans {{ countdown }}s...
      </p>
      <p class="error-contact">
        <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>
      </p>
    </div>
  `,
  styleUrls: ['../error-page.scss']
})
export class MaintenanceComponent implements OnInit, OnDestroy {
  countdown = 60;
  private timerId: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.timerId = setInterval(() => {
      this.countdown--;
      if (this.countdown <= 0) {
        window.location.reload();
      }
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}
