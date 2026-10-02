// 백엔드는 모든 응답을 ApiResponse { success, data, message } 로 감싼다.
async function request(path, options = {}) {
  const res = await fetch(`/api/v1${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const body = await res.json().catch(() => null);
  if (!res.ok || (body && body.success === false)) {
    throw new Error(body?.message || `요청 실패 (${res.status})`);
  }
  return body && 'data' in body ? body.data : body;
}

const post = (path, data) => request(path, { method: 'POST', body: data === undefined ? undefined : JSON.stringify(data) });
const put = (path, data) => request(path, { method: 'PUT', body: JSON.stringify(data) });
const qs = (o) => {
  const p = Object.entries(o).filter(([, v]) => v !== undefined && v !== null && v !== '');
  return p.length ? `?${p.map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')}` : '';
};

export const api = {
  // 공통
  home: () => request('/home'),
  regions: () => request('/policies/regions'),
  glossary: () => request('/glossary'),
  policies: (asOf) => request(`/policies/current${qs({ asOf })}`),
  updateParam: (id, body) => put(`/policies/parameters/${id}`, body),

  // Phase 1 투자 분석
  calculate: (input) => post('/investment/analyze', input),
  save: (title, memo, input) => post('/investment/analyses', { title, memo, input }),
  listSaved: () => request('/investment/analyses'),
  getSaved: (id) => request(`/investment/analyses/${id}`),
  deleteSaved: (id) => request(`/investment/analyses/${id}`, { method: 'DELETE' }),
  loanSchedule: (principal, annualRate, termYears) => post('/investment/loan-schedule', { principal, annualRate, termYears }),
  profile: () => request('/investment/profile'),
  saveProfile: (p) => put('/investment/profile', p),
  capacity: () => request('/investment/capacity'),

  // Phase 2 시장 데이터
  regionSummary: (code, o = {}) => request(`/market/regions/${code}/summary${qs(o)}`),
  complexSummary: (id, o = {}) => request(`/market/complexes/${id}/summary${qs(o)}`),
  complexTrades: (id, months = 12) => request(`/market/complexes/${id}/trades${qs({ months })}`),
  complexes: (code, q) => request(`/market/regions/${code}/complexes${qs({ q })}`),
  collect: (regionCode, months, types) => post(`/market/collect${qs({ regionCode, months, types })}`),
  collectLogs: () => request('/market/collect/logs'),

  // Phase 3 관심·알림
  watches: () => request('/watch'),
  addWatch: (body) => post('/watch', body),
  updateWatch: (id, body) => put(`/watch/${id}`, body),
  deleteWatch: (id) => request(`/watch/${id}`, { method: 'DELETE' }),
  rules: (id) => request(`/watch/${id}/rules`),
  saveRules: (id, rules) => put(`/watch/${id}/rules`, rules),
  evaluate: () => post('/watch/evaluate'),
  alerts: (limit = 100) => request(`/alerts${qs({ limit })}`),
  unread: () => request('/alerts/unread-count'),
  readAlert: (id) => post(`/alerts/${id}/read`),
  readAll: () => post('/alerts/read-all'),

  // Phase 4 뉴스
  news: (o = {}) => request(`/news${qs(o)}`),
  collectRegionNews: (code) => post(`/news/collect/region/${code}`),
  collectNationalNews: () => post('/news/collect/national'),

  // Phase 5 원인 분석
  insight: (o) => request(`/insights/price-change${qs(o)}`),
};
