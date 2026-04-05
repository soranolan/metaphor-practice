DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS balances;
DROP TABLE IF EXISTS markets;
DROP TABLE IF EXISTS assets;
DROP TABLE IF EXISTS traders;

-- 1. 交易員表 (紀錄是誰在交易)
CREATE TABLE traders (
    id BIGSERIAL PRIMARY KEY,                                -- 交易員唯一 ID (升級為 8-byte)
    name VARCHAR(50) NOT NULL UNIQUE,                        -- 交易員名稱 (如: Alice, Bob)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 註冊時間
);

-- 2. 資產定義表 (所有的貨幣、股票、代幣都在這裡定義)
CREATE TABLE assets (
    id BIGSERIAL PRIMARY KEY,                                -- 資產唯一 ID (升級為 8-byte)
    symbol VARCHAR(20) NOT NULL UNIQUE,                      -- 資產代碼 (如: VT, USD)
    name VARCHAR(100),                                       -- 資產全稱 (如: US Dollar)
    precision INT NOT NULL DEFAULT 4,                        -- 小數點位數
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 建立時間
);

-- 3. 交易市場表 (Market / Trading Pairs)
-- 定義哪些資產可以互相交易，並分配標的 (Base) 與計價物 (Quote)
CREATE TABLE markets (
    id BIGSERIAL PRIMARY KEY,                                -- 市場唯一 ID
    base_asset_id BIGINT NOT NULL,                           -- 標的資產 ID (例如 VT)
    quote_asset_id BIGINT NOT NULL,                          -- 計價資產 ID (例如 USD)
    symbol VARCHAR(40) NOT NULL UNIQUE,                      -- 市場代號 (例如: VT/USD)
    status INT NOT NULL DEFAULT 1,                           -- 狀態：0: 暫停, 1: 正常交易
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 最後更新時間
    UNIQUE(base_asset_id, quote_asset_id)                    -- 同樣的資產組合只能建立一個市場
);

-- 4. 客戶持倉表 (最終事實表)
CREATE TABLE balances (
    id BIGSERIAL PRIMARY KEY,                                -- 持倉紀錄唯一 ID
    trader_id BIGINT NOT NULL,                               -- 對應交易員 ID
    asset_id BIGINT NOT NULL,                                -- 對應資產 ID
    available_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,      -- 可用餘額
    frozen_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,         -- 凍結餘額 (掛單中)
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 最後更新時間
    UNIQUE(trader_id, asset_id)                              -- 確保一個交易員的一種資產只有一筆紀錄
);

-- 5. 委託單簿 (Order Book / 記錄買賣掛單)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,                                -- 委託單唯一 ID
    trader_id BIGINT NOT NULL,                               -- 下單的交易員 ID
    base_asset_id BIGINT NOT NULL,                           -- 交易對標的資產 ID (如 VT)
    quote_asset_id BIGINT NOT NULL,                          -- 計價貨幣資產 ID (如 USD)
    side INT NOT NULL,                                       -- 交易方向：0: 買入 (BUY), 1: 賣出 (SELL)
    price DECIMAL(19, 4) NOT NULL,                           -- 掛單價格
    total_qty DECIMAL(19, 4) NOT NULL,                       -- 委託總數量
    filled_qty DECIMAL(19, 4) NOT NULL DEFAULT 0,            -- 已成交數量
    status INT NOT NULL DEFAULT 0,                           -- 狀態：0: 新委託, 1: 部分成交, 2: 完全成交, 3: 已撤單
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 下單時間
);
