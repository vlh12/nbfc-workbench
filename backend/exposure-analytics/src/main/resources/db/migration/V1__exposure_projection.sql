CREATE TABLE processed_event (id VARCHAR2(36 CHAR) PRIMARY KEY);
CREATE TABLE exposure_record (
 id VARCHAR2(50 CHAR) PRIMARY KEY,
 batch_id VARCHAR2(36 CHAR) NOT NULL,
 bank_id VARCHAR2(44 CHAR) NOT NULL,
 nbfc_id VARCHAR2(44 CHAR) NOT NULL,
 exposure_amount NUMBER(18,2) NOT NULL CHECK (exposure_amount >= 0)
);
CREATE INDEX ix_exposure_nbfc ON exposure_record(nbfc_id);

