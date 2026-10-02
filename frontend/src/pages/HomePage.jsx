import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api.js';
import { pct, won } from '../lib/format.js';
import { Stat } from '../components/Inputs.jsx';
import { Term } from '../components/Glossary.jsx';
import { Change, ErrorBox, Flag, Loading, Trend } from '../components/Common.jsx';
import { NewsList } from './NewsPage.jsx';

export default function HomePage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.home().then(setData).catch((e) => setError(`대시보드를 불러오지 못했습니다. 백엔드 실행 여부를 확인하세요.\n${e.message}`));
  }, []);

  if (error) return <ErrorBox error={error} />;
  if (!data) return <Loading />;
  const c = data.capacity;

  return (
    <>
      <div className="page-head">
        <div>
          <h1>대시보드</h1>
          <p>내 자금, 관심 지역·아파트, 오늘의 뉴스, 알림을 한 화면에서 봅니다.</p>
        </div>
      </div>

      {/* 나의 투자 가능 금액 */}
      <div className="card">
        <div className="card-head">
          <h3>나의 투자 가능 금액</h3>
          <Link to="/profile" className="small">내 자금 정보 수정 →</Link>
        </div>
        {data.capacityError && <div className="notice bad">{data.capacityError}</div>}
        {c && (
          <>
            <div className="grid-4">
              <Stat label="보유 현금" value={won(c.cash)} sub={c.borrowerLabel} />
              <Stat label={<Term k="LTV">대출 가능 예상금액</Term>} value={won(c.expectedLoanAtMaxPrice)}
                sub={`${c.targetRegion} · ${c.bindingConstraint} 기준`} />
              <Stat tone="hero" label="총 투자 가능금액" value={won(c.totalInvestable)}
                sub={`최대 매입 가능 가격 약 ${won(c.maxPurchasePrice)} (취득비용 ${won(c.acquisitionCostAtMaxPrice)} 포함)`} />
              <Stat label={<Term k="DSR">월 상환 가능금액</Term>} value={won(c.monthlyRepaymentCapacity)}
                sub={`최대 대출 시 월 ${won(c.monthlyPaymentAtMaxPrice)} 상환`} />
            </div>
            <p className="small muted" style={{ margin: '10px 0 0' }}>{c.notes?.[c.notes.length - 1]} 실제 대출은 금융기관 심사에 따라 달라집니다.</p>
          </>
        )}
      </div>

      {/* 관심 지역 */}
      <div className="section-title"><h2>관심 지역</h2><Link to="/watch" className="small">관심 목록 관리 →</Link></div>
      <div className="card">
        {data.regions.length === 0
          ? <div className="empty" style={{ padding: 24 }}>등록한 관심 지역이 없습니다. <Link to="/watch">관심 목록</Link>에서 추가하세요.</div>
          : <WatchTable rows={data.regions} kind="REGION" />}
      </div>

      {/* 관심 아파트 */}
      <div className="section-title"><h2>관심 아파트</h2></div>
      <div className="card">
        {data.complexes.length === 0
          ? <div className="empty" style={{ padding: 24 }}>등록한 관심 아파트가 없습니다. <Link to="/market">시장 데이터</Link>에서 단지를 찾아 등록하세요.</div>
          : <WatchTable rows={data.complexes} kind="COMPLEX" />}
      </div>

      <div className="grid-2" style={{ marginTop: 22 }}>
        {/* 오늘의 뉴스 */}
        <div className="card">
          <div className="card-head">
            <h3>오늘의 부동산 뉴스</h3>
            <Link to="/news" className="small">전체 →</Link>
          </div>
          {data.news.mockProvider && <div className="notice info small" style={{ marginBottom: 10 }}>네이버 뉴스 API 키가 없어 <b>샘플 뉴스</b>가 표시됩니다.</div>}
          <NewsGroup title="관심 지역" list={data.news.region} />
          <NewsGroup title="정책" list={data.news.policy} />
          <NewsGroup title="금리" list={data.news.rate} />
          <NewsGroup title="공급" list={data.news.supply} />
        </div>

        {/* 알림 */}
        <div className="card">
          <div className="card-head">
            <h3>알림 {data.unreadAlerts > 0 && <span className="badge up">{data.unreadAlerts} 안 읽음</span>}</h3>
            <Link to="/alerts" className="small">전체 →</Link>
          </div>
          {data.alerts.length === 0 && <div className="muted small">아직 발생한 알림이 없습니다. 관심 항목을 등록하면 매일 조건을 확인합니다.</div>}
          {data.alerts.map((a) => (
            <div key={a.id} className="news-item">
              <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                <span className={`badge ${a.severity === 'UP' ? 'up' : a.severity === 'DOWN' ? 'down' : 'neutral'}`}>{a.ruleLabel}</span>
                {!a.read && <span className="badge neutral">NEW</span>}
              </div>
              <div style={{ marginTop: 4, fontWeight: 600 }}>{a.title}</div>
              <div className="news-meta">{a.triggeredAt?.slice(0, 16).replace('T', ' ')}{a.detail?.volume ? ` · ${a.detail.volume}` : ''}</div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
}

function NewsGroup({ title, list }) {
  if (!list?.length) return null;
  return (
    <div style={{ marginBottom: 10 }}>
      <div className="small" style={{ fontWeight: 700, color: 'var(--text-2)' }}>{title}</div>
      <NewsList news={list.slice(0, 4)} compact />
    </div>
  );
}

export function WatchTable({ rows, kind }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>{kind === 'COMPLEX' ? '아파트' : '지역'}</th>
            <th>기준월</th>
            <th>{kind === 'COMPLEX' ? '최근 평균 거래가' : '평균가(84㎡ 환산)'}</th>
            <th>1개월</th><th>3개월</th><th>6개월</th><th>1년</th>
            <th><Term k="VOLUME">거래량</Term></th>
            <th>전세(84㎡ 환산)</th>
            <th><Term k="JEONSE_RATIO">전세가율</Term></th>
            {kind === 'COMPLEX' && <th><Term k="GAP_INVEST">갭</Term></th>}
            <th><Term k="NEW_HIGH">최근 신고가</Term></th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((w) => (
            <tr key={w.id}>
              <td>
                <Link to={`/insight?${w.complexId ? `complexId=${w.complexId}` : `regionCode=${w.regionCode}`}`}>{w.label}</Link>
                <div className="small muted">{w.areaLabel}</div>
              </td>
              <td className="small">{w.refMonth || '-'}{w.refSample ? ` (${w.refSample}건)` : ''}</td>
              <td>{won(kind === 'COMPLEX' ? w.currentAvgPrice : w.currentPrice84)}</td>
              <td><Change value={w.change1m} /></td>
              <td><Change value={w.change3m} /></td>
              <td><Change value={w.change6m} /></td>
              <td><Change value={w.change12m} /></td>
              <td><Trend trend={w.volumeTrend} ratio={w.volumeRatio} /><div className="small muted">{w.volumeRef ?? 0}건</div></td>
              <td>{won(w.jeonsePrice84)}{w.jeonseChange3m != null && <div className="small"><Change value={w.jeonseChange3m} /> 3개월</div>}</td>
              <td>{w.jeonseRatio == null ? '-' : pct(w.jeonseRatio, 0)}</td>
              {kind === 'COMPLEX' && <td>{w.currentPrice84 && w.jeonsePrice84 ? won(w.currentPrice84 - w.jeonsePrice84) : '-'}</td>}
              <td className="small">{w.latestNewHigh ? <>{won(w.latestNewHigh.price)}<div className="muted">{w.latestNewHigh.date} · {w.newHighCount}건</div></> : '-'}</td>
              <td><Flag flag={w.flag} />{w.dropCount > 0 && <div className="small muted">하락거래 {w.dropCount}건</div>}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="small muted" style={{ marginTop: 8 }}>
        변동률은 거래가 있는 달의 ㎡당 평균을 84㎡로 환산해 비교합니다. 거래가 적으면 '-'로 표시하며, 최근 1개월은 신고 지연으로 덜 채워질 수 있습니다.
      </p>
    </div>
  );
}
