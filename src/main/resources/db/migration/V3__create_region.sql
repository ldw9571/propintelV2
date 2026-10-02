CREATE TABLE complex_score (
                               id               BIGSERIAL PRIMARY KEY,
                               complex_id       BIGINT NOT NULL REFERENCES complex(id),
                               supply_score     INTEGER,
                               demand_score     INTEGER,
                               transport_score  INTEGER,
                               school_score     INTEGER,
                               population_score INTEGER,
                               total_score      INTEGER,
                               scored_at        DATE NOT NULL
);

CREATE TABLE ai_report (
                           id           BIGSERIAL PRIMARY KEY,
                           complex_id   BIGINT NOT NULL REFERENCES complex(id),
                           advantages   TEXT,
                           risks        TEXT,
                           checkpoints  TEXT,
                           created_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE development_hotspot (
                                     id          BIGSERIAL PRIMARY KEY,
                                     region_id   BIGINT REFERENCES region(id),
                                     title       VARCHAR(200) NOT NULL,  -- 개발 호재명
                                     type        VARCHAR(50),            -- GTX / 재개발 / 신도시 등
                                     status      VARCHAR(50),            -- 계획 / 진행 / 완료
                                     expected_at DATE,
                                     lat         DOUBLE PRECISION,
                                     lng         DOUBLE PRECISION
);