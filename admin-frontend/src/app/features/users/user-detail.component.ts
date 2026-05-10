import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AdminApiService } from '../../core/services/admin-api.service';
import { User } from '../../shared/models/admin.model';

@Component({
  selector: 'admin-user-detail',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink],
  template: `
    <div class="breadcrumb">
      <a routerLink="/users">← Utilisateurs</a>
    </div>

    <div *ngIf="errorMessage()" class="alert error">{{ errorMessage() }}</div>
    <div *ngIf="loading()" class="loading">Chargement…</div>

    <ng-container *ngIf="user() as u">
      <h1>{{ u.firstName }} {{ u.lastName }}</h1>
      <p class="subtitle">{{ u.email }}</p>

      <div class="detail-grid">
        <div class="card">
          <h3>Informations</h3>
          <dl class="info-list">
            <div><dt>ID</dt><dd class="mono">{{ u.id }}</dd></div>
            <div><dt>Email</dt><dd>{{ u.email }}</dd></div>
            <div><dt>Prénom</dt><dd>{{ u.firstName }}</dd></div>
            <div><dt>Nom</dt><dd>{{ u.lastName }}</dd></div>
            <div><dt>Inscription</dt><dd>{{ u.createdAt | date:'dd/MM/yyyy HH:mm' }}</dd></div>
          </dl>
        </div>

        <div class="card">
          <h3>Plan & accès</h3>
          <dl class="info-list">
            <div>
              <dt>Plan actuel</dt>
              <dd>
                <span class="badge"
                      [class.free]="u.plan === 'FREE'"
                      [class.premium]="u.plan === 'PREMIUM'"
                      [class.premium-plus]="u.plan === 'PREMIUM_PLUS'">{{ u.plan }}</span>
              </dd>
            </div>
            <div>
              <dt>Rôle</dt>
              <dd>
                <span class="badge" [class.admin]="u.role === 'ADMIN'" [class.user]="u.role === 'USER'">
                  {{ u.role }}
                </span>
              </dd>
            </div>
            <div>
              <dt>Statut</dt>
              <dd>
                <span class="badge" [class.success]="u.enabled" [class.disabled]="!u.enabled">
                  {{ u.enabled ? 'Actif' : 'Bloqué' }}
                </span>
              </dd>
            </div>
          </dl>
        </div>
      </div>

      <div class="card">
        <h3>Actions disponibles</h3>
        <p class="hint">
          La gestion des actions (changement de plan, blocage, rôle, suppression) s’effectue
          depuis la liste des utilisateurs pour bénéficier des dialogues de confirmation.
        </p>
        <a class="btn btn-primary" routerLink="/users">Retour à la liste</a>
      </div>
    </ng-container>
  `,
  styles: [`
    h3 { font-size: 1.05rem; color: var(--gold); margin-bottom: 1rem; }
    .breadcrumb { margin-bottom: 1rem; }
    .breadcrumb a { font-size: 0.85rem; color: var(--text-dim); }
    .breadcrumb a:hover { color: var(--gold); }

    .detail-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }
    @media (max-width: 800px) {
      .detail-grid { grid-template-columns: 1fr; }
    }

    .info-list { display: flex; flex-direction: column; gap: 0.65rem; }
    .info-list > div {
      display: grid;
      grid-template-columns: 130px 1fr;
      align-items: center;
      padding: 0.4rem 0;
      border-bottom: 1px solid rgba(201, 168, 76, 0.06);
    }
    .info-list dt {
      color: var(--text-dim);
      font-size: 0.78rem;
      letter-spacing: 0.05em;
      text-transform: uppercase;
    }
    .info-list dd { color: var(--text); font-size: 0.92rem; }
    .mono { font-family: 'Courier New', monospace; font-size: 0.8rem; color: var(--text-dim); }

    .hint { color: var(--text-dim); margin-bottom: 1rem; font-size: 0.9rem; }
    .loading { text-align: center; padding: 2rem; color: var(--text-dim); }
  `]
})
export class UserDetailComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly user = signal<User | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage.set('Identifiant utilisateur manquant');
      this.loading.set(false);
      return;
    }
    this.api.getUser(id).subscribe({
      next: u => {
        this.user.set(u);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Utilisateur introuvable');
        this.loading.set(false);
      }
    });
  }
}
