package com.propintel.domain.investment.controller;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.investment.service.PolicyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** 규제·세율·지역 기준 데이터 (출처·기준일·검증여부 포함) */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    /** 대출규제·관심지역·뉴스에서 공통으로 쓰는 지역 목록 (규제지역 여부 포함) */
    @GetMapping("/regions")
    public ApiResponse<List<PolicyService.RegionView>> regions() {
        return ApiResponse.ok(policyService.regionViews());
    }

    @GetMapping("/current")
    public ApiResponse<PolicyService.PolicyView> current(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.ok(policyService.view(asOf));
    }

    @PutMapping("/parameters/{id}")
    public ApiResponse<PolicyService.ParamView> updateParameter(@PathVariable Long id,
                                                                @RequestBody PolicyService.UpdateParamRequest req) {
        return ApiResponse.ok(policyService.updateParameter(id, req));
    }
}
