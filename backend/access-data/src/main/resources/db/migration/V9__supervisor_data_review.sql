ALTER TABLE portfolio_document ADD lock_version NUMBER(19) DEFAULT 0 NOT NULL;
CREATE TABLE portfolio_data_review (
 id VARCHAR2(36 CHAR) PRIMARY KEY,
 document_id VARCHAR2(36 CHAR) NOT NULL REFERENCES portfolio_document(id),
 decision VARCHAR2(20 CHAR) NOT NULL,
 reviewed_by VARCHAR2(255 CHAR) NOT NULL,
 reviewed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
 review_note VARCHAR2(2000 CHAR) NOT NULL
);
CREATE INDEX ix_data_review_document ON portfolio_data_review(document_id,reviewed_at);
