import { Component } from '@angular/core';
import { Router, RouterModule, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { AuthUser } from '../core/models';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterModule],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">
          <span class="logo">⇄</span>
          <span class="brand-name">Trade<span class="accent">Desk</span></span>
        </div>
        <nav>
          <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/trade" routerLinkActive="active">Trade</a>
          <a routerLink="/portfolio" routerLinkActive="active">Portfolio</a>
          <a routerLink="/orders" routerLinkActive="active">Orders</a>
          <a routerLink="/transactions" routerLinkActive="active">Transactions</a>
          <a routerLink="/account" routerLinkActive="active">Account</a>
        </nav>
      </aside>
      <div class="main">
        <header class="topbar">
          <span class="muted page-dots">● ● ●</span>
          <span class="topbar-right">
            <span class="user-chip">{{ user?.firstName }} {{ user?.lastName }}</span>
            <span class="user-mail muted">{{ user?.email }}</span>
            <button class="btn sm" (click)="logout()">Logout</button>
          </span>
        </header>
        <main class="content">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .shell { display: flex; min-height: 100vh; }
    .sidebar { width: 220px; background: var(--bg-2); border-right: 1px solid var(--border); padding: 16px 12px; flex-shrink: 0; }
    .brand { display: flex; align-items: center; gap: 10px; padding: 8px 10px 24px; }
    .logo { font-size: 22px; color: var(--accent); }
    .brand-name { font-size: 18px; font-weight: 800; letter-spacing: .5px; }
    .accent { color: var(--accent); }
    nav a { display: block; padding: 10px 12px; border-radius: 8px; color: var(--muted); font-weight: 600; margin-bottom: 4px; }
    nav a:hover { background: var(--panel); color: var(--text); }
    nav a.active { background: var(--panel-2); color: var(--accent); }
    .main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
    .topbar { display: flex; justify-content: space-between; align-items: center; padding: 12px 24px; border-bottom: 1px solid var(--border); }
    .topbar-right { display: flex; align-items: center; gap: 12px; }
    .user-chip { background: var(--panel-2); padding: 6px 12px; border-radius: 20px; font-size: 13px; font-weight: 600; }
    .user-mail { font-size: 12px; }
    .content { padding: 24px; flex: 1; }
    .page-dots { letter-spacing: 6px; font-size: 10px; }
  `],
})
export class AppShellComponent {
  user: AuthUser | null;

  constructor(private auth: AuthService, private router: Router) {
    this.user = auth.user;
  }

  logout() {
    this.auth.logout().subscribe({
      next: () => {
        this.auth.clear();
        this.router.navigate(['/login']);
      },
      error: () => {
        this.auth.clear();
        this.router.navigate(['/login']);
      },
    });
  }
}