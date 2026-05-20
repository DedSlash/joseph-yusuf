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
      background: transparent;
      border: 1px solid rgba(201, 168, 76, 0.3);
      border-radius: 50%;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: #C9A84C;
      font-size: 1rem;
      transition: background 0.2s;
    }
    .bell-btn:hover {
      background: rgba(201, 168, 76, 0.1);
    }
    .bell-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      color: rgba(201, 168, 76, 0.55);
      transition: color 0.2s;
    }
    .bell-icon.has-unread {
      color: #C9A84C;
      animation: bell-ring 0.6s ease-in-out;
    }
    @keyframes bell-ring {
      0%   { transform: rotate(0); }
      15%  { transform: rotate(10deg); }
      30%  { transform: rotate(-10deg); }
      45%  { transform: rotate(6deg); }
      60%  { transform: rotate(-6deg); }
      75%  { transform: rotate(3deg); }
      100% { transform: rotate(0); }
    }
    .bell-btn:hover .bell-icon {
      color: #C9A84C;
    }
    .badge {
      position: absolute;
      top: -4px;
      right: -4px;
      background: #C9A84C;
      color: #0D0B07;
      font-size: 0.65rem;
      font-weight: 700;
      min-width: 18px;
      height: 18px;
      border-radius: 9px;
      padding: 0 5px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .drawer {
      position: absolute;
      top: calc(100% + 10px);
      right: 0;
      width: 380px;
      max-height: 480px;
      background: #1A1710;
      border: 1px solid rgba(201, 168, 76, 0.2);
      border-radius: 10px;
      box-shadow: 0 12px 36px rgba(0, 0, 0, 0.5);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      z-index: 1100;
    }
    .drawer-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem 1rem;
      border-bottom: 1px solid rgba(201, 168, 76, 0.15);
    }
    .drawer-header h3 {
      margin: 0;
      font-size: 0.95rem;
      color: #F0E8D0;
    }
    .header-actions {
      display: flex;
      gap: 0.6rem;
      align-items: center;
    }
    .link-btn {
      background: none;
      border: none;
      color: #C9A84C;
      cursor: pointer;
      font-size: 0.75rem;
    }
    .delete-all-btn {
      color: #f87171;
    }
    .delete-all-btn:hover {
      color: #fca5a5;
    }
    .confirm-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.5rem 1rem;
      background: rgba(248, 113, 113, 0.08);
      border-bottom: 1px solid rgba(248, 113, 113, 0.2);
      font-size: 0.78rem;
      color: #fca5a5;
    }
    .confirm-actions {
      display: flex;
      gap: 0.4rem;
    }
    .confirm-yes {
      padding: 0.2rem 0.6rem;
      background: #f87171;
      color: #0D0B07;
      border: none;
      border-radius: 4px;
      font-size: 0.72rem;
      font-weight: 700;
      cursor: pointer;
    }
    .confirm-no {
      padding: 0.2rem 0.6rem;
      background: transparent;
      color: rgba(240, 232, 208, 0.6);
      border: 1px solid rgba(240, 232, 208, 0.2);
      border-radius: 4px;
      font-size: 0.72rem;
      cursor: pointer;
    }
    .drawer-body {
      overflow-y: auto;
      flex: 1;
    }
    .empty {
      padding: 2rem 1rem;
      text-align: center;
      color: rgba(240, 232, 208, 0.5);
      font-size: 0.85rem;
    }
    .alert-item {
      display: flex;
      gap: 0.5rem;
      padding: 0.75rem 1rem;
      border-bottom: 1px solid rgba(201, 168, 76, 0.08);
      border-left: 3px solid transparent;
      transition: background 0.2s;
    }
    .alert-item.unread {
      background: rgba(201, 168, 76, 0.04);
    }
    .alert-item.sev-success { border-left-color: #4ade80; }
    .alert-item.sev-warning { border-left-color: #fbbf24; }
    .alert-item.sev-danger  { border-left-color: #f87171; }
    .alert-item.sev-info    { border-left-color: #60a5fa; }
    .alert-content {
      flex: 1;
      cursor: pointer;
    }
    .alert-title {
      font-size: 0.85rem;
      font-weight: 600;
      color: #F0E8D0;
      margin-bottom: 0.2rem;
    }
    .alert-message {
      font-size: 0.78rem;
      color: rgba(240, 232, 208, 0.75);
      line-height: 1.35;
    }
    .alert-date {
      font-size: 0.7rem;
      color: rgba(240, 232, 208, 0.45);
      margin-top: 0.3rem;
    }
    .delete-btn {
      background: transparent;
      border: none;
      color: rgba(240, 232, 208, 0.4);
      cursor: pointer;
      font-size: 1.1rem;
      padding: 0 0.25rem;
      align-self: flex-start;
    }
    .delete-btn:hover {
      color: #f87171;
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
