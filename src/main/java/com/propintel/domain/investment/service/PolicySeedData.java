package com.propintel.domain.investment.service;

import com.propintel.common.source.SourceInfo;
import com.propintel.domain.investment.calc.model.BorrowerType;
import com.propintel.domain.investment.calc.model.RegionType;
import com.propintel.domain.investment.entity.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 초기 규제·세율·지역 데이터 (2026-09-28 입력).
 * 모두 verified=false — 2차 출처(언론·블로그 요약) 기준이므로 화면에 "미검증"으로 표시된다.
 * 공식 원문과 대조한 뒤 "기준 데이터" 화면에서 값·출처·검증여부를 갱신하세요.
 */
final class PolicySeedData {

    private PolicySeedData() {}

    private static SourceInfo src(String key) {
        return switch (key) {
            case "R1015" -> new SourceInfo("국토교통부·금융위원회 「주택시장 안정화 대책」(2025.10.15) — 서울 전역·경기 12곳 투기과열지구·조정대상지역·토지거래허가구역 지정", "https://www.molit.go.kr", LocalDate.parse("2025-10-15"), false, "2차 출처(언론 요약) 기준 입력. 국토부 고시 원문 확인 필요");
            case "R0701" -> new SourceInfo("대한민국 정책브리핑 「동탄·기흥·구리 투기과열지구·조정대상지역 지정」(2026.6.30 발표, 7.1 효력)", "https://www.korea.kr/news/policyNewsView.do?newsId=148967354", LocalDate.parse("2026-06-30"), false, "발표 기사 제목 기준 입력. 원문 확인 필요");
            case "RNON" -> new SourceInfo("규제지역 지정 목록(2026.7.1 기준)에 포함되지 않음 → 비규제로 분류", null, LocalDate.parse("2026-07-01"), false, "지정 목록 외 지역. 추가 지정 여부 확인 필요");
            case "LOAN" -> new SourceInfo("금융위원회 6.27 가계부채 관리방안(2025.6.27)·10.15 주택시장 안정화 대책(2025.10.15) — 2차 출처 요약", "https://www.achimgol.com/2026/07/mortgage-regulation.html", LocalDate.parse("2026-07-01"), false, null);
            case "CAP" -> new SourceInfo("금융위원회 10.15 대책: 수도권·규제지역 주담대 주택가격별 한도 / 6.27 대책: 수도권 6억 한도 — 2차 출처 요약", "https://www.easyzetec.com/blog/stress-dsr-3-stage-mortgage-limit-2026", LocalDate.parse("2025-10-16"), false, "원문 대조 전 데이터 — 실제 적용 전 공식 출처 확인 필요");
            case "DSR" -> new SourceInfo("금융위원회 「3단계 스트레스 DSR 시행방안」 및 10.15 대책(수도권·규제지역 스트레스 금리 3.0%p)", "https://www.fsc.go.kr/no010101/84617", LocalDate.parse("2026-07-01"), false, "은행권 DSR 40%. 금리유형별 반영비율(변동 100%·혼합 80%·주기 40%)은 3단계 시행방안 기준 가정. 지방 1.5%p는 2026.7 적용 보도 기준");
            case "ACQ" -> new SourceInfo("지방세법 제11조(주택 유상취득 세율)·제13조의2(다주택 중과)·지방교육세·농어촌특별세법 — 2차 출처 요약", "https://achimida.com/%EB%B6%80%EB%8F%99%EC%82%B0-%EC%B7%A8%EB%93%9D%EC%84%B8-%EC%B4%9D%EC%A0%95%EB%A6%AC-2026/", LocalDate.parse("2026-01-01"), false, "원문 대조 전 데이터 — 실제 적용 전 공식 출처 확인 필요");
            case "BRK" -> new SourceInfo("공인중개사법 시행규칙 [별표1] 주택 매매 중개보수 상한요율(2021.10.19 시행)", "https://www.law.go.kr", LocalDate.parse("2021-10-19"), false, "원문 대조 전 데이터 — 실제 적용 전 공식 출처 확인 필요");
            case "CGT" -> new SourceInfo("소득세법 제55조(기본세율)·제95조(장기보유특별공제)·제104조(단기양도세율)·시행령 제154조(1세대1주택 비과세)", "https://www.law.go.kr", LocalDate.parse("2023-01-01"), false, "원문 대조 전 데이터 — 실제 적용 전 공식 출처 확인 필요");
            case "PT" -> new SourceInfo("지방세법 제111조(재산세 세율)·제112조(도시지역분)·시행령 공정시장가액비율(1세대1주택 특례)", "https://www.law.go.kr", LocalDate.parse("2025-01-01"), false, "원문 대조 전 데이터 — 실제 적용 전 공식 출처 확인 필요");
            case "EST" -> new SourceInfo("서비스 자체 추정치(사용자가 수정 가능)", null, LocalDate.parse("2026-09-28"), false, "추정치이므로 실제 비용과 다를 수 있음");
            default -> throw new IllegalArgumentException(key);
        };
    }

    private static SourceInfo loanSrc(String note) {
        SourceInfo base = src("LOAN");
        return new SourceInfo(base.getSourceName(), base.getSourceUrl(), base.getBaseDate(), false, note);
    }

    private static PolicyRegion r(String code, String sido, String sigungu, boolean metro, boolean regulated,
                                  boolean landPermit, boolean collectable, String srcKey) {
        return new PolicyRegion(code, sido, sigungu, metro, regulated, landPermit, collectable, src(srcKey));
    }

    static List<PolicyRegion> regions() {
        List<PolicyRegion> l = new ArrayList<>();
        l.add(r("11110", "서울특별시", "종로구", true, true, true, true, "R1015"));
        l.add(r("11140", "서울특별시", "중구", true, true, true, true, "R1015"));
        l.add(r("11170", "서울특별시", "용산구", true, true, true, true, "R1015"));
        l.add(r("11200", "서울특별시", "성동구", true, true, true, true, "R1015"));
        l.add(r("11215", "서울특별시", "광진구", true, true, true, true, "R1015"));
        l.add(r("11230", "서울특별시", "동대문구", true, true, true, true, "R1015"));
        l.add(r("11260", "서울특별시", "중랑구", true, true, true, true, "R1015"));
        l.add(r("11290", "서울특별시", "성북구", true, true, true, true, "R1015"));
        l.add(r("11305", "서울특별시", "강북구", true, true, true, true, "R1015"));
        l.add(r("11320", "서울특별시", "도봉구", true, true, true, true, "R1015"));
        l.add(r("11350", "서울특별시", "노원구", true, true, true, true, "R1015"));
        l.add(r("11380", "서울특별시", "은평구", true, true, true, true, "R1015"));
        l.add(r("11410", "서울특별시", "서대문구", true, true, true, true, "R1015"));
        l.add(r("11440", "서울특별시", "마포구", true, true, true, true, "R1015"));
        l.add(r("11470", "서울특별시", "양천구", true, true, true, true, "R1015"));
        l.add(r("11500", "서울특별시", "강서구", true, true, true, true, "R1015"));
        l.add(r("11530", "서울특별시", "구로구", true, true, true, true, "R1015"));
        l.add(r("11545", "서울특별시", "금천구", true, true, true, true, "R1015"));
        l.add(r("11560", "서울특별시", "영등포구", true, true, true, true, "R1015"));
        l.add(r("11590", "서울특별시", "동작구", true, true, true, true, "R1015"));
        l.add(r("11620", "서울특별시", "관악구", true, true, true, true, "R1015"));
        l.add(r("11650", "서울특별시", "서초구", true, true, true, true, "R1015"));
        l.add(r("11680", "서울특별시", "강남구", true, true, true, true, "R1015"));
        l.add(r("11710", "서울특별시", "송파구", true, true, true, true, "R1015"));
        l.add(r("11740", "서울특별시", "강동구", true, true, true, true, "R1015"));
        l.add(r("41290", "경기도", "과천시", true, true, true, true, "R1015"));
        l.add(r("41210", "경기도", "광명시", true, true, true, true, "R1015"));
        l.add(r("41131", "경기도", "성남시 수정구", true, true, true, true, "R1015"));
        l.add(r("41133", "경기도", "성남시 중원구", true, true, true, true, "R1015"));
        l.add(r("41135", "경기도", "성남시 분당구", true, true, true, true, "R1015"));
        l.add(r("41111", "경기도", "수원시 장안구", true, true, true, true, "R1015"));
        l.add(r("41115", "경기도", "수원시 팔달구", true, true, true, true, "R1015"));
        l.add(r("41117", "경기도", "수원시 영통구", true, true, true, true, "R1015"));
        l.add(r("41173", "경기도", "안양시 동안구", true, true, true, true, "R1015"));
        l.add(r("41465", "경기도", "용인시 수지구", true, true, true, true, "R1015"));
        l.add(r("41430", "경기도", "의왕시", true, true, true, true, "R1015"));
        l.add(r("41450", "경기도", "하남시", true, true, true, true, "R1015"));
        l.add(r("41463", "경기도", "용인시 기흥구", true, true, true, true, "R0701"));
        l.add(r("41310", "경기도", "구리시", true, true, true, true, "R0701"));
        l.add(r("41590-DT", "경기도", "화성시 동탄구", true, true, true, false, "R0701"));
        l.add(r("41113", "경기도", "수원시 권선구", true, false, false, true, "RNON"));
        l.add(r("41171", "경기도", "안양시 만안구", true, false, false, true, "RNON"));
        l.add(r("41150", "경기도", "의정부시", true, false, false, true, "RNON"));
        l.add(r("41192", "경기도", "부천시 원미구", true, false, false, true, "RNON"));
        l.add(r("41194", "경기도", "부천시 소사구", true, false, false, true, "RNON"));
        l.add(r("41196", "경기도", "부천시 오정구", true, false, false, true, "RNON"));
        l.add(r("41220", "경기도", "평택시", true, false, false, true, "RNON"));
        l.add(r("41271", "경기도", "안산시 상록구", true, false, false, true, "RNON"));
        l.add(r("41273", "경기도", "안산시 단원구", true, false, false, true, "RNON"));
        l.add(r("41281", "경기도", "고양시 덕양구", true, false, false, true, "RNON"));
        l.add(r("41285", "경기도", "고양시 일산동구", true, false, false, true, "RNON"));
        l.add(r("41287", "경기도", "고양시 일산서구", true, false, false, true, "RNON"));
        l.add(r("41360", "경기도", "남양주시", true, false, false, true, "RNON"));
        l.add(r("41390", "경기도", "시흥시", true, false, false, true, "RNON"));
        l.add(r("41410", "경기도", "군포시", true, false, false, true, "RNON"));
        l.add(r("41461", "경기도", "용인시 처인구", true, false, false, true, "RNON"));
        l.add(r("41480", "경기도", "파주시", true, false, false, true, "RNON"));
        l.add(r("41570", "경기도", "김포시", true, false, false, true, "RNON"));
        l.add(r("41590", "경기도", "화성시", true, false, false, true, "RNON"));
        l.add(r("28185", "인천광역시", "연수구", true, false, false, true, "RNON"));
        l.add(r("28200", "인천광역시", "남동구", true, false, false, true, "RNON"));
        l.add(r("28237", "인천광역시", "부평구", true, false, false, true, "RNON"));
        l.add(r("28260", "인천광역시", "서구", true, false, false, true, "RNON"));
        l.add(r("26350", "부산광역시", "해운대구", false, false, false, true, "RNON"));
        l.add(r("26500", "부산광역시", "수영구", false, false, false, true, "RNON"));
        l.add(r("26260", "부산광역시", "동래구", false, false, false, true, "RNON"));
        l.add(r("27260", "대구광역시", "수성구", false, false, false, true, "RNON"));
        l.add(r("30200", "대전광역시", "유성구", false, false, false, true, "RNON"));
        l.add(r("30170", "대전광역시", "서구", false, false, false, true, "RNON"));
        l.add(r("29155", "광주광역시", "남구", false, false, false, true, "RNON"));
        l.add(r("31140", "울산광역시", "남구", false, false, false, true, "RNON"));
        l.add(r("36110", "세종특별자치시", null, false, false, false, true, "RNON"));
        l.add(r("99999", "기타 지방(비수도권)", null, false, false, false, false, "RNON"));
        return l;
    }

    static List<LoanRegulation> loanRules() {
        LocalDate from = LocalDate.parse("2025-10-16");
        List<LoanRegulation> l = new ArrayList<>();
        l.add(new LoanRegulation(RegionType.REGULATED, BorrowerType.FIRST_TIME, true, 0.7, true, from, loanSrc("규제지역 생애최초 LTV 70%, 6개월 내 전입 의무")));
        l.add(new LoanRegulation(RegionType.REGULATED, BorrowerType.NO_HOUSE, true, 0.4, true, from, loanSrc("규제지역 무주택자 LTV 40%, 6개월 내 전입 의무")));
        l.add(new LoanRegulation(RegionType.REGULATED, BorrowerType.ONE_HOUSE_DISPOSING, true, 0.4, true, from, loanSrc("1주택자는 기존주택 처분 조건부로만 가능(처분기한 확인 필요)")));
        l.add(new LoanRegulation(RegionType.REGULATED, BorrowerType.ONE_HOUSE, false, 0, true, from, loanSrc("규제지역 1주택자 추가 주택 구입 목적 주담대 불가")));
        l.add(new LoanRegulation(RegionType.REGULATED, BorrowerType.MULTI_HOUSE, false, 0, true, from, loanSrc("규제지역 다주택자 주택 구입 목적 주담대 불가")));
        l.add(new LoanRegulation(RegionType.METRO_NON_REGULATED, BorrowerType.FIRST_TIME, true, 0.7, true, from, loanSrc("수도권 생애최초 LTV 70%, 6개월 내 전입 의무(6.27 대책)")));
        l.add(new LoanRegulation(RegionType.METRO_NON_REGULATED, BorrowerType.NO_HOUSE, true, 0.7, true, from, loanSrc("수도권 비규제 무주택 LTV 70%, 6개월 내 전입 의무(6.27 대책)")));
        l.add(new LoanRegulation(RegionType.METRO_NON_REGULATED, BorrowerType.ONE_HOUSE_DISPOSING, true, 0.7, true, from, loanSrc("기존주택 처분 조건부")));
        l.add(new LoanRegulation(RegionType.METRO_NON_REGULATED, BorrowerType.ONE_HOUSE, false, 0, true, from, loanSrc("수도권 추가 주택 구입 목적 주담대 불가(6.27 대책)")));
        l.add(new LoanRegulation(RegionType.METRO_NON_REGULATED, BorrowerType.MULTI_HOUSE, false, 0, true, from, loanSrc("수도권 다주택자 주택 구입 목적 주담대 불가(6.27 대책)")));
        l.add(new LoanRegulation(RegionType.NON_METRO, BorrowerType.FIRST_TIME, true, 0.8, false, from, loanSrc("지방 생애최초 LTV 80%")));
        l.add(new LoanRegulation(RegionType.NON_METRO, BorrowerType.NO_HOUSE, true, 0.7, false, from, loanSrc("지방 비규제 LTV 70%")));
        l.add(new LoanRegulation(RegionType.NON_METRO, BorrowerType.ONE_HOUSE_DISPOSING, true, 0.7, false, from, loanSrc(null)));
        l.add(new LoanRegulation(RegionType.NON_METRO, BorrowerType.ONE_HOUSE, true, 0.6, false, from, loanSrc("지방 추가 구입 시 LTV 60% 가정")));
        l.add(new LoanRegulation(RegionType.NON_METRO, BorrowerType.MULTI_HOUSE, true, 0.6, false, from, loanSrc("지방 다주택 LTV 60% 가정")));
        return l;
    }

