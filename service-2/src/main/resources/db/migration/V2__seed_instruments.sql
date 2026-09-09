-- =====================================================================
-- Trade Platform - Service 2 Seed Data
-- Instruments reference data duplicated into Service 2's own DB so that
-- execution pricing (fallback to last_price) is self-contained.
--
-- IMPORTANT: instrument ids are FIXED (not RANDOM_UUID) and identical to
-- Service 1's seed so instrument UUIDs stay globally consistent across the
-- two independent databases (events carry instrument_id).
-- =====================================================================

INSERT INTO instruments (id, symbol, name, exchange, currency, type, tick_size, lot_size, last_price, change, change_percent) VALUES
  ('00000000-0000-0000-0000-000000000001', 'NVDA', 'NVIDIA Corporation',          'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 182.50,   2.50,  1.38),
  ('00000000-0000-0000-0000-000000000002', 'AAPL', 'Apple Inc.',                  'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 232.85,  -1.15, -0.49),
  ('00000000-0000-0000-0000-000000000003', 'MSFT', 'Microsoft Corporation',       'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 428.30,   3.10,  0.73),
  ('00000000-0000-0000-0000-000000000004', 'TSLA', 'Tesla, Inc.',                 'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 252.40,  -6.20, -2.40),
  ('00000000-0000-0000-0000-000000000005', 'AMZN', 'Amazon.com, Inc.',            'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 198.05,   1.90,  0.97),
  ('00000000-0000-0000-0000-000000000006', 'META', 'Meta Platforms, Inc.',        'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 561.72,   8.45,  1.53),
  ('00000000-0000-0000-0000-000000000007', 'GOOG', 'Alphabet Inc.',               'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 176.15,   0.65,  0.37),
  ('00000000-0000-0000-0000-000000000008', 'NFLX', 'Netflix, Inc.',               'NASDAQ', 'USD', 'EQUITY', 0.01, 1, 705.60, -12.30, -1.71),
  ('00000000-0000-0000-0000-000000000009', 'AMD',  'Advanced Micro Devices, Inc.','NASDAQ', 'USD', 'EQUITY', 0.01, 1, 167.95,   4.20,  2.56),
  ('00000000-0000-0000-0000-00000000000a', 'CRM',  'Salesforce, Inc.',            'NYSE',   'USD', 'EQUITY', 0.01, 1, 297.38,  -2.25, -0.75),
  ('00000000-0000-0000-0000-00000000000b', 'DIS',  'The Walt Disney Company',     'NYSE',   'USD', 'EQUITY', 0.01, 1,  96.42,   1.02,  1.07),
  ('00000000-0000-0000-0000-00000000000c', 'XOM',  'Exxon Mobil Corporation',     'NYSE',   'USD', 'EQUITY', 0.01, 1, 115.60,  -0.44, -0.38);