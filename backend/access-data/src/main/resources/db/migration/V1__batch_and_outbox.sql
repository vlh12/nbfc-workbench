CREATE TABLE exposure_batch (
 id VARCHAR2(36 CHAR) PRIMARY KEY,
 submitted_by VARCHAR2(255 CHAR) NOT NULL,
 row_count NUMBER(10) NOT NULL,
 status VARCHAR2(30 CHAR) NOT NULL,
 created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE TABLE outbox_event (
 id VARCHAR2(36 CHAR) PRIMARY KEY,
 batch_id VARCHAR2(36 CHAR) NOT NULL REFERENCES exposure_batch(id),
 payload CLOB NOT NULL,
 published NUMBER(1) DEFAULT 0 NOT NULL CHECK (published IN (0,1)),
 created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_outbox_pending ON outbox_event(published,created_at);

