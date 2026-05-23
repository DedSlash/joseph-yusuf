import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-corn-logo',
  standalone: true,
  imports: [CommonModule],
  template: `<span [style.font-size.px]="size" [attr.aria-label]="ariaLabel" role="img">🌾</span>`,
  styles: [`
    :host {
      display: inline-flex;
      align-items: center;
      line-height: 1;
    }
    span { display: block; line-height: 1; }
  `]
})
export class CornLogoComponent {
  @Input() size: number | string = 28;
  @Input() ariaLabel = 'Joseph Yusuf';
}
