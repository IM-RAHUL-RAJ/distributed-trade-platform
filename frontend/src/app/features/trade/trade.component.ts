import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/auth.service';
import { Instrument, money } from '../../core/models';

type Side = 'BUY' | 'SELL';
type OrderType = 'MARKET' | 'LIMIT';

@Component({
  selector: 'app-trade',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <h2>Place an Order</h2>
      <p class="muted">Orders are validated by Service 1 and executed asynchronously by the Trade Executor (Service 2) via Kafka.</p>

      <div class="grid cols-2">
        <div class="panel">
          <h3>Order Ticket</h3>

          <div class="side-toggle">
            <button [class.active-buy]="side === 'BUY'" [class.active-sell]="side === 'SELL'"
                    (click)="setSide('BUY')">Buy</button>
            <button [class.active-sell]="side === 'SELL'" [class.active-buy]="side === 'BUY'"
                    (click)="setSide('SELL')">Sell</button>
          </div>

          <div class="field">
            <label>Instrument</label>
            <select [(ngModel)]="symbol" (change)="onInstrumentChange()">
              <option *ngFor="let i of instruments" [value]="i.symbol">{{ i.symbol }} — {{ i.name }}</option>
            </select>
            <div *ngIf="selected as sel" class="quote">
              <b>{{ sel.symbol }}</b> &nbsp; {{ money(sel.lastPrice) }}
              <span [class.up]="sel.changePercent >= 0" [class.down]="sel.changePercent < 0">
                {{ sel.change >= 0 ? '+' : '' }}{{ money(sel.change) }} ({{ sel.changePercent >= 0 ? '+' : '' }}{{ sel.changePercent.toFixed(2) }}%)
              </span>
            </div>
          </div>

          <div class="field">
            <label>Order type</label>
            <select [(ngModel)]="orderType">
              <option value="MARKET">Market</option>
              <option value="LIMIT">Limit</option>
            </select>
          </div>

          <div *ngIf="orderType === 'LIMIT'" class="field">
            <label>Limit price</label>
            <input type="number" step="0.01" [(ngModel)]="limitPrice" />
          </div>

          <div class="field">
            <label>Quantity</label>
            <input type="number" min="1" step="1" [(ngModel)]="quantity" />
          </div>

          <div class="est" *ngIf="est != null">
            Estimated value: <b>{{ money(est ?? 0) }}</b>
          </div>

          <p *ngIf="message.text" [class.success]="message.ok" [class.error]="!message.ok" class="msg">{{ message.text }}</p>

          <button class="btn full-order" [class.buy-btn]="side === 'BUY'" [class.sell-btn]="side === 'SELL'"
                  (click)="placeOrder()" [disabled]="placing">
            {{ placing ? 'Placing order…' : (side === 'BUY' ? 'Buy' : 'Sell') + ' ' + (quantity || '') }}
          </button>
        </div>

        <!-- Instrument list -->
        <div class="panel">
          <h3>Live Instruments</h3>
          <table>
            <thead><tr><th>Symbol</th><th>Name</th><th>LTP</th><th>Change</th><th>%</th></tr></thead>
            <tbody>
              <tr *ngFor="let i of instruments" (click)="pick(i.symbol)" class="clickable">
                <td><b>{{ i.symbol }}</b></td>
                <td class="muted">{{ i.name }}</td>
                <td>{{ money(i.lastPrice) }}</td>
                <td [class.up]="i.change >= 0" [class.down]="i.change < 0">{{ money(i.change) }}</td>
                <td [class.up]="i.changePercent >= 0" [class.down]="i.changePercent < 0">{{ i.changePercent.toFixed(2) }}%</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .side-toggle { display: flex; gap: 10px; margin-bottom: 18px; }
    .side-toggle button {
      flex: 1; padding: 10px; font-weight: 700; font-size: 14px; cursor: pointer;
      border: 1px solid var(--border); border-radius: 8px; background: var(--panel-2); color: var(--muted);
    }
    .side-toggle button.active-buy { background: var(--buy); border-color: var(--buy); color: #fff; }
    .side-toggle button.active-sell { background: var(--sell); border-color: var(--sell); color: #fff; }
    .quote { margin-top: 8px; font-size: 14px; color: var(--muted); }
    .est { padding: 10px; border-radius: 8px; background: var(--bg-2); margin-bottom: 12px; }
    .msg { margin: 10px 0 0; font-size: 13px; }
    .full-order { width: 100%; padding: 12px; font-size: 15px; }
    .clickable { cursor: pointer; }
  `],
})
export class TradeComponent implements OnInit {
  instruments: Instrument[] = [];
  selected: Instrument | null = null;
  symbol = '';
  side: Side = 'BUY';
  orderType: OrderType = 'MARKET';
  quantity = 1;
  limitPrice: number | null = null;
  placing = false;
  message: { text: string; ok: boolean } = { text: '', ok: true };

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.instruments().subscribe({
      next: (list) => {
        this.instruments = list;
        if (list.length) {
          this.symbol = list[0].symbol;
          this.selected = list[0];
        }
      },
      error: (err) => this.message = { text: err.message, ok: false },
    });
  }

  get est(): number | null {
    if (!this.selected || !this.quantity) {
      return null;
    }
    return this.selected.lastPrice * this.quantity;
  }

  setSide(s: Side) {
    this.side = s;
  }

  onInstrumentChange() {
    this.selected = this.instruments.find((i) => i.symbol === this.symbol) ?? null;
  }

  pick(symbol: string) {
    this.symbol = symbol;
    this.onInstrumentChange();
  }

  placeOrder() {
    if (!this.quantity || this.quantity < 1) {
      this.message = { text: 'Quantity must be at least 1.', ok: false };
      return;
    }
    this.placing = true;
    this.message = { text: '', ok: true };
    this.api.orders
      .create({
        symbol: this.symbol,
        side: this.side,
        orderType: this.orderType,
        quantity: this.quantity,
        requestedPrice: this.orderType === 'LIMIT' ? this.limitPrice ?? undefined : undefined,
      })
      .subscribe({
        next: (order) => {
          this.placing = false;
          this.message = {
            text: `Order ${order.id.slice(0, 8)}… placed (${order.status}). The executor will process it via Kafka.`,
            ok: true,
          };
          this.quantity = 1;
        },
        error: (err) => {
          this.placing = false;
          this.message = { text: err.error?.message || 'Order failed.', ok: false };
        },
      });
  }

  money = money;
}