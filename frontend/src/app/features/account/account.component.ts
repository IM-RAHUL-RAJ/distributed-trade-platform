import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService, AuthService } from '../../core/auth.service';
import { AccountSummary, money } from '../../core/models';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page" *ngIf="account as ac">
      <h2>Account</h2>

      <div class="grid cols-3 mb">
        <div class="metric-card">
          <div class="label">Cash Balance</div>
          <div class="value">{{ money(ac.cash) }}</div>
        </div>
        <div class="metric-card">
          <div class="label">Margin Used</div>
          <div class="value">{{ money(ac.margin) }}</div>
        </div>
        <div class="metric-card">
          <div class="label">Status</div>
          <div class="value" style="font-size:16px">{{ ac.symbol }}</div>
        </div>
      </div>

      <div class="panel">
        <h3>Profile</h3>
        <table>
          <tbody>
            <tr><td class="muted">Name</td><td>{{ user?.firstName }} {{ user?.lastName }}</td></tr>
            <tr><td class="muted">Email</td><td>{{ user?.email }}</td></tr>
            <tr><td class="muted">User ID</td><td>{{ user?.id }}</td></tr>
            <tr><td class="muted">Role</td><td>{{ user?.role }}</td></tr>
            <tr><td class="muted">Customer Preferences</td><td><a routerLink="/customer-preferences">Manage preferences</a></td></tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`.mb { margin-bottom: 16px; }`],
})
export class AccountComponent implements OnInit {
  account: AccountSummary | null = null;
  user = this.auth.user;

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit() {
    this.api.get<AccountSummary>('account').subscribe((a) => (this.account = a));
  }

  money = money;
}