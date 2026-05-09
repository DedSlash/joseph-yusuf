import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminApiService } from '../../core/services/admin-api.service';
import { Plan, Role, User } from '../../shared/models/admin.model';

type EnabledFilter = '' | 'true' | 'false';

@Component({
  selector: 'admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe],
  template: `
    <h1>Utilisateurs</h1>
    <p class="subtitle">Gestion des comptes, plans et accès</p>

    <div *ngIf="errorMessage()" class="alert error">{{ errorMessage() }}</div>
    <div *ngIf="successMessage()" class="alert success">{{ successMessage() }}</div>

    <div class="card">
      <div class="filters">
        <input type="text" class="input" placeholder="Rechercher par email…"
               [(ngModel)]="search" (keyup.enter)="reload()" style="max-width: 280px;" />

        <select class="select" [(ngModel)]="planFilter" (change)="reload()" style="max-width: 180px;">
          <option [ngValue]="''">Tous les plans</option>
          <option [ngValue]="'FREE'">FREE</option>
          <option [ngValue]="'PREMIUM'">PREMIUM</option>
          <option [ngValue]="'PREMIUM_PLUS'">PREMIUM_PLUS</option>
        </select>

        <select class="select" [(ngModel)]="enabledFilter" (change)="reload()" style="max-width: 180px;">
          <option [ngValue]="''">Tous les statuts</option>
          <option [ngValue]="'true'">Actifs</option>
          <option [ngValue]="'false'">Bloqués</option>
        </select>

        <button class="btn btn-ghost" (click)="reload()">Filtrer</button>
        <button class="btn btn-ghost" (click)="resetFilters()">Réinitialiser</button>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>Email</th>
            <th>Nom</th>
            <th>Plan</th>
            <th>Rôle</th>
            <th>Statut</th>
            <th>Inscription</th>
            <th style="text-align: right;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let u of users(); trackBy: trackById">
            <td><a [routerLink]="['/users', u.id]">{{ u.email }}</a></td>
            <td>{{ u.firstName }} {{ u.lastName }}</td>
            <td>
              <select class="select" style="padding: 0.3rem 0.5rem; font-size: 0.8rem;"
                      [ngModel]="u.plan" (ngModelChange)="changePlan(u, $event)"
                      [disabled]="busyId() === u.id">
                <option value="FREE">FREE</option>
                <option value="PREMIUM">PREMIUM</option>
                <option value="PREMIUM_PLUS">PREMIUM_PLUS</option>
              </select>
            </td>
            <td>
              <span class="badge" [class.admin]="u.role === 'ADMIN'" [class.user]="u.role === 'USER'">
                {{ u.role }}
              </span>
            </td>
            <td>
              <span class="badge" [class.success]="u.enabled" [class.disabled]="!u.enabled">
                {{ u.enabled ? 'Actif' : 'Bloqué' }}
              </span>
            </td>
            <td>{{ u.createdAt | date:'dd/MM/yyyy' }}</td>
            <td style="text-align: right;">
              <button class="btn btn-ghost mini" (click)="toggleBlock(u)" [disabled]="busyId() === u.id">
                {{ u.enabled ? 'Bloquer' : 'Débloquer' }}
              </button>
              <button class="btn btn-ghost mini" (click)="toggleRole(u)" [disabled]="busyId() === u.id">
                {{ u.role === 'ADMIN' ? 'Rétrograder' : 'Promouvoir' }}
              </button>
              <button class="btn btn-danger mini" (click)="deleteUser(u)" [disabled]="busyId() === u.id">
                Supprimer
              </button>
            </td>
          </tr>
          <tr *ngIf="!loading() && users().length === 0">
            <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Aucun utilisateur trouvé
            </td>
          </tr>
          <tr *ngIf="loading()">
            <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Chargement…
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span>{{ totalElements() }} utilisateurs</span>
        <button class="btn btn-ghost mini" (click)="prevPage()" [disabled]="page() === 0">Précédent</button>
        <span>Page {{ page() + 1 }} / {{ totalPages() || 1 }}</span>
        <button class="btn btn-ghost mini" (click)="nextPage()"
                [disabled]="page() + 1 >= totalPages()">Suivant</button>
      </div>
    </div>

    <div class="modal-backdrop" *ngIf="confirmAction() as ca" (click)="cancelConfirm()">
      <div class="modal" (click)="$event.stopPropagation()">
        <h3>{{ ca.title }}</h3>
        <p>{{ ca.message }}</p>
        <div class="modal-actions">
          <button class="btn btn-ghost" (click)="cancelConfirm()">Annuler</button>
          <button class="btn" [class.btn-danger]="ca.danger" [class.btn-primary]="!ca.danger"
                  (click)="confirm()">
            Confirmer
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mini { padding: 0.3rem 0.6rem; font-size: 0.75rem; margin-left: 0.3rem; }
    h3 { font-size: 1.1rem; color: var(--gold); margin-bottom: 0.6rem; }

    .modal-backdrop {
      position: fixed; inset: 0;
      background: rgba(13, 11, 7, 0.7);
      display: flex; align-items: center; justify-content: center;
      z-index: 100;
    }
    .modal {
      background: var(--night-mid);
      border: 1px solid var(--border-gold);
      border-radius: 12px;
      padding: 1.5rem;
      width: 100%; max-width: 440px;
    }
    .modal p { color: var(--text-dim); margin: 0.5rem 0 1.25rem; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; }
  `]
})
export class UsersComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly users = signal<User[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly busyId = signal<string | null>(null);

  protected readonly page = signal(0);
  protected readonly size = signal(20);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  protected readonly confirmAction = signal<{
    title: string;
    message: string;
    danger: boolean;
    run: () => void;
  } | null>(null);

  protected search = '';
  protected planFilter: Plan | '' = '';
  protected enabledFilter: EnabledFilter = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const enabled = this.enabledFilter === '' ? undefined : this.enabledFilter === 'true';
    const plan = this.planFilter === '' ? undefined : this.planFilter;
    const search = this.search.trim() || undefined;

    this.api.listUsers(this.page(), this.size(), plan, enabled, search).subscribe({
      next: page => {
        this.users.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les utilisateurs');
        this.loading.set(false);
      }
    });
  }

  resetFilters(): void {
    this.search = '';
    this.planFilter = '';
    this.enabledFilter = '';
    this.page.set(0);
    this.reload();
  }

  prevPage(): void {
    if (this.page() === 0) return;
    this.page.set(this.page() - 1);
    this.reload();
  }

  nextPage(): void {
    if (this.page() + 1 >= this.totalPages()) return;
    this.page.set(this.page() + 1);
    this.reload();
  }

  changePlan(user: User, plan: Plan): void {
    if (user.plan === plan) return;
    this.busyId.set(user.id);
    this.api.updatePlan(user.id, plan).subscribe({
      next: updated => this.replaceUser(updated, `Plan de ${user.email} → ${plan}`),
      error: () => this.failUpdate(user.id, 'Échec du changement de plan')
    });
  }

  toggleBlock(user: User): void {
    const newState = !user.enabled;
    this.askConfirm({
      title: newState ? 'Débloquer ce compte ?' : 'Bloquer ce compte ?',
      message: `${user.email} sera ${newState ? 'autorisé à se connecter' : 'empêché de se connecter'}.`,
      danger: !newState,
      run: () => {
        this.busyId.set(user.id);
        this.api.setEnabled(user.id, newState).subscribe({
          next: updated => this.replaceUser(updated,
            newState ? `${user.email} débloqué` : `${user.email} bloqué`),
          error: () => this.failUpdate(user.id, 'Échec de l’opération')
        });
      }
    });
  }

  toggleRole(user: User): void {
    const newRole: Role = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
    this.askConfirm({
      title: newRole === 'ADMIN' ? 'Promouvoir ADMIN ?' : 'Rétrograder en USER ?',
      message: `${user.email} aura le rôle ${newRole}.`,
      danger: false,
      run: () => {
        this.busyId.set(user.id);
        this.api.updateRole(user.id, newRole).subscribe({
          next: updated => this.replaceUser(updated, `Rôle mis à jour : ${updated.role}`),
          error: () => this.failUpdate(user.id, 'Échec du changement de rôle')
        });
      }
    });
  }

  deleteUser(user: User): void {
    this.askConfirm({
      title: 'Supprimer ce compte ?',
      message: `Suppression définitive (RGPD) de ${user.email}. Cette action est irréversible.`,
      danger: true,
      run: () => {
        this.busyId.set(user.id);
        this.api.deleteUser(user.id).subscribe({
          next: () => {
            this.users.update(list => list.filter(u => u.id !== user.id));
            this.totalElements.update(n => Math.max(0, n - 1));
            this.busyId.set(null);
            this.flash(`${user.email} supprimé`);
          },
          error: () => this.failUpdate(user.id, 'Échec de la suppression')
        });
      }
    });
  }

  askConfirm(action: { title: string; message: string; danger: boolean; run: () => void }): void {
    this.confirmAction.set(action);
  }

  confirm(): void {
    const action = this.confirmAction();
    this.confirmAction.set(null);
    action?.run();
  }

  cancelConfirm(): void {
    this.confirmAction.set(null);
  }

  trackById(_: number, u: User): string { return u.id; }

  private replaceUser(updated: User, message: string): void {
    this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
    this.busyId.set(null);
    this.flash(message);
  }

  private failUpdate(userId: string, message: string): void {
    this.busyId.set(null);
    this.errorMessage.set(message);
    setTimeout(() => this.errorMessage.set(null), 4000);
  }

  private flash(message: string): void {
    this.successMessage.set(message);
    setTimeout(() => this.successMessage.set(null), 3000);
  }
}
