import { useEffect, useState } from 'react';
import { api } from '../lib/api.js';
import { pct, won } from '../lib/format.js';
import { SourceLine, VerifiedBadge } from '../components/Source.jsx';

const REGION_LABEL = { REGULATED: '규제지역', METRO_NON_REGULATED: '수도권 비규제', NON_METRO: '지방' };
const BORROWER_LABEL = { FIRST_TIME: '생애최초', NO_HOUSE: '무주택', ONE_HOUSE_DISPOSING: '1주택(처분조건)', ONE_HOUSE: '1주택(추가구입)', MULTI_HOUSE: '2주택+' };
const TABLE_LABEL = { BROKERAGE_SALE: '중개보수 (매매)', CGT: '양도소득세 기본세율', PROPERTY_TAX: '재산세 세율' };

export default function PolicyPage() {
  const [asOf, setAsOf] = useState('');
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);
  const [regions, setRegions] = useState([]);
  useEffect(() => { api.regions().then(setRegions).catch(() => {}); }, []);

  const load = () => api.policies(asOf || undefined).then(setData).catch((e) => setError(e.message));
  useEffect(() => { load(); /* eslint-disable-next-line */ }, [asOf]);

  if (error) return <div className="notice bad">{error}</div>;
  if (!data) return <div className="muted">불러오는 중…</div>;
  const fr = data.freshness;

  return (
    <>
      <div className="page-head">
        <div>
          <h1>기준 데이터 (규제·세율)</h1>
          <p>계산에 쓰이는 모든 값과 출처·기준일입니다. 값은 코드가 아니라 DB에서 관리됩니다.</p>
        </div>
        <div className="field" style={{ margin: 0 }}>
          <label>조회 기준일</label>
          <input type="date" value={asOf} onChange={(e) => setAsOf(e.target.value)} />
        </div>
      </div>

      <div className={`notice ${fr.unverified > 0 || fr.stale > 0 ? 'warn' : 'info'}`} style={{ marginBottom: 16 }}>
        전체 {fr.total}건 중 <b>미검증 {fr.unverified}건</b>, {fr.staleAfterDays}일 이상 미갱신 {fr.stale}건.
        미검증 데이터는 2차 출처(언론·블로그 요약)로 입력된 값이므로, 금융위원회·국토교통부·법제처 원문과 대조한 뒤 ‘검증됨’으로 바꾸세요.
      </div>

      <div className="card">
        <h3 style={{ marginBottom: 10 }}>LTV 규칙 (지역 × 차주)</h3>
        <div className="table-wrap">
          <table>
            <thead><tr><th>지역</th><th>차주</th><th>가능</th><th>LTV</th><th>전입 의무</th><th>메모</th><th>검증</th></tr></thead>
            <tbody>
              {data.loanRules.map((r, i) => (
                <tr key={i}>
                  <td>{REGION_LABEL[r.regionType]}</td><td>{BORROWER_LABEL[r.borrowerType]}</td>
                  <td>{r.allowed ? '가능' : '불가'}</td><td>{r.allowed ? pct(r.ltvRatio, 0) : '-'}</td>
                  <td>{r.requiresMoveIn ? '있음' : '-'}</td>
                  <td style={{ whiteSpace: 'normal', textAlign: 'left', minWidth: 220 }} className="small">{r.note}</td>
                  <td><VerifiedBadge source={r.source} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div style={{ marginTop: 10 }}><SourceLine source={data.loanRules[0]?.source} /></div>
      </div>

      <div className="grid-2" style={{ marginTop: 16 }}>
        <div className="card">
          <h3 style={{ marginBottom: 10 }}>주택가격별 주담대 한도</h3>
          <table>
            <thead><tr><th>지역</th><th>주택가격</th><th>최대 한도</th></tr></thead>
            <tbody>
              {data.capTiers.map((c, i) => (
                <tr key={i}><td>{REGION_LABEL[c.regionType]}</td>
                  <td>{c.priceOver ? `${won(c.priceOver)} 초과` : ''}{c.priceUpTo ? ` ${won(c.priceUpTo)} 이하` : (c.priceOver ? '' : '전체')}</td>
                  <td>{won(c.maxAmount)}</td></tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 10 }}><SourceLine source={data.capTiers[0]?.source} /></div>
        </div>
        <div className="card">
          <h3 style={{ marginBottom: 10 }}>DSR · 스트레스 금리</h3>
          <table>
            <thead><tr><th>지역</th><th>DSR 한도</th><th>스트레스 금리</th><th>변동/혼합/주기</th></tr></thead>
            <tbody>
              {data.dsrRules.map((d, i) => (
                <tr key={i}><td>{REGION_LABEL[d.regionType]}</td><td>{pct(d.dsrLimit, 0)}</td><td>+{pct(d.stressRate, 1)}p</td>
                  <td>{pct(d.variableRatio, 0)} / {pct(d.mixedRatio, 0)} / {pct(d.periodicRatio, 0)}</td></tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 10 }}><SourceLine source={data.dsrRules[0]?.source} /></div>
        </div>
      </div>

      <div className="grid-3" style={{ marginTop: 16 }}>
        {Object.entries(data.brackets).map(([code, list]) => (
          <div className="card" key={code}>
            <h3 style={{ marginBottom: 10 }}>{TABLE_LABEL[code] || code}</h3>
            <table>
              <thead><tr><th>구간</th><th>세율</th><th>{code === 'BROKERAGE_SALE' ? '한도' : '누진공제'}</th></tr></thead>
              <tbody>
                {list.map((b, i) => (
                  <tr key={i}>
                    <td>{b.upTo ? `~${won(b.upTo)}` : `${won(b.over)} 초과`}</td>
                    <td>{pct(b.rate, 2)}</td>
                    <td>{code === 'BROKERAGE_SALE' ? (b.cap ? won(b.cap) : '-') : won(b.deduction)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div style={{ marginTop: 10 }}><SourceLine source={list[0]?.source} /></div>
          </div>
        ))}
      </div>

      <div className="card" style={{ marginTop: 16 }}>
        <h3 style={{ marginBottom: 10 }}>지역 지정 (규제지역·토지거래허가구역)</h3>
        <div className="table-wrap" style={{ maxHeight: 360 }}>
          <table>
            <thead><tr><th>지역</th><th>코드</th><th>수도권</th><th>규제지역</th><th>토허구역</th><th>실거래 수집</th><th>출처 / 기준일</th></tr></thead>
            <tbody>
              {regions.map((r) => (
                <tr key={r.code}>
                  <td>{r.displayName}</td><td className="small">{r.code}</td><td>{r.metro ? '○' : '-'}</td>
                  <td>{r.regulated ? '○' : '-'}</td><td>{r.landPermitZone ? '○' : '-'}</td><td>{r.collectable ? '가능' : '불가(대표코드)'}</td>
                  <td style={{ textAlign: 'left', whiteSpace: 'normal', minWidth: 260 }} className="small"><VerifiedBadge source={r.source} /> {r.source.sourceName} <span className="muted">({r.source.baseDate})</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card" style={{ marginTop: 16 }}>
        <h3 style={{ marginBottom: 10 }}>세율·기준금액 파라미터</h3>
        <div className="table-wrap">
          <table>
            <thead><tr><th>키</th><th>설명</th><th>값</th><th>출처 / 기준일</th><th></th></tr></thead>
            <tbody>
              {data.parameters.map((p) => (
                <tr key={p.id}>
                  <td className="small">{p.key}</td>
                  <td style={{ textAlign: 'left', whiteSpace: 'normal', minWidth: 200 }}>{p.description}</td>
                  <td>{p.value >= 100 ? won(p.value) : p.value}</td>
                  <td style={{ textAlign: 'left', whiteSpace: 'normal', minWidth: 240 }} className="small">
                    <VerifiedBadge source={p.source} /> {p.source.sourceName} <span className="muted">({p.source.baseDate})</span>
                  </td>
                  <td><button className="ghost" onClick={() => setEditing({ ...p, sourceName: p.source.sourceName, sourceUrl: p.source.sourceUrl || '', baseDate: p.source.baseDate, verified: p.source.verified, note: p.source.note || '' })}>수정</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {editing && <EditParam p={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); load(); }} />}
    </>
  );
}

function EditParam({ p, onClose, onSaved }) {
  const [f, setF] = useState(p);
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
  async function save() {
    try {
      await api.updateParam(p.id, { value: Number(f.value), sourceName: f.sourceName, sourceUrl: f.sourceUrl || null, baseDate: f.baseDate, verified: f.verified, note: f.note || null });
      onSaved();
    } catch (e) { setErr(e.message); }
  }
  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', display: 'grid', placeItems: 'center', zIndex: 100, padding: 16 }} onClick={onClose}>
      <div className="card" style={{ width: 480, maxWidth: '100%' }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginBottom: 4 }}>{p.key}</h3>
        <p className="small muted" style={{ marginTop: 0 }}>{p.description}</p>
        <div className="field"><label>값</label><input type="number" step="any" value={f.value} onChange={set('value')} /></div>
        <div className="field"><label>출처 (필수)</label><input value={f.sourceName} onChange={set('sourceName')} /></div>
        <div className="field"><label>출처 URL</label><input value={f.sourceUrl} onChange={set('sourceUrl')} /></div>
        <div className="row">
          <div className="field"><label>기준일 (필수)</label><input type="date" value={f.baseDate} onChange={set('baseDate')} /></div>
          <label className="check" style={{ alignSelf: 'end' }}><input type="checkbox" checked={f.verified} onChange={set('verified')} /> 공식 원문 확인함</label>
        </div>
        <div className="field"><label>메모</label><input value={f.note} onChange={set('note')} /></div>
        {err && <div className="notice bad" style={{ marginBottom: 10 }}>{err}</div>}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="ghost" onClick={onClose}>취소</button>
          <button className="primary" onClick={save}>저장</button>
        </div>
      </div>
    </div>
  );
}
