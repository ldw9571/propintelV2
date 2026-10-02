package com.propintel.common.util;

/** 금액·비율 표시 (알림 문장 등) */
public final class Won {
    private Won() {}

    /** 825_624_000 → "8억 2,562만원" */
    public static String format(Long value) {
        if (value == null) return "-";
        long n = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        long eok = n / 100_000_000L;
        long man = (n % 100_000_000L) / 10_000L;
        if (eok > 0 && man > 0) return String.format("%s%,d억 %,d만원", sign, eok, man);
        if (eok > 0) return String.format("%s%,d억원", sign, eok);
        if (man > 0) return String.format("%s%,d만원", sign, man);
        return String.format("%s%,d원", sign, n);
    }

    /** 0.1023 → "+10.2%" */
    public static String pct(Double ratio) {
        if (ratio == null) return "-";
        return String.format("%+.1f%%", ratio * 100);
    }
}