    static List<LoanAmountCap> caps() {
        LocalDate from = LocalDate.parse("2025-10-16");
        List<LoanAmountCap> l = new ArrayList<>();
        l.add(new LoanAmountCap(RegionType.REGULATED, 0L, 1500000000L, 600000000L, from, src("CAP")));
        l.add(new LoanAmountCap(RegionType.REGULATED, 1500000000L, 2500000000L, 400000000L, from, src("CAP")));
        l.add(new LoanAmountCap(RegionType.REGULATED, 2500000000L, null, 200000000L, from, src("CAP")));
        l.add(new LoanAmountCap(RegionType.METRO_NON_REGULATED, 0L, null, 600000000L, from, src("CAP")));
        return l;
    }

    static List<DsrRegulation> dsr() {
        LocalDate from = LocalDate.parse("2026-07-01");
        List<DsrRegulation> l = new ArrayList<>();
        l.add(new DsrRegulation(RegionType.REGULATED, 0.4, 0.03, 1.0, 0.8, 0.4, from, src("DSR")));
        l.add(new DsrRegulation(RegionType.METRO_NON_REGULATED, 0.4, 0.03, 1.0, 0.8, 0.4, from, src("DSR")));
        l.add(new DsrRegulation(RegionType.NON_METRO, 0.4, 0.015, 1.0, 0.8, 0.4, from, src("DSR")));
        return l;
    }

