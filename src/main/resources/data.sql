-- 1. 初始化資產代碼
INSERT INTO assets (id, symbol, name, precision) VALUES (1, 'USD', 'US Dollar', 2);
INSERT INTO assets (id, symbol, name, precision) VALUES (2, 'TSM', 'TSMC Stock', 0);

-- 2. 初始化交易員
INSERT INTO traders (id, name) VALUES (1, 'Alice');
INSERT INTO traders (id, name) VALUES (2, 'Bob');

-- 3. 初始化持倉 (各 10,000 USD 可用餘額)
INSERT INTO balances (trader_id, asset_id, available_amount) VALUES (1, 1, 10000.0000);
INSERT INTO balances (trader_id, asset_id, available_amount) VALUES (2, 1, 10000.0000);

-- (可選) 給他們一點 TSM 庫存，不然等一下沒辦法做賣單實驗
INSERT INTO balances (trader_id, asset_id, available_amount) VALUES (1, 2, 10.0000);
INSERT INTO balances (trader_id, asset_id, available_amount) VALUES (2, 2, 10.0000);
