import { useState } from 'react';
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { NumberField } from '../components/Inputs.jsx';
import { Term } from '../components/Glossary.jsx';
import { api } from '../lib/api.js';
import { pct, short, won } from '../lib/format.js';

const COLORS = { EQUAL_PAYMENT: 'var(--series-base)', EQUAL_PRINCIPAL: 'var(--series-conservative)', BULLET: 'var(--series-optimistic)' };

export default function LoanCalculatorPage() {
  const [principal, setPrincipal] = useState(30000);
  const [rate, setRate] = useState(4);
  const [years, setYears] = useState(30);
  const [data, setData] = useState(null);
  const [selected, setSelected] = useState('EQUAL_PAYMENT');
  const [error, setError] = useState('');

  async function run(e) {
    e?.preventDefault();
    setError('');
    try {
      setData(await api.loanSchedule(Math.round(principal * 10_000), rate / 100, Number(years)));
    } catch (err) { setError(err.message); }
  }

  const sel = data?.schedules.find((s) => s.method === selected);
  const chart = data ? data.schedules[0].years.map((y, i) => {
    const row = { year: `${y.year}년` };
    data.schedules.forEach((s) => { row[s.method] = s.years[i].endBalance; });
    return row;
  }) : [];

  return (
    <>
      <div className="page-head">
        <div><h1>대출 원리금 계산기</h1><p>대출금·금리·기간을 넣으면 세 가지 상환 방식을 한 번에 비교합니다.</p></div>
      </div>
      <form className="card" onSubmit={run} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12, alignItems: 'end' }}>
        <NumberField label="대출금" unit="만원" value={principal} onChange={setPrincipal} showWon />
        <NumberField label={<Term k="INTEREST_RATE">연 금리</Term>} unit="%" step="0.01" value={rate} onChange={setRate} />
        <NumberField label="기간" unit="년" value={years} onChange={setYears} />
        <div className="field"><button className="primary" type="submit">계산하기</button></div>
      </form>
      {error && <div className="notice bad" style={{ marginTop: 16 }}>{error}</div>}
      {data && (
        <>
          <div className="card">
            <div className="table-wrap">
              <table>
                <thead><tr><th>상환 방식</th><th>첫 달 납입</th><th>마지막 달 납입</th><th>1년차 연간 이자</th><th>1년차 연간 원금</th><th>총 이자</th><th>총 상환액</th></tr></thead>
                <tbody>
                  {data.schedules.map((s) => (
                    <tr key={s.method} className={s.method === selected ? 'highlight' : ''} onClick={() => setSelected(s.method)} style={{ cursor: 'pointer' }}>
                      <td><span className="scenario-dot" style={{ background: COLORS[s.method], marginRight: 6 }} /><Term k={s.method}>{s.methodLabel}</Term></td>
                      <td>{won(s.firstMonthPayment)}</td><td>{won(s.lastMonthPayment)}</td>
                      <td>{won(s.years[0].interest)}</td><td>{won(s.years[0].principal)}</td>
                      <td>{won(s.totalInterest)}</td><td>{won(s.totalPayment)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="small muted" style={{ marginTop: 8 }}>행을 클릭하면 아래 연도별 표가 바뀝니다. 금리는 전 기간 고정으로 가정했습니다 (연 {pct(rate / 100)}).</p>
          </div>
          <div className="card">
            <div className="card-head">
              <h3>남은 대출잔액</h3>
              <div className="legend">{data.schedules.map((s) => <span key={s.method}><i style={{ background: COLORS[s.method] }} />{s.methodLabel}</span>)}</div>
            </div>
            <div style={{ height: 260 }}>
              <ResponsiveContainer>
                <LineChart data={chart} margin={{ top: 8, right: 16, left: 8, bottom: 0 }}>
                  <CartesianGrid stroke="var(--grid)" vertical={false} />
                  <XAxis dataKey="year" tick={{ fill: 'var(--text-3)', fontSize: 12 }} tickLine={false} axisLine={{ stroke: 'var(--border)' }} interval="preserveStartEnd" />
                  <YAxis tickFormatter={short} tick={{ fill: 'var(--text-3)', fontSize: 12 }} axisLine={false} tickLine={false} width={56} />
                  <Tooltip formatter={(v, n) => [won(v), data.schedules.find((s) => s.method === n)?.methodLabel]} contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }} />
                  {data.schedules.map((s) => <Line key={s.method} dataKey={s.method} stroke={COLORS[s.method]} strokeWidth={2} dot={false} activeDot={{ r: 4 }} />)}
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
          <div className="card">
            <h3 style={{ marginBottom: 10 }}>연도별 상환 — {sel.methodLabel}</h3>
            <div className="table-wrap" style={{ maxHeight: 480 }}>
              <table>
                <thead><tr><th>연차</th><th>월 평균 납입</th><th>연간 이자</th><th>연간 원금</th><th>남은 잔액</th><th>누적 이자</th><th>누적 원금</th></tr></thead>
                <tbody>
                  {sel.years.map((y) => (
                    <tr key={y.year}><td>{y.year}년차</td><td>{won(y.avgMonthlyPayment)}</td><td>{won(y.interest)}</td><td>{won(y.principal)}</td><td>{won(y.endBalance)}</td><td>{won(y.cumulativeInterest)}</td><td>{won(y.cumulativePrincipal)}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </>
  );
}
