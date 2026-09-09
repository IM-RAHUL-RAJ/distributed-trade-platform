import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/auth.service';
import { Position, money, num, pct } from '../../core/models';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page" *ngIf="positions">
      <h2>Portfolio</h2>

      <div class="grid cols-3 mb">
        <div class="metric-card">
          <div class="label">Total Positions</div>
          <div class="value">{{ holdingsCount }}</div>
        </div>
        <div class="metric-card">
          <div class="label">Invested</div>
          <div class="value">{{ money(invested) }}</div>
        </div>
        <div class="metric-card">
          <div class="label">Current Value</div>
          <div class="value">{{ money(currentValue) }}</div>
        </div>
      </div>

      <div class="panel">
        <h3>Positions</h3>
        <table>
          <thead>
            <tr><th>Symbol</th><th>Quantity</th><th>Avg Price</th><th>LTP</th><th>Current Value</th><th>P&L</th><th>P&L %</th><th>Realized</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let p of holdings">
              <td><b>{{ p.symbol }}</b></td>
              <td>{{ num(p.quantity, 0) }}</td>
              <td>{{ money(p.averagePrice) }}</td>
              <td>{{ money(p.lastPrice) }}</td>
              <td>{{ money(p.currentValue) }}</td>
              <td [class.up]="p.pnl >= 0" [class.down]="p.pnl < 0">{{ money(p.pnl) }}</td>
              <td [class.up]="p.pnl >= 0" [class.down]="p.pnl < 0">{{ pct(p.pnlPercent) }}</td>
              <td>{{ money(p.realizedPnl) }}</td>
            </tr>
            <tr *ngIf="!holdings.length"><td colspan="8" class="muted">No open positions. Place an order from the Trade page.</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`.mb { margin-bottom: 16px; }`],
})
export class PortfolioComponent implements OnInit {
  positions: Position[] | null = null;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.positions().subscribe((p) => (this.positions = p));
  }

  get holdings(): Position[] {
    return (this.positions ?? []).filter((p) => p.quantity > 0);
  }

  get holdingsCount(): number {
    return this.holdings.length;
  }

  get invested(): number {
    return this.holdings.reduce((s, p) => s + p.averagePrice * p.quantity, 0);
  }

  get currentValue(): number {
    return this.holdings.reduce((s, p) => s + p.currentValue, 0);
  }

  money = money;
  num = num;
  pct = pct;
}