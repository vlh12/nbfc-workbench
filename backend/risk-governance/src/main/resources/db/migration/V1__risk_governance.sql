CREATE TABLE risk_assessment (
 id VARCHAR2(36 CHAR) PRIMARY KEY,
 batch_id VARCHAR2(36 CHAR) NOT NULL,
 submitted_by VARCHAR2(255 CHAR) NOT NULL,
 status VARCHAR2(20 CHAR) NOT NULL CHECK (status IN ('SUBMITTED','RETURNED','APPROVED')),
 submission_note VARCHAR2(2000 CHAR),
 decision_note VARCHAR2(2000 CHAR),
 decided_by VARCHAR2(255 CHAR),
 submitted_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
 decided_at TIMESTAMP(6) WITH TIME ZONE
);
CREATE INDEX ix_risk_assessment_status ON risk_assessment(status,submitted_at);
CREATE TABLE risk_follow_up (
 id VARCHAR2(36 CHAR) PRIMARY KEY,
 assessment_id VARCHAR2(36 CHAR) NOT NULL REFERENCES risk_assessment(id),
 title VARCHAR2(500 CHAR) NOT NULL,
 owner VARCHAR2(255 CHAR) NOT NULL,
 due_date DATE NOT NULL,
 status VARCHAR2(20 CHAR) NOT NULL CHECK (status IN ('OPEN','COMPLETED')),
 created_by VARCHAR2(255 CHAR) NOT NULL,
 created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
 updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_risk_follow_up_due ON risk_follow_up(status,due_date);
CREATE TABLE risk_audit_event (
 id VARCHAR2(36 CHAR) PRIMARY KEY,
 entity_type VARCHAR2(40 CHAR) NOT NULL,
 entity_id VARCHAR2(36 CHAR) NOT NULL,
 action VARCHAR2(60 CHAR) NOT NULL,
 actor VARCHAR2(255 CHAR) NOT NULL,
 details VARCHAR2(2000 CHAR),
 created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_risk_audit_created ON risk_audit_event(created_at);

