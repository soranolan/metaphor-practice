-- 1. 初始化資產 (Asset IDs: 1=USD, 2=VT)
INSERT INTO assets (id, symbol, name) VALUES (1, 'USD', 'United States Dollar');
INSERT INTO assets (id, symbol, name) VALUES (2, 'VT', 'Vanguard Total World Stock');

-- 2. 初始化 10 個交易員
INSERT INTO traders (id, name) VALUES (1, 'Alice');
INSERT INTO traders (id, name) VALUES (2, 'Bob');
INSERT INTO traders (id, name) VALUES (3, 'Charlie');
INSERT INTO traders (id, name) VALUES (4, 'Dave');
INSERT INTO traders (id, name) VALUES (5, 'Eve');
INSERT INTO traders (id, name) VALUES (6, 'Frank');
INSERT INTO traders (id, name) VALUES (7, 'Grace');
INSERT INTO traders (id, name) VALUES (8, 'Heidi');
INSERT INTO traders (id, name) VALUES (9, 'Ivan');
INSERT INTO traders (id, name) VALUES (10, 'Judy');

-- 3. 初始化資金 (每個人都有 10,000,000 USD 與 1,000 VT)
-- Asset 1: USD
INSERT INTO balances (trader_id, asset_id, available_amount) SELECT id, 1, 10000000.0000 FROM traders;
-- Asset 2: VT
INSERT INTO balances (trader_id, asset_id, available_amount) SELECT id, 2, 1000.0000 FROM traders;

-- 4. 初始化市場 (VT/USD)
INSERT INTO markets (id, base_asset_id, quote_asset_id, symbol) VALUES (1, 2, 1, 'VT/USD');
