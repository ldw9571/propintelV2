# 추가 기능 — 투자 분석 · 시장 데이터 · 관심지역 알림 · 뉴스 · 원인 분석 · 프론트엔드

기존 기능(지역·단지 조회, 매매 실거래 수집 `BatchController`, 대시보드 `/api/v1/dashboard`, 투자점수, AI 리포트)은 **수정하지 않았습니다.** 같은 규칙(`com.propintel.domain.*`, `/api/v1`, `ApiResponse`)으로 새 패키지만 추가했습니다.

기존 파일 중 바꾼 것은 두 개입니다.
- `application.yml`: 공개 레포에 올라가 있던 DB 비밀번호와 API 키를 **환경변수로 바꾸고**, 새 설정을 추가했습니다.
- `.gitignore`: `.env`, `frontend/node_modules`, `frontend/dist`를 추가했습니다.

> ⚠️ 이전 커밋에 들어 있던 Gemini 키, 공공데이터 키, DB 비밀번호는 git 히스토리에 남아 있습니다. **키를 재발급(기존 키 폐기)**하세요.

---

## 1. 프로젝트 구조 (추가분)

```
src/main/java/com/propintel/
├─ common/
│  ├─ source/SourceInfo.java          출처·기준일·업데이트일·검증여부 (모든 정책 데이터 공통)
│  ├─ util/Won.java                   금액·비율 표시
│  └─ web/ValidationExceptionHandler  입력 검증 오류 → 400 (기존 핸들러는 500으로 처리하던 것)
├─ config/SchedulingConfig.java       @EnableScheduling
├─ infra/
│  ├─ molit/MolitRawClient.java       매매(해제거래 표시 포함)·전월세 API, 페이지 처리
│  ├─ news/                           NewsProvider(네이버 뉴스 / 샘플 Mock)
│  └─ ai/AiTextClient*                none|openai|gemini → 기존 OpenAiClient/GeminiClient 에 위임
└─ domain/
   ├─ investment/   Phase 1  투자금·대출·원리금·취득비용·1~10년 시뮬레이션·IRR
   │  ├─ calc/      ★ 순수 Java 계산 엔진 (Spring·DB 의존 없음, 단위 테스트 23개)
   │  ├─ entity/    PolicyRegion, LoanRegulation, LoanAmountCap, DsrRegulation, RateBracket,
   │  │             PolicyParameter, InvestmentAnalysis, InvestorProfile
   │  └─ service/   PolicyService(DB→PolicySnapshot), PolicySeeder(초기 데이터), InvestmentService, CapacityService
   ├─ market/       Phase 2  전월세 저장, 시장 지표(변동률·거래량·전세가율·신고가·하락거래)
   │  └─ calc/MarketStatsCalculator  ★ 순수 함수 (단위 테스트 7개)
   ├─ watch/        Phase 3  관심 지역/단지(+평형), 알림 조건, 모니터링, 알림, 스케줄러
   ├─ news/         Phase 4  뉴스 수집·분류·요약(원문에 있는 내용만)
   ├─ insight/      Phase 5  가격 변화 원인 분석 (근거 기반)
   ├─ glossary/     공부 기능: 용어 사전 33개
   └─ home/         첫 화면 대시보드 API
frontend/                              React 18 + Vite + Recharts (새로 추가)
docker-compose.yml, Dockerfile, .env.example
```

## 2. DB 설계 (추가 테이블)

기존 설정대로 `ddl-auto: update`가 엔티티로 테이블을 만듭니다. 초기 데이터는 앱 시작 시 `PolicySeeder`와 `GlossarySeeder`가 넣습니다. 비어 있는 경우에만 넣으므로, 화면에서 수정한 값은 덮어쓰지 않습니다.

