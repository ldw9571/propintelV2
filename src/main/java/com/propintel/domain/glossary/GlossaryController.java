package com.propintel.domain.glossary;

import com.propintel.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/glossary")
public class GlossaryController {

    public record Term(String key, String term, String category, String shortDesc, String longDesc, String example) {}

    private final GlossaryTermRepository repository;

    public GlossaryController(GlossaryTermRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<List<Term>> all() {
        return ApiResponse.ok(repository.findAllByOrderByCategoryAscTermAsc().stream()
                .map(t -> new Term(t.getTermKey(), t.getTerm(), t.getCategory(), t.getShortDesc(), t.getLongDesc(), t.getExample()))
                .toList());
    }
}
