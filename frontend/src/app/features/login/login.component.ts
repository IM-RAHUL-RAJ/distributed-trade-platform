import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ApiService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="auth-wrap">
      <div class="auth-card">
        <div class="auth-brand"><span class="logo">⇄</span> <b>TradeDesk</b></div>
        <h1>Sign in to your account</h1>
        <p class="muted">Real-time trading platform · Demo environment</p>

        <form (ngSubmit)="onSubmit()">
          <div class="field">
            <label>Email</label>
            <input type="email" name="email" [(ngModel)]="email" required autocomplete="email" />
          </div>
          <div class="field">
            <label>Password</label>
            <input type="password" name="password" [(ngModel)]="password" required autocomplete="current-password" />
          </div>
          <button class="btn primary full" type="submit" [disabled]="loading">
            {{ loading ? 'Signing in…' : 'Sign In' }}
          </button>
          <p *ngIf="error" class="error">{{ error }}</p>
        </form>

        <p class="muted switch">New here? <a routerLink="/register">Create an account</a></p>
      </div>
    </div>
  `,
  styles: [`
    .auth-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: radial-gradient(1200px 600px at 70% -10%, #1a2240 0%, var(--bg) 55%); }
    .auth-card { width: 400px; background: var(--panel); border: 1px solid var(--border); border-radius: 14px; padding: 32px; }
    .auth-brand { font-size: 18px; margin-bottom: 24px; display: flex; align-items: center; gap: 8px; }
    .logo { color: var(--accent); font-size: 22px; }
    h1 { font-size: 22px; margin: 0 0 4px; }
    .full { width: 100%; margin-top: 8px; }
    .error { color: var(--sell); font-size: 13px; margin-top: 12px; }
    .switch { margin-top: 20px; text-align: center; font-size: 13px; }
  `],
})
export class LoginComponent {
  email = '';
  password = '';
  loading = false;
  error = '';

  constructor(
    private auth: AuthService,
    private api: ApiService,
    private router: Router,
  ) {}

  async onSubmit() {
    this.loading = true;
    this.error = '';
    this.auth.login(this.email, this.password).subscribe({
      next: async (res) => {
        this.auth.saveSession(res);
        this.assignUser();
        try {
          const prefs = await this.api.preferences.get().toPromise();
          this.router.navigate(prefs?.completed ? ['/dashboard'] : ['/customer-preferences']);
        } catch {
          this.router.navigate(['/dashboard']);
        }
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Login failed. Check your credentials.';
      },
    });
  }

  private assignUser() {
    const u = this.auth.user;
    if (u) {
      localStorage.setItem('tp_user', JSON.stringify(u));
    }
  }
}