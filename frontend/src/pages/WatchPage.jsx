import { useEffect, useState } from 'react';
import { api } from '../lib/api.js';
import { AREA_PRESETS, ErrorBox, Loading, RegionSelect } from '../components/Common.jsx';
import { WatchTable } from './HomePage.jsx';

export default function WatchPage() {
  const [regions, setRegions] = useState([]);
  const [list, setList] = useState(null);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ targetType: 'REGION', regionCode: '11560', complexId: '', area: 0, label: '' });
  const [complexes, setComplexes] = useState([]);
  const [q, setQ] = useState('');

  const load = () => api.watches().then(setList).catch((e) => setError(e.message));
  useEffect(() => { api.regions().then(setRegions).catch(() => {}); load(); }, []);
  useEffect(() => {
    if (form.targetType === 'COMPLEX') api.complexes(form.regionCode, q).then(setComplexes).catch(() => setComplexes([]));
  }, [form.targetType, form.regionCode, q]);

  async function add(e) {
    e.preventDefault(); setError(''); setMsg('');
    const p = AREA_PRESETS[form.area];
    try {
      await api.addWatch({
        targetType: form.targetType, regionCode: form.regionCode,
        complexId: form.targetType === 'COMPLEX' ? Number(form.complexId) || null : null,
        label: form.label || null, areaMin: p.min || null, areaMax: p.max || null,
      });
      setMsg('추가했습니다. 기본 알림 조건 9개가 함께 만들어졌습니다(‘조건’에서 수정).');
      load();
    } catch (err) { setError(err.message); }
  }

  async function evaluate() {
    setError(''); setMsg('');
    try {
      const r = await api.evaluate();
      setMsg(`관심 ${r.watches}건을 평가해 새 알림 ${r.alertsCreated}건이 생겼습니다.${r.errors.length ? ` 오류: ${r.errors.join(', ')}` : ''}`);
      load();
    } catch (e) { setError(e.message); }
  }

  async function remove(w) {
    if (!window.confirm(`'${w.label}'을(를) 관심 목록에서 지울까요? 관련 알림도 함께 지워집니다.`)) return;
    await api.deleteWatch(w.id).catch((e) => setError(e.message));
    load();
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>관심 목록</h1>
          <p>관심 지역·아파트(평형)를 등록하면 매일 실거래·뉴스를 모으고, 설정한 조건에 맞으면 알림을 만듭니다.</p>
        </div>
        <button className="secondary" onClick={evaluate}>지금 알림 조건 확인</button>
      </div>

      <form className="card toolbar" onSubmit={add}>
        <div className="field"><label>종류</label>
          <select value={form.targetType} onChange={(e) => setForm({ ...form, targetType: e.target.value })}>
            <option value="REGION">지역</option><option value="COMPLEX">아파트 단지</option>
          </select>
        </div>
        <div className="field"><label>지역</label><RegionSelect regions={regions} value={form.regionCode} onChange={(v) => setForm({ ...form, regionCode: v, complexId: '' })} /></div>
        {form.targetType === 'COMPLEX' && (
          <div className="field" style={{ minWidth: 260 }}><label>단지 (수집된 실거래 기준)</label>
            <input placeholder="단지명 검색" value={q} onChange={(e) => setQ(e.target.value)} style={{ marginBottom: 4 }} />
            <select value={form.complexId} onChange={(e) => setForm({ ...form, complexId: e.target.value })}>
              <option value="">선택하세요 ({complexes.length}개)</option>
              {complexes.map((c) => <option key={c.id} value={c.id}>{c.name} · {c.address}</option>)}
            </select>
          </div>
        )}
        <div className="field"><label>평형</label>
          <select value={form.area} onChange={(e) => setForm({ ...form, area: Number(e.target.value) })}>{AREA_PRESETS.map((p, i) => <option key={i} value={i}>{p.label}</option>)}</select>
        </div>
        <div className="field"><label>이름 (선택)</label><input value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} placeholder="자동" /></div>
        <button className="primary" type="submit">추가</button>
      </form>
      {msg && <div className="notice info" style={{ marginTop: 12 }}>{msg}</div>}
      <div style={{ marginTop: 12 }}><ErrorBox error={error} /></div>

      {list === null ? <Loading /> : (
        <>
          {['REGION', 'COMPLEX'].map((k) => {
            const rows = list.filter((w) => w.targetType === k);
            if (!rows.length) return null;
            return (
              <div className="card" key={k}>
                <h3 style={{ marginBottom: 10 }}>{k === 'REGION' ? '관심 지역' : '관심 아파트'}</h3>
                <WatchTable rows={rows} kind={k} />
                <div style={{ marginTop: 10, display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                  {rows.map((w) => (
                    <span key={w.id} className="badge neutral" style={{ gap: 6 }}>
                      {w.label}
                      <button className="link-btn small" onClick={() => setEditing(w)}>조건</button>
                      <button className="link-btn small" onClick={() => remove(w)}>삭제</button>
                    </span>
                  ))}
                </div>
              </div>
            );
          })}
          {list.length === 0 && <div className="card empty">아직 관심 항목이 없습니다.</div>}
        </>
      )}
      {editing && <RulesModal watch={editing} onClose={() => setEditing(null)} />}
    </>
  );
}

function RulesModal({ watch, onClose }) {
  const [rules, setRules] = useState(null);
  const [err, setErr] = useState('');
  useEffect(() => { api.rules(watch.id).then(setRules).catch((e) => setErr(e.message)); }, [watch.id]);
  const isRatio = (t) => ['PRICE_UP_1M', 'PRICE_UP_3M', 'PRICE_DOWN_1M', 'PRICE_DOWN_3M', 'ABOVE_AVERAGE'].includes(t);
  const set = (i, patch) => setRules(rules.map((r, j) => (i === j ? { ...r, ...patch } : r)));

  async function save() {
    try {
      await api.saveRules(watch.id, rules.map((r) => ({ ruleType: r.ruleType, threshold: Number(r.threshold), enabled: r.enabled })));
      onClose();
    } catch (e) { setErr(e.message); }
  }

  return (
    <div className="modal-bg" onClick={onClose}>
      <div className="card modal" onClick={(e) => e.stopPropagation()}>
        <h3>알림 조건 — {watch.label}</h3>
        <p className="small muted">조건에 맞으면 알림을 만듭니다. 같은 기준월·같은 거래로는 한 번만 알립니다.</p>
        {!rules ? <Loading /> : (
          <table>
            <thead><tr><th>사용</th><th>조건</th><th>기준값</th></tr></thead>
            <tbody>
              {rules.map((r, i) => (
                <tr key={r.ruleType}>
                  <td><input type="checkbox" checked={r.enabled} onChange={(e) => set(i, { enabled: e.target.checked })} /></td>
                  <td style={{ textAlign: 'left' }}>{r.label}<div className="small muted">{r.thresholdHelp}</div></td>
                  <td style={{ width: 130 }}>
                    <div className="input-unit">
                      <input type="number" step="any" value={isRatio(r.ruleType) ? Math.round(r.threshold * 1000) / 10 : r.threshold}
                        onChange={(e) => set(i, { threshold: isRatio(r.ruleType) ? Number(e.target.value) / 100 : Number(e.target.value) })} />
                      <span>{isRatio(r.ruleType) ? '%' : r.ruleType.startsWith('VOLUME') ? '배' : '건'}</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {err && <div className="notice bad" style={{ marginTop: 10 }}>{err}</div>}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 12 }}>
          <button className="ghost" onClick={onClose}>취소</button>
          <button className="primary" onClick={save}>저장</button>
        </div>
      </div>
    </div>
  );
}
