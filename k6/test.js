import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    web: {
      executor: 'constant-arrival-rate',
      rate: 300, // requests per timeUnit
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 60,
      maxVUs: 200,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate<0.005'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERNAME = __ENV.USERNAME || 'user';
const PASSWORD = __ENV.PASSWORD || 'pass';

export function setup() {
  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, {
    'login status 200': r => r.status === 200,
    'login returned token': r => !!(r.json('access_token')),
  });
  const token = res.json('access_token');
  return { token };
}

function randomProductId() {
  // IDs 1001–1100 como en jmeter/data/products.csv
  const min = 1001;
  const max = 1100;
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

export default function (data) {
  const id = randomProductId();
  const params = {
    headers: {
      Authorization: `Bearer ${data.token}`,
    },
  };
  const res = http.get(`${BASE_URL}/api/products/${id}`, params);
  check(res, {
    '200 OK': r => r.status === 200,
    'has id': r => r.json('id') == String(id),
  });
  // Pausa ligera para no saturar únicamente con el scheduler
  sleep(Math.random() * 0.6 + 0.2); // 200–800 ms
}