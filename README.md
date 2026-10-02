# PropIntel — 부동산 투자 분석 (학습용)

> **추가 기능 안내**: 투자금·대출·원리금·취득비용·10년 시뮬레이션·IRR, 전월세 실거래와 시장 지표(변동률·전세가율·신고가·하락거래),
> 관심지역·단지 모니터링과 급등락 알림, 뉴스 수집·요약, 가격 변화 원인 분석, 용어 사전, React 프론트엔드가 추가되었습니다.
> 설계·API·실행·테스트 방법은 **[docs/ADDED_FEATURES.md](docs/ADDED_FEATURES.md)** 를 보세요.
>
> 빠른 실행: `.env.example` → `.env` 복사 후 `docker compose up --build` → http://localhost:5173

## 기존 API

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
대시보드
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
GET  /api/v1/dashboard/summary
→ 전국 KPI (평균가, 거래량, 전세가율)

GET  /api/v1/dashboard/price-index?area=ALL|SEOUL|METRO|LOCAL
→ 월별 가격지수 시계열

GET  /api/v1/dashboard/volume?months=12
→ 월별 거래량 추이

GET  /api/v1/dashboard/top-regions?limit=5
→ 상승률 TOP 지역

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
지역 분석
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
GET  /api/v1/regions
GET  /api/v1/regions/{code}
GET  /api/v1/regions/{code}/stats?from=2015-01&to=2025-06
GET  /api/v1/regions/{code}/supply?years=3
GET  /api/v1/regions/{code}/population
GET  /api/v1/regions/{code}/jobs

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
단지 분석
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
GET  /api/v1/complexes?regionCode=&name=&page=0&size=20
GET  /api/v1/complexes/{id}
GET  /api/v1/complexes/{id}/transactions?type=SALE&from=&to=
GET  /api/v1/complexes/{id}/price-chart
GET  /api/v1/complexes/{id}/similar

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
투자 점수
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
GET  /api/v1/scores/complex/{id}
POST /api/v1/scores/complex/{id}/refresh

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AI 리포트
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
GET  /api/v1/reports/complex/{id}
POST /api/v1/reports/complex/{id}/generate

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
지도
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
GET  /api/v1/map/complexes?lat=&lng=&radius=3000
GET  /api/v1/map/hotspots?regionCode=
GET  /api/v1/map/subway-stations