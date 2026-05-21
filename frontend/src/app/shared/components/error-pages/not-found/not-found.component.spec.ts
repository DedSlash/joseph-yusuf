import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NotFoundComponent } from './not-found.component';

describe('NotFoundComponent', () => {
  let fixture: ComponentFixture<NotFoundComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotFoundComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(NotFoundComponent);
    fixture.detectChanges();
  });

  it('se crée correctement', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('affiche le titre "Page introuvable"', () => {
    const title = fixture.nativeElement.querySelector('.error-title');
    expect(title.textContent).toContain('Page introuvable');
  });

  it('contient un bouton vers /dashboard', () => {
    const link: HTMLAnchorElement = fixture.nativeElement.querySelector('a.btn-primary');
    expect(link).toBeTruthy();
    expect(link.getAttribute('href')).toBe('/dashboard');
    expect(link.textContent).toContain('Dashboard');
  });

  it('contient un lien vers /support', () => {
    const links: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('a'));
    const supportLink = links.find(a => a.getAttribute('href') === '/support');
    expect(supportLink).toBeTruthy();
  });
});
