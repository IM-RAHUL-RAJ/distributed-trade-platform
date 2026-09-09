import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/auth.service';
import { Transaction, money } from '../../core/models';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page" *ngIf="transactions">
      <h2>Transactions</h2>
      <p class="muted">Cash-flow audit trail written by the Trade Executor (Service 2).</p>

      <div class="panel">
        <table>
          <thead>
            <tr><th>Time</th><th>Type</th><th>Description</th><th>Amount</th><th>Balance After</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let t of transactions">
              <td>{{ ts(t.createdAt) }}</td>
              <td>{{ t.type }}</td>
              <td class="muted">{{ t.description || '—' }}</td>
              <td [class.buy]="t.amount >= 0" [class.sell]="t.amount < 0">{{ money(t.amount) }}</td>
              <td>{{ money(t.balanceAfter) }}</td>
            </tr>
            <tr *ngIf="!transactions.length"><td colspan="5" class="muted">No transactions yet.</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class TransactionsComponent implements OnInit {
  transactions: Transaction[] | null = null;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.transactions().subscribe((t) => (this.transactions = t));
  }

  ts = (s: string) => new Date(s).toLocaleString();
  money = money;
}