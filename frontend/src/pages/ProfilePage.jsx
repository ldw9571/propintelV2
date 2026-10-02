import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api.js';
import { NumberField } from '../components/Inputs.jsx';
import { ErrorBox, Loading, RegionSelect } from '../components/Common.jsx';

const toMan = (v) => Math.round(Number(v || 0) / 10_000);

export default function ProfilePage() {
  const [regions, setRegions] = useState([]);
  const [f, setF] = useState(null);
  const [error, setError] = useState('');
  const nav = useNavigate();

  useEffect(() => {
    api.regions().then(setRegions).catch(() => {});
    api.profile().then((p) => setF({
      cash: toMan(p.cash), monthlyIncome: toMan(p.monthlyIncome), existingLoanBalance: toMan(p.existingLoanBalance),
      existingAnnualDebtService: toMan(p.existingAnnualDebtService), housesOwned: p.housesOwned, firstTimeBuyer: p.firstTimeBuyer,
      targetRegionCode: p.targetRegionCode, loanRate: Math.round(p.loanRate * 10000) / 100, loanTermYears: p.loanTermYears,
      repaymentMethod: p.repaymentMethod, rateType: p.rateType,
    })).catch((e) => setError(e.message));
  }, []);

  if (!f) return error ? <ErrorBox error={error} /> : <Loading />;
  const set = (k) => (v) => setF({ ...f, [k]: v });
  const man = (v) => Math.round(Number(v || 0) * 10_000);

  async function save(e) {
    e.preventDefault();
    try {
      await api.saveProfile({
        cash: man(f.cash), monthlyIncome: man(f.monthlyIncome), existingLoanBalance: man(f.existingLoanBalance),
        existingAnnualDebtService: man(f.existingAnnualDebtService), housesOwned: Number(f.housesOwned),
        firstTimeBuyer: Number(f.housesOwned) === 0 && f.firstTimeBuyer, targetRegionCode: f.targetRegionCode,
        loanRate: Number(f.loanRate) / 100, loanTermYears: Number(f.loanTermYears), repaymentMethod: f.repaymentMethod, rateType: f.rateType,
      });
      nav('/');
    } catch (err) { setError(err.message); }
  }

  return (
    <>
      <div className="page-head"><div><h1>내 자금 정보</h1><p>대시보드의 “나의 투자 가능 금액” 계산에 쓰입니다.</p></div></div>
      <ErrorBox error={error} />
      <form className="card" style={{ maxWidth: 640 }} onSubmit={save}>
        <div className="row">
          <NumberField label="보유 현금" unit="만원" value={f.cash} onChange={set('cash')} showWon />
          <NumberField label="월 소득(세전)" unit="만원" value={f.monthlyIncome} onChange={set('monthlyIncome')} showWon />
        </div>
        <div className="row">
          <NumberField label="기존 대출 잔액" unit="만원" value={f.existingLoanBalance} onChange={set('existingLoanBalance')} />
          <NumberField label="기존 대출 연 원리금" unit="만원" value={f.existingAnnualDebtService} onChange={set('existingAnnualDebtService')} />
        </div>
        <div className="row">
          <div className="field"><label>보유 주택 수</label>
            <select value={f.housesOwned} onChange={(e) => set('housesOwned')(Number(e.target.value))}>{[0, 1, 2, 3].map((n) => <option key={n} value={n}>{n === 3 ? '3주택 이상' : `${n}주택`}</option>)}</select>
          </div>
          <div className="field"><label>매수 희망 지역</label><RegionSelect regions={regions} value={f.targetRegionCode} onChange={set('targetRegionCode')} /></div>
        </div>
        {Number(f.housesOwned) === 0 && <label className="check"><input type="checkbox" checked={f.firstTimeBuyer} onChange={(e) => set('firstTimeBuyer')(e.target.checked)} /> 생애최초 주택 구입</label>}
        <div className="row-3">
          <NumberField label="예상 금리" unit="%" step="0.01" value={f.loanRate} onChange={set('loanRate')} />
          <NumberField label="대출 기간" unit="년" value={f.loanTermYears} onChange={set('loanTermYears')} />
          <div className="field"><label>금리 유형</label>
            <select value={f.rateType} onChange={(e) => set('rateType')(e.target.value)}><option value="VARIABLE">변동형</option><option value="MIXED">혼합형</option><option value="PERIODIC">주기형</option></select>
          </div>
        </div>
        <div className="field"><label>상환 방식</label>
          <select value={f.repaymentMethod} onChange={(e) => set('repaymentMethod')(e.target.value)}><option value="EQUAL_PAYMENT">원리금균등</option><option value="EQUAL_PRINCIPAL">원금균등</option><option value="BULLET">만기일시</option></select>
        </div>
        <button className="primary" type="submit">저장하고 대시보드로</button>
      </form>
    </>
  );
}