    static List<RateBracket> brackets() {
        List<RateBracket> l = new ArrayList<>();
        l.add(new RateBracket("BROKERAGE_SALE", 0L, 50000000L, 0.006, 250000L, 0L, src("BRK").getBaseDate(), src("BRK")));
        l.add(new RateBracket("BROKERAGE_SALE", 50000000L, 200000000L, 0.005, 800000L, 0L, src("BRK").getBaseDate(), src("BRK")));
        l.add(new RateBracket("BROKERAGE_SALE", 200000000L, 900000000L, 0.004, null, 0L, src("BRK").getBaseDate(), src("BRK")));
        l.add(new RateBracket("BROKERAGE_SALE", 900000000L, 1200000000L, 0.005, null, 0L, src("BRK").getBaseDate(), src("BRK")));
        l.add(new RateBracket("BROKERAGE_SALE", 1200000000L, 1500000000L, 0.006, null, 0L, src("BRK").getBaseDate(), src("BRK")));
        l.add(new RateBracket("BROKERAGE_SALE", 1500000000L, null, 0.007, null, 0L, src("BRK").getBaseDate(), src("BRK")));
        l.add(new RateBracket("CGT", 0L, 14000000L, 0.06, null, 0L, src("CGT").getBaseDate(), src("CGT")));
        l.add(new RateBracket("CGT", 14000000L, 50000000L, 0.15, null, 1260000L, src("CGT").getBaseDate(), src("CGT")));
        l.add(new RateBracket("CGT", 50000000L, 88000000L, 0.24, null, 5760000L, src("CGT").getBaseDate(), src("CGT")));
        l.add(new RateBracket("CGT", 88000000L, 150000000L, 0.35, null, 15440000L, src("CGT").getBaseDate(), src("CGT")));
        l.add(new RateBracket("CGT", 150000000L, 300000000L, 0.38, null, 19940000L, src("CGT").getBaseDate(), src("CGT")));
        l.add(new RateBracket("CGT", 300000000L, 500000000L, 0.4, null, 25940000L, src("CGT").getBaseDate(), src("CGT")));
        l.add(new RateBracket("CGT", 500000000L, 1000000000L, 0.42, null, 35940000L, src("CGT").getBaseDate(), src("CGT")));
        l.add(new RateBracket("CGT", 1000000000L, null, 0.45, null, 65940000L, src("CGT").getBaseDate(), src("CGT")));
        l.add(new RateBracket("PROPERTY_TAX", 0L, 60000000L, 0.001, null, 0L, src("PT").getBaseDate(), src("PT")));
        l.add(new RateBracket("PROPERTY_TAX", 60000000L, 150000000L, 0.0015, null, 30000L, src("PT").getBaseDate(), src("PT")));
        l.add(new RateBracket("PROPERTY_TAX", 150000000L, 300000000L, 0.0025, null, 180000L, src("PT").getBaseDate(), src("PT")));
        l.add(new RateBracket("PROPERTY_TAX", 300000000L, null, 0.004, null, 630000L, src("PT").getBaseDate(), src("PT")));
        return l;
    }

