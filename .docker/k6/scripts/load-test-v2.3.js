/**
 * =========================================================================
 * k6 壓力測試 — V2-3 固定 RPS 階梯容量測試
 *
 * 目標：用 constant/ramping arrival rate 控制每秒請求數，找出本機環境下
 * p95 latency 開始失控前的健康吞吐區間。
 *
 * 測試意義：
 *   - V1 / V2.0 / V2.1：有 sleep，模擬交易員手動下單節奏。
 *   - V2.2：無 sleep，讓 k6 儘量打滿，用來找飽和訊號。
 *   - V2.3：固定 RPS 階梯，避免 no-sleep 模式把 client/server 一起打爆，
 *     較適合回答「這台本機在多少 RPS 內仍健康」。
 *
 * 階梯設計：
 *   - 50 rps  -> 100 rps -> 150 rps -> 200 rps -> 250 rps
 *   - 每段維持 1 分鐘，最後降回 0
 *
 * 啟動指令 (在專案根目錄執行):
 * docker run --rm -i grafana/k6 run - <.docker/k6/scripts/load-test-v2.3.js
 *
 * 解讀方式：
 *   - 若某段開始 p95/p99 急升，該段附近就是本機健康容量邊界。
 *   - 同時看 Prometheus 的 CPU、Hikari pending、GC、error/warn logs。
 *   - 這仍是本機容量，不是 production benchmark。
 *
 * 2026-06-19 本機實測結論：
 *   - 本腳本跑到 250 rps 階段時仍健康，尚未找到本機瓶頸。
 *   - k6 summary: http_req_duration p95 約 2.74ms、p99 約 4.02ms、
 *     max 約 39.24ms、http_req_failed 0%、checks 100%。
 *   - Prometheus: RPS 峰值約 239 rps（30s rate 會平滑 250 rps 階段）、
 *     process CPU 峰值約 4%、system CPU 峰值約 32%、
 *     Hikari pending = 0、Hikari active max = 2、GC 無明顯壓力。
 *   - 結論：固定 arrival rate 到 250 rps 對本機環境仍太輕，
 *     下一輪若要找邊界，應提高階梯，例如 250/350/450/550/650 rps。
 * =========================================================================
 */
import http from 'k6/http';
import { check } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  scenarios: {
    fixed_rps_steps: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 500,
      stages: [
        { duration: '1m', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '1m', target: 150 },
        { duration: '1m', target: 200 },
        { duration: '1m', target: 250 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<100', 'p(99)<250'],
    checks: ['rate>0.99'],
  },
};

const BASE_URL = 'http://host.docker.internal:8080/api/v2/trading/order';

export default function () {
  const side = Math.random() < 0.5 ? 0 : 1;

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
