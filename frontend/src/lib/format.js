// 금액·비율 표시 유틸. 서버는 원 단위 금액, 소수 비율(0.04 = 4%)을 주고받는다.

/** 원 → "8억 2,562만원" 형태 */
export function won(value, { unit = true } = {}) {
  if (value === null || value === undefined || Number.isNaN(value)) return '-';
  const n = Math.round(Number(value));
  const sign = n < 0 ? '-' : '';
  const abs = Math.abs(n);
  const eok = Math.floor(abs / 100_000_000);
  const man = Math.floor((abs % 100_000_000) / 10_000);
  const suffix = unit ? '원' : '';
  if (eok > 0 && man > 0) return `${sign}${eok.toLocaleString('ko-KR')}억 ${man.toLocaleString('ko-KR')}만${suffix}`;
  if (eok > 0) return `${sign}${eok.toLocaleString('ko-KR')}억${suffix}`;
  if (man > 0) return `${sign}${man.toLocaleString('ko-KR')}만${suffix}`;
  return `${sign}${abs.toLocaleString('ko-KR')}${suffix}`;
}

/** 원 → "12,345,678원" */
export function wonExact(value) {
  if (value === null || value === undefined) return '-';
  return `${Math.round(value).toLocaleString('ko-KR')}원`;
}

/** 0.0412 → "4.12%" */
export function pct(ratio, digits = 2) {
  if (ratio === null || ratio === undefined || Number.isNaN(ratio)) return '-';
  return `${(ratio * 100).toFixed(digits)}%`;
}

/** 만원 입력값 → 원 */
export const manToWon = (man) => Math.round(Number(man || 0) * 10_000);

/** % 입력값 → 소수 */
export const pctToRatio = (p) => Number(p || 0) / 100;

/** 차트 축용 짧은 표기: 8.2억, 3,500만 */
export function short(value) {
  const abs = Math.abs(value);
  if (abs >= 100_000_000) return `${(value / 100_000_000).toFixed(1)}억`;
  if (abs >= 10_000) return `${Math.round(value / 10_000).toLocaleString('ko-KR')}만`;
  return `${value}`;
}
