import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/auth.service';
import { Order, money } from '../../core/models';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page" *ngIf="orders">
      <h2>Orders</h2>
      <p class="muted">Order lifecycle: PENDING (Service 1) → Executed/Rejected by Service 2 via Kafka.</p>

      <div class="panel">
        <table>
          <thead>
            <tr><th>Time</th><th>Symbol</th><th>Side</th><th>Type</th><th>Qty</th><th>Limit</th><th>Fill</th><th>Status</th><th>Reason</th><th></th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let o of orders">
              <td>{{ ts(o.createdAt) }}</td>
              <td><b>{{ o.symbol }}</b></td>
              <td [class.buy]="o.side === 'BUY'" [class.sell]="o.side !== 'BUY'">{{ o.side }}</td>
              <td>{{ o.orderType }}</td>
              <td>{{ o.quantity }}</td>
              <td>{{ o.requestedPrice ? money(o.requestedPrice) : '—' }}</td>
              <td>{{ o.executedPrice ? money(o.executedPrice) : '—' }}</td>
              <td><span class="badge" [class]="o.status.toLowerCase()">{{ o.status }}</span></td>
              <td class="muted">{{ o.rejectReason || '—' }}</td>
              <td>
                <button *ngIf="o.status === 'PENDING'" class="btn sm sell-btn" (click)="cancel(o)">Cancel</button>
              </td>
            </tr>
            <tr *ngIf="!orders.length"><td colspan="10" class="muted">No orders yet.</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class OrdersComponent implements OnInit {
  orders: Order[] | null = null;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.load();
  }

  private load() {
    this.api.orders.list().subscribe((orders) => (this.orders = orders));
  }

  cancel(o: Order) {
    this.api.orders.cancel(o.id).subscribe(() => this.load());
  }

  ts = (s: string) => new Date(s).toLocaleString();
  money = money;
}