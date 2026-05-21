import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { MaintenanceComponent } from './maintenance.component';

describe('MaintenanceComponent', () => {
  let fixture: ComponentFixture<MaintenanceComponent>;
  let component: MaintenanceComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MaintenanceComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(MaintenanceComponent);
    component = fixture.componentInstance;
  });

  it('initialise countdown à 60', () => {
    fixture.detectChanges();
    expect(component.countdown).toBe(60);
  });

  it('affiche le titre Maintenance et la citation Joseph', () => {
    fixture.detectChanges();
    const title = fixture.nativeElement.querySelector('.error-title');
    expect(title.textContent).toContain('Maintenance');
    const quote = fixture.nativeElement.querySelector('.error-quote');
    expect(quote.textContent).toContain('Joseph');
  });

  it('décrémente le countdown chaque seconde', fakeAsync(() => {
    fixture.detectChanges();
    tick(5000);
    expect(component.countdown).toBe(55);
    component.ngOnDestroy();
    discardPeriodicTasks();
  }));

  it('ngOnDestroy nettoie le timer', fakeAsync(() => {
    fixture.detectChanges();
    component.ngOnDestroy();
    tick(5000);
    expect(component.countdown).toBe(60);
  }));
});
