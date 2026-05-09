import { Component, OnDestroy, OnInit } from '@angular/core';
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
        <span class="bell-icon">&#128276;</span>
        <span *ngIf="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </button>

      <div *ngIf="drawerOpen" class="drawer">
        <div class="drawer-header">
          <h3>Notifications</h3>
          <button *ngIf="alerts.length > 0 && hasUnread()" class="link-btn" (click)="markAllAsRead()">
            Tout marquer comme lu
          </button>
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
              <div class="alert-message">{{ alert.message }}</div>
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
      filter: grayscale(0);
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
    .link-btn {
      background: none;
      border: none;
      color: #C9A84C;
      cursor: pointer;
      font-size: 0.75rem;
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

  private subs: Subscription[] = [];

  constructor(private alertService: AlertService) {}

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

  onDelete(event: Event, alert: AlertDto): void {
    event.stopPropagation();
    this.alertService.delete(alert.id).subscribe(() => {
      this.alerts = this.alerts.filter(a => a.id !== alert.id);
    });
  }

  severityClass(severity: AlertSeverity): string {
    return `sev-${severity.toLowerCase()}`;
  }

  formatDate(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
