package com.propintel.infra.molit.dto;

public record MolitTransactionDto(
        String aptName,     // 아파트명
        String umdNm,       // 법정동
        String excluUseAr,  // 전용면적
        String floor,       // 층
        String dealAmount,  // 거래금액
        String dealYear,    // 년
        String dealMonth,   // 월
        String dealDay      // 일
) {}