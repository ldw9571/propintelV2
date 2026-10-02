import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api.js';
import { ErrorBox, Loading } from '../components/Common.jsx';

export default function AlertsPage() {
  const [alerts, setAlerts] = useState(null);
  const [error, setError] = useState('');
  const load = () => api.alerts(200).then(setAlerts).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  async function read(a) { if (!a.read) { await api.readAlert(a.id).catch(() => {}); load(); } }
  async function readAll() { await api.readAll().catch((e) => setError(e.message)); load(); }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>알림</h1>
          <p>급등·급락·거래량·신고가·중요 뉴스 알림입니다. 각 알림에 어디서·얼마나·언제부터·거래량·신고가·관련 뉴스를 함께 담았습니다.</p>
        </div>
        <button className="secondary" onClick={readAll}>모두 읽음</button>
      </div>
      <ErrorBox error={error} />
      {alerts === null ? <Loading /> : alerts.length === 0 ? (
        <div className="card empty">아직 알림이 없습니다. <Link to="/watch">관심 목록</Link>에서 항목을 등록하고 ‘지금 알림 조건 확인’을 눌러 보세요.</div>
      ) : alerts.map((a) => <AlertCard key={a.id} a={a} onOpen={() => read(a)} />)}
    </>
  );
}

function AlertCard({ a, onOpen }) {
  const [open, setOpen] = useState(false);
  const d = a.detail;
  return (
    <div className={`alert-item ${a.read ? '' : 'unread'}`}>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
        <span className={`badge ${a.severity === 'UP' ? 'up' : a.severity === 'DOWN' ? 'down' : 'neutral'}`}>{a.ruleLabel}</span>
        <span className="small muted">{a.triggeredAt?.slice(0, 16).replace('T', ' ')}</span>
        {!a.read && <span className="badge neutral">NEW</span>}
      </div>
      <div style={{ fontWeight: 700, marginTop: 6 }}>{a.title}</div>
      <button className="link-btn small" onClick={() => { setOpen(!open); onOpen(); }}>{open ? '접기' : '자세히'}</button>
      {open && d && (
        <>
          <dl className="facts">
            <dt>어디서</dt><dd>{d.where}</dd>
            <dt>얼마나</dt><dd>{d.howMuch}</dd>
            <dt>언제부터</dt><dd>{d.since}</dd>
            <dt>거래량</dt><dd>{d.volume}</dd>
            <dt>신고가</dt><dd>{d.newHigh}</dd>
            <dt>관련 뉴스</dt>
            <dd>
              <div className="small muted">{d.newsNote}</div>
              {d.news?.map((n) => (
                <div key={n.id}>{n.mock && <span className="badge mock">샘플</span>} <a href={n.url} target="_blank" rel="noreferrer">{n.title}</a> <span className="small muted">{n.publishedAt?.slice(0, 10)} · {n.category}</span></div>
              ))}
            </dd>
            <dt>데이터</dt><dd className="small">{d.dataSource}</dd>
          </dl>
          <div className="small muted" style={{ marginTop: 8 }}>{d.caution}</div>
        </>
      )}
    </div>
  );
}
