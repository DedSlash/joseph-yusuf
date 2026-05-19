import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, EMPTY } from 'rxjs';
import { SupportComponent } from './support.component';
import { SupportService } from '../../core/services/support.service';
import { PageResponse, Ticket } from '../../shared/models/support.model';

describe('SupportComponent', () => {
  let component: SupportComponent;
  let supportSpy: jasmine.SpyObj<SupportService>;

  const mockPage: PageResponse<Ticket> = {
    content: [
      {
        id: '1', userId: 'u1', subject: 'Test', message: 'msg',
        category: 'ACCOUNT', status: 'OPEN', priority: 'NORMAL',
        aiHandled: false, createdAt: '2026-01-01', updatedAt: '2026-01-01'
      }
    ],
    page: 0, size: 20, totalElements: 1, totalPages: 1
  };

  beforeEach(async () => {
    supportSpy = jasmine.createSpyObj<SupportService>('SupportService', [
      'listMyTickets', 'createTicket', 'getTicket', 'addResponse',
      'searchKnowledge', 'listByCategory', 'listPublic', 'getArticle'
    ]);
    supportSpy.listMyTickets.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [SupportComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: SupportService, useValue: supportSpy }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(SupportComponent);
    component = fixture.componentInstance;
  });

  it('se crée correctement', () => {
    expect(component).toBeTruthy();
  });

  it('charge les tickets au ngOnInit', () => {
    component.ngOnInit();
    expect(supportSpy.listMyTickets).toHaveBeenCalledWith(0, 20);
    expect(component.tickets().length).toBe(1);
    expect(component.loading()).toBe(false);
  });

  it('filtre les tickets par statut', () => {
    component.ngOnInit();
    component.filterBy('OPEN');
    expect(component.activeFilter()).toBe('OPEN');
    expect(component.filteredTickets().length).toBe(1);
  });

  it('countByStatus retourne le bon nombre', () => {
    component.ngOnInit();
    expect(component.countByStatus('OPEN')).toBe(1);
    expect(component.countByStatus('CLOSED')).toBe(0);
  });

  it('getProgress retourne le bon pourcentage', () => {
    expect(component.getProgress({ status: 'OPEN' } as Ticket)).toBe('10%');
    expect(component.getProgress({ status: 'IN_PROGRESS' } as Ticket)).toBe('50%');
    expect(component.getProgress({ status: 'RESOLVED' } as Ticket)).toBe('90%');
    expect(component.getProgress({ status: 'CLOSED' } as Ticket)).toBe('100%');
  });
});
