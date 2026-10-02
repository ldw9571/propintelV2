package com.propintel.infra.molit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 국토교통부 아파트 매매·전월세 실거래 API 클라이언트 (전체 필드 + 페이지 처리).
 * 기존 MolitApiClient 와 달리 해제 여부(cdealType), 건축연도, 계약구분 등을 함께 읽는다.
 *
 * 매매: 기존 MolitApiClient 와 같은 /RTMSDataSvcAptTradeDev (molit.trade-path 로 변경 가능)
 * 전월세: /RTMSDataSvcAptRent/getRTMSDataSvcAptRent
 */
@Component
public class MolitRawClient {

    private static final Logger log = LoggerFactory.getLogger(MolitRawClient.class);
    private static final int PAGE_SIZE = 1000;

    private final WebClient.Builder webClientBuilder;
    private final XmlMapper xmlMapper = new XmlMapper();

    @Value("${molit.api-key:}")
    private String apiKey;

    @Value("${molit.base-url:https://apis.data.go.kr/1613000}")
    private String baseUrl;

    @Value("${molit.trade-path:/RTMSDataSvcAptTradeDev/getRTMSDataSvcAptTradeDev}")
    private String tradePath;

    @Value("${molit.rent-path:/RTMSDataSvcAptRent/getRTMSDataSvcAptRent}")
    private String rentPath;

    public MolitRawClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /** 매매 1건 */
    public record SaleItem(String aptName, String umdName, String jibun, double area, int floor, long priceWon,
                           int year, int month, int day, Integer buildYear, boolean canceled, String dealingType) {}

    /** 전월세 1건 */
    public record RentItem(String aptName, String umdName, String jibun, double area, int floor, long depositWon,
                           long monthlyRentWon, int year, int month, int day, Integer buildYear,
                           String contractType, String contractTerm, Long preDepositWon, Long preMonthlyRentWon) {}

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<SaleItem> fetchSales(String lawdCd, String dealYmd) {
        List<SaleItem> out = new ArrayList<>();
        for (JsonNode n : fetchAll(tradePath, lawdCd, dealYmd)) {
            try {
                out.add(new SaleItem(text(n, "aptNm"), text(n, "umdNm"), text(n, "jibun"),
                        dbl(n, "excluUseAr"), intOr(n, "floor", 0), manwon(n, "dealAmount"),
                        intOr(n, "dealYear", 0), intOr(n, "dealMonth", 0), intOr(n, "dealDay", 0),
                        intOrNull(n, "buildYear"), "O".equalsIgnoreCase(text(n, "cdealType")),
                        text(n, "dealingGbn")));
            } catch (RuntimeException e) {
                log.debug("매매 항목 파싱 건너뜀: {}", e.getMessage());
            }
        }
        return out;
    }

    public List<RentItem> fetchRents(String lawdCd, String dealYmd) {
        List<RentItem> out = new ArrayList<>();
        for (JsonNode n : fetchAll(rentPath, lawdCd, dealYmd)) {
            try {
                String pre = text(n, "preDeposit");
                String preRent = text(n, "preMonthlyRent");
                out.add(new RentItem(text(n, "aptNm"), text(n, "umdNm"), text(n, "jibun"),
                        dbl(n, "excluUseAr"), intOr(n, "floor", 0), manwon(n, "deposit"),
                        manwonOrZero(n, "monthlyRent"),
                        intOr(n, "dealYear", 0), intOr(n, "dealMonth", 0), intOr(n, "dealDay", 0),
                        intOrNull(n, "buildYear"), text(n, "contractType"), text(n, "contractTerm"),
                        pre == null || pre.isBlank() ? null : manwon(n, "preDeposit"),
                        preRent == null || preRent.isBlank() ? null : manwonOrZero(n, "preMonthlyRent")));
            } catch (RuntimeException e) {
                log.debug("전월세 항목 파싱 건너뜀: {}", e.getMessage());
            }
        }
        return out;
    }

    private List<JsonNode> fetchAll(String path, String lawdCd, String dealYmd) {
        if (!isConfigured()) {
            throw new IllegalStateException("국토교통부 API 키(MOLIT_API_KEY)가 설정되지 않았습니다.");
        }
        List<JsonNode> items = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = baseUrl + path + "?serviceKey=" + apiKey + "&LAWD_CD=" + lawdCd
                    + "&DEAL_YMD=" + dealYmd + "&pageNo=" + page + "&numOfRows=" + PAGE_SIZE;
            String xml = webClientBuilder.build().get().uri(url).retrieve()
                    .bodyToMono(String.class).block(Duration.ofSeconds(30));
            if (xml == null || xml.isBlank()) break;
            JsonNode root;
            try {
                root = xmlMapper.readTree(xml);
            } catch (Exception e) {
                throw new IllegalStateException("국토부 API 응답을 해석할 수 없습니다: " + abbreviate(xml));
            }
            String code = root.path("header").path("resultCode").asText("");
            if (!code.isEmpty() && !code.equals("000") && !code.equals("00")) {
                throw new IllegalStateException("국토부 API 오류 " + code + ": " + root.path("header").path("resultMsg").asText());
            }
            if (root.has("cmmMsgHeader")) {
                throw new IllegalStateException("국토부 API 인증/호출 오류: " + root.path("cmmMsgHeader").path("returnAuthMsg").asText(abbreviate(xml)));
            }
            JsonNode body = root.path("body");
            JsonNode item = body.path("items").path("item");
            if (item.isArray()) item.forEach(items::add);
            else if (item.isObject()) items.add(item);
            int total = body.path("totalCount").asInt(0);
            if (page * PAGE_SIZE >= total) break;
            page++;
            sleep(150);
        }
        return items;
    }

    private static String text(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v == null || v.isNull() ? null : v.asText().trim();
    }

    private static double dbl(JsonNode n, String f) {
        return Double.parseDouble(text(n, f));
    }

    private static int intOr(JsonNode n, String f, int d) {
        String s = text(n, f);
        if (s == null || s.isBlank()) return d;
        return Integer.parseInt(s.trim());
    }

    private static Integer intOrNull(JsonNode n, String f) {
        String s = text(n, f);
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    /** "12,500" (만원) → 원 */
    private static long manwon(JsonNode n, String f) {
        return Long.parseLong(text(n, f).replace(",", "").trim()) * 10_000L;
    }

    private static long manwonOrZero(JsonNode n, String f) {
        String s = text(n, f);
        if (s == null || s.isBlank()) return 0;
        return Long.parseLong(s.replace(",", "").trim()) * 10_000L;
    }

    private static String abbreviate(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
