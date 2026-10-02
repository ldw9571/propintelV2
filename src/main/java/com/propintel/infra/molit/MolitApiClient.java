package com.propintel.infra.molit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.propintel.domain.region.entity.Region;
import com.propintel.domain.region.repository.RegionRepository;
import com.propintel.infra.molit.dto.MolitTransactionDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MolitApiClient {

    @Value("${molit.api-key}")
    private String apiKey;

    @Value("${molit.base-url}")
    private String baseUrl;

    private final WebClient.Builder webClientBuilder;
    private final RegionRepository regionRepository;
    private final XmlMapper xmlMapper = new XmlMapper();

    /**
     * ⬇️ 오버로드: 지역 코드 없이 dealYmd만 넘기면
     * DB에 등록된 전체 지역을 순회하며 모두 수집
     */
    public List<MolitTransactionDto> fetchTransactions(String dealYmd) {
        List<Region> regions = regionRepository.findAll();
        List<MolitTransactionDto> all = new ArrayList<>();

        for (Region region : regions) {
            List<MolitTransactionDto> part = fetchTransactions(dealYmd, region.getCode());
            all.addAll(part);
            try {
                Thread.sleep(200); // API 과부하 방지
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("전체 지역 수집 완료: dealYmd={}, 총 {}건", dealYmd, all.size());
        return all;
    }

    /**
     * @param dealYmd 거래년월 6자리 (예: "202405")
     * @param lawdCd  법정동 코드 5자리 (예: "11680")
     */
    public List<MolitTransactionDto> fetchTransactions(String dealYmd, String lawdCd) {
        try {
            String xml = webClientBuilder.build()
                    .get()
                    .uri(baseUrl + "/RTMSDataSvcAptTradeDev/getRTMSDataSvcAptTradeDev"
                            + "?serviceKey=" + apiKey
                            + "&LAWD_CD=" + lawdCd
                            + "&DEAL_YMD=" + dealYmd
                            + "&pageNo=1&numOfRows=1000")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xml == null || xml.isBlank()) {
                log.warn("국토부 API 응답이 비어있음: dealYmd={}, lawdCd={}", dealYmd, lawdCd);
                return Collections.emptyList();
            }

            XmlItemResponse response = xmlMapper.readValue(xml, XmlItemResponse.class);

            if (response.getBody() == null
                    || response.getBody().getItems() == null
                    || response.getBody().getItems().getItem() == null) {
                log.info("국토부 API 결과 없음: dealYmd={}, lawdCd={}", dealYmd, lawdCd);
                return Collections.emptyList();
            }

            List<MolitTransactionDto> result = response.getBody().getItems().getItem().stream()
                    .map(item -> new MolitTransactionDto(
                            item.getAptNm(),
                            item.getUmdNm(),
                            item.getExcluUseAr(),
                            item.getFloor(),
                            item.getDealAmount(),
                            item.getDealYear(),
                            item.getDealMonth(),
                            item.getDealDay()
                    ))
                    .collect(Collectors.toList());

            log.info("국토부 API 수신 성공: dealYmd={}, lawdCd={}, 건수={}", dealYmd, lawdCd, result.size());
            return result;

        } catch (Exception e) {
            log.error("국토부 API 호출/파싱 실패: dealYmd={}, lawdCd={}, error={}", dealYmd, lawdCd, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ===== XML 파싱 전용 내부 클래스 =====

    @JacksonXmlRootElement(localName = "response")
    @Getter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class XmlItemResponse {
        @JacksonXmlProperty(localName = "body")
        private XmlBody body;
    }

    @Getter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class XmlBody {
        @JacksonXmlProperty(localName = "items")
        private XmlItems items;
    }

    @Getter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class XmlItems {
        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<XmlItem> item;
    }

    @Getter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class XmlItem {
        @JacksonXmlProperty(localName = "aptNm") private String aptNm;
        @JacksonXmlProperty(localName = "dealAmount") private String dealAmount;
        @JacksonXmlProperty(localName = "excluUseAr") private String excluUseAr;
        @JacksonXmlProperty(localName = "dealYear") private String dealYear;
        @JacksonXmlProperty(localName = "dealMonth") private String dealMonth;
        @JacksonXmlProperty(localName = "dealDay") private String dealDay;
        @JacksonXmlProperty(localName = "floor") private String floor;
        @JacksonXmlProperty(localName = "umdNm") private String umdNm;
    }
}