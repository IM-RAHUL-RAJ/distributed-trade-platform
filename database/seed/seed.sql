-- =====================================================================
-- Trade Platform - Seed Data
-- Instruments reference data (applied by Flyway migration V2 on Service 1)
-- =====================================================================

INSERT INTO instruments (symbol, name, exchange, currency, type, tick_size, lot_size, last_price, change, change_percent) VALUES
  ('NVDA', 'NVIDIA Corporation',         'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 182.50,   2.50,  1.38),
  ('AAPL', 'Apple Inc.',                 'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 232.85,  -1.15, -0.49),
  ('MSFT', 'Microsoft Corporation',      'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 428.30,   3.10,  0.73),
  ('TSLA', 'Tesla, Inc.',                'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 252.40,  -6.20, -2.40),
  ('AMZN', 'Amazon.com, Inc.',           'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 198.05,   1.90,  0.97),
  ('META', 'Meta Platforms, Inc.',       'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 561.72,   8.45,  1.53),
  ('GOOG', 'Alphabet Inc.',              'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 176.15,   0.65,  0.37),
  ('NFLX', 'Netflix, Inc.',              'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 705.60, -12.30, -1.71),
  ('AMD',  'Advanced Micro Devices, Inc.','NASDAQ', 'USD', 'EQUITY', 0.01, 1, 167.95,   4.20,  2.56),
  ('CRM',  'Salesforce, Inc.',           'NYSE',   'USD', 'EQUITY', 0.01, 1, 297.38,  -2.25, -0.75),
  ('DIS',  'The Walt Disney Company',    'NYSE',   'USD', 'EQUITY', 0.01, 1,  96.42,   1.02,  1.07),
  ('XOM',  'Exxon Mobil Corporation',    'NYSE',   'USD', 'EQUITY', 0.01, 1, 115.60,  -0.44, -0.38)
ON CONFLICT (symbol) DO NOTHING;