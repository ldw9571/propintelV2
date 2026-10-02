import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../lib/api.js';
import { pct, short, won } from '../lib/format.js';
import { Term } from '../components/Glossary.jsx';
import { SourceLine } from '../components/Source.jsx';
import { AREA_PRESETS, Change, ErrorBox, Loading, RegionSelect, Trend } from '../components/Common.jsx';

export default function MarketPage() {
  const [params, setParams] = useSearchParams();
  const [regions, setRegions] = useState([]);
  const regionCode = params.get('regionCode') || '11560';
  const complexId = params.get('complexId');
  const [area, setArea] = useState(0);
  const [summary, setSummary] = useState(null);
  const [complexes, setComplexes] = useState([]);
  const [q, setQ] = useState('');
  const [trades, setTrades] = useState(null);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);

  const preset = AREA_PRESETS[area];
  const areaQ = { areaMin: preset.min, areaMax: preset.max, months: 24 };

  useEffect(() => { api.regions().then(setRegions).catch((e) => setError(e.message)); }, []);

  const load = () => {
    setSummary(null); setError('');
    const p = complexId ? api.complexSummary(complexId, areaQ) : api.regionSummary(regionCode, areaQ);
    p.then(setSummary).catch((e) => setError(e.message));
    if (complexId) api.complexTrades(complexId, 12).then(setTrades).catch(() => setTrades([]));
    else setTrades(null);
  };
  useEffect(load, [regionCode, complexId, area]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { api.complexes(regionCode, q).then(setComplexes).catch(() => setComplexes([])); }, [regionCode, q, msg]);

  async function collect(months) {
    setBusy(true); setMsg(''); setError('');
    try {
      const r = await api.collect(regionCode, months, 'SALE,RENT');
      const errs = r.months.filter((m) => m.error);
      setMsg(`${r.regionName} 최근 ${months}개월: 새로 저장 ${r.totalSaved.toLocaleString()}건` + (errs.length ? ` · 오류 ${errs.length}건 (${errs[0].error})` : ''));
      load();
    } catch (e) { setError(e.message); } finally { setBusy(false); }
  }

  async function watch(targetType, cid, label) {
    try {
      await api.addWatch({ targetType, regionCode, complexId: cid, label, areaMin: preset.min || null, areaMax: preset.max || null });
      setMsg('관심 목록에 추가했습니다. 대시보드와 관심 목록에서 확인하세요.');
    } catch (e) { setError(e.message); }
  }

  const s = summary?.stats;
  const region = regions.find((r) => r.code === regionCode);

  return (
    <>
      <div className="page-head">
        <div>
          <h1>시장 데이터</h1>
          <p>국토교통부 실거래(매매·전월세)로 가격, 거래량, 전세가, 전세가율, 신고가·하락 거래를 봅니다.</p>
        </div>
      </div>

      <div className="card toolbar">
        <div className="field"><label>지역</label>
          <RegionSelect regions={regions} value={regionCode} onlyCollectable onChange={(v) => setParams({ regionCode: v })} />
        </div>
        <div className="field"><label>평형</label>
          <select value={area} onChange={(e) => setArea(Number(e.target.value))}>{AREA_PRESETS.map((p, i) => <option key={i} value={i}>{p.label}</option>)}</select>
        </div>
        <div className="field"><label>실거래 수집 (국토부 API)</label>
          <div style={{ display: 'flex', gap: 6 }}>
            {[3, 12, 36].map((m) => <button key={m} className="secondary" disabled={busy} onClick={() => collect(m)}>{m}개월</button>)}
          </div>
        </div>
        {!complexId && <button className="ghost" onClick={() => watch('REGION', null, null)}>☆ 이 지역 관심 등록</button>}
      </div>
      {busy && <div className="notice info" style={{ marginTop: 12 }}>수집 중입니다. 기간이 길면 몇 분 걸릴 수 있습니다…</div>}
      {msg && <div className="notice info" style={{ marginTop: 12 }}>{msg}</div>}
      <div style={{ marginTop: 12 }}><ErrorBox error={error} /></div>

      <div className="analysis" style={{ gridTemplateColumns: 'minmax(0, 1fr) 300px' }}>
        <div>
          {complexId && (
            <div className="notice info" style={{ marginBottom: 12 }}>
              단지 보기 중: <b>{summary?.scopeName}</b> · <button className="link-btn" onClick={() => setParams({ regionCode })}>지역 전체로 돌아가기</button>
              {' · '}<button className="link-btn" onClick={() => watch('COMPLEX', Number(complexId), null)}>☆ 이 단지 관심 등록</button>
            </div>
          )}
          {!summary ? <Loading /> : <Summary summary={summary} isComplex={!!complexId} />}
          {trades && <TradesTable trades={trades} />}
        </div>

        <div className="card" style={{ position: 'sticky', top: 16 }}>
          <h3 style={{ marginBottom: 8 }}>{region?.displayName || regionCode} 단지</h3>
          <input placeholder="단지명 검색" value={q} onChange={(e) => setQ(e.target.value)} />
          <div style={{ marginTop: 8, maxHeight: 520, overflow: 'auto' }}>
            {complexes.length === 0 && <div className="small muted" style={{ padding: 8 }}>실거래를 수집하면 단지가 나타납니다.</div>}
            {complexes.map((c) => (
              <div key={c.id} className="list-item" style={{ padding: '8px 0' }}>
                <button className="link-btn" style={{ textAlign: 'left' }} onClick={() => setParams({ regionCode, complexId: c.id })}>
                  {c.name}<div className="small muted">{c.address}{c.buildYear ? ` · ${c.buildYear}년` : ''}</div>
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  );
}

function Summary({ summary, isComplex }) {
  const s = summary.stats;
  if (!s.refMonth) {
    return <div className="card empty"><h2>실거래 데이터가 없습니다</h2><p>위의 ‘실거래 수집’ 버튼으로 먼저 데이터를 모으세요. (MOLIT_API_KEY 필요)</p></div>;
  }
  const ch = (m) => s.changes.find((c) => c.months === m);
  return (
    <>
      <div className="card">
        <div className="card-head">
          <h3>{summary.scopeName} <span className="small muted">· 기준월 {s.refMonth} ({s.refSampleSize}건)</span></h3>
          <Link className="small" to={`/insight?${isComplex ? `complexId=${summary.complexId}` : `regionCode=${summary.regionCode}`}`}>가격 변화 원인 분석 →</Link>
        </div>
        <div className="kpi-grid">
          <Kpi label={<Term k="PRICE_PER_SQM">84㎡ 환산 평균가</Term>} value={won(s.currentPrice84)} sub={`실제 평균 거래가 ${won(s.currentAvgPrice)}`} />
          {[1, 3, 6, 12].map((m) => (
            <Kpi key={m} label={`${m === 12 ? '1년' : `${m}개월`} 변동`} value={<Change value={ch(m)?.rate} />}
              sub={ch(m)?.rate == null ? (ch(m)?.note || '-') : `${ch(m).baseMonth} 대비`} />
          ))}
          <Kpi label={<Term k="VOLUME">거래량</Term>} value={<Trend trend={s.volume.trend} ratio={s.volume.ratio} />}
            sub={`${s.volume.refCount}건 / 직전 6개월 평균 ${s.volume.baselineAvg?.toFixed(1) ?? '-'}건`} />
          <Kpi label="전세 84㎡ 환산" value={won(s.jeonse.price84)} sub={<>3개월 <Change value={s.jeonse.change3m} /></>} />
          <Kpi label={<Term k="JEONSE_RATIO">전세가율</Term>} value={s.jeonse.jeonseRatio == null ? '-' : pct(s.jeonse.jeonseRatio, 0)} sub={s.jeonse.note} />
          <Kpi label={<Term k="NEW_HIGH">신고가</Term>} value={`${s.newHighs.length}건`} sub="최근 3개월" />
          <Kpi label={<Term k="DROP_TRADE">하락 거래</Term>} value={`${s.drops.length}건`} sub="최근 3개월, 직전 대비 −5%↓" />
        </div>
        <ul className="small muted" style={{ margin: '12px 0 0', paddingLeft: 18 }}>{s.notes.map((n, i) => <li key={i}>{n}</li>)}</ul>
        <div style={{ marginTop: 8 }}><SourceLine source={summary.source} /></div>
        {summary.lastCollectedAt && <div className="small muted">마지막 수집 {summary.lastCollectedAt.slice(0, 16).replace('T', ' ')}</div>}
      </div>

      <div className="card">
        <div className="card-head">
          <h3>월별 84㎡ 환산가</h3>
          <div className="legend"><span><i style={{ background: 'var(--series-base)' }} />매매</span><span><i style={{ background: 'var(--series-conservative)' }} />전세</span></div>
        </div>
        <div style={{ height: 260 }}>
          <ResponsiveContainer>
            <LineChart data={s.monthly} margin={{ top: 8, right: 16, left: 8, bottom: 0 }}>
              <CartesianGrid stroke="var(--grid)" vertical={false} />
              <XAxis dataKey="month" tick={{ fill: 'var(--text-3)', fontSize: 11 }} tickLine={false} axisLine={{ stroke: 'var(--border)' }} interval="preserveStartEnd" />
              <YAxis tickFormatter={short} tick={{ fill: 'var(--text-3)', fontSize: 12 }} axisLine={false} tickLine={false} width={56} />
              <Tooltip content={<Tip />} />
              <Line dataKey="salePrice84" name="매매" stroke="var(--series-base)" strokeWidth={2} dot={{ r: 2 }} connectNulls />
              <Line dataKey="jeonsePrice84" name="전세" stroke="var(--series-conservative)" strokeWidth={2} dot={{ r: 2 }} connectNulls />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: 8 }}>월별 매매 거래량</h3>
        <div style={{ height: 180 }}>
          <ResponsiveContainer>
            <BarChart data={s.monthly} margin={{ top: 8, right: 16, left: 8, bottom: 0 }}>
              <CartesianGrid stroke="var(--grid)" vertical={false} />
              <XAxis dataKey="month" tick={{ fill: 'var(--text-3)', fontSize: 11 }} tickLine={false} axisLine={{ stroke: 'var(--border)' }} interval="preserveStartEnd" />
              <YAxis allowDecimals={false} tick={{ fill: 'var(--text-3)', fontSize: 12 }} axisLine={false} tickLine={false} width={40} />
              <Tooltip formatter={(v) => [`${v}건`, '매매 거래']} contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }} />
              <Bar dataKey="saleCount" fill="var(--series-base)" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
        <p className="small muted" style={{ margin: '6px 0 0' }}>최근 1~2개월은 신고 지연으로 거래량이 덜 채워져 있을 수 있습니다.</p>
      </div>

      <Notable title="최근 신고가" list={s.newHighs} refLabel="이전 최고" />
      <Notable title="최근 하락 거래" list={s.drops} refLabel="직전 거래" />
    </>
  );
}

function Kpi({ label, value, sub }) {
  return <div className="stat"><div className="label">{label}</div><div className="value" style={{ fontSize: '1.1rem' }}>{value}</div><div className="sub">{sub}</div></div>;
}

function Tip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  const p = payload[0].payload;
  return (
    <div className="chart-tip">
      <div className="t">{label}</div>
      <div className="r"><span>매매 (84㎡ 환산)</span><b>{won(p.salePrice84)}</b></div>
      <div className="r"><span>매매 거래</span><b>{p.saleCount}건</b></div>
      <div className="r"><span>전세 (84㎡ 환산)</span><b>{won(p.jeonsePrice84)}</b></div>
      <div className="r"><span>전세 거래</span><b>{p.jeonseCount}건</b></div>
    </div>
  );
}

function Notable({ title, list, refLabel }) {
  return (
    <div className="card">
      <h3 style={{ marginBottom: 8 }}>{title} <span className="small muted">{list.length}건</span></h3>
      {list.length === 0 ? <div className="small muted">없음</div> : (
        <div className="table-wrap" style={{ maxHeight: 320 }}>
          <table>
            <thead><tr><th>단지</th><th>면적·층</th><th>거래가</th><th>{refLabel}</th><th>차이</th><th>날짜</th></tr></thead>
            <tbody>
              {list.slice(0, 30).map((t, i) => (
                <tr key={i}>
                  <td>{t.complexName}</td><td>{Math.round(t.area)}㎡ · {t.floor}층</td><td>{won(t.price)}</td>
                  <td>{won(t.referencePrice)}<div className="small muted">{t.referenceDate}</div></td>
                  <td><Change value={t.diffRate} /></td><td className="small">{t.date}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function TradesTable({ trades }) {
  return (
    <div className="card" style={{ marginTop: 16 }}>
      <h3 style={{ marginBottom: 8 }}>최근 12개월 거래 <span className="small muted">{trades.length}건</span></h3>
      <div className="table-wrap" style={{ maxHeight: 420 }}>
        <table>
          <thead><tr><th>날짜</th><th>구분</th><th>면적</th><th>층</th><th>매매가</th><th>보증금</th><th>월세</th><th>계약</th></tr></thead>
          <tbody>
            {trades.map((t, i) => (
              <tr key={i}>
                <td>{t.date}</td><td>{t.kind}</td><td>{Number(t.area).toFixed(1)}㎡</td><td>{t.floor}</td>
                <td>{t.price ? won(t.price) : '-'}</td><td>{t.deposit ? won(t.deposit) : '-'}</td>
                <td>{t.monthlyRent ? won(t.monthlyRent) : '-'}</td><td>{t.contractType || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
