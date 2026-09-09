export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface AuthResponse {
  user: AuthUser;
  accessToken: string;
  refreshToken: string;
}

export interface Preference {
  completed: boolean;
  tradingExperience: string;
  riskTolerance: string;
  tradingStyle: string;
  investmentHorizon: string;
  preferredSectors: string[];
  tradingFrequency: string;
}

export interface PreferenceRequest {
  tradingExperience: string;
  riskTolerance: string;
  tradingStyle: string;
  investmentHorizon: string;
  preferredSectors: string[];
  tradingFrequency: string;
}

export interface Instrument {
  id: string;
  symbol: string;
  name: string;
  exchange: string;
  currency: string;
  type: string;
  lastPrice: number;
  change: number;
  changePercent: number;
}

export interface MarketData {
  symbol: string;
  name: string;
  price: number;
  change: number;
  changePercent: number;
}

export interface AccountSummary {
  symbol: string;
  name: string;
  cash: number;
  margin: number;
}

export interface PortfolioSummary {
  availableCash: number;
  invested: number;
  totalValue: number;
  dayChange: number;
  dayChangePercent: number;
  totalPnl: number;
}

export interface Position {
  id: string;
  instrumentId: string;
  symbol: string;
  quantity: number;
  averagePrice: number;
  realizedPnl: number;
  lastPrice: number;
  currentValue: number;
  pnl: number;
  pnlPercent: number;
}

export type OrderStatus = 'PENDING' | 'EXECUTED' | 'REJECTED' | 'CANCELLED';

export interface Order {
  id: string;
  instrumentId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT';
  quantity: number;
  requestedPrice: number | null;
  executedPrice: number | null;
  status: OrderStatus;
  rejectReason: string | null;
  createdAt: string;
}

export interface OrderRequest {
  symbol: string;
  side: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT';
  quantity: number;
  requestedPrice?: number;
}

export interface Trade {
  id: string;
  orderId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  quantity: number;
  price: number;
  executedAt: string;
}

export interface Transaction {
  id: string;
  orderId: string | null;
  type: string;
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export interface Dashboard {
  account: AccountSummary;
  portfolio: PortfolioSummary;
  holdings: Position[];
  positions: Position[];
  watchlist: MarketData[];
  marketData: MarketData[];
  openOrders: Order[];
  recentTransactions: Transaction[];
  recentTrades: Trade[];
}

export const money = (n: number): string =>
  '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export const num = (n: number, digits = 2): string =>
  (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits });

export const pct = (n: number): string =>
  (n >= 0 ? '+' : '') + (n ?? 0).toFixed(2) + '%';