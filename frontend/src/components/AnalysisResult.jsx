import { useState } from 'react';
import { CartesianGrid, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Stat } from './Inputs.jsx';
import { Term } from './Glossary.jsx';
import { SourceList } from './Source.jsx';
import { pct, short, won } from '../lib/format.js';

const SCENARIO_COLOR = {
  conservative: 'var(--series-conservative)',
  base: 'var(--series-base)',
  optimistic: 'var(--series-optimistic)',
};

const TABS = [
  ['summary', '요약'],
  ['funding', '자금·대출 한도'],
  ['loan', '원리금'],
  ['simulation', '1~10년 시뮬레이션'],
  ['returns', '수익 분석'],
  ['basis', '가정·출처'],
];

export default function AnalysisResult({ result, holdingYears }) {
  const [tab, setTab] = useState('summary');
  return (
    <div>
      <div className="tabs" role="tablist">
        {TABS.map(([k, label]) => (
          <button key={k} className={tab === k ? 'on' : ''} onClick={() => setTab(k)} role="tab" aria-selected={tab === k}>{label}</button>
        ))}
      </div>
      {tab === 'summary' && <Summary r={result} holdingYears={holdingYears} />}
      {tab === 'funding' && <Funding r={result} />}
      {tab === 'loan' && <Loan r={result} holdingYears={holdingYears} />}
      {tab === 'simulation' && <Simulation r={result} holdingYears={holdingYears} />}
      {tab === 'returns' && <Returns r={result} />}
      {tab === 'basis' && <Basis r={result} />}
      <p className="disclaimer">⚠ {result.disclaimer} (규제·세율 기준일: {result.policyAsOf})</p>
    </div>
  );
}

// ───────────────────────── 요약 ─────────────────────────

function Summary({ r, holdingYears }) {
  const f = r.funding;
  return (
    <>
      <div className="grid-4">
        <Stat tone="hero" label="실제 필요한 현금" value={won(f.requiredCash)} sub="매매가 + 취득비용 − 보증금 − 대출" />
        <Stat tone={f.shortfall > 0 ? 'bad' : 'good'} label={f.shortfall > 0 ? '부족한 자금' : '남는 현금'}
          value={won(f.shortfall > 0 ? f.shortfall : f.surplus)} sub={`보유 현금 ${won(f.cash)}`} />
        <Stat label={<Term k="LTV">예상 대출 가능 금액</Term>} value={won(f.maxLoan)}
          sub={r.loanLimit.allowed ? `${r.loanLimit.bindingConstraint} 기준 · 적용 ${won(f.loanAmount)}` : '규제상 불가로 계산'} />
        <Stat label="대출 후 월 상환액" value={won(f.monthlyPaymentFirst)}
          sub={<>첫 달 · <Term k="DSR">DSR</Term> {pct(f.dsrStress, 1)} (스트레스 적용)</>} />
      </div>

      {r.warnings.length > 0 && (
        <div className="notice warn" style={{ marginTop: 16 }}>
          <strong>확인이 필요한 점</strong>
          <ul>{r.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
        </div>
      )}

      <div className="card" style={{ marginTop: 16 }}>
        <div className="card-head">
          <h3>{holdingYears}년 후 매도 시 결과 (시나리오별)</h3>
          <span className="small muted">세금·비용·이자 모두 차감한 추정치</span>
        </div>
        <div className="grid-3">
          {r.scenarios.map((s) => (
            <div key={s.key} className="stat" style={{ borderTop: `3px solid ${SCENARIO_COLOR[s.key]}` }}>
              <div className="label">{s.label} (연 {pct(s.annualRate, 1)})</div>
              <div className="value" style={{ color: s.exit.netProfit < 0 ? 'var(--bad)' : 'var(--text)' }}>{won(s.exit.netProfit)}</div>
              <div className="sub">
                <Term k="ROI">수익률</Term> {pct(s.exit.roi, 1)} · <Term k="IRR">IRR</Term> {s.exit.irr == null ? '-' : pct(s.exit.irr, 1)}
              </div>
              <div className="sub">매도가 {won(s.exit.salePrice)}</div>
            </div>
          ))}
        </div>
      </div>

      <div className="card">
        <div className="card-head">
          <h3>매도 시점별 세후 순수익</h3>
          <Legend scenarios={r.scenarios} />
        </div>
        <ProfitChart scenarios={r.scenarios} holdingYears={holdingYears} />
        <p className="small muted" style={{ margin: '8px 0 0' }}>
          각 연도 말에 팔았다고 가정한 순수익입니다. 2년 미만 보유 시 양도세 단기세율, 보유 중 이자·보유세가 반영되어 초반에는 손실이 나기 쉽습니다.
        </p>
      </div>
    </>
  );
}

function Legend({ scenarios }) {
  return (
    <div className="legend">
      {scenarios.map((s) => (
        <span key={s.key}><i style={{ background: SCENARIO_COLOR[s.key] }} />{s.label} {pct(s.annualRate, 1)}</span>
      ))}
    </div>
  );
}

function ProfitChart({ scenarios, holdingYears }) {
  const data = scenarios[0].years.map((y, i) => {
    const row = { year: `${y.year}년` };
    scenarios.forEach((s) => { row[s.key] = s.years[i].netProfitIfSold; });
    return row;
  });
  return (
    <div style={{ width: '100%', height: 280 }}>
      <ResponsiveContainer>
        <LineChart data={data} margin={{ top: 10, right: 16, bottom: 0, left: 8 }}>
          <CartesianGrid stroke="var(--grid)" vertical={false} />
          <XAxis dataKey="year" tick={{ fill: 'var(--text-3)', fontSize: 12 }} axisLine={{ stroke: 'var(--border)' }} tickLine={false} />
          <YAxis tickFormatter={short} tick={{ fill: 'var(--text-3)', fontSize: 12 }} axisLine={false} tickLine={false} width={56} />
          <ReferenceLine y={0} stroke="var(--text-3)" />
          <ReferenceLine x={`${holdingYears}년`} stroke="var(--text-3)" strokeDasharray="4 4" label={{ value: '보유기간', fill: 'var(--text-3)', fontSize: 11, position: 'insideTopRight' }} />
          <Tooltip content={<ChartTip scenarios={scenarios} />} cursor={{ stroke: 'var(--text-3)', strokeDasharray: '3 3' }} />
          {scenarios.map((s) => (
            <Line key={s.key} type="monotone" dataKey={s.key} name={s.label} stroke={SCENARIO_COLOR[s.key]} strokeWidth={2}
              dot={{ r: 3, strokeWidth: 0, fill: SCENARIO_COLOR[s.key] }} activeDot={{ r: 5, stroke: 'var(--surface)', strokeWidth: 2 }} />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

function ChartTip({ active, payload, label, scenarios }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="chart-tip">
      <div className="t">{label} 후 매도 시</div>
      {scenarios.map((s) => {
        const p = payload.find((x) => x.dataKey === s.key);
        return p ? <div className="r" key={s.key}><span>{s.label}</span><b>{won(p.value)}</b></div> : null;
      })}
    </div>
  );
}

// ───────────────────────── 자금·대출 한도 ─────────────────────────

function Funding({ r }) {
  const f = r.funding;
  const a = r.acquisition;
  const l = r.loanLimit;
  return (
    <div className="grid-2">
      <div className="card">
        <h3 style={{ marginBottom: 12 }}>필요 자금 계산</h3>
        <dl className="kv">
          <dt>매입 가격</dt><dd>{won(f.price)}</dd>
          <dt><Term k="ACQ_TAX">취득세</Term> ({pct(a.acquisitionTaxRate)}{a.heavyTaxed ? ' 중과' : ''})</dt><dd>{won(a.acquisitionTax)}</dd>
          {a.firstTimeRelief > 0 && <><dt className="indent">생애최초 감면</dt><dd className="indent">−{won(a.firstTimeRelief)}</dd></>}
          <dt className="indent">지방교육세</dt><dd className="indent">{won(a.localEducationTax)}</dd>
          <dt className="indent">농어촌특별세</dt><dd className="indent">{won(a.ruralSpecialTax)}</dd>
          <dt><Term k="BROKERAGE">중개보수</Term> ({pct(a.brokerageRate, 1)} + VAT)</dt><dd>{won(a.brokerageFee)}</dd>
          <dt>법무비·채권·인지세 등 (추정)</dt><dd>{won(a.miscCost)}</dd>
          {a.otherCost > 0 && <><dt>기타 취득비용</dt><dd>{won(a.otherCost)}</dd></>}
          <dt className="total">취득 관련 비용 합계</dt><dd className="total">{won(a.total)}</dd>
          {f.tenantDeposit > 0 && <><dt>− 임차보증금</dt><dd>−{won(f.tenantDeposit)}</dd></>}
          <dt>− 대출금 {f.loanAutoSelected ? '(자동)' : '(희망액)'}</dt><dd>−{won(f.loanAmount)}</dd>
          <dt className="total">실제 필요한 현금 (필요 자기자본)</dt><dd className="total">{won(f.requiredCash)}</dd>
          <dt>보유 현금</dt><dd>{won(f.cash)}</dd>
          <dt className="total">{f.shortfall > 0 ? '부족한 자금' : '남는 현금'}</dt>
          <dd className="total" style={{ color: f.shortfall > 0 ? 'var(--bad)' : 'var(--good)' }}>{won(f.shortfall > 0 ? f.shortfall : f.surplus)}</dd>
        </dl>
        <div className="notice info" style={{ marginTop: 14 }}>
          총 투자 가능 금액(현금 + 예상 대출한도 + 보증금): <b>{won(f.totalInvestable)}</b><br />
          월 상환 가능 금액(DSR 한도 기준): <b>{won(f.monthlyRepaymentCapacity)}</b>
        </div>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: 4 }}>예상 대출 한도</h3>
        <p className="small muted" style={{ margin: '0 0 12px' }}>{l.regionTypeLabel} · {l.borrowerLabel}</p>
        <table>
          <thead><tr><th>기준</th><th>한도</th></tr></thead>
          <tbody>
            <tr className={l.bindingConstraint === 'LTV' ? 'highlight' : ''}>
              <td><Term k="LTV">LTV</Term> {pct(l.ltvRatio, 0)}</td><td>{won(l.ltvLimit)}</td>
            </tr>
            <tr className={l.bindingConstraint === '주택가격별 한도' ? 'highlight' : ''}>
              <td>주택가격별 한도</td><td>{l.capLimit == null ? '없음' : won(l.capLimit)}</td>
            </tr>
            <tr className={l.bindingConstraint === 'DSR' ? 'highlight' : ''}>
              <td><Term k="DSR">DSR</Term> {pct(l.dsrLimitRatio, 0)} (심사금리 {pct(l.dsrTestRate)})</td><td>{won(l.dsrLimit)}</td>
            </tr>
            <tr><td><b>예상 한도 (가장 작은 값)</b></td><td><b>{won(l.maxLoan)}</b></td></tr>
          </tbody>
        </table>
        <dl className="kv" style={{ marginTop: 14 }}>
          <dt>적용 대출금</dt><dd>{won(f.loanAmount)}</dd>
          <dt>적용 LTV</dt><dd>{pct(f.ltvApplied, 1)}</dd>
          <dt><Term k="STRESS_DSR">DSR (스트레스 금리 +{pct(l.stressRateApplied)})</Term></dt><dd>{pct(f.dsrStress, 1)}</dd>
          <dt>DSR (실제 금리)</dt><dd>{pct(f.dsrActual, 1)}</dd>
        </dl>
        {l.reasons.length > 0 && (
          <div className="notice info" style={{ marginTop: 14 }}>
            <ul style={{ margin: 0 }}>{l.reasons.map((x, i) => <li key={i}>{x}</li>)}</ul>
          </div>
        )}
        <p className="small muted" style={{ marginTop: 12 }}>
          실제 한도는 금융기관의 소득 인정 방식, 신용도, 상품 조건에 따라 달라집니다. 적용 규제 데이터의 출처는 ‘가정·출처’ 탭에서 확인하세요.
        </p>
      </div>
    </div>
  );
}

// ───────────────────────── 원리금 ─────────────────────────

function Loan({ r, holdingYears }) {
  const s = r.selectedSchedule;
  if (!s.principal) return <div className="card empty">대출금이 0원이라 원리금 계산이 없습니다.</div>;
  return (
    <>
      <div className="card">
        <div className="card-head">
          <h3>상환 방식 비교 — {won(s.principal)}, 연 {pct(s.annualRate)}, {s.termYears}년</h3>
        </div>
        <div className="table-wrap">
          <table>
            <thead><tr><th>상환 방식</th><th>첫 달 납입</th><th>마지막 달 납입</th><th>1년차 이자</th><th>1년차 원금</th><th>총 이자</th><th>{holdingYears}년 후 잔액</th></tr></thead>
            <tbody>
              {r.loanComparison.map((c) => (
                <tr key={c.method} className={c.method === s.method ? 'highlight' : ''}>
                  <td><Term k={c.method}>{c.label}</Term></td>
                  <td>{won(c.firstMonthPayment)}</td><td>{won(c.lastMonthPayment)}</td>
                  <td>{won(c.firstYearInterest)}</td><td>{won(c.firstYearPrincipal)}</td>
                  <td>{won(c.totalInterest)}</td><td>{won(c.balanceAtHoldingEnd)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <div className="card">
        <div className="card-head"><h3>연도별 상환 ({s.methodLabel})</h3><span className="small muted">총 이자 {won(s.totalInterest)}</span></div>
        <div className="table-wrap" style={{ maxHeight: 420 }}>
          <table>
            <thead><tr><th>연차</th><th>월 평균 납입</th><th>연간 이자</th><th>연간 원금</th><th>남은 잔액</th><th>누적 이자</th><th>누적 원금</th></tr></thead>
            <tbody>
              {s.years.map((y) => (
                <tr key={y.year} className={y.year === holdingYears ? 'highlight' : ''}>
                  <td>{y.year}년차</td><td>{won(y.avgMonthlyPayment)}</td><td>{won(y.interest)}</td><td>{won(y.principal)}</td>
                  <td>{won(y.endBalance)}</td><td>{won(y.cumulativeInterest)}</td><td>{won(y.cumulativePrincipal)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

// ───────────────────────── 시뮬레이션 ─────────────────────────

function Simulation({ r, holdingYears }) {
  const [key, setKey] = useState('base');
  const s = r.scenarios.find((x) => x.key === key);
  return (
    <div className="card">
      <div className="card-head">
        <h3>1~10년 자산 변화</h3>
        <div className="seg" style={{ minWidth: 300 }}>
          {r.scenarios.map((x) => (
            <button key={x.key} className={x.key === key ? 'on' : ''} onClick={() => setKey(x.key)}>
              <span className="scenario-dot" style={{ background: SCENARIO_COLOR[x.key], marginRight: 6 }} />{x.label} {pct(x.annualRate, 1)}
            </button>
          ))}
        </div>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>시점</th><th>예상 가격</th><th>대출 잔액</th><th>보증금</th><th><Term k="NET_EQUITY">순자산</Term></th>
              <th>누적 이자</th><th>누적 원금상환</th><th>누적 보유비용</th><th>매도 시 순수익</th><th>수익률</th>
            </tr>
          </thead>
          <tbody>
            {s.years.map((y) => (
              <tr key={y.year} className={y.year === holdingYears ? 'highlight' : ''}>
                <td>{y.year}년 후</td><td>{won(y.price)}</td><td>{won(y.loanBalance)}</td><td>{won(y.tenantDeposit)}</td>
                <td>{won(y.netEquity)}</td><td>{won(y.cumulativeInterest)}</td><td>{won(y.cumulativePrincipal)}</td>
                <td>{won(y.cumulativeHoldingCost)}</td>
                <td className={y.netProfitIfSold < 0 ? 'neg' : 'pos'}>{won(y.netProfitIfSold)}</td>
                <td>{pct(y.roiIfSold, 1)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="small muted" style={{ marginTop: 10 }}>
        순자산 = 예상 가격 − 대출 잔액 − 보증금 (매도비용·세금 차감 전). 매도 시 순수익은 매도비용·양도세·이자·보유세까지 모두 뺀 값입니다.
      </p>
    </div>
  );
}

// ───────────────────────── 수익 분석 ─────────────────────────

const RETURN_ROWS = [
  ['초기 투자금 (내 돈)', (e) => e.initialInvestment],
  ['취득 관련 비용', (e) => e.acquisitionCost],
  ['대출금', (e) => e.loanAmount],
  ['이자비용 (누적)', (e) => -e.totalInterest],
  ['원금 상환 (누적)', (e) => -e.totalPrincipalRepaid],
  ['전세·월세 보증금', (e) => e.tenantDeposit],
  ['월세 수익 (누적)', (e) => e.totalRentIncome],
  ['관리비 등 보유자 부담 (누적)', (e) => -e.totalOwnerCost],
  [<Term k="HOLDING_TAX" key="h">보유세 (누적, 재산세 추정)</Term>, (e) => -e.totalHoldingTax],
  ['최종 매도가', (e) => e.salePrice, 'sep'],
  ['매도 비용 (중개보수)', (e) => -e.sellingCost],
  [<Term k="CGT" key="c">예상 양도소득세 (지방세 포함)</Term>, (e) => -e.capitalGainsTax.total],
  ['대출 상환', (e) => -e.loanRepayment],
  ['보증금 반환', (e) => -e.depositReturn],
  ['최종 회수금', (e) => e.finalRecovery, 'bold'],
  ['보유 중 추가 투입 현금', (e) => e.additionalCashInvested, 'sep'],
  ['총 투입 현금', (e) => e.totalInvested],
  ['예상 순수익', (e) => e.netProfit, 'bold'],
];

function Returns({ r }) {
  const ex = r.scenarios.map((s) => s.exit);
  return (
    <>
      <div className="card">
        <div className="card-head"><h3>{ex[0].holdingYears}년 보유 후 매도 — 수익 구조</h3></div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>항목</th>{r.scenarios.map((s) => <th key={s.key}><span className="scenario-dot" style={{ background: SCENARIO_COLOR[s.key], marginRight: 6 }} />{s.label} ({pct(s.annualRate, 1)})</th>)}</tr>
            </thead>
            <tbody>
              {RETURN_ROWS.map(([label, get, style], i) => (
                <tr key={i} style={{ fontWeight: style === 'bold' ? 700 : 400, borderTop: style === 'sep' ? '2px solid var(--border)' : undefined }}>
                  <td>{label}</td>
                  {ex.map((e, j) => <td key={j}>{won(get(e))}</td>)}
                </tr>
              ))}
              <tr><td><Term k="ROI">투자원금 대비 수익률</Term></td>{ex.map((e, j) => <td key={j}>{pct(e.roi, 1)}</td>)}</tr>
              <tr><td><Term k="CAGR">연환산 수익률</Term></td>{ex.map((e, j) => <td key={j}>{pct(e.annualizedReturn, 2)}</td>)}</tr>
              <tr><td><Term k="IRR">IRR (내부수익률)</Term></td>{ex.map((e, j) => <td key={j}>{e.irr == null ? '계산 불가' : pct(e.irr, 2)}</td>)}</tr>
            </tbody>
          </table>
        </div>
        <p className="small muted" style={{ marginTop: 10 }}>
          총 투입 현금 = 초기 투자금 + 보유 중 이자·보유세 등으로 추가로 넣은 돈. 순수익 = 모든 현금흐름의 합.
        </p>
      </div>

      <div className="grid-3" style={{ marginTop: 16 }}>
        {r.scenarios.map((s) => <CgtCard key={s.key} s={s} />)}
      </div>

      <div className="card" style={{ marginTop: 16 }}>
        <h3 style={{ marginBottom: 10 }}>연도별 현금흐름 (기준 시나리오)</h3>
        <div className="table-wrap">
          <table>
            <thead><tr>{r.scenarios[1].exit.cashFlows.map((_, i) => <th key={i}>{i === 0 ? '매수 시점' : `${i}년차`}</th>)}</tr></thead>
            <tbody><tr>{r.scenarios[1].exit.cashFlows.map((v, i) => <td key={i} className={v < 0 ? 'neg' : 'pos'}>{won(v)}</td>)}</tr></tbody>
          </table>
        </div>
        <p className="small muted" style={{ marginTop: 8 }}>IRR은 이 현금흐름의 현재가치 합이 0이 되는 연 수익률입니다. 마지막 해에는 최종 회수금이 더해집니다.</p>
      </div>
    </>
  );
}

function CgtCard({ s }) {
  const c = s.exit.capitalGainsTax;
  return (
    <div className="card" style={{ borderTop: `3px solid ${SCENARIO_COLOR[s.key]}` }}>
      <h4 style={{ marginBottom: 8 }}>{s.label} — 양도소득세</h4>
      <dl className="kv small">
        <dt>양도차익</dt><dd>{won(c.gain)}</dd>
        {!c.fullyExempt && <>
          <dt>과세 대상 차익</dt><dd>{won(c.taxableGainBeforeDeduction)}</dd>
          <dt><Term k="LTD">장기보유특별공제</Term> {pct(c.longTermDeductionRate, 0)}</dt><dd>−{won(c.longTermDeduction)}</dd>
          <dt>기본공제</dt><dd>−{won(c.basicDeduction)}</dd>
          <dt>과세표준</dt><dd>{won(c.taxBase)}</dd>
        </>}
        <dt>세율</dt><dd>{c.rateDescription}</dd>
        <dt className="total">세액 합계</dt><dd className="total">{won(c.total)}</dd>
      </dl>
      {c.notes.length > 0 && <ul className="small muted" style={{ paddingLeft: 16, marginBottom: 0 }}>{c.notes.map((n, i) => <li key={i}>{n}</li>)}</ul>}
    </div>
  );
}

// ───────────────────────── 가정·출처 ─────────────────────────

function Basis({ r }) {
  const h = r.holdingTaxFirstYear;
  return (
    <>
      <div className="grid-2">
        <div className="card">
          <h3 style={{ marginBottom: 10 }}>계산 가정</h3>
          <ul style={{ paddingLeft: 18, margin: 0 }} className="small">
            {r.assumptions.map((a, i) => <li key={i} style={{ marginBottom: 4 }}>{a}</li>)}
          </ul>
        </div>
        <div className="card">
          <h3 style={{ marginBottom: 10 }}><Term k="HOLDING_TAX">1년차 보유세 추정</Term></h3>
          <dl className="kv small">
            <dt><Term k="PUBLIC_PRICE">추정 공시가격</Term></dt><dd>{won(h.publicPrice)}</dd>
            <dt>공정시장가액비율</dt><dd>{pct(h.fairMarketRatio, 0)}</dd>
            <dt>과세표준</dt><dd>{won(h.taxBase)}</dd>
            <dt>재산세</dt><dd>{won(h.propertyTax)}</dd>
            <dt>도시지역분</dt><dd>{won(h.urbanAreaTax)}</dd>
            <dt>지방교육세</dt><dd>{won(h.localEducationTax)}</dd>
            <dt className="total">합계</dt><dd className="total">{won(h.total)}</dd>
          </dl>
        </div>
      </div>
      <div className="card" style={{ marginTop: 16 }}>
        <div className="card-head">
          <h3>이 계산에 사용된 데이터 출처</h3>
          <span className="small muted">규제·세율 기준일 {r.policyAsOf}</span>
        </div>
        <SourceList sources={r.sources} />
        <p className="small muted" style={{ marginTop: 12 }}>
          ‘미검증’ 표시는 공식 원문과 대조하기 전 데이터입니다. ‘기준 데이터’ 메뉴에서 전체 값을 확인하고, 공식 출처로 확인한 뒤 수정할 수 있습니다.
        </p>
      </div>
    </>
  );
}
