import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { ServerErrorComponent } from './server-error.component';

describe('ServerErrorComponent', () => {
  let fixture: ComponentFixture<ServerErrorComponent>;
  let component: ServerErrorComponent;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServerErrorComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(ServerErrorComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('initialise countdown à 30', () => {
    fixture.detectChanges();
    expect(component.countdown).toBe(30);
  });

  it('affiche le titre et le bouton réessayer', () => {
    fixture.detectChanges();
    const title = fixture.nativeElement.querySelector('.error-title');
    expect(title.textContent).toContain("mal pass");
    const btn = fixture.nativeElement.querySelector('button.btn-primary');
    expect(btn).toBeTruthy();
  });

  it('retry() navigue vers /dashboard', () => {
    fixture.detectChanges();
    component.retry();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('décrémente le countdown chaque seconde', fakeAsync(() => {
    fixture.detectChanges();
    tick(3000);
    expect(component.countdown).toBe(27);
    component.ngOnDestroy();
    discardPeriodicTasks();
  }));

  it('redirige automatiquement quand countdown atteint 0', fakeAsync(() => {
    fixture.detectChanges();
    tick(30000);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    discardPeriodicTasks();
  }));

  it('ngOnDestroy nettoie le timer', fakeAsync(() => {
    fixture.detectChanges();
    component.ngOnDestroy();
    tick(5000);
    expect(component.countdown).toBe(30);
  }));
});
