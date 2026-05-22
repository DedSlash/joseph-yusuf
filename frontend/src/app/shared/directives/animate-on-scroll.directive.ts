import { Directive, ElementRef, Input, OnDestroy, OnInit, Renderer2 } from '@angular/core';

@Directive({
  selector: '[appAnimateOnScroll]',
  standalone: true
})
export class AnimateOnScrollDirective implements OnInit, OnDestroy {
  @Input() animationDelay = 0;
  @Input() rootMargin = '0px 0px -10% 0px';
  @Input() threshold = 0.15;
  @Input() animationClass = 'animated';
  @Input() once = true;

  private observer: IntersectionObserver | null = null;

  constructor(private readonly el: ElementRef<HTMLElement>, private readonly renderer: Renderer2) {}

  ngOnInit(): void {
    const node = this.el.nativeElement;
    this.renderer.addClass(node, 'animate-on-scroll');
    if (this.animationDelay > 0) {
      this.renderer.setStyle(node, 'transition-delay', `${this.animationDelay}ms`);
    }

    if (typeof IntersectionObserver === 'undefined') {
      this.renderer.addClass(node, this.animationClass);
      return;
    }

    this.observer = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          this.renderer.addClass(node, this.animationClass);
          if (this.once && this.observer) {
            this.observer.unobserve(node);
          }
        } else if (!this.once) {
          this.renderer.removeClass(node, this.animationClass);
        }
      }
    }, { rootMargin: this.rootMargin, threshold: this.threshold });

    this.observer.observe(node);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    this.observer = null;
  }
}
