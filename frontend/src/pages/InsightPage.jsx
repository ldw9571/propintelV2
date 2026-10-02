import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../lib/api.js';
import { won } from '../lib/format.js';
import { SourceLine } from '../components/Source.jsx';
import { AREA_PRESETS, Change, ErrorBox, Loading, RegionSelect } from '../components/Common.jsx';

const STATUS = {
  OBSERVED: ['관찰됨', 'up'],
  NOT_OBSERVED: ['해당 없음', 'neutral'],
  NO_DATA: ['데이터 없음', 'unverified'],
};

export default function InsightPage() {
  const [params, setParams] = useSearchParams();
  const [regions, setRegions] = useState([]);
  const regionCode = params.get('regionCode') || (params.get('complexId') ? '' : '11560');
  const complexId = params.get('complexId') || '';
  const [months, setMonths] = useState(3);
  const [area, setArea] = useState(0);
  const [useAi, setUseAi] = useState(false);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => { api.regions().then(setRegions).catch(() => {}); }, []);
  useEffect(() => {
    setData(null); setError('');
    const p = AREA_PRESETS[area];
    api.insight({ regionCode: complexId ? undefined : regionCode, complexId: complexId || undefined, months, areaMin: p.min, areaMax: p.max, ai: useAi })
      .then(setData).catch((e) => setError(e.message));
  }, [regionCode, complexId, months, area, useAi]);

  return (
    <>
      <div className="page-head">
        <div>
          <h1>가격 변화 원인 분석</h1>
          <p>“왜 이 지역 가격이 움직였는가?”를 저장된 데이터로 확인합니다. 근거가 없으면 원인을 만들지 않습니다.</p>
        </div>
      </div>
      <div className="card toolbar">
        {!complexId && <div className="field"><label>지역</label><RegionSelect regions={regions} value={regionCode} onlyCollectable onChange={(v) => setParams({ regionCode: v })} /></div>}
        {complexId && <div className="field"><label>단지</label><div>{data?.scopeName || '…'} <button className="link-btn small" onClick={() => setParams({ regionCode: data?.regionCode || '11560' })}>지역으로 보기</button></div></div>}
        <div className="field"><label>기간</label>
          <select value={months} onChange={(e) => setMonths(Number(e.target.value))}>{[1, 3, 6, 12].map((m) => <option key={m} value={m}>최근 {m}개월</option>)}</select>
        </div>
        <div className="field"><label>평형</label>
          <select value={area} onChange={(e) => setArea(Number(e.target.value))}>{AREA_PRESETS.map((p, i) => <option key={i} value={i}>{p.label}</option>)}</select>
        </div>
        <label className="check" style={{ alignSelf: 'center' }}><input type="checkbox" checked={useAi} onChange={(e) => setUseAi(e.target.checked)} /> AI로 문장 정리 (확인된 데이터만 사용)</label>
      </div>
      <div style={{ marginTop: 12 }}><ErrorBox error={error} /></div>
      {!data && !error && <Loading />}
      {data && (
        <>
          <div className="card">
            <h3 style={{ marginBottom: 6 }}>{data.headline}</h3>
            {data.changeRate != null && (
              <div className="small muted">비교 표본: 기준 {data.baseSample}건 → 현재 {data.currentSample}건 · {data.lastYearNote}</div>
            )}
            <div className={`notice ${data.factors.some((f) => f.status === 'OBSERVED') ? 'info' : 'warn'}`} style={{ marginTop: 12, fontSize: '0.95rem' }}>
              <b>{data.conclusion}</b>
            </div>
            {data.aiNarrative && (
              <div className="notice info" style={{ marginTop: 10 }}>
                <div className="small muted" style={{ marginBottom: 4 }}>AI 정리 ({data.aiProvider}) — 아래 요인 목록만 입력했습니다</div>
                {data.aiNarrative}
              </div>
            )}
            {useAi && !data.aiNarrative && <div className="small muted" style={{ marginTop: 8 }}>AI가 설정되지 않았거나(AI_PROVIDER) 응답이 없어 규칙 기반 결과만 표시합니다.</div>}
          </div>

          {(data.upSide.length > 0 || data.downSide.length > 0) && (
            <div className="grid-2" style={{ marginTop: 16 }}>
              <div className="card"><h3 style={{ marginBottom: 8 }}>상승 방향과 함께 나타난 요인</h3>
                {data.upSide.length ? <ul className="small" style={{ paddingLeft: 18, margin: 0 }}>{data.upSide.map((x, i) => <li key={i}>{x}</li>)}</ul> : <div className="small muted">없음</div>}
              </div>
              <div className="card"><h3 style={{ marginBottom: 8 }}>하락 방향과 함께 나타난 요인</h3>
                {data.downSide.length ? <ul className="small" style={{ paddingLeft: 18, margin: 0 }}>{data.downSide.map((x, i) => <li key={i}>{x}</li>)}</ul> : <div className="small muted">없음</div>}
              </div>
            </div>
          )}

          <div className="card" style={{ marginTop: 16 }}>
            <h3 style={{ marginBottom: 4 }}>확인한 항목 ({data.period})</h3>
            {data.factors.map((f, i) => (
              <div className="factor" key={i}>
                <div><span className={`badge ${STATUS[f.status][1]}`}>{STATUS[f.status][0]}</span></div>
                <div>
                  <div style={{ fontWeight: 600 }}>{f.name}</div>
                  <div className="small">{f.summary}</div>
                  {f.evidence?.length > 0 && (
                    <ul className="small" style={{ margin: '6px 0 0', paddingLeft: 18 }}>
                      {f.evidence.map((e, j) => <li key={j}>{e.date} · {e.url ? <a href={e.url} target="_blank" rel="noreferrer">{e.text}</a> : e.text}</li>)}
                    </ul>
                  )}
                  {f.source && <div className="small muted">출처: {f.source}</div>}
                </div>
              </div>
            ))}
          </div>
          <div className="card">
            <ul className="small muted" style={{ margin: 0, paddingLeft: 18 }}>{data.cautions.map((c, i) => <li key={i}>{c}</li>)}</ul>
            <div style={{ marginTop: 8 }}><SourceLine source={data.marketSource} /></div>
            {data.basePrice84 != null && <div className="small muted">84㎡ 환산 {won(data.basePrice84)} → {won(data.currentPrice84)} (<Change value={data.changeRate} />)</div>}
          </div>
        </>
      )}
    </>
  );
}
