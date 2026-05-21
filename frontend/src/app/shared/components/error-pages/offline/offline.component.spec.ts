import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { OfflineComponent } from './offline.component';

describe('OfflineComponent', () => {
  let fixture: ComponentFixture<OfflineComponent>;
  let component: OfflineComponent;
  let router: Router;
  let onlineSpy: jasmine.Spy;

  beforeEach(async () => {
    onlineSpy = spyOnProperty(navigator, 'onLine', 'get').and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [OfflineComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(OfflineComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('affiche le titre "Pas de connexion internet"', () => {
    fixture.detectChanges();
    const title = fixture.nativeElement.querySelector('.error-title');
    expect(title.textContent).toContain('Pas de connexion');
  });

  it('retry() navigue vers /dashboard si online', () => {
    fixture.detectChanges();
    component.retry();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('retry() ne navigue pas si offline', () => {
    onlineSpy.and.returnValue(false);
    fixture.detectChanges();
    component.retry();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it("réagit à l'événement window 'online' en redirigeant", () => {
    fixture.detectChanges();
    window.dispatchEvent(new Event('online'));
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it("ngOnDestroy retire l'écouteur online", () => {
    fixture.detectChanges();
    const removeSpy = spyOn(window, 'removeEventListener').and.callThrough();
    component.ngOnDestroy();
    expect(removeSpy).toHaveBeenCalledWith('online', jasmine.any(Function));
  });
});
