DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS balances;
DROP TABLE IF EXISTS assets;
DROP TABLE IF EXISTS traders;

-- 1. 交易員表 (紀錄是誰在交易)
CREATE TABLE traders (
    id SERIAL PRIMARY KEY,                                   -- 交易員唯一 ID
    name VARCHAR(50) NOT NULL UNIQUE,                        -- 交易員名稱 (如: Alice, Bob)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 註冊時間
);

-- 2. 資產定義表 (所有的貨幣、股票、代幣都在這裡定義)
CREATE TABLE assets (
    id SERIAL PRIMARY KEY,                                   -- 資產唯一 ID
    symbol VARCHAR(20) NOT NULL UNIQUE,                      -- 資產代碼 (如: USD, TSM)
    name VARCHAR(100),                                       -- 資產全稱 (如: US Dollar)
    precision INT NOT NULL DEFAULT 4,                        -- 小數點位數
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 建立時間
);

-- 3. 客戶持倉表 (最終事實表)
CREATE TABLE balances (
    id SERIAL PRIMARY KEY,                                   -- 持倉紀錄唯一 ID
    trader_id INT NOT NULL,                                  -- 對應交易員 ID
    asset_id INT NOT NULL,                                   -- 對應資產 ID
    available_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,      -- 可用餘額
    frozen_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,         -- 凍結餘額 (掛單中)
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 最後更新時間
    UNIQUE(trader_id, asset_id)                              -- 確保一個交易員的一種資產只有一筆紀錄
);

-- 4. 委託單簿 (Order Book / 記錄買賣掛單)
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,                                   -- 委託單唯一 ID
    trader_id INT NOT NULL,                                  -- 下單的交易員 ID
    base_asset_id INT NOT NULL,                              -- 交易對標的資產 ID (如 TSM)
    quote_asset_id INT NOT NULL,                             -- 計價貨幣資產 ID (如 USD)
    side INT NOT NULL,                                       -- 交易方向：0: 買入 (BUY), 1: 賣出 (SELL)
    price DECIMAL(19, 4) NOT NULL,                           -- 掛單價格
    total_qty DECIMAL(19, 4) NOT NULL,                       -- 委託總數量
    filled_qty DECIMAL(19, 4) NOT NULL DEFAULT 0,            -- 已成交數量
    status INT NOT NULL DEFAULT 0,                           -- 狀態：0: 新委託, 1: 部分成交, 2: 完全成交, 3: 已撤單
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 下單時間
);
