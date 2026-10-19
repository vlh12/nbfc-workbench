CREATE TABLE stress_run (
 id VARCHAR2(36 CHAR) PRIMARY KEY,
 bank_id VARCHAR2(44 CHAR) NOT NULL,
 created_by VARCHAR2(255 CHAR) NOT NULL,
 created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
 request_key VARCHAR2(64 CHAR) NOT NULL,
 request_hash VARCHAR2(64 CHAR) NOT NULL,
 payload CLOB NOT NULL,
 CONSTRAINT uq_stress_request UNIQUE (bank_id,created_by,request_key)
);
