import { useEffect, useState } from 'react';
import { api } from '../lib/api.js';
import { ErrorBox, Loading, RegionSelect } from '../components/Common.jsx';

const CATEGORIES = [
  ['', '전체'], ['REGION', '지역 일반'], ['REDEVELOPMENT', '재건축·재개발'], ['TRANSPORT', '교통·개발'],
  ['SUPPLY', '입주·분양·공급'], ['POLICY', '정책·규제'], ['RATE', '금리·대출'],
];

export function NewsList({ news, compact }) {
  if (!news?.length) return <div className="muted small" style={{ padding: '8px 0' }}>뉴스가 없습니다.</div>;
  return (
    <div>
      {news.map((n) => (
        <div key={n.id} className="news-item">
          <a className="title" href={n.url} target="_blank" rel="noreferrer">{n.title}</a>
          <div className="news-meta">
            {n.mock && <span className="badge mock">샘플(Mock)</span>}
            <span className="badge neutral">{n.categoryLabel}</span>
            <span>{n.publisher || '언론사 미상'}</span>
            <span>{n.publishedAt?.slice(0, 16).replace('T', ' ')}</span>
          </div>
          {!compact && (
            <>
              {n.summary && <div className="small" style={{ marginTop: 6 }}><b>핵심 내용</b> {n.summary}</div>}
              {n.impactNote && <div className="small" style={{ marginTop: 3, color: 'var(--text-2)' }}><b>지역 관련 언급</b> {n.impactNote}</div>}
              <div className="news-meta">
                <span>출처 {n.sourceName}</span>
                <span>요약 방식: {n.summaryMethod === 'AI' ? 'AI (제목·요약문만 사용)' : '원문 요약문 발췌'}</span>
                {n.keyword && <span>검색어 “{n.keyword}”</span>}
              </div>
            </>
          )}
        </div>
      ))}
    </div>
  );
}

export default function NewsPage() {
  const [regions, setRegions] = useState([]);
  const [regionCode, setRegionCode] = useState('');
  const [category, setCategory] = useState('');
  const [days, setDays] = useState(7);
  const [news, setNews] = useState(null);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { api.regions().then(setRegions).catch((e) => setError(e.message)); }, []);
  const load = () => api.news({ regionCode, category, days, limit: 100 }).then(setNews).catch((e) => setError(e.message));
  useEffect(() => { load(); /* eslint-disable-next-line */ }, [regionCode, category, days]);

  async function collect() {
    setBusy(true); setMsg(''); setError('');
    try {
      const r = regionCode ? await api.collectRegionNews(regionCode) : await api.collectNationalNews();
      setMsg(`${r.provider}: 검색어 ${r.queries}개, 새 기사 ${r.saved}건 저장, 중복 ${r.duplicates}건${r.errors.length ? `, 오류 ${r.errors.length}건` : ''}`);
      load();
    } catch (e) { setError(e.message); } finally { setBusy(false); }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>부동산 뉴스</h1>
          <p>관심 지역(부동산·재개발·재건축·교통·입주·분양·정비사업)과 정책·금리·공급 뉴스를 모읍니다. 요약은 기사 제목·요약문에 있는 내용만 씁니다.</p>
        </div>
      </div>
      <div className="card toolbar">
        <div className="field"><label>지역</label><RegionSelect regions={regions} value={regionCode} onChange={setRegionCode} allowEmpty emptyLabel="전체 (정책·금리 포함)" /></div>
        <div className="field"><label>분류</label>
          <select value={category} onChange={(e) => setCategory(e.target.value)}>{CATEGORIES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select>
        </div>
        <div className="field"><label>기간</label>
          <select value={days} onChange={(e) => setDays(Number(e.target.value))}>{[1, 3, 7, 30, 90].map((d) => <option key={d} value={d}>최근 {d}일</option>)}</select>
        </div>
        <button className="secondary" onClick={collect} disabled={busy}>{busy ? '수집 중…' : regionCode ? '이 지역 뉴스 지금 수집' : '정책·금리·공급 뉴스 지금 수집'}</button>
      </div>
      {msg && <div className="notice info" style={{ marginTop: 12 }}>{msg}</div>}
      <div style={{ marginTop: 12 }}><ErrorBox error={error} /></div>
      <div className="card">{news === null ? <Loading /> : <NewsList news={news} />}</div>
    </>
  );
}