| 테이블 | 용도 | 신뢰성 컬럼 |
|---|---|---|
| `policy_region` | 시군구 73곳: 규제지역·토지거래허가구역 여부, 실거래 수집 가능 여부 | source_name, source_url, base_date, updated_at, verified, note |
| `loan_regulation` | LTV (지역유형 × 차주유형: 생애최초/무주택/1주택 처분조건/1주택/다주택) | + effective_from/to |
| `loan_amount_cap` | 주택가격 구간별 주담대 한도 (15억 이하 6억 등) | 〃 |
| `dsr_regulation` | DSR 40%, 스트레스 금리(수도권 3.0%p·지방 1.5%p), 금리유형별 반영비율 | 〃 |
| `rate_bracket` | 중개보수·양도세·재산세 누진 구간 | 〃 |
| `policy_parameter` | 취득세율, 비과세 기준 등 42개 | 〃 |
| `investment_analysis` | 저장한 분석 (입력·결과 JSON, 계산 기준일) | policy_as_of |
| `investor_profile` | 내 자금 정보 (대시보드 투자 가능 금액) | |
| `rent_transaction` | **전월세 실거래** (기존 `transaction`은 매매 전용 평균가 쿼리가 있어 분리) | source_name, collected_at |
| `collection_log` | 실거래 수집 이력 (월·유형별 건수, 오류) | collected_at |
| `watch_item` | 관심 지역/단지 + 면적 범위 | |
| `alert_rule` | 관심 항목별 알림 조건 9종 (기준값·사용 여부) | |
| `alert` | 발생한 알림 + 상세 JSON (어디서·얼마나·언제부터·거래량·신고가·관련 뉴스) | dedupe_key |
| `news_article` | 뉴스 (제목·언론사·날짜·원문 링크·요약·지역 관련 언급·분류) | source_name, mock |
| `glossary_term` | 용어 사전 | |

**매매 실거래**는 기존 `transaction` 테이블(type=SALE, 금액 원 단위)을 그대로 씁니다. 새 수집기에는 세 가지를 추가했습니다: 해제된 거래 제외, 같은 거래 중복 저장 방지, 건축연도 저장.

> 초기 규제·세율 데이터는 2026-09-28에 입력했습니다. 언론 요약 같은 2차 출처 기준이라 **전부 `verified=false`(미검증)**입니다. 금융위원회·국토교통부·법제처 원문과 대조한 뒤 ‘기준 데이터’ 화면에서 ‘공식 원문 확인함’으로 바꾸세요.

