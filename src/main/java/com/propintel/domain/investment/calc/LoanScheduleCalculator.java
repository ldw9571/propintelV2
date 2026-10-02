package com.propintel.domain.investment.calc;

import com.propintel.domain.investment.calc.model.RepaymentMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * 원리금 상환 스케줄 계산기 (월 단위로 계산한 뒤 연 단위로 집계).
 * 금리는 대출기간 내내 고정이라고 가정한다.
 */
public final class LoanScheduleCalculator {

    private LoanScheduleCalculator() {}

    /** 연도별 집계 */
    public record YearRow(
            int year,
            long totalPayment,
            long interest,
            long principal,
            long endBalance,
            long cumulativeInterest,
            long cumulativePrincipal,
            long avgMonthlyPayment) {}

    public record Schedule(
            RepaymentMethod method,
            String methodLabel,
            long principal,
            double annualRate,
            int termYears,
            long firstMonthPayment,
            long lastMonthPayment,
            long totalInterest,
            long totalPayment,
            List<YearRow> years) {

        /** n년 경과 시점의 대출잔액 (n=0 이면 원금) */
        public long balanceAfterYear(int n) {
            if (n <= 0) return principal;
            if (n > years.size()) return 0;
            return years.get(n - 1).endBalance();
        }

        public long cumulativeInterest(int n) {
            if (n <= 0) return 0;
            return years.get(Math.min(n, years.size()) - 1).cumulativeInterest();
        }

        public long cumulativePrincipal(int n) {
            if (n <= 0) return 0;
            return years.get(Math.min(n, years.size()) - 1).cumulativePrincipal();
        }

        /** n번째 연도(1부터)의 이자/원금. 상환 완료 후에는 0 */
        public YearRow year(int n) {
            if (n < 1 || n > years.size()) return new YearRow(n, 0, 0, 0, 0,
                    years.isEmpty() ? 0 : years.get(years.size() - 1).cumulativeInterest(),
                    principal, 0);
            return years.get(n - 1);
        }
    }

    /** 원리금균등 월 상환액 */
    public static double equalPaymentMonthly(double principal, double annualRate, int months) {
        if (months <= 0) return principal;
        double r = annualRate / 12.0;
        if (r == 0) return principal / months;
        double f = Math.pow(1 + r, months);
        return principal * r * f / (f - 1);
    }

    /** 첫 해 연간 원리금 상환액 (DSR 계산용) */
    public static double firstYearDebtService(double principal, double annualRate, int termYears, RepaymentMethod method) {
        Schedule s = calculate(Math.round(principal), annualRate, termYears, method);
        return s.years().isEmpty() ? 0 : s.years().get(0).totalPayment();
    }

    public static Schedule calculate(long principal, double annualRate, int termYears, RepaymentMethod method) {
        if (principal < 0) throw new IllegalArgumentException("대출금은 0 이상이어야 합니다.");
        if (termYears < 1 || termYears > 50) throw new IllegalArgumentException("대출기간은 1~50년이어야 합니다.");
        if (annualRate < 0 || annualRate > 0.3) throw new IllegalArgumentException("금리는 0~30% 범위여야 합니다.");

        int months = termYears * 12;
        double r = annualRate / 12.0;
        double balance = principal;
        double fixedPayment = equalPaymentMonthly(principal, annualRate, months);
        double fixedPrincipal = (double) principal / months;

        List<YearRow> years = new ArrayList<>();
        double yPay = 0, yInt = 0, yPrin = 0, cumInt = 0, cumPrin = 0;
        double firstPayment = 0, lastPayment = 0;

        for (int m = 1; m <= months; m++) {
            double interest = balance * r;
            double prin = switch (method) {
                case EQUAL_PAYMENT -> fixedPayment - interest;
                case EQUAL_PRINCIPAL -> fixedPrincipal;
                case BULLET -> (m == months) ? balance : 0;
            };
            if (m == months) prin = balance; // 반올림 오차 정리
            double payment = interest + prin;
            balance -= prin;
            if (m == 1) firstPayment = payment;
            if (m == months) lastPayment = payment;

            yPay += payment; yInt += interest; yPrin += prin;
            if (m % 12 == 0) {
                cumInt += yInt; cumPrin += yPrin;
                years.add(new YearRow(m / 12, Math.round(yPay), Math.round(yInt), Math.round(yPrin),
                        Math.round(Math.max(balance, 0)), Math.round(cumInt), Math.round(cumPrin),
                        Math.round(yPay / 12)));
                yPay = yInt = yPrin = 0;
            }
        }
        return new Schedule(method, method.label(), principal, annualRate, termYears,
                Math.round(firstPayment), Math.round(lastPayment), Math.round(cumInt),
                Math.round(cumInt + cumPrin), years);
    }
}
