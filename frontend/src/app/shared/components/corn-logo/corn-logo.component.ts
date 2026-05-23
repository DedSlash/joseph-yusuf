import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-corn-logo',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg
      [attr.width]="size"
      [attr.height]="size"
      viewBox="0 0 64 96"
      xmlns="http://www.w3.org/2000/svg"
      [attr.aria-label]="ariaLabel"
      role="img"
    >
      <defs>
        <linearGradient id="cornKernelGrad" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" [attr.stop-color]="lightColor"/>
          <stop offset="100%" [attr.stop-color]="goldColor"/>
        </linearGradient>
        <linearGradient id="cornLeafGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" [attr.stop-color]="leafLightColor"/>
          <stop offset="100%" [attr.stop-color]="leafDarkColor"/>
        </linearGradient>
      </defs>

      <!-- Feuille gauche -->
      <path
        d="M 32 78 Q 14 70 6 50 Q 14 56 22 60 Q 28 64 32 70 Z"
        fill="url(#cornLeafGrad)"
        opacity="0.85"
      />

      <!-- Feuille droite -->
      <path
        d="M 32 78 Q 50 70 58 50 Q 50 56 42 60 Q 36 64 32 70 Z"
        fill="url(#cornLeafGrad)"
        opacity="0.85"
      />

      <!-- Épi central : silhouette ovale -->
      <ellipse
        cx="32"
        cy="44"
        rx="15"
        ry="32"
        fill="url(#cornKernelGrad)"
      />

      <!-- Grains (5 rangs × 6 par rang en quinconce) -->
      <g [attr.fill]="kernelHighlightColor" opacity="0.55">
        <!-- rang 1 -->
        <ellipse cx="24" cy="18" rx="3" ry="3.2"/>
        <ellipse cx="32" cy="16" rx="3" ry="3.2"/>
        <ellipse cx="40" cy="18" rx="3" ry="3.2"/>
        <!-- rang 2 -->
        <ellipse cx="20" cy="26" rx="3.2" ry="3.4"/>
        <ellipse cx="28" cy="24" rx="3.2" ry="3.4"/>
        <ellipse cx="36" cy="24" rx="3.2" ry="3.4"/>
        <ellipse cx="44" cy="26" rx="3.2" ry="3.4"/>
        <!-- rang 3 (centre) -->
        <ellipse cx="18" cy="36" rx="3.4" ry="3.6"/>
        <ellipse cx="26" cy="34" rx="3.4" ry="3.6"/>
        <ellipse cx="34" cy="34" rx="3.4" ry="3.6"/>
        <ellipse cx="42" cy="36" rx="3.4" ry="3.6"/>
        <!-- rang 4 -->
        <ellipse cx="20" cy="46" rx="3.4" ry="3.6"/>
        <ellipse cx="28" cy="44" rx="3.4" ry="3.6"/>
        <ellipse cx="36" cy="44" rx="3.4" ry="3.6"/>
        <ellipse cx="44" cy="46" rx="3.4" ry="3.6"/>
        <!-- rang 5 -->
        <ellipse cx="22" cy="56" rx="3.2" ry="3.4"/>
        <ellipse cx="30" cy="54" rx="3.2" ry="3.4"/>
        <ellipse cx="38" cy="54" rx="3.2" ry="3.4"/>
        <!-- rang 6 (pointe) -->
        <ellipse cx="26" cy="66" rx="2.6" ry="2.8"/>
        <ellipse cx="34" cy="64" rx="2.6" ry="2.8"/>
      </g>

      <!-- Soies (filaments du haut) -->
      <g [attr.stroke]="lightColor" stroke-width="1.2" stroke-linecap="round" fill="none" opacity="0.7">
        <path d="M 28 14 Q 24 6 22 4"/>
        <path d="M 32 13 Q 32 5 32 2"/>
        <path d="M 36 14 Q 40 6 42 4"/>
        <path d="M 30 14 Q 28 7 26 5"/>
        <path d="M 34 14 Q 36 7 38 5"/>
      </g>
    </svg>
  `,
  styles: [`
    :host {
      display: inline-flex;
      align-items: center;
      line-height: 0;
    }
    svg { display: block; }
  `]
})
export class CornLogoComponent {
  @Input() size: number | string = 28;
  @Input() ariaLabel = 'Joseph Yusuf';
  @Input() goldColor = '#9D8235';
  @Input() lightColor = '#E8C876';
  @Input() kernelHighlightColor = '#FBE9B4';
  @Input() leafLightColor = '#7A6428';
  @Input() leafDarkColor = '#4A3D18';
}
