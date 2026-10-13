-- Equity Composition Service — PostgreSQL 15 schema (idempotent)

CREATE SEQUENCE IF NOT EXISTS eq_comp_seq START WITH 1 INCREMENT BY 1 NO CYCLE;

CREATE TABLE IF NOT EXISTS equity_composition (
    id                 BIGINT       NOT NULL DEFAULT nextval('eq_comp_seq'),
    portfolio_id       VARCHAR(50),
    as_of_date         DATE,
    status             VARCHAR(16)  NOT NULL DEFAULT 'STARTED',
    total_chunks       INTEGER      NOT NULL DEFAULT 0,
    total_bytes        BIGINT       NOT NULL DEFAULT 0,
    stored_bytes       BIGINT       NOT NULL DEFAULT 0,
    meta_length        INTEGER,
    document_checksum  VARCHAR(64),
    validated_at       TIMESTAMPTZ,
    validation_msg     VARCHAR(2000),
    error_message      VARCHAR(2000),
    version_timestamp  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    latest             CHAR(1)      NOT NULL DEFAULT 'N',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ,
    CONSTRAINT pk_equity_composition PRIMARY KEY (id),
    CONSTRAINT chk_eq_comp_latest CHECK (latest IN ('Y','N'))
);
ALTER SEQUENCE eq_comp_seq OWNED BY equity_composition.id;

ALTER TABLE equity_composition
    ADD COLUMN IF NOT EXISTS document_checksum VARCHAR(64),
    ADD COLUMN IF NOT EXISTS validated_at      TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS validation_msg    VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS version_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS latest            CHAR(1)       NOT NULL DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS stored_bytes      BIGINT        NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS meta_length       INTEGER;

CREATE INDEX IF NOT EXISTS idx_eq_comp_portfolio ON equity_composition (portfolio_id);
CREATE INDEX IF NOT EXISTS idx_eq_comp_as_of     ON equity_composition (as_of_date);
CREATE INDEX IF NOT EXISTS idx_eq_comp_status    ON equity_composition (status);
CREATE INDEX IF NOT EXISTS idx_eq_comp_latest    ON equity_composition (latest);
CREATE INDEX IF NOT EXISTS idx_eq_comp_portfolio_version
    ON equity_composition (portfolio_id, version_timestamp DESC);

CREATE UNIQUE INDEX IF NOT EXISTS ux_eq_comp_latest
    ON equity_composition (portfolio_id)
    WHERE latest = 'Y';

CREATE TABLE IF NOT EXISTS equity_composition_chunk (
    composition_id     BIGINT      NOT NULL,
    chunk_seq          INTEGER     NOT NULL,
    chunk_size         INTEGER     NOT NULL,
    uncompressed_size  INTEGER,
    chunk_data         BYTEA       NOT NULL,
    status             VARCHAR(16) NOT NULL,
    checksum           VARCHAR(64),
    compressed         CHAR(1)     NOT NULL DEFAULT 'N',
    compression        VARCHAR(16),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_eq_comp_chunk PRIMARY KEY (composition_id, chunk_seq),
    CONSTRAINT fk_eq_comp_chunk FOREIGN KEY (composition_id)
        REFERENCES equity_composition (id) ON DELETE CASCADE,
    CONSTRAINT chk_eq_comp_chunk_compressed CHECK (compressed IN ('Y','N'))
);

ALTER TABLE equity_composition_chunk
    ADD COLUMN IF NOT EXISTS checksum          VARCHAR(64),
    ADD COLUMN IF NOT EXISTS compressed        CHAR(1) NOT NULL DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS compression       VARCHAR(16),
    ADD COLUMN IF NOT EXISTS uncompressed_size INTEGER;

CREATE INDEX IF NOT EXISTS idx_eq_comp_chunk_status     ON equity_composition_chunk (status);
CREATE INDEX IF NOT EXISTS idx_eq_comp_chunk_compressed ON equity_composition_chunk (compressed);
