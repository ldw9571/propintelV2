package com.propintel.domain.glossary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, Long> {
    List<GlossaryTerm> findAllByOrderByCategoryAscTermAsc();
}
