import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AuthResponse,
  AuthUser,
  Dashboard,
  Instrument,
  Order,
  OrderRequest,
  MarketData,
  Position,
  Preference,
  PreferenceRequest,
  Transaction,
  Trade,
} from './models';

const ACCESS_KEY = 'tp_access';
const REFRESH_KEY = 'tp_refresh';
const USER_KEY = 'tp_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/login', { email, password });
  }

  register(email: string, firstName: string, lastName: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/register', { email, firstName, lastName, password });
  }

  refresh(refreshToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/refresh', { refreshToken });
  }

  logout(): Observable<{ success: boolean }> {
    const rt = this.refreshToken;
    return this.http.post<{ success: boolean }>('/api/v1/auth/logout', { refreshToken: rt });
  }

  me(): Observable<{ user: AuthUser }> {
    return this.http.get<{ user: AuthUser }>('/api/v1/auth/me');
  }

  saveSession(res: AuthResponse) {
    localStorage.setItem(ACCESS_KEY, res.accessToken);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
  }

  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
  }

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  get user(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  }

  get isAuthenticated(): boolean {
    return !!this.accessToken;
  }
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  get<T>(path: string): Observable<T> {
    return this.http.get<T>(`/api/v1/${path}`);
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(`/api/v1/${path}`, body);
  }

  // Convenience passthroughs
  preferences = {
    get: () => this.get<Preference>('preferences'),
    save: (body: PreferenceRequest) => this.post<Preference>('preferences', body),
  };

  dashboard = () => this.get<Dashboard>('dashboard');

  instruments = () => this.get<Instrument[]>('instruments');

  marketData = () => this.get<MarketData[]>('market-data');

  portfolio = () => this.get<Position[]>('portfolio');

  positions = () => this.get<Position[]>('positions');

  orders = {
    list: () => this.get<Order[]>('orders'),
    create: (body: OrderRequest) => this.post<Order>('orders', body),
    get: (id: string) => this.get<Order>(`orders/${id}`),
    cancel: (id: string) => this.post<Order>(`orders/${id}/cancel`, {}),
  };

  trades = () => this.get<Trade[]>('trades');

  transactions = () => this.get<Transaction[]>('transactions');
}