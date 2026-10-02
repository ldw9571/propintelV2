package com.propintel.domain.glossary;

import jakarta.persistence.*;

/** 공부 기능: 지표 설명 (화면의 ⓘ 툴팁) */
@Entity
@Table(name = "glossary_term")
public class GlossaryTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "term_key", nullable = false, unique = true, length = 50) private String termKey;
    @Column(nullable = false, length = 100) private String term;
    @Column(nullable = false, length = 30) private String category;
    @Column(name = "short_desc", nullable = false, length = 300) private String shortDesc;
    @Column(name = "long_desc", nullable = false, columnDefinition = "TEXT") private String longDesc;
    @Column(columnDefinition = "TEXT") private String example;

    protected GlossaryTerm() {}

    public GlossaryTerm(String termKey, String term, String category, String shortDesc, String longDesc, String example) {
        this.termKey = termKey;
        this.term = term;
        this.category = category;
        this.shortDesc = shortDesc;
        this.longDesc = longDesc;
        this.example = example;
    }

    public Long getId() { return id; }
    public String getTermKey() { return termKey; }
    public String getTerm() { return term; }
    public String getCategory() { return category; }
    public String getShortDesc() { return shortDesc; }
    public String getLongDesc() { return longDesc; }
    public String getExample() { return example; }
}
