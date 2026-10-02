package com.propintel.infra.news;

import java.time.LocalDateTime;
import java.util.List;

/** 뉴스 검색 제공자 (NewsDataProvider) */
public interface NewsProvider {

    record RawNews(String title, String link, String originalLink, String description, LocalDateTime publishedAt) {}

    String name();

    /** 실제 뉴스가 아닌 샘플 데이터인지 */
    boolean isMock();

    List<RawNews> search(String query, int display);
}
