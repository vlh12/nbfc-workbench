CREATE TABLE portfolio_document (
 id VARCHAR2(36 CHAR) PRIMARY KEY,
 kind VARCHAR2(20 CHAR) NOT NULL,
 bank_id VARCHAR2(44 CHAR) NOT NULL,
 reporting_date DATE,
 status VARCHAR2(20 CHAR) NOT NULL,
 submitted_by VARCHAR2(255 CHAR) NOT NULL,
 created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
 row_count NUMBER(10) NOT NULL,
 payload CLOB NOT NULL
);
CREATE INDEX ix_portfolio_bank_created ON portfolio_document(bank_id,created_at);
