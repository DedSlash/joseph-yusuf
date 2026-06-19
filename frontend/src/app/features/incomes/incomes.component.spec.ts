import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { EMPTY, of, Subject } from 'rxjs';
import { IncomesComponent } from './incomes.component';
import { IncomeService } from '../../core/services/income.service';
import { AuthService } from '../../core/auth/auth.service';

describe('IncomesComponent', () => {
  let component: IncomesComponent;
  let incomeSpy: jasmine.SpyObj<IncomeService> & { incomeUpdated$: any };
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    incomeSpy = jasmine.createSpyObj<IncomeService>('IncomeService', [
      'getSources', 'getEntries', 'getSummary', 'getHistory',
      'createSource', 'updateSource', 'deleteSource',
      'createEntry', 'updateEntry', 'getAllEntriesForSource',
      'getMoneyTips', 'notifyIncomeUpdated'
    ]) as any;
    incomeSpy.getSources.and.returnValue(EMPTY);
    incomeSpy.getEntries.and.returnValue(of([]));
    incomeSpy.getSummary.and.returnValue(EMPTY);
    incomeSpy.getHistory.and.returnValue(of([]));
    incomeSpy.getMoneyTips.and.returnValue(EMPTY);
    incomeSpy.incomeUpdated$ = new Subject<void>().asObservable();

    authSpy = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['getPlan', 'isLoggedIn', 'getCurrentUser'],
      { currentUser$: of(null) }
    );
    authSpy.getPlan.and.returnValue('FREE');
    authSpy.isLoggedIn.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [IncomesComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideAnimations(),
        { provide: IncomeService, useValue: incomeSpy },
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(IncomesComponent);
    component = fixture.componentInstance;
  });

  it('se crée correctement', () => {
    expect(component).toBeTruthy();
  });

  describe('extractAmount', () => {
    it('parse les nombres au format français (espace + virgule)', () => {
      const c = component as any;
      expect(c.extractAmount({ Montant: '1 234,56' })).toBeCloseTo(1234.56);
    });

    it('accepte différents alias de colonnes', () => {
      const c = component as any;
      expect(c.extractAmount({ amount: '500' })).toBe(500);
      expect(c.extractAmount({ Amount: '750.25' })).toBeCloseTo(750.25);
      expect(c.extractAmount({ montant: '42' })).toBe(42);
    });

    it('retourne 0 si aucun montant', () => {
      const c = component as any;
      expect(c.extractAmount({})).toBe(0);
      expect(c.extractAmount({ Montant: 'abc' })).toBe(0);
    });
  });

  describe('parseDateValue', () => {
    it('parse une date ISO YYYY-MM-DD', () => {
      const c = component as any;
      expect(c.parseDateValue('2026-05-17')).toEqual({ year: 2026, month: 5 });
    });

    it('parse une date FR DD/MM/YYYY avec / ou -', () => {
      const c = component as any;
      expect(c.parseDateValue('03/11/2025')).toEqual({ month: 3, year: 2025 });
      expect(c.parseDateValue('03-11-2025')).toEqual({ month: 3, year: 2025 });
    });

    it('convertit un serial Excel en mois/année', () => {
      const c = component as any;
      const result = c.parseDateValue(44927); // Excel serial ~2023-01-01
      expect(result.year).toBe(2023);
      expect(result.month).toBe(1);
    });

    it('retourne {0,0} pour une chaîne non reconnue', () => {
      const c = component as any;
      expect(c.parseDateValue('not-a-date')).toEqual({ month: 0, year: 0 });
    });
  });

  describe('extractMonthYear', () => {
    it('utilise Date si présente', () => {
      const c = component as any;
      expect(c.extractMonthYear({ Date: '2025-07-15' })).toEqual({ year: 2025, month: 7 });
    });

    it('utilise Mois + Année si Date absente', () => {
      const c = component as any;
      expect(c.extractMonthYear({ Mois: '4', Année: '2024' })).toEqual({ month: 4, year: 2024 });
    });

    it('retourne {0,0} sinon', () => {
      const c = component as any;
      expect(c.extractMonthYear({})).toEqual({ month: 0, year: 0 });
    });
  });

  describe('extractJsonEntries', () => {
    it('retourne data.entries si présent', () => {
      const c = component as any;
      expect(c.extractJsonEntries({ entries: [{ a: 1 }] })).toEqual([{ a: 1 }]);
    });

    it('retourne data si tableau direct', () => {
      const c = component as any;
      expect(c.extractJsonEntries([{ b: 2 }])).toEqual([{ b: 2 }]);
    });

    it('retourne [] sinon (null, object vide)', () => {
      const c = component as any;
      expect(c.extractJsonEntries(null)).toEqual([]);
      expect(c.extractJsonEntries({})).toEqual([]);
    });
  });

  describe('markRowValidity', () => {
    it('flag invalide si année hors plage', () => {
      const c = component as any;
      const row: any = { month: 5, year: 1990, amount: 100, source: 's', note: '', status: 'new', statusLabel: '', valid: true };
      c.markRowValidity(row, 2026);
      expect(row.valid).toBe(false);
      expect(row.status).toBe('invalid');
      expect(row.statusLabel).toContain('année invalide');
    });

    it('flag invalide si mois hors plage', () => {
      const c = component as any;
      const row: any = { month: 13, year: 2025, amount: 100, source: 's', note: '', status: 'new', statusLabel: '', valid: true };
      c.markRowValidity(row, 2026);
      expect(row.valid).toBe(false);
      expect(row.statusLabel).toContain('mois invalide');
    });

    it('flag invalide si montant nul ou négatif', () => {
      const c = component as any;
      const row: any = { month: 5, year: 2025, amount: 0, source: 's', note: '', status: 'new', statusLabel: '', valid: true };
      c.markRowValidity(row, 2026);
      expect(row.valid).toBe(false);
      expect(row.statusLabel).toContain('montant invalide');
    });

    it('garde valide une ligne correcte', () => {
      const c = component as any;
      const row: any = { month: 5, year: 2025, amount: 100, source: 's', note: '', status: 'new', statusLabel: 'Nouveau', valid: true };
      c.markRowValidity(row, 2026);
      expect(row.valid).toBe(true);
      expect(row.status).toBe('new');
    });
  });

  describe('collectNewSources', () => {
    it('détecte les sources absentes de la liste existante', () => {
      component.sources = [{ id: '1', name: 'Salaire', active: true } as any];
      const c = component as any;
      const rows = [
        { source: 'Freelance', valid: true } as any,
        { source: 'Salaire', valid: true } as any,
        { source: '', valid: true } as any,
        { source: 'Bonus', valid: false } as any
      ];
      expect(c.collectNewSources(rows)).toEqual(['Freelance']);
    });

    it('retourne [] si toutes existent', () => {
      component.sources = [{ id: '1', name: 'Salaire', active: true } as any];
      const c = component as any;
      expect(c.collectNewSources([{ source: 'Salaire', valid: true } as any])).toEqual([]);
    });
  });

  describe('parseCsvImport', () => {
    it('strip les guillemets autour des headers et cellules', () => {
      const c = component as any;
      const text = '"Source";"Montant";"Date"\n"Freelance";"1000";"2025-05-01"';
      c.parseCsvImport(text);
      expect(component.importPreviewRows.length).toBe(1);
      expect(component.importPreviewRows[0].source).toBe('Freelance');
      expect(component.importPreviewRows[0].amount).toBe(1000);
      expect(component.importPreviewRows[0].month).toBe(5);
      expect(component.importPreviewRows[0].year).toBe(2025);
    });

    it('détecte automatiquement le séparateur virgule', () => {
      const c = component as any;
      const text = 'Source,Montant,Date\nSalaire,2500,2025-06-01';
      c.parseCsvImport(text);
      expect(component.importPreviewRows.length).toBe(1);
      expect(component.importPreviewRows[0].source).toBe('Salaire');
    });
  });

  describe('checkTipsAvailability', () => {
    it('met hasTipsForCurrentMonth à false sans entries saisies', () => {
      component.entryForms = [
        { sourceId: 's1', sourceName: 'A', currency: 'XOF', amount: 0, entryId: null }
      ];
      (component as any).checkTipsAvailability();
      expect(component.hasTipsForCurrentMonth).toBe(false);
      expect(incomeSpy.getMoneyTips).not.toHaveBeenCalled();
    });

    it('met hasTipsForCurrentMonth à true si des tips sont retournés', () => {
      component.entryForms = [
        { sourceId: 's1', sourceName: 'A', currency: 'XOF', amount: 500, entryId: 'e1' }
      ];
      incomeSpy.getMoneyTips.and.returnValue(of({
        month: 5, year: 2026, currency: 'XOF', totalAmount: 500,
        josephStatus: 'NORMAL', tips: [{ id: 't1', title: 'x', description: 'y', icon: '', locked: false }],
        recommendedSplit: { needs: 250, wants: 150, savings: 100 }
      } as any));

      (component as any).checkTipsAvailability();

      expect(incomeSpy.getMoneyTips).toHaveBeenCalled();
      expect(component.hasTipsForCurrentMonth).toBe(true);
      expect(component.moneyTips).toBeTruthy();
    });

    it('showMoneyTipsModal() ouvre la modale tips', () => {
      expect(component.showTipsModal).toBe(false);
      component.showMoneyTipsModal();
      expect(component.showTipsModal).toBe(true);
    });
  });
});
