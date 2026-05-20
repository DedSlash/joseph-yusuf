import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { NotificationBellComponent } from './notification-bell.component';
import { AlertService } from '../../../core/services/alert.service';
import { AlertDto } from '../../models/alert.model';

describe('NotificationBellComponent', () => {
  let component: NotificationBellComponent;
  let alertSpy: jasmine.SpyObj<AlertService> & { unreadCount$: any };

  const alerts: AlertDto[] = [
    { id: 'a1', userId: 'u', type: 'RULE_APPLIED', severity: 'INFO', title: 'T1', message: 'Règle RULE_50_30_20 appliquée', read: false, createdAt: new Date().toISOString() },
    { id: 'a2', userId: 'u', type: 'INFO', severity: 'SUCCESS', title: 'T2', message: 'ok', read: true, createdAt: new Date().toISOString() }
  ];

  beforeEach(async () => {
    alertSpy = jasmine.createSpyObj<AlertService>('AlertService', [
      'startPolling', 'stopPolling', 'list', 'markAsRead', 'markAllAsRead', 'delete', 'deleteAll'
    ]) as any;
    alertSpy.unreadCount$ = new BehaviorSubject<number>(2).asObservable();
    alertSpy.list.and.returnValue(of(alerts));
    alertSpy.markAllAsRead.and.returnValue(of(void 0));
    alertSpy.delete.and.returnValue(of(void 0));
    alertSpy.deleteAll.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [NotificationBellComponent],
      providers: [
        { provide: AlertService, useValue: alertSpy }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(NotificationBellComponent);
    component = fixture.componentInstance;
  });

  it('se crée correctement', () => {
    expect(component).toBeTruthy();
  });

  it('confirmDeleteAll() puis deleteAll() appelle alertService.deleteAll et vide la liste', () => {
    component.alerts = [...alerts];
    component.confirmDeleteAll();
    expect(component.showDeleteConfirm).toBe(true);

    component.deleteAll();

    expect(alertSpy.deleteAll).toHaveBeenCalled();
    expect(component.alerts.length).toBe(0);
    expect(component.showDeleteConfirm).toBe(false);
  });

  it('formatAlertMessage remplace RULE_50_30_20 par 50/30/20', () => {
    expect(component.formatAlertMessage('Règle RULE_50_30_20 appliquée'))
      .toBe('Règle 50/30/20 appliquée');
  });

  it('formatAlertMessage remplace RULE_JOSEPH par Joseph', () => {
    expect(component.formatAlertMessage('Maintenant: RULE_JOSEPH'))
      .toBe('Maintenant: Joseph');
  });

  it('formatAlertMessage retourne chaîne vide si message vide', () => {
    expect(component.formatAlertMessage('')).toBe('');
  });

  it('severityClass formatte la classe CSS attendue', () => {
    expect(component.severityClass('INFO')).toBe('sev-info');
    expect(component.severityClass('DANGER')).toBe('sev-danger');
  });

  it('hasUnread() retourne true si au moins une alerte non lue', () => {
    component.alerts = alerts;
    expect(component.hasUnread()).toBe(true);
  });

  it('toggleDrawer() ouvre le drawer et charge les alertes', () => {
    component.drawerOpen = false;
    component.toggleDrawer();
    expect(component.drawerOpen).toBe(true);
    expect(alertSpy.list).toHaveBeenCalledWith(false);
    expect(component.alerts.length).toBe(2);
  });

  it('markAllAsRead met à jour les alertes localement', () => {
    component.alerts = [...alerts];
    component.markAllAsRead();
    expect(component.alerts.every(a => a.read)).toBe(true);
  });
});
