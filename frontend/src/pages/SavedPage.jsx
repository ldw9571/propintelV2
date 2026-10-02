import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api.js';
import { won } from '../lib/format.js';

export default function SavedPage() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const load = () => api.listSaved().then(setItems).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  async function open(id) {
    const d = await api.getSaved(id);
    navigate('/analysis', { state: { savedInput: d.input } });
  }
  async function remove(id) {
    await api.deleteSaved(id);
    load();
  }

  return (
    <>
      <div className="page-head"><div><h1>저장한 분석</h1><p>다시 열면 <b>현재 기준 데이터</b>로 재계산합니다. 저장 당시 기준일도 함께 표시됩니다.</p></div></div>
      {error && <div className="notice bad">{error}</div>}
      <div className="card">
        {items?.length === 0 && <div className="empty">아직 저장한 분석이 없습니다.</div>}
        {items?.map((s) => (
          <div key={s.id} className="list-item">
            <div>
              <div style={{ fontWeight: 600 }}>{s.title}</div>
              <div className="small muted">{won(s.price)} · {s.holdingYears}년 보유 · 저장 {s.createdAt?.slice(0, 16).replace('T', ' ')} · 계산 기준일 {s.policyAsOf}</div>
            </div>
            <div style={{ display: 'flex', gap: 6 }}>
              <button className="secondary" onClick={() => open(s.id)}>다시 열기</button>
              <button className="ghost" onClick={() => remove(s.id)}>삭제</button>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
