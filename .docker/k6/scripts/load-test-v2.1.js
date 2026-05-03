/**
 * =========================================================================
 * 🚀 k6 壓力測試 — V2-1 高撮合率壓測
 *
 * 目標：最大化實際撮合成交率，壓測 Disruptor 完整熱路徑
 *   - 只送限價單（排除市價單不支援的干擾）
 *   - 買賣價格刻意交叉（買高賣低），確保大量實際成交
 *   - 數量縮小，減少餘額不足的機率
 *
 * 啟動指令 (在專案根目錄執行):
 * docker run --rm -i grafana/k6 run - <.docker/k6/scripts/load-test-v2.1.js
 * =========================================================================
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // 30 秒內緩步提升到 50 個併發交易員
    { duration: '1m', target: 50 },   // 維持 50 個併發 1 分鐘
    { duration: '30s', target: 100 }, // 提升到 100 個併發
    { duration: '1m', target: 100 },  // 維持 1 分鐘
    { duration: '30s', target: 0 },   // 冷卻下線
  ],
};

const BASE_URL = 'http://host.docker.internal:8080/api/v2/trading/order';

export default function () {
  const side = Math.random() < 0.5 ? 0 : 1;

  // 買方出高價（100~110），賣方出低價（90~100），製造價格交叉 → 高撮合率
  const price = side === 0
    ? randomIntBetween(100, 110)   // 買入：願意出 100~110
    : randomIntBetween(90, 100);   // 賣出：願意賣 90~100

  const payload = JSON.stringify({
    traderId: randomIntBetween(1, 10),
    marketId: 1,
    side: side,
    type: 0,                         // 只送限價單
    price: price,
    qty: 1,                          // 縮小數量，降低餘額不足的機率
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(BASE_URL, payload, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
  });

  sleep(Math.random() * 0.4 + 0.1);
}
