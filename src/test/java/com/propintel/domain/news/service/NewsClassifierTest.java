package com.propintel.domain.news.service;

import com.propintel.domain.news.entity.NewsCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NewsClassifierTest {

    @Test
    void 키워드로_분류한다() {
        assertEquals(NewsCategory.REDEVELOPMENT, NewsClassifier.classify("영등포 재건축 조합 설립", null, true));
        assertEquals(NewsCategory.TRANSPORT, NewsClassifier.classify("신안산선 개통 연기", "영등포역 인근", true));
        assertEquals(NewsCategory.SUPPLY, NewsClassifier.classify("내년 입주 물량 감소", null, true));
        assertEquals(NewsCategory.POLICY, NewsClassifier.classify("정부, 추가 규제 검토", null, false));
        assertEquals(NewsCategory.RATE, NewsClassifier.classify("한국은행 기준금리 동결", null, false));
        assertEquals(NewsCategory.REGION, NewsClassifier.classify("영등포구 축제 개최", null, true));
        assertEquals(NewsCategory.OTHER, NewsClassifier.classify("축제 개최", null, false));
    }

    @Test
    void 영향_문구는_예측이_아니라_언급_사실() {
        for (NewsCategory c : NewsCategory.values()) {
            String note = NewsClassifier.impactNote(c);
            assertFalse(note.contains("오를") || note.contains("상승할") || note.contains("하락할"), note);
        }
    }
}
