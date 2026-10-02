package com.propintel.infra.news;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 네이버 API 키가 없을 때 쓰는 샘플 뉴스. 화면에 "샘플(Mock)"로 표시되며 실제 기사가 아니다.
 */
public class MockNewsProvider implements NewsProvider {

    @Override public String name() { return "샘플 뉴스(Mock) — 실제 기사 아님"; }
    @Override public boolean isMock() { return true; }

    @Override
    public List<RawNews> search(String query, int display) {
        String slug = Integer.toHexString(query.hashCode());
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        return List.of(
                new RawNews("[샘플] " + query + " 관련 기사 제목 예시", "https://example.com/mock/" + slug + "/1", null,
                        "네이버 뉴스 API 키(NAVER_CLIENT_ID/SECRET)를 설정하면 실제 기사로 바뀝니다. 이 문장은 화면 확인용 샘플입니다.",
                        now.minusHours(2)),
                new RawNews("[샘플] " + query + " 두 번째 기사 예시", "https://example.com/mock/" + slug + "/2", null,
                        "샘플 요약문입니다. 실제 뉴스가 아니므로 투자 판단에 사용하지 마세요.", now.minusDays(1)));
    }
}
