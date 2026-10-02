package com.propintel.domain.investment.calc;

/** 연 단위 현금흐름의 내부수익률(IRR). 부호 변화가 없거나 해가 없으면 null */
public final class Irr {

    private Irr() {}

    public static Double compute(double[] flows) {
        boolean pos = false, neg = false;
        for (double f : flows) { if (f > 0) pos = true; if (f < 0) neg = true; }
        if (!pos || !neg) return null;

        double lo = -0.9999, hi = 10.0;
        double fLo = npv(flows, lo), fHi = npv(flows, hi);
        if (Double.isNaN(fLo) || Double.isNaN(fHi) || fLo * fHi > 0) return null;
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2;
            double fMid = npv(flows, mid);
            if (Math.abs(fMid) < 1e-6) return mid;
            if (fLo * fMid < 0) { hi = mid; } else { lo = mid; fLo = fMid; }
        }
        return (lo + hi) / 2;
    }

    public static double npv(double[] flows, double rate) {
        double v = 0;
        for (int t = 0; t < flows.length; t++) v += flows[t] / Math.pow(1 + rate, t);
        return v;
    }
}
