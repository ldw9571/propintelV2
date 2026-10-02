// 화면 입력값(만원, %) ↔ API 요청값(원, 소수) 변환

export const DEFAULT_FORM = {
  cash: 30000,               // 만원
  monthlyIncome: 700,        // 만원 (세전)
  existingLoanBalance: 0,
  existingAnnualDebtService: 0,
  regionCode: '11560',
  apartmentName: '',
  price: 80000,
  exclusiveArea: 84.9,
  housesOwned: 0,
  firstTimeBuyer: false,
  willSellExistingHome: false,
  usage: 'OWNER_OCCUPY',
  jeonseDeposit: 50000,
  rentDeposit: 5000,
  monthlyRent: 200,
  ownerMonthlyCost: 0,
  autoLoan: true,
  desiredLoanAmount: 30000,
  loanRate: 4.0,             // %
  loanTermYears: 30,
  repaymentMethod: 'EQUAL_PAYMENT',
  rateType: 'VARIABLE',
  holdingYears: 5,
  conservativeRate: -1,
  baseRate: 3,
  optimisticRate: 6,
  publicPriceRatio: 69,
  otherAcquisitionCost: 0,
  residenceYears: '',
};

const man = (v) => Math.round(Number(v || 0) * 10_000);
const ratio = (v) => Number(v || 0) / 100;

export function toRequest(f) {
  return {
    cash: man(f.cash),
    monthlyIncome: man(f.monthlyIncome),
    existingLoanBalance: man(f.existingLoanBalance),
    existingAnnualDebtService: man(f.existingAnnualDebtService),
    regionCode: f.regionCode,
    apartmentName: f.apartmentName?.trim() || null,
    price: man(f.price),
    exclusiveArea: Number(f.exclusiveArea || 0),
    housesOwned: Number(f.housesOwned),
    firstTimeBuyer: Number(f.housesOwned) === 0 && !!f.firstTimeBuyer,
    willSellExistingHome: Number(f.housesOwned) === 1 && !!f.willSellExistingHome,
    usage: f.usage,
    jeonseDeposit: f.usage === 'JEONSE' ? man(f.jeonseDeposit) : 0,
    rentDeposit: f.usage === 'MONTHLY_RENT' ? man(f.rentDeposit) : 0,
    monthlyRent: f.usage === 'MONTHLY_RENT' ? man(f.monthlyRent) : 0,
    ownerMonthlyCost: man(f.ownerMonthlyCost),
    desiredLoanAmount: f.autoLoan ? null : man(f.desiredLoanAmount),
    loanRate: ratio(f.loanRate),
    loanTermYears: Number(f.loanTermYears),
    repaymentMethod: f.repaymentMethod,
    rateType: f.rateType,
    holdingYears: Number(f.holdingYears),
    conservativeRate: ratio(f.conservativeRate),
    baseRate: ratio(f.baseRate),
    optimisticRate: ratio(f.optimisticRate),
    publicPriceRatio: f.publicPriceRatio === '' ? null : ratio(f.publicPriceRatio),
    otherAcquisitionCost: man(f.otherAcquisitionCost),
    residenceYears: f.residenceYears === '' ? null : Number(f.residenceYears),
  };
}

const toMan = (v) => Math.round((Number(v || 0) / 10_000) * 100) / 100;
const toPct = (v) => Math.round(Number(v || 0) * 100 * 1000) / 1000;

/** 저장된 요청값을 다시 화면 입력값으로 */
export function fromRequest(r) {
  return {
    ...DEFAULT_FORM,
    cash: toMan(r.cash),
    monthlyIncome: toMan(r.monthlyIncome),
    existingLoanBalance: toMan(r.existingLoanBalance),
    existingAnnualDebtService: toMan(r.existingAnnualDebtService),
    regionCode: r.regionCode,
    apartmentName: r.apartmentName || '',
    price: toMan(r.price),
    exclusiveArea: r.exclusiveArea,
    housesOwned: r.housesOwned,
    firstTimeBuyer: r.firstTimeBuyer,
    willSellExistingHome: r.willSellExistingHome,
    usage: r.usage,
    jeonseDeposit: r.usage === 'JEONSE' ? toMan(r.jeonseDeposit) : DEFAULT_FORM.jeonseDeposit,
    rentDeposit: r.usage === 'MONTHLY_RENT' ? toMan(r.rentDeposit) : DEFAULT_FORM.rentDeposit,
    monthlyRent: r.usage === 'MONTHLY_RENT' ? toMan(r.monthlyRent) : DEFAULT_FORM.monthlyRent,
    ownerMonthlyCost: toMan(r.ownerMonthlyCost),
    autoLoan: r.desiredLoanAmount === null || r.desiredLoanAmount === undefined,
    desiredLoanAmount: r.desiredLoanAmount ? toMan(r.desiredLoanAmount) : DEFAULT_FORM.desiredLoanAmount,
    loanRate: toPct(r.loanRate),
    loanTermYears: r.loanTermYears,
    repaymentMethod: r.repaymentMethod,
    rateType: r.rateType,
    holdingYears: r.holdingYears,
    conservativeRate: toPct(r.conservativeRate),
    baseRate: toPct(r.baseRate),
    optimisticRate: toPct(r.optimisticRate),
    publicPriceRatio: r.publicPriceRatio == null ? '' : toPct(r.publicPriceRatio),
    otherAcquisitionCost: toMan(r.otherAcquisitionCost),
    residenceYears: r.residenceYears ?? '',
  };
}
