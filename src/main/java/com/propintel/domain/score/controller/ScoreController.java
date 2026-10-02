package com.propintel.domain.score.controller;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.score.entity.ComplexScore;
import com.propintel.domain.score.service.ScoreCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreCalculatorService scoreService;

    @GetMapping("/complex/{id}")
    public ApiResponse<ComplexScore> getScore(@PathVariable Long id) {
        return ApiResponse.ok(
                scoreService.getLatestScore(id)
                        .orElseThrow(() -> new IllegalStateException("점수 미계산"))
        );
    }

    @PostMapping("/complex/{id}/refresh")
    public ApiResponse<ComplexScore> refresh(@PathVariable Long id) {
        return ApiResponse.ok(scoreService.calculate(id));
    }
}