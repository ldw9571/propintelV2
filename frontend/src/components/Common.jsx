import { useMemo } from 'react';
import { pct } from '../lib/format.js';

/** 시·도별로 묶인 지역 선택 */
export function RegionSelect({ regions, value, onChange, onlyCollectable, allowEmpty, emptyLabel = '전체' }) {
  const grouped = useMemo(() => {
    const g = {};
    regions.filter((r) => !onlyCollectable || r.collectable).forEach((r) => { (g[r.sido] ||= []).push(r); });
    return g;
  }, [regions, onlyCollectable]);
  return (
    <select value={value ?? ''} onChange={(e) => onChange(e.target.value)}>
      {allowEmpty && <option value="">{emptyLabel}</option>}
      {Object.entries(grouped).map(([sido, list]) => (
        <optgroup key={sido} label={sido}>
          {list.map((r) => (
            <option key={r.code} value={r.code}>{r.sigungu || r.sido}{r.regulated ? ' · 규제' : ''}</option>
          ))}
        </optgroup>
      ))}
    </select>
  );
}

/** 상승 빨강 / 하락 파랑 (국내 관례) + 부호 표시 */
export function Change({ value, digits = 1, suffix }) {
  if (value === null || value === undefined) return <span className="muted">-</span>;
  const cls = value > 0.0005 ? 'up' : value < -0.0005 ? 'down' : '';
  const sign = value > 0 ? '+' : '';
  return <span className={`chg ${cls}`}>{sign}{pct(value, digits)}{suffix}</span>;
}

export function Flag({ flag }) {
  if (!flag) return null;
  return <span className={`badge ${flag === '급등' ? 'up' : 'down'}`}>{flag === '급등' ? '▲ 급등' : '▼ 급락'}</span>;
}

export function Trend({ trend, ratio }) {
  if (!trend || trend === '비교 불가' || trend === '데이터 없음') return <span className="muted">{trend || '-'}</span>;
  const cls = trend === '급증' ? 'up' : trend === '급감' ? 'down' : 'neutral';
  return <span className={`badge ${cls}`}>{trend}{ratio != null ? ` ${ratio.toFixed(1)}배` : ''}</span>;
}

export function Loading({ text = '불러오는 중…' }) {
  return <div className="muted" style={{ padding: 20 }}>{text}</div>;
}

export function ErrorBox({ error }) {
  if (!error) return null;
  return <div className="notice bad error" style={{ marginBottom: 16 }}>{String(error)}</div>;
}

export const AREA_PRESETS = [
  { label: '전체', min: '', max: '' },
  { label: '소형 (~60㎡)', min: '', max: 60 },
  { label: '59㎡대', min: 55, max: 62 },
  { label: '84㎡대', min: 80, max: 86 },
  { label: '중대형 (85㎡~)', min: 85, max: '' },
];
