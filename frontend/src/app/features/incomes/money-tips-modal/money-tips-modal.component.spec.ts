import { TestBed } from '@angular/core/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { BehaviorSubject } from 'rxjs';
import { MoneyTipsModalComponent } from './money-tips-modal.component';
import { MoneyTips } from '../../../shared/models/income.model';
import { CurrencyDisplayService } from '../../../core/services/currency-display.service';

describe('MoneyTipsModalComponent', () => {
  let component: MoneyTipsModalComponent;

  const sampleTips: MoneyTips = {
    josephStatus: 'NORMAL',
    totalAmount: 500_000,
    currency: 'XOF',
    country: 'SN',
    recommendedSavings: 100_000,
    recommendedSplit: { needs: 250_000, wants: 150_000, savings: 100_000 },
    tips: [
      { id: 't1', title: 'A', description: 'd1', icon: '💡', method: 'm', countries: ['SN'], requiredPlan: 'FREE', locked: false, actionUrl: null, actionLabel: null },
      { id: 't2', title: 'B', description: 'd2', icon: '🔒', method: 'm', countries: ['SN'], requiredPlan: 'PREMIUM', locked: true, actionUrl: null, actionLabel: null }
    ]
  };

  let currencyStub: Partial<CurrencyDisplayService>;

  beforeEach(async () => {
    currencyStub = {
      displayCurrency$: new BehaviorSubject<string>('XOF').asObservable(),
      displayCurrency: 'XOF',
      formatAmount: (xof: number | null | undefined) => `${xof ?? 0} XOF`,
      fromXof: (xof: number) => xof,
      loadRates: () => new BehaviorSubject<Record<string, number>>({ XOF: 1 }).asObservable() as any
    };

    await TestBed.configureTestingModule({
      imports: [MoneyTipsModalComponent],
      providers: [
        provideAnimations(),
        { provide: CurrencyDisplayService, useValue: currencyStub }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(MoneyTipsModalComponent);
    component = fixture.componentInstance;
    component.tips = sampleTips;
    component.monthLabel = 'Mai';
    component.visible = true;
  });

  it('se crée correctement', () => {
    expect(component).toBeTruthy();
  });

  it('onClose() émet visibleChange=false et passe visible à false', () => {
    let emitted: boolean | undefined;
    component.visibleChange.subscribe(v => emitted = v);

    component.onClose();

    expect(component.visible).toBe(false);
    expect(emitted).toBe(false);
  });

  it('onClose() émet dismissedForMonth si la case est cochée', () => {
    let dismissed = false;
    component.dismissedForMonth.subscribe(() => dismissed = true);

    component.dismissForMonth = true;
    component.onClose();

    expect(dismissed).toBe(true);
  });

  it('switchLang("en") met à jour currentLang et émet langChanged', () => {
    let lang: string | undefined;
    component.langChanged.subscribe(l => lang = l);

    component.switchLang('en');

    expect(component.currentLang).toBe('en');
    expect(lang).toBe('en');
  });

  it('switchLang ignore une langue identique', () => {
    component.currentLang = 'fr';
    let emitted = false;
    component.langChanged.subscribe(() => emitted = true);

    component.switchLang('fr');

    expect(emitted).toBe(false);
  });

  it('onUnlock() émet unlockRequested', () => {
    let emitted = false;
    component.unlockRequested.subscribe(() => emitted = true);

    component.onUnlock();

    expect(emitted).toBe(true);
  });

  it('accessibleTips() filtre les tips verrouillés', () => {
    expect(component.accessibleTips().length).toBe(1);
    expect(component.accessibleTips()[0].id).toBe('t1');
  });

  it('lockedCount() compte les tips verrouillés', () => {
    expect(component.lockedCount()).toBe(1);
  });

  it('formatAmount() délègue à CurrencyDisplayService (devise d\'affichage)', () => {
    expect(component.formatAmount(500_000)).toBe('500000 XOF');
  });

  it('headerTitle() utilise la devise d\'affichage et n\'appose plus tips.currency', () => {
    component.currentLang = 'fr';
    const title = component.headerTitle();
    expect(title).toContain('Revenu de Mai');
    expect(title).toContain('500000 XOF');
    expect(title.match(/XOF/g)?.length).toBe(1);
  });

  it('resolveDescription() substitue {recommendedSavings} {currency} via la devise d\'affichage', () => {
    const out = component.resolveDescription(
      'Montant recommandé ce mois : {recommendedSavings} {currency}.'
    );
    expect(out).toBe('Montant recommandé ce mois : 100000 XOF.');
  });

  it('resolveDescription() ne touche pas une description sans placeholder', () => {
    expect(component.resolveDescription('Texte simple sans placeholder.')).toBe('Texte simple sans placeholder.');
  });
});
