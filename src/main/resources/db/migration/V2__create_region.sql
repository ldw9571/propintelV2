CREATE TABLE complex (
                         id           BIGSERIAL PRIMARY KEY,
                         region_id    BIGINT NOT NULL REFERENCES region(id),
                         name         VARCHAR(200) NOT NULL,
                         address      VARCHAR(500) NOT NULL,
                         build_year   INTEGER,
                         total_units  INTEGER,
                         floor_count  INTEGER,
                         lat          DOUBLE PRECISION,
                         lng          DOUBLE PRECISION,
                         created_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE transaction (
                             id          BIGSERIAL PRIMARY KEY,
                             complex_id  BIGINT NOT NULL REFERENCES complex(id),
                             type        VARCHAR(10) NOT NULL,   -- SALE / JEONSE / MONTHLY
                             area_sqm    INTEGER,                -- 전용면적 ㎡
                             floor       INTEGER,
                             price       BIGINT,                 -- 매매/전세 (만원)
                             deposit     BIGINT,                 -- 보증금 (월세)
                             monthly     BIGINT,                 -- 월세
                             deal_date   DATE NOT NULL
);

CREATE INDEX idx_transaction_complex_id ON transaction(complex_id);
CREATE INDEX idx_transaction_deal_date  ON transaction(deal_date);
CREATE INDEX idx_transaction_type       ON transaction(type);