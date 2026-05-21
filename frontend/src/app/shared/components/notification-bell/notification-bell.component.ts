import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { AlertService } from '../../../core/services/alert.service';
import { AlertDto, AlertSeverity } from '../../models/alert.model';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bell-wrapper">
      <button class="bell-btn" (click)="toggleDrawer()" aria-label="Notifications">
        <span class="bell-icon" [class.has-unread]="unreadCount > 0">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="17" height="17" aria-hidden="true">
            <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6V11c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/>
          </svg>
        </span>
        <span *ngIf="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </button>

      <div *ngIf="drawerOpen" class="drawer">
        <div class="drawer-header">
          <h3>Notifications</h3>
          <div class="header-actions">
            <button *ngIf="alerts.length > 0 && hasUnread()" class="link-btn" (click)="markAllAsRead()">
              Tout marquer comme lu
            </button>
            <button *ngIf="alerts.length > 0" class="link-btn delete-all-btn" (click)="confirmDeleteAll()">
              🗑️ Tout supprimer
            </button>
          </div>
        </div>

        <!-- Confirmation suppression -->
        <div *ngIf="showDeleteConfirm" class="confirm-bar">
          <span>Supprimer toutes les notifications ?</span>
          <div class="confirm-actions">
            <button class="confirm-yes" (click)="deleteAll()">Oui</button>
            <button class="confirm-no" (click)="showDeleteConfirm = false">Non</button>
          </div>
        </div>

        <div class="drawer-body">
          <div *ngIf="loading" class="empty">Chargement...</div>
          <div *ngIf="!loading && alerts.length === 0" class="empty">Aucune notification</div>

          <div
            *ngFor="let alert of alerts"
            class="alert-item"
            [class.unread]="!alert.read"
            [ngClass]="severityClass(alert.severity)"
          >
            <div class="alert-content" (click)="onAlertClick(alert)">
              <div class="alert-title">{{ alert.title }}</div>
              <div class="alert-message">{{ formatAlertMessage(alert.message) }}</div>
              <div class="alert-date">{{ formatDate(alert.createdAt) }}</div>
            </div>
            <button class="delete-btn" (click)="onDelete($event, alert)" aria-label="Supprimer">&times;</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .bell-wrapper {
      position: relative;
    }
    .bell-btn {
      position: relative;
      width: 38px; height: 38px;
      border-radius: 10px;
      display: grid; place-items: center;
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--line-soft);
      color: var(--text-1);
      cursor: pointer;
      transition: 0.15s;
    }
    .bell-btn:hover {
      background: rgba(255, 255, 255, 0.07);
      color: var(--text-0);
    }
    .bell-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-2);
      transition: color 0.2s;
    }
    .bell-icon.has-unread {
      color: var(--gold-light);
      animation: bell-shake 1.6s ease-in-out 1s 2;
    }
    @keyframes bell-shake {
      0%,100% { transform: rotate(0); }
      20% { transform: rotate(-12deg); }
      40% { transform: rotate(10deg); }
      60% { transform: rotate(-6deg); }
      80% { transform: rotate(4deg); }
    }
    .bell-btn:hover .bell-icon {
      color: var(--text-0);
    }
    .badge {
      position: absolute;
      top: 4px; right: 4px;
      min-width: 16px; height: 16px;
      padding: 0 4px;
      border-radius: 8px;
      background: var(--gold);
      color: #1b1500;
      font-size: 10px; font-weight: 700;
      display: grid; place-items: center;
      border: 2px solid var(--night-1);
    }
    .drawer {
      position: absolute;
      top: calc(100% + 10px);
      right: 0;
      width: 400px;
      max-height: 500px;
      background: linear-gradient(180deg, var(--night-2), var(--night-1));
      border: 1px solid var(--line);
      border-radius: 14px;
      box-shadow: -20px 0 60px -15px rgba(0, 0, 0, 0.5);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      z-index: 1100;
    }
    .drawer-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 18px 20px 14px;
      border-bottom: 1px solid var(--line-soft);
    }
    .drawer-header h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--text-0);
    }
    .header-actions {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .link-btn {
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--line-soft);
      border-radius: 8px;
      padding: 5px 10px;
      color: var(--text-1);
      cursor: pointer;
      font-size: 11px;
      font-weight: 500;
      transition: 0.15s;
    }
    .link-btn:hover {
      background: rgba(255, 255, 255, 0.08);
      color: var(--text-0);
    }
    .delete-all-btn { color: #ff7a6c; border-color: rgba(231, 76, 60, 0.2); }
    .delete-all-btn:hover { color: #ff9f93; background: rgba(231, 76, 60, 0.08); }
    .confirm-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 20px;
      background: rgba(231, 76, 60, 0.06);
      border-bottom: 1px solid rgba(231, 76, 60, 0.15);
      font-size: 12px;
      color: #ff7a6c;
    }
    .confirm-actions { display: flex; gap: 6px; }
    .confirm-yes {
      padding: 4px 10px;
      background: #E74C3C;
      color: #fff;
      border: none;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
      cursor: pointer;
    }
    .confirm-no {
      padding: 4px 10px;
      background: transparent;
      color: var(--text-2);
      border: 1px solid var(--line-soft);
      border-radius: 6px;
      font-size: 11px;
      cursor: pointer;
    }
    .drawer-body {
      overflow-y: auto;
      flex: 1;
      padding: 8px;
    }
    .empty {
      padding: 48px 20px;
      text-align: center;
      color: var(--text-2);
      font-size: 14px;
    }
    .alert-item {
      display: flex;
      gap: 12px;
      padding: 12px 14px;
      border-radius: 12px;
      margin: 2px 0;
      border-left: 3px solid transparent;
      transition: background 0.15s;
      cursor: default;
    }
    .alert-item:hover { background: rgba(255, 255, 255, 0.03); }
    .alert-item.unread { background: rgba(201, 168, 76, 0.05); }
    .alert-item.sev-success { border-left-color: #5cdb83; }
    .alert-item.sev-warning { border-left-color: #f5b041; }
    .alert-item.sev-danger  { border-left-color: #ff7a6c; }
    .alert-item.sev-info    { border-left-color: #7fc1ea; }
    .alert-content { flex: 1; cursor: pointer; min-width: 0; }
    .alert-title {
      font-size: 13.5px;
      font-weight: 600;
      color: var(--text-0);
      margin-bottom: 2px;
    }
    .alert-message {
      font-size: 13px;
      color: var(--text-2);
      line-height: 1.45;
    }
    .alert-date {
      font-size: 11px;
      color: var(--text-3);
      margin-top: 4px;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    .delete-btn {
      background: transparent;
      border: none;
      color: var(--text-3);
      cursor: pointer;
      font-size: 16px;
      padding: 0 4px;
      align-self: flex-start;
      border-radius: 6px;
      width: 28px; height: 28px;
      display: grid; place-items: center;
      transition: 0.15s;
    }
    .delete-btn:hover {
      color: #ff7a6c;
      background: rgba(231, 76, 60, 0.08);
    }
  `]
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  unreadCount = 0;
  alerts: AlertDto[] = [];
  drawerOpen = false;
  loading = false;
  showDeleteConfirm = false;

  private subs: Subscription[] = [];

  private static readonly RULE_MAP: Record<string, string> = {
    'RULE_50_30_20': '50/30/20',
    'RULE_70_20_10': '70/20/10',
    'RULE_80_20': '80/20',
    'RULE_JOSEPH': 'Joseph'
  };

  constructor(private readonly alertService: AlertService, private readonly elRef: ElementRef) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.drawerOpen && !this.elRef.nativeElement.contains(event.target)) {
      this.drawerOpen = false;
      this.showDeleteConfirm = false;
    }
  }

  ngOnInit(): void {
    this.alertService.startPolling();
    this.subs.push(
      this.alertService.unreadCount$.subscribe(count => (this.unreadCount = count))
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.alertService.stopPolling();
  }

  toggleDrawer(): void {
    this.drawerOpen = !this.drawerOpen;
    this.showDeleteConfirm = false;
    if (this.drawerOpen) {
      this.fetchAlerts();
    }
  }

  fetchAlerts(): void {
    this.loading = true;
    this.alertService.list(false).subscribe({
      next: alerts => {
        this.alerts = alerts;
        this.loading = false;
      },
      error: () => {
        this.alerts = [];
        this.loading = false;
      }
    });
  }

  hasUnread(): boolean {
    return this.alerts.some(a => !a.read);
  }

  onAlertClick(alert: AlertDto): void {
    if (!alert.read) {
      this.alertService.markAsRead(alert.id).subscribe(updated => {
        const idx = this.alerts.findIndex(a => a.id === alert.id);
        if (idx >= 0) {
          this.alerts[idx] = updated;
        }
      });
    }
  }

  markAllAsRead(): void {
    this.alertService.markAllAsRead().subscribe(() => {
      this.alerts = this.alerts.map(a => ({ ...a, read: true }));
    });
  }

  confirmDeleteAll(): void {
    this.showDeleteConfirm = true;
  }

  deleteAll(): void {
    this.alertService.deleteAll().subscribe(() => {
      this.alerts = [];
      this.showDeleteConfirm = false;
    });
  }

  onDelete(event: Event, alert: AlertDto): void {
    event.stopPropagation();
    this.alertService.delete(alert.id).subscribe(() => {
      this.alerts = this.alerts.filter(a => a.id !== alert.id);
    });
  }

  formatAlertMessage(message: string): string {
    if (!message) return '';
    return message.replace(/RULE_(\w+)/g, (match) => {
      return NotificationBellComponent.RULE_MAP[match] || match.replace('RULE_', '').replace(/_/g, '/');
    });
  }

  severityClass(severity: AlertSeverity): string {
    return `sev-${severity.toLowerCase()}`;
  }

  formatDate(iso: string): string {
    const d = new Date(iso);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMin = Math.floor(diffMs / 60_000);
    const diffH   = Math.floor(diffMs / 3_600_000);

    if (diffMin < 1)  return "À l'instant";
    if (diffMin < 60) return `Il y a ${diffMin} min`;
    if (diffH   < 24) return `Il y a ${diffH} h`;

    return d.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: d.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