## 3. API 설계 (모두 `ApiResponse { success, data, message }`)

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/v1/home` | 첫 화면: 투자 가능 금액, 관심 지역·아파트, 오늘의 뉴스(지역/정책/금리/공급), 알림 |
| POST | `/api/v1/investment/analyze` | 투자 분석 (대출 한도·필요 현금·원리금·취득비용·3개 시나리오 1~10년·IRR) |
| POST | `/api/v1/investment/loan-schedule` | 원리금균등/원금균등/만기일시 비교 |
| POST/GET/DELETE | `/api/v1/investment/analyses[/{id}]` | 분석 저장·목록·상세·삭제 |
| GET/PUT | `/api/v1/investment/profile` | 내 자금 정보 |
| GET | `/api/v1/investment/capacity` | 나의 투자 가능 금액 (최대 매입 가능 가격 역산) |
| GET | `/api/v1/policies/regions` | 지역 목록 (규제·토허 여부 + 출처) |
| GET | `/api/v1/policies/current?asOf=` | 기준일에 유효한 규제·세율 전체 + 미검증/오래된 데이터 개수 |
| PUT | `/api/v1/policies/parameters/{id}` | 세율·기준금액 수정 (출처·기준일 필수) |
| POST | `/api/v1/market/collect?regionCode=&months=&types=SALE,RENT` | 국토부 매매·전월세 수집 |
| GET | `/api/v1/market/collect/logs` | 수집 이력 |
| GET | `/api/v1/market/regions/{code}/summary?areaMin=&areaMax=` | 지역 지표 |
| GET | `/api/v1/market/complexes/{id}/summary` · `/trades` | 단지 지표 · 최근 거래 |
| GET | `/api/v1/market/regions/{code}/complexes?q=` | 지역 내 단지 검색 |
| GET/POST/PUT/DELETE | `/api/v1/watch[/{id}]` | 관심 항목 + 모니터링 지표 |
| GET/PUT | `/api/v1/watch/{id}/rules` | 알림 조건 (기준값·사용 여부) |
| POST | `/api/v1/watch/evaluate` | 지금 모든 관심 항목 평가 |
| GET | `/api/v1/alerts` · `/alerts/unread-count` | 알림 |
| POST | `/api/v1/alerts/{id}/read` · `/alerts/read-all` | 읽음 처리 |
| GET | `/api/v1/news?regionCode=&category=&days=` | 뉴스 |
| POST | `/api/v1/news/collect/region/{code}` · `/news/collect/national` | 뉴스 즉시 수집 |
| GET | `/api/v1/insights/price-change?regionCode=\|complexId=&months=1\|3\|6\|12&ai=false` | 가격 변화 원인 분석 |
| GET | `/api/v1/glossary` | 용어 사전 |

## 4. 화면 설계 (frontend/)

| 메뉴 | 내용 |
|---|---|
| 대시보드 | 나의 투자 가능 금액(보유 현금·대출 가능액·총 투자 가능액·월 상환 가능액), 관심 지역 표(1/3/6/12개월 변동률·거래량·전세가율·신고가·급등락), 관심 아파트 표(현재가·전세가·갭), 오늘의 뉴스(지역/정책/금리/공급), 알림 |
| 투자 분석 | 입력 폼 + 결과 6개 탭 (요약·자금/대출 한도·원리금·1~10년 시뮬레이션·수익 분석·가정/출처) |
| 대출 계산기 | 3가지 상환방식 비교표·잔액 차트·연도별 표 |
| 시장 데이터 | 지역/평형 선택, 실거래 수집 버튼, 지표 카드, 월별 매매·전세 84㎡ 환산가 차트, 거래량 차트, 신고가·하락 거래 표, 단지 검색 → 단지 지표·최근 거래, 관심 등록 |
| 관심 목록 | 지역/단지(+평형) 등록, 모니터링 표, 알림 조건 편집(9종), 지금 평가 |
| 알림 | 어디서·얼마나·언제부터·거래량·신고가·관련 뉴스 |
| 뉴스 | 지역·분류·기간 필터, 즉시 수집, 핵심 내용·지역 관련 언급·출처 |
| 원인 분석 | 변화율, 결론(근거 없으면 “확인된 직접적인 원인이 없습니다”), 항목별 관찰됨/해당 없음/데이터 없음과 근거 링크, 1년 전 같은 기간 비교, 선택적 AI 정리 |
| 기준 데이터 | LTV·한도·DSR·세율·지역 지정과 출처, 미검증 개수, 값 수정 |
| 용어 사전 | 33개. 모든 화면의 ⓘ 에 마우스를 올려도 표시 |

## 5. 계산·판단 규칙 요약

- **대출 한도**: LTV, 주택가격별 한도, (스트레스)DSR 한도 중 가장 작은 값입니다. 수도권·규제지역에서 전세나 월세를 끼고 사면 전입 의무 때문에 주담대를 0원으로 계산합니다.
- **가격 지표**: 면적이 다른 거래끼리 비교하려고 모든 가격을 ㎡당 평균 × 84(84㎡ 환산가)로 바꿉니다. 기준월은 거래가 3건 이상(단지는 1건)인 가장 최근 달입니다. 비교할 달의 거래가 부족하면 변동률을 비워 둡니다.
- **신고가 / 하락 거래**: 같은 단지에서 면적 차이가 ±2㎡ 이내인 과거 거래와 비교합니다.
  - 신고가: 지금까지의 최고가보다 높은 거래
  - 하락 거래: 직전 거래보다 5% 이상 낮은 거래
- **알림 조건 기본값**:

  | 조건 | 기본 기준 |
  |---|---|
  | 1개월 가격 변동 | ±5% |
  | 3개월 가격 변동 | ±10% |
  | 거래량 급증 / 급감 | 직전 6개월 평균의 2배 이상 / 0.5배 이하 |
  | 신고가 | 1건 이상 |
  | 12개월 평균 대비 | 10% 이상 높음 |
  | 중요 뉴스 | 최근 3일 1건 이상 |

  같은 기준월이나 같은 거래로는 한 번만 알립니다.
- **뉴스**:
  - 입력은 제목과 검색 API 요약문뿐이고, 네이버 API는 기사 본문을 주지 않습니다.
  - AI를 쓰더라도 이 텍스트에 없는 내용은 쓰지 않도록 지시합니다. 응답 형식이 이상하면 원문 요약문을 그대로 씁니다.
  - ‘지역 관련 언급’에는 예측이 아니라 “기사에 무엇이 언급됐는지”만 적습니다.
- **원인 분석**:
  - 기간 안에 저장된 데이터만 확인합니다: 거래량, 신고가, 하락 거래, 전세가, 규제 시행일, 분류별 뉴스.
  - 데이터가 없는 항목(금리 시계열, 입주 물량)은 “데이터 없음”으로 표시합니다.
  - 관찰된 요인이 하나도 없으면 “확인된 직접적인 원인이 없습니다”라고 답합니다.

## 6. 실행 방법

### 준비
1. `.env.example`을 `.env`로 복사해 키를 채웁니다. 키가 없어도 실행은 됩니다.
   - `MOLIT_API_KEY`: 공공데이터포털의 ‘국토교통부_아파트 매매 실거래가 상세 자료’와 ‘국토교통부_아파트 전월세 실거래가 자료’를 활용 신청하면 발급됩니다(같은 인증키).
   - `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET`: 네이버 개발자센터에서 검색 API를 등록하면 발급됩니다. 없으면 **샘플(Mock) 뉴스**가 화면에 ‘샘플’로 표시됩니다.
   - `AI_PROVIDER`: `none`(기본값) / `openai` / `gemini`

### A. Docker로 한 번에
```bash
docker compose up --build
# 화면 http://localhost:5173 · API http://localhost:8080/swagger-ui.html
```

### B. 개발 모드
```bash
docker compose up -d db            # 또는 로컬 PostgreSQL에 propintel DB
# 백엔드 (IntelliJ에서 PropintelApplication 실행 시 환경변수에 DB_PASSWORD, MOLIT_API_KEY 등 설정)
./gradlew bootRun                  # wrapper 가 안 되면 IntelliJ 의 Gradle 로 실행
# 프론트엔드
cd frontend && npm install && npm run dev   # http://localhost:5173 (/api → 8080 프록시)
```

### 처음 써 볼 때 순서
1. **내 자금**: 보유 현금과 소득을 입력하면 대시보드에 투자 가능 금액이 나옵니다.
2. **투자 분석**: 매물 조건을 넣고 ‘분석하기’를 누릅니다.
3. **시장 데이터**: 영등포구 등을 고르고 ‘12개월’을 눌러 실거래를 수집합니다(MOLIT 키 필요). 지표와 신고가가 나옵니다.
4. 같은 화면에서 ‘☆ 이 지역 관심 등록’을 누르거나, 단지를 골라 ‘☆ 이 단지 관심 등록’을 누릅니다.
5. **관심 목록**에서 ‘지금 알림 조건 확인’을 누르면 **알림**이 생깁니다.
6. **뉴스**에서 ‘이 지역 뉴스 지금 수집’을 누른 뒤, **원인 분석**에서 결과를 봅니다.
7. 이후에는 스케줄러가 매일 06:30(실거래 → 뉴스 → 알림)과 3시간마다(뉴스) 자동으로 실행합니다.

## 7. 테스트 방법

```bash
./gradlew test --tests "com.propintel.domain.*"
```
- `CalculatorTest` (23개): 원리금, 취득세, LTV·DSR 한도, 양도세, 재산세, IRR, 통합 시뮬레이션 항등식
- `MarketStatsCalculatorTest` (7개): 기준월 선정, 1·3·12개월 변동률, 거래량 급증, 신고가·하락 거래, 전세가율, 면적 필터, 데이터 없음
- `NewsClassifierTest` (2개): 분류, ‘예측하지 않는 문구’ 확인

기존 `PropintelApplicationTests`(@SpringBootTest)는 DB가 있어야 통과합니다.

화면에서 확인할 것:
- 투자 분석 기본값(영등포 8억, 현금 3억, 월 700만원) → 대출 3.2억(LTV 40%가 한도), 부족 자금 2억 562만원
- 부천시 원미구로 바꾸면 한도 결정 요인이 DSR로 바뀝니다.
- 수집 후 단지를 관심 등록하고 알림 조건의 ‘신고가’ 기준을 1로 둔 뒤 ‘지금 확인’ → 신고가 알림에 이전 최고가·거래량·관련 뉴스가 함께 표시됩니다.

## 알려진 제약 / 기존 코드에서 발견한 점

- **스케줄러**: `@EnableScheduling`을 켜면 기존 `BatchScheduler`도 동작합니다. 이 클래스는 존재하지 않는 `kosisStatJob` 대신 `molitTransactionJob`을 주입받아, 매월 1일과 5일에 두 번 실행됩니다. 끄려면 `app.scheduling.enabled=false`로 두면 되지만, 그러면 새 모니터링 스케줄도 함께 꺼집니다.
- **기존 지역 코드**: `BatchController.ALL_REGIONS`의 일부 코드는 국토부 API가 받지 않는 시(市) 단위이거나 옛 코드입니다(41110 수원시, 41460 하남, 41630 화성, 21500 해운대 등). 새 수집기는 `policy_region`의 구 단위 코드(41111~41117, 41450, 26350 …)를 씁니다.
- **기존 매매 수집기**: 해제 거래를 걸러내지 않고, 다시 실행하면 같은 거래가 중복 저장됩니다. 새 수집기(`/api/v1/market/collect`)에서는 해결했습니다.
- **기존 GeminiClient**: `gemini-pro` 모델을 호출하는데, 이 모델은 현재 제공되지 않을 수 있습니다. `AI_PROVIDER=gemini`를 쓰려면 모델명을 확인하세요.
- **아직 연동하지 않은 데이터**: 입주 물량, 금리 시계열, 인구·학군. 원인 분석에서 ‘데이터 없음’으로 표시됩니다.
