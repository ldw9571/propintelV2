/** 출처·기준일·검증여부 표시 */
export function VerifiedBadge({ source }) {
  if (!source) return null;
  if (source.sourceName?.startsWith('Mock')) return <span className="badge mock">Mock</span>;
  return source.verified
    ? <span className="badge verified">검증됨</span>
    : <span className="badge unverified" title="공식 원문과 대조하기 전 데이터입니다">미검증</span>;
}

export function SourceLine({ source }) {
  if (!source) return null;
  return (
    <div className="small" style={{ display: 'flex', gap: 8, alignItems: 'baseline', flexWrap: 'wrap' }}>
      <VerifiedBadge source={source} />
      <span>
        {source.sourceUrl
          ? <a href={source.sourceUrl} target="_blank" rel="noreferrer">{source.sourceName}</a>
          : source.sourceName}
      </span>
      <span className="muted">기준일 {source.baseDate ?? '-'} · 업데이트 {source.updatedAt ?? '-'}</span>
      {source.note && <span className="muted">— {source.note}</span>}
    </div>
  );
}

export function SourceList({ sources }) {
  if (!sources?.length) return null;
  return (
    <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'grid', gap: 10 }}>
      {sources.map((s, i) => <li key={i}><SourceLine source={s} /></li>)}
    </ul>
  );
}
