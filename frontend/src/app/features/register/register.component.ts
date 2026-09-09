import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="auth-wrap">
      <div class="auth-card">
        <div class="auth-brand"><span class="logo">⇄</span> <b>TradeDesk</b></div>
        <h1>Create your account</h1>
        <p class="muted">A virtual trading account with \$1,000,000 is created for you.</p>

        <form (ngSubmit)="onSubmit()">
          <div class="row">
            <div class="field half">
              <label>First name</label>
              <input type="text" [(ngModel)]="firstName" name="firstName" required />
            </div>
            <div class="field half">
              <label>Last name</label>
              <input type="text" [(ngModel)]="lastName" name="lastName" required />
            </div>
          </div>
          <div class="field">
            <label>Email</label>
            <input type="email" [(ngModel)]="email" name="email" required />
          </div>
          <div class="field">
            <label>Password (min 8 characters)</label>
            <input type="password" [(ngModel)]="password" name="password" minlength="8" required />
          </div>
          <button class="btn primary full" type="submit" [disabled]="loading">
            {{ loading ? 'Registering…' : 'Create Account' }}
          </button>
          <p *ngIf="error" class="error">{{ error }}</p>
        </form>

        <p class="muted switch">Already registered? <a routerLink="/login">Sign in</a></p>
      </div>
    </div>
  `,
  styles: [`
    .auth-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: radial-gradient(1200px 600px at 70% -10%, #1a2240 0%, var(--bg) 55%); }
    .auth-card { width: 440px; background: var(--panel); border: 1px solid var(--border); border-radius: 14px; padding: 32px; }
    .auth-brand { font-size: 18px; margin-bottom: 24px; display: flex; align-items: center; gap: 8px; }
    .logo { color: var(--accent); font-size: 22px; }
    h1 { font-size: 22px; margin: 0 0 4px; }
    .row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .full { width: 100%; margin-top: 8px; }
    .error { color: var(--sell); font-size: 13px; margin-top: 12px; }
    .switch { margin-top: 20px; text-align: center; font-size: 13px; }
  `],
})
export class RegisterComponent {
  firstName = '';
  lastName = '';
  email = '';
  password = '';
  loading = false;
  error = '';

  constructor(private auth: AuthService, private router: Router) {}

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.auth
      .register(this.email, this.firstName, this.lastName, this.password)
      .subscribe({
        next: (res) => {
          this.auth.saveSession(res);
          this.loading = false;
          this.router.navigate(['/customer-preferences']);
        },
        error: (err) => {
          this.loading = false;
          this.error = err.error?.message || 'Registration failed.';
        },
      });
  }
}