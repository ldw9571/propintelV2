import { useMemo } from 'react';
import { NumberField, Segmented } from './Inputs.jsx';
import { Term } from './Glossary.jsx';

const USAGES = [
  { value: 'OWNER_OCCUPY', label: '실거주' },
  { value: 'JEONSE', label: '전세 임대' },
  { value: 'MONTHLY_RENT', label: '월세 임대' },
];

export default function AnalysisForm({ form, setForm, regions, onSubmit, loading, refRate }) {
  const set = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));
  const region = regions.find((r) => r.code === form.regionCode);

  const grouped = useMemo(() => {
    const g = {};
    regions.forEach((r) => { (g[r.sido] ||= []).push(r); });
    return g;
  }, [regions]);

  return (
    <form className="card" onSubmit={(e) => { e.preventDefault(); onSubmit(); }}>
      <fieldset>
        <legend>1. 나의 자금</legend>
        <div className="row">
          <NumberField label="보유 현금" unit="만원" value={form.cash} onChange={set('cash')} showWon />
          <NumberField label="월 소득(세전)" unit="만원" value={form.monthlyIncome} onChange={set('monthlyIncome')} showWon />
        </div>
        <div className="row">
          <NumberField label="기존 대출 잔액" unit="만원" value={form.existingLoanBalance} onChange={set('existingLoanBalance')} />
          <NumberField label={<Term k="DSR">기존 대출 연 원리금</Term>} unit="만원" value={form.existingAnnualDebtService}
            onChange={set('existingAnnualDebtService')} hint="DSR 계산에 사용" />
        </div>
      </fieldset>

      <fieldset>
        <legend>2. 매물</legend>
        <div className="field">
          <label>지역</label>
          <select value={form.regionCode} onChange={(e) => set('regionCode')(e.target.value)}>
            {Object.entries(grouped).map(([sido, list]) => (
              <optgroup key={sido} label={sido}>
                {list.map((r) => (
                  <option key={r.code} value={r.code}>
                    {r.sigungu || r.sido}{r.regulated ? ' · 규제지역' : ''}
                  </option>
                ))}
              </optgroup>
            ))}
          </select>
          {region && (
            <div className="hint">
              {region.regulated ? <Term k="REGULATED_AREA">규제지역</Term> : (region.metro ? '수도권 비규제' : '지방')}
              {region.landPermitZone && <> · <Term k="LAND_PERMIT">토지거래허가구역</Term></>}
              {' '}(기준일 {region.source?.baseDate})
            </div>
          )}
        </div>
        <div className="field">
          <label>아파트명 / 매물 메모</label>
          <input value={form.apartmentName} onChange={(e) => set('apartmentName')(e.target.value)} placeholder="예: ○○아파트 84㎡ 10층" />
        </div>
        <div className="row">
          <NumberField label="매입 희망 가격" unit="만원" value={form.price} onChange={set('price')} showWon />
          <NumberField label="전용면적" unit="㎡" value={form.exclusiveArea} onChange={set('exclusiveArea')} hint="85㎡ 초과 시 농특세" />
        </div>
      </fieldset>

      <fieldset>
        <legend>3. 주택 보유 상황</legend>
        <div className="field">
          <label>현재 보유 주택 수 (매수 전)</label>
          <Segmented value={Number(form.housesOwned)} onChange={set('housesOwned')}
            options={[{ value: 0, label: '무주택' }, { value: 1, label: '1주택' }, { value: 2, label: '2주택' }, { value: 3, label: '3주택+' }]} />
        </div>
        {Number(form.housesOwned) === 0 && (
          <label className="check"><input type="checkbox" checked={form.firstTimeBuyer} onChange={(e) => set('firstTimeBuyer')(e.target.checked)} />
            생애최초 주택 구입</label>
        )}
        {Number(form.housesOwned) === 1 && (
          <label className="check"><input type="checkbox" checked={form.willSellExistingHome} onChange={(e) => set('willSellExistingHome')(e.target.checked)} />
            기존 주택을 처분할 예정 (일시적 2주택)</label>
        )}
      </fieldset>

      <fieldset>
        <legend>4. 활용 방식</legend>
        <Segmented options={USAGES} value={form.usage} onChange={set('usage')} />
        <div style={{ marginTop: 10 }}>
          {form.usage === 'JEONSE' && (
            <NumberField label={<Term k="GAP_INVEST">예상 전세보증금</Term>} unit="만원" value={form.jeonseDeposit} onChange={set('jeonseDeposit')} showWon />
          )}
          {form.usage === 'MONTHLY_RENT' && (
            <div className="row">
              <NumberField label="월세 보증금" unit="만원" value={form.rentDeposit} onChange={set('rentDeposit')} showWon />
              <NumberField label="월세" unit="만원" value={form.monthlyRent} onChange={set('monthlyRent')} />
            </div>
          )}
          <NumberField label="보유자 부담 월 비용" unit="만원" value={form.ownerMonthlyCost} onChange={set('ownerMonthlyCost')}
            hint={form.usage === 'OWNER_OCCUPY' ? '관리비 등 투자 비용으로 볼 금액 (생활비라면 0)' : '공실 관리비·수선비 등'} />
        </div>
      </fieldset>

      <fieldset>
        <legend>5. 대출 조건</legend>
        <label className="check">
          <input type="checkbox" checked={form.autoLoan} onChange={(e) => set('autoLoan')(e.target.checked)} />
          부족한 만큼만 대출 (예상 한도 내 자동)
        </label>
        {!form.autoLoan && (
          <NumberField label="희망 대출금" unit="만원" value={form.desiredLoanAmount} onChange={set('desiredLoanAmount')} showWon />
        )}
        <div className="row">
          <NumberField label={<Term k="INTEREST_RATE">대출 금리</Term>} unit="%" step="0.01" value={form.loanRate} onChange={set('loanRate')}
            hint={refRate ? `참고: ${(refRate.data.rate * 100).toFixed(2)}% (${refRate.mock ? 'Mock 예시값' : refRate.source?.sourceName})` : undefined} />
          <NumberField label="대출 기간" unit="년" step="1" min={1} max={50} value={form.loanTermYears} onChange={set('loanTermYears')} />
        </div>
        <div className="row">
          <div className="field">
            <label>상환 방식</label>
            <select value={form.repaymentMethod} onChange={(e) => set('repaymentMethod')(e.target.value)}>
              <option value="EQUAL_PAYMENT">원리금균등</option>
              <option value="EQUAL_PRINCIPAL">원금균등</option>
              <option value="BULLET">만기일시</option>
            </select>
          </div>
          <div className="field">
            <label><Term k="STRESS_DSR">금리 유형</Term></label>
            <select value={form.rateType} onChange={(e) => set('rateType')(e.target.value)}>
              <option value="VARIABLE">변동형</option>
              <option value="MIXED">혼합형</option>
              <option value="PERIODIC">주기형</option>
            </select>
          </div>
        </div>
      </fieldset>

      <fieldset className="scenario-rates">
        <legend>6. 보유 기간과 가격 시나리오</legend>
        <div className="field">
          <label>보유 기간</label>
          <select value={form.holdingYears} onChange={(e) => set('holdingYears')(Number(e.target.value))}>
            {Array.from({ length: 10 }, (_, i) => i + 1).map((y) => <option key={y} value={y}>{y}년 후 매도</option>)}
          </select>
        </div>
        <div className="hint" style={{ marginBottom: 6 }}>연평균 가격 변동률 — 예측이 아니라 가정입니다. 직접 바꿔 보세요.</div>
        <div className="row-3">
          <NumberField label={<><span className="scenario-dot" style={{ background: 'var(--series-conservative)' }} /> 보수적</>} unit="%" step="0.1" value={form.conservativeRate} onChange={set('conservativeRate')} />
          <NumberField label={<><span className="scenario-dot" style={{ background: 'var(--series-base)' }} /> 기준</>} unit="%" step="0.1" value={form.baseRate} onChange={set('baseRate')} />
          <NumberField label={<><span className="scenario-dot" style={{ background: 'var(--series-optimistic)' }} /> 낙관적</>} unit="%" step="0.1" value={form.optimisticRate} onChange={set('optimisticRate')} />
        </div>
      </fieldset>

      <details style={{ marginBottom: 14 }}>
        <summary className="small" style={{ cursor: 'pointer', color: 'var(--text-2)' }}>고급 설정 (세금 추정 조정)</summary>
        <div style={{ marginTop: 10 }}>
          <NumberField label={<Term k="PUBLIC_PRICE">공시가격 / 시세 비율</Term>} unit="%" value={form.publicPriceRatio} onChange={set('publicPriceRatio')} hint="보유세 추정용. 비우면 기본값(69%)" />
          <NumberField label="기타 취득비용 (인테리어·이사 등)" unit="만원" value={form.otherAcquisitionCost} onChange={set('otherAcquisitionCost')} />
          <NumberField label="예상 실거주 기간" unit="년" value={form.residenceYears} onChange={set('residenceYears')} hint="비우면 실거주=보유기간, 임대=0년 (양도세 비과세 판단용)" />
        </div>
      </details>

      <div className="form-actions">
        <button className="primary" type="submit" disabled={loading}>{loading ? '계산 중…' : '분석하기'}</button>
      </div>
    </form>
  );
}
