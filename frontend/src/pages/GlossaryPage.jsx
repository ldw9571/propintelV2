import { useMemo, useState } from 'react';
import { useGlossary } from '../components/Glossary.jsx';

export default function GlossaryPage() {
  const terms = useGlossary();
  const [q, setQ] = useState('');
  const groups = useMemo(() => {
    const g = {};
    Object.values(terms)
      .filter((t) => !q || (t.term + t.longDesc).includes(q))
      .forEach((t) => { (g[t.category] ||= []).push(t); });
    return g;
  }, [terms, q]);

  return (
    <>
      <div className="page-head">
        <div><h1>용어 사전</h1><p>분석 화면의 ⓘ 에 마우스를 올려도 같은 설명이 나옵니다.</p></div>
        <input style={{ maxWidth: 260 }} placeholder="검색 (예: DSR, 전세)" value={q} onChange={(e) => setQ(e.target.value)} />
      </div>
      {Object.entries(groups).map(([cat, list]) => (
        <div className="card" key={cat}>
          <h3 style={{ marginBottom: 12 }}>{cat}</h3>
          <div className="grid-2">
            {list.map((t) => (
              <div key={t.key}>
                <div style={{ fontWeight: 700 }}>{t.term}</div>
                <div className="small" style={{ color: 'var(--text-2)', margin: '2px 0 6px' }}>{t.shortDesc}</div>
                <div className="small">{t.longDesc}</div>
                {t.example && <div className="small muted" style={{ marginTop: 4 }}>예) {t.example}</div>}
              </div>
            ))}
          </div>
        </div>
      ))}
    </>
  );
}
