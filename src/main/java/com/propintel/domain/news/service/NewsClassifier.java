package com.propintel.domain.news.service;

import com.propintel.domain.news.entity.NewsCategory;

import java.util.List;

/** 제목·요약문 키워드로 분류하고, "지역에 영향을 줄 수 있는 내용이 언급됐는지"를 사실 그대로 적는다 */
public final class NewsClassifier {

    private NewsClassifier() {}

    private static final List<String> REDEV = List.of("재건축", "재개발", "정비사업", "정비구역", "조합", "리모델링", "신통기획", "모아타운");
    private static final List<String> TRANSPORT = List.of("GTX", "지하철", "노선", "역세권", "개통", "착공", "도로", "철도", "신안산선", "경전철", "광역", "교통");
    private static final List<String> SUPPLY = List.of("입주", "분양", "청약", "공급", "미분양", "착공 물량", "신축");
    private static final List<String> POLICY = List.of("규제", "대책", "토지거래허가", "조정대상", "투기과열", "LTV", "DSR", "취득세", "양도세", "보유세", "종부세", "세제", "정부");
    private static final List<String> RATE = List.of("금리", "기준금리", "주담대", "대출", "한국은행", "금통위");

    public static NewsCategory classify(String title, String description, boolean regional) {
        String t = (title == null ? "" : title) + " " + (description == null ? "" : description);
        if (containsAny(t, REDEV)) return NewsCategory.REDEVELOPMENT;
        if (containsAny(t, TRANSPORT)) return NewsCategory.TRANSPORT;
        if (containsAny(t, SUPPLY)) return NewsCategory.SUPPLY;
        if (containsAny(t, POLICY)) return NewsCategory.POLICY;
        if (containsAny(t, RATE)) return NewsCategory.RATE;
        return regional ? NewsCategory.REGION : NewsCategory.OTHER;
    }

    /** 예측이 아니라 "기사에 무엇이 언급됐는지"만 적는다 */
    public static String impactNote(NewsCategory c) {
        return switch (c) {
            case REDEVELOPMENT -> "기사에 재건축·재개발·정비사업 관련 내용이 언급되어 있습니다.";
            case TRANSPORT -> "기사에 교통(노선·역·도로) 또는 개발 계획 관련 내용이 언급되어 있습니다.";
            case SUPPLY -> "기사에 입주·분양 등 주택 공급 관련 내용이 언급되어 있습니다.";
            case POLICY -> "기사에 대출·세금·규제 등 정책 관련 내용이 언급되어 있습니다.";
            case RATE -> "기사에 금리·대출 관련 내용이 언급되어 있습니다.";
            default -> "기사 제목·요약만으로는 지역 가격과 관련된 내용을 확인할 수 없습니다.";
        };
    }

    static boolean containsAny(String text, List<String> words) {
        return words.stream().anyMatch(text::contains);
    }
}
