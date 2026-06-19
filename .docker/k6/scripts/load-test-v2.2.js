/**
 * =========================================================================
 * k6 壓力測試 — V2-2 高撮合率極限壓測（No Sleep）
 *
 * 目標：移除 VU think time，讓 k6 儘量把請求打滿，用來觀察服務端極限。
 *   - 沿用 V2-1 的高撮合率限價單策略
 *   - 不 sleep，避免 iteration_duration 被測試腳本等待時間主導
 *   - 檢查 HTTP 200 與 API body code=200
 *
 * 啟動指令 (在專案根目錄執行):
 * docker run --rm -i grafana/k6 run - <.docker/k6/scripts/load-test-v2.2.js
 *
 * 注意：這不是模擬真實使用者手速；這支用來找 CPU、RingBuffer、
 * HikariCP、GC、HTTP latency 或錯誤率的飽和點。
 * =========================================================================
 */
import http from 'k6/http';
import { check } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 100 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<100', 'p(99)<250'],
    checks: ['rate>0.99'],
  },
};

const BASE_URL = 'http://host.docker.internal:8080/api/v2/trading/order';

export default function () {
  const side = Math.random() < 0.5 ? 0 : 1;

  // 買方出高價（100~110），賣方出低價（90~100），製造價格交叉。
  const price = side === 0
    ? randomIntBetween(100, 110)
    : randomIntBetween(90, 100);

  const payload = JSON.stringify({
    traderId: randomIntBetween(1, 10),
    marketId: 1,
    side: side,
    type: 0,
    price: price,
    qty: 1,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(BASE_URL, payload, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
    'is api success': (r) => {
      try {
        return r.json('code') === 200;
      } catch (_) {
        return false;
      }
    },
  });
}
