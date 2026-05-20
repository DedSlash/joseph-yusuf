import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { EMPTY, of, Subject } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { IncomeService } from '../../core/services/income.service';
import { RuleService } from '../../core/services/rule.service';
import { AuthService } from '../../core/auth/auth.service';
import { ReportService } from '../../core/services/report.service';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let incomeSpy: jasmine.SpyObj<IncomeService> & { incomeUpdated$: any };
  let ruleSpy: jasmine.SpyObj<RuleService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let reportSpy: jasmine.SpyObj<ReportService>;

  beforeEach(async () => {
    incomeSpy = jasmine.createSpyObj<IncomeService>('IncomeService', [
      'getSummary', 'getHistory', 'getMoneyTips', 'notifyIncomeUpdated'
    ]) as any;
    incomeSpy.getSummary.and.returnValue(EMPTY);
    incomeSpy.getHistory.and.returnValue(of([]));
    incomeSpy.getMoneyTips.and.returnValue(EMPTY);
    incomeSpy.incomeUpdated$ = new Subject<void>().asObservable();

    ruleSpy = jasmine.createSpyObj<RuleService>('RuleService', [
      'getAvailableRules', 'getConfig', 'calculate', 'updateConfig'
    ]);
    ruleSpy.getAvailableRules.and.returnValue(of([]));
    ruleSpy.getConfig.and.returnValue(EMPTY);
    ruleSpy.calculate.and.returnValue(EMPTY);

    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getPlan']);
    authSpy.getPlan.and.returnValue('FREE');

    reportSpy = jasmine.createSpyObj<ReportService>('ReportService', ['generateMonthly', 'generateAnnual', 'download']);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideAnimations(),
        { provide: IncomeService, useValue: incomeSpy },
        { provide: RuleService, useValue: ruleSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: ReportService, useValue: reportSpy }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('se crée correctement', () => {
    expect(component).toBeTruthy();
  });

  it('openMoneyTips() affiche la modale tips', () => {
    expect(component.showTipsModal).toBe(false);
    component.openMoneyTips();
    expect(component.showTipsModal).toBe(true);
  });

  it('getRuleLabel mappe les codes vers les libellés humains', () => {
    expect(component.getRuleLabel('RULE_50_30_20')).toBe('50 / 30 / 20');
    expect(component.getRuleLabel('RULE_JOSEPH')).toBe('Principe de Joseph');
    expect(component.getRuleLabel('UNKNOWN')).toBe('UNKNOWN');
  });

  it('isPremium() reflète le plan utilisateur', () => {
    authSpy.getPlan.and.returnValue('PREMIUM');
    expect(component.isPremium()).toBe(true);
    authSpy.getPlan.and.returnValue('FREE');
    expect(component.isPremium()).toBe(false);
  });
});
