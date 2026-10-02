CREATE TABLE region (
                        id          BIGSERIAL PRIMARY KEY,
                        code        VARCHAR(10)  NOT NULL UNIQUE,  -- 법정동 코드
                        name        VARCHAR(100) NOT NULL,
                        city        VARCHAR(50),                   -- 시/도
                        district    VARCHAR(50),                   -- 시/군/구
                        dong        VARCHAR(50),                   -- 읍/면/동
                        lat         DOUBLE PRECISION,
                        lng         DOUBLE PRECISION,
                        created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE region_stat (
                             id            BIGSERIAL PRIMARY KEY,
                             region_id     BIGINT NOT NULL REFERENCES region(id),
                             stat_year     INTEGER NOT NULL,
                             stat_month    INTEGER NOT NULL,
                             population    INTEGER,       -- 인구 수
                             jobs          INTEGER,       -- 취업자 수
                             price_index   DOUBLE PRECISION,  -- 가격지수 (2015=100)
                             jeonse_rate   DOUBLE PRECISION,  -- 전세가율 %
                             trade_volume  INTEGER,       -- 월 거래량
                             UNIQUE(region_id, stat_year, stat_month)
);

CREATE TABLE supply_plan (
                             id          BIGSERIAL PRIMARY KEY,
                             region_id   BIGINT NOT NULL REFERENCES region(id),
                             plan_year   INTEGER NOT NULL,
                             plan_month  INTEGER NOT NULL,
                             units       INTEGER,         -- 입주 예정 세대수
                             status      VARCHAR(20) DEFAULT 'PLANNED'  -- PLANNED/COMPLETED
);

CREATE INDEX idx_region_stat_region_id ON region_stat(region_id);
CREATE INDEX idx_region_stat_year_month ON region_stat(stat_year, stat_month);