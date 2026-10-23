ALTER TABLE risk_assessment ADD (
 scenario_id VARCHAR2(36 CHAR),
 scenario_evidence CLOB,
 version NUMBER(19) DEFAULT 0 NOT NULL
);
CREATE INDEX ix_assessment_scenario ON risk_assessment(scenario_id);
