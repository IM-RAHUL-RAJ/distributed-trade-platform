import { Routes } from '@angular/router';
import { LoginComponent } from './features/login/login.component';
import { RegisterComponent } from './features/register/register.component';
import { AppShellComponent } from './app-shell/app-shell.component';
import { AuthGuard } from './core/auth.guard';
import { CustomerPreferencesComponent } from './features/preferences/preferences.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { TradeComponent } from './features/trade/trade.component';
import { PortfolioComponent } from './features/portfolio/portfolio.component';
import { OrdersComponent } from './features/orders/orders.component';
import { TransactionsComponent } from './features/transactions/transactions.component';
import { AccountComponent } from './features/account/account.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'customer-preferences', component: CustomerPreferencesComponent },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'trade', component: TradeComponent },
      { path: 'portfolio', component: PortfolioComponent },
      { path: 'orders', component: OrdersComponent },
      { path: 'transactions', component: TransactionsComponent },
      { path: 'account', component: AccountComponent },
    ],
  },
  { path: '**', redirectTo: '' },
];