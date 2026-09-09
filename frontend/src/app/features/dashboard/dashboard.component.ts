import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { interval } from 'rxjs';
import { ApiService } from '../../core/auth.service';
import { Dashboard, Order, money, num, pct } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page" *ngIf="data as d; else loading">
      <div class="grid cols-4">
        <div class="metric-card">
          <div class="label">Available Cash</div>
          <div class="value">{{ money(d.account.cash) }}</div>
          <div class="muted small">margin {{ money(d.account.margin) }}</div>
        </div>
        <div class="metric-card">
          <div class="label">Portfolio Value</div>
          <div class="value">{{ money(d.portfolio.totalValue) }}</div>
          <div class="muted small">invested {{ money(d.portfolio.invested) }}</div>
        </div>
        <div class="metric-card">
          <div class="label">Day P&L</div>
          <div class="value" [class.up]="d.portfolio.dayChange >= 0" [class.down]="d.portfolio.dayChange < 0">
            {{ money(d.portfolio.dayChange) }}
            <span class="small">{{ pct(d.portfolio.dayChangePercent) }}</span>
          </div>
          <div class="muted small">unrealized {{ money(d.portfolio.totalPnl) }}</div>
        </div>
        <div class="metric-card">
          <div class="label">Positions</div>
          <div class="value">{{ d.holdings.length }}</div>
          <div class="muted small">{{ d.openOrders.length }} open order(s)</div>
        </div>
      </div>

      <div class="grid cols-2 mt">
        <div class="panel">
          <h3>Watchlist</h3>
          <table>
            <thead><tr><th>Symbol</th><th>LTP</th><th>Change</th><th>%</th></tr></thead>
            <tbody>
              <tr *ngFor="let w of d.watchlist">
                <td><b>{{ w.symbol }}</b><br><span class="muted small">{{ w.name }}</span></td>
                <td>{{ money(w.price) }}</td>
                <td [class.up]="w.change >= 0" [class.down]="w.change < 0">{{ money(w.change) }}</td>
                <td [class.up]="w.changePercent >= 0" [class.down]="w.changePercent < 0">{{ pct(w.changePercent) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="panel">
          <h3>Holdings</h3>
          <table>
            <thead><tr><th>Symbol</th><th>Qty</th><th>Avg</th><th>LTP</th><th>Value</th><th>P&L</th></tr></thead>
            <tbody>
              <tr *ngFor="let h of d.holdings">
                <td><b>{{ h.symbol }}</b></td>
                <td>{{ num(h.quantity, 0) }}</td>
                <td>{{ money(h.averagePrice) }}</td>
                <td>{{ money(h.lastPrice) }}</td>
                <td>{{ money(h.currentValue) }}</td>
                <td [class.up]="h.pnl >= 0" [class.down]="h.pnl < 0">{{ money(h.pnl) }} ({{ pct(h.pnlPercent) }})</td>
              </tr>
              <tr *ngIf="!d.holdings.length"><td colspan="6" class="muted">No holdings yet — <a routerLink="/trade">place your first trade</a></td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="panel mt" *ngIf="d.openOrders.length">
        <h3>Open Orders (Pending)</h3>
        <table>
          <thead><tr><th>Time</th><th>Symbol</th><th>Side</th><th>Type</th><th>Qty</th><th>Limit</th><th>Status</th></tr></thead>
          <tbody>
            <tr *ngFor="let o of d.openOrders">
              <td>{{ ts(o.createdAt) }}</td>
              <td><b>{{ o.symbol }}</b></td>
              <td [class.buy]="isBuy(o)" [class.sell]="!isBuy(o)">{{ o.side }}</td>
              <td>{{ o.orderType }}</td>
              <td>{{ o.quantity }}</td>
              <td>{{ o.requestedPrice ?? '—' }}</td>
              <td><span class="badge pending">{{ o.status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="grid cols-2 mt">
        <div class="panel">
          <h3>Recent Transactions</h3>
          <table>
            <thead><tr><th>Time</th><th>Type</th><th>Amount</th><th>Balance</th></tr></thead>
            <tbody>
              <tr *ngFor="let t of d.recentTransactions">
                <td>{{ ts(t.createdAt) }}</td>
                <td>{{ t.type }}</td>
                <td [class.buy]="t.amount >= 0" [class.sell]="t.amount < 0">{{ money(t.amount) }}</td>
                <td>{{ money(t.balanceAfter) }}</td>
              </tr>
              <tr *ngIf="!d.recentTransactions.length"><td colspan="4" class="muted">No transactions yet.</td></tr>
            </tbody>
          </table>
        </div>

        <div class="panel">
          <h3>Recent Trades (live from Kafka)</h3>
          <table>
            <thead><tr><th>Time</th><th>Symbol</th><th>Side</th><th>Qty</th><th>Price</th></tr></thead>
            <tbody>
              <tr *ngFor="let t of d.recentTrades">
                <td>{{ ts(t.executedAt) }}</td>
                <td><b>{{ t.symbol }}</b></td>
                <td [class.buy]="t.side === 'BUY'" [class.sell]="t.side !== 'BUY'">{{ t.side }}</td>
                <td>{{ t.quantity }}</td>
                <td>{{ money(t.price) }}</td>
              </tr>
              <tr *ngIf="!d.recentTrades.length"><td colspan="5" class="muted">Trades executed by Service 2 will appear here.</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <ng-template #loading><div class="center"><div class="spinner"></div></div></ng-template>
  `,
  styles: [`
    .mt { margin-top: 16px; }
    .small { font-size: 12px; font-weight: 400; }
  `],
})
export class DashboardComponent implements OnInit, OnDestroy {
  data: Dashboard | null = null;
  private poller: any;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.load();
    this.poller = interval(5000).subscribe(() => this.load());
  }

  ngOnDestroy() {
    this.poller?.unsubscribe();
  }

  private load() {
    this.api.dashboard().subscribe({
      next: (d) => (this.data = d),
      error: () => undefined,
    });
  }

  money = money;
  pct = pct;
  num = num;
  isBuy = (o: Order) => o.side === 'BUY';
  ts = (s: string) => new Date(s).toLocaleString();
}