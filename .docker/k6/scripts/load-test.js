import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  // 模擬壓力隨時間變化的階段
  stages: [
    { duration: '30s', target: 50 },  // 30 秒內緩步提升到 50 個併發交易員
    { duration: '1m', target: 50 },   // 維持 50 個併發 1 分鐘
    { duration: '30s', target: 100 }, // 提升到 100 個併發
    { duration: '1m', target: 100 },  // 維持 1 分鐘
    { duration: '30s', target: 0 },   // 冷卻下線
  ],
};

const BASE_URL = 'http://host.docker.internal:8080/api/v1/orders';

export default function () {
  // 隨機決定是買還是賣 (0: Buy, 1: Sell)
  const side = Math.random() < 0.5 ? 0 : 1;
  
  // 隨機決定訂單類型 (0: Limit, 1: Market)
  const orderType = Math.random() < 0.8 ? 0 : 1; 

  const payload = JSON.stringify({
    traderId: randomIntBetween(1, 10), // 隨機 1~10 號交易員
    marketId: 1,                      // VT/USD 市場
    side: side,
    type: orderType,
    price: orderType === 0 ? randomIntBetween(90, 110) : 120, // 限價單在 90~110 震盪，市價單固定出高價確保成交
    totalQty: randomIntBetween(1, 5), // 買 1~5 股
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

  // 每個虛擬用戶休息 0.1 ~ 0.5 秒，模擬真實人類下單手速
  sleep(Math.random() * 0.4 + 0.1);
}