    static List<PolicyParameter> params() {
        List<PolicyParameter> l = new ArrayList<>();
        l.add(new PolicyParameter("acq.basic.lowThreshold", 600000000, "1주택 취득세 1% 적용 상한 가격", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.basic.highThreshold", 900000000, "1주택 취득세 3% 적용 시작 가격", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.basic.lowRate", 0.01, "1주택 6억 이하 취득세율", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.basic.highRate", 0.03, "1주택 9억 초과 취득세율", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.heavy.2house.regulated", 0.08, "조정대상지역 2주택 취득세 중과세율", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.heavy.3house.regulated", 0.12, "조정대상지역 3주택 취득세 중과세율", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.heavy.3house.nonregulated", 0.08, "비조정지역 3주택 취득세 중과세율", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.heavy.4house", 0.12, "4주택 이상 취득세 중과세율", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.edu.basicFactor", 0.1, "지방교육세 = 취득세율 × 0.1 (주택 유상거래 기본)", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.edu.heavyRate", 0.004, "중과 시 지방교육세율 0.4%", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.rural.areaThreshold", 85, "농어촌특별세 과세 전용면적 기준(㎡ 초과)", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.rural.basic", 0.002, "농특세 0.2% (기본세율)", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.rural.heavy8", 0.006, "농특세 0.6% (8% 중과)", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.rural.heavy12", 0.01, "농특세 1.0% (12% 중과)", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.firstTime.maxRelief", 2000000, "생애최초 취득세 감면 한도", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.firstTime.priceLimit", 1200000000, "생애최초 감면 대상 취득가액 상한", src("ACQ").getBaseDate(), src("ACQ")));
        l.add(new PolicyParameter("acq.miscCostRate", 0.002, "법무사 보수·국민주택채권 할인·인지세 등 부대비용 추정 비율", LocalDate.parse("2020-01-01"), src("EST")));
        l.add(new PolicyParameter("brokerage.vatRate", 0.1, "중개보수 부가가치세(일반과세자 가정)", LocalDate.parse("2020-01-01"), src("EST")));
        l.add(new PolicyParameter("ptax.publicPriceRatio.default", 0.69, "공시가격/시세 비율 기본 가정(단지별로 다름)", LocalDate.parse("2020-01-01"), src("EST")));
        l.add(new PolicyParameter("ptax.fmv.standard", 0.6, "재산세 공정시장가액비율(일반)", src("PT").getBaseDate(), src("PT")));
        l.add(new PolicyParameter("ptax.fmv.oneHouse.le3", 0.43, "1세대1주택 공정시장가액비율(공시 3억 이하)", src("PT").getBaseDate(), src("PT")));
        l.add(new PolicyParameter("ptax.fmv.oneHouse.le6", 0.44, "1세대1주택 공정시장가액비율(공시 3~6억)", src("PT").getBaseDate(), src("PT")));
        l.add(new PolicyParameter("ptax.fmv.oneHouse.over6", 0.45, "1세대1주택 공정시장가액비율(공시 6억 초과)", src("PT").getBaseDate(), src("PT")));
        l.add(new PolicyParameter("ptax.urbanRate", 0.0014, "재산세 도시지역분 세율", src("PT").getBaseDate(), src("PT")));
        l.add(new PolicyParameter("ptax.eduRate", 0.2, "재산세분 지방교육세율", src("PT").getBaseDate(), src("PT")));
        l.add(new PolicyParameter("ptax.comprehensive.oneHouseThreshold", 1200000000, "종부세 1세대1주택 공시가격 공제기준", src("PT").getBaseDate(), src("PT")));
        l.add(new PolicyParameter("ptax.comprehensive.threshold", 900000000, "종부세 일반 공시가격 공제기준", src("PT").getBaseDate(), src("PT")));
        l.add(new PolicyParameter("cgt.oneHouse.exemptPrice", 1200000000, "1세대1주택 비과세 양도가액 기준(고가주택 기준)", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.oneHouse.minHoldYears", 2, "1세대1주택 비과세 최소 보유기간(년)", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.oneHouse.minResidenceYearsRegulated", 2, "조정지역 취득 시 비과세 최소 거주기간(년)", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.basicDeduction", 2500000, "양도소득 기본공제", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.shortTerm.lt1", 0.7, "주택 보유 1년 미만 양도세율", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.shortTerm.lt2", 0.6, "주택 보유 2년 미만 양도세율", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.localTaxRate", 0.1, "지방소득세(양도소득세의 10%)", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.ltd.minYears", 3, "장기보유특별공제 최소 보유기간", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.ltd.general.perYear", 0.02, "일반 장특공 연 공제율", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.ltd.general.max", 0.3, "일반 장특공 최대", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.ltd.oneHouse.minResidence", 2, "1주택 장특공(표2) 최소 거주기간", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.ltd.oneHouse.holdPerYear", 0.04, "1주택 장특공 보유 연 공제율", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.ltd.oneHouse.holdMax", 0.4, "1주택 장특공 보유분 최대", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.ltd.oneHouse.residePerYear", 0.04, "1주택 장특공 거주 연 공제율", src("CGT").getBaseDate(), src("CGT")));
        l.add(new PolicyParameter("cgt.ltd.oneHouse.resideMax", 0.4, "1주택 장특공 거주분 최대", src("CGT").getBaseDate(), src("CGT")));
        return l;
    }
}
