package com.propintel.domain.news.repository;

import com.propintel.domain.news.entity.NewsArticle;
import com.propintel.domain.news.entity.NewsCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsByUrl(String url);

    @Query("""
        select n from NewsArticle n
        where (:regionCode is null or n.regionCode = :regionCode)
          and (:category is null or n.category = :category)
          and n.publishedAt >= :from
        order by n.publishedAt desc
    """)
    List<NewsArticle> search(@Param("regionCode") String regionCode, @Param("category") NewsCategory category,
                             @Param("from") LocalDateTime from, Pageable pageable);

    List<NewsArticle> findByRegionCodeAndPublishedAtBetweenOrderByPublishedAtDesc(
            String regionCode, LocalDateTime from, LocalDateTime to);

    List<NewsArticle> findByRegionCodeIsNullAndCategoryInAndPublishedAtBetweenOrderByPublishedAtDesc(
            Collection<NewsCategory> categories, LocalDateTime from, LocalDateTime to);
}
