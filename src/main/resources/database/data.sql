-- 1. 初始化資產代碼
INSERT INTO assets (id, symbol, name, precision) VALUES (1, 'USD', 'US Dollar', 2);
INSERT INTO assets (id, symbol, name, precision) VALUES (2, 'VT', 'VT Stock', 0);

-- 2. 初始化交易員
INSERT INTO traders (id, name) VALUES (1, 'Alice');
INSERT INTO traders (id, name) VALUES (2, 'Bob');

-- 3. 初始化持倉
INSERT INTO balances (trader_id, asset_id, available_amount) VALUES (1, 1, 10000.0000);
INSERT INTO balances (trader_id, asset_id, available_amount) VALUES (2, 1, 0.0000);
INSERT INTO balances (trader_id, asset_id, available_amount) VALUES (1, 2, 0.0000);
INSERT INTO balances (trader_id, asset_id, available_amount) VALUES (2, 2, 100.0000);

-- 4. 初始化官方交易市場
INSERT INTO markets (id, base_asset_id, quote_asset_id, symbol) VALUES (1, 2, 1, 'VT/USD');
