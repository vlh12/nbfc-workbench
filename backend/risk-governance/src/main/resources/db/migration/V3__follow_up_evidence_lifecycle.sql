ALTER TABLE risk_follow_up ADD (evidence VARCHAR2(2000 CHAR), version NUMBER(19) DEFAULT 0 NOT NULL);
DECLARE
BEGIN
 FOR c IN (SELECT constraint_name FROM user_constraints
           WHERE table_name = 'RISK_FOLLOW_UP' AND constraint_type = 'C'
             AND UPPER(search_condition_vc) LIKE '%STATUS%'
             AND UPPER(search_condition_vc) LIKE '%COMPLETED%')
 LOOP
   EXECUTE IMMEDIATE 'ALTER TABLE risk_follow_up DROP CONSTRAINT "' || c.constraint_name || '"';
 END LOOP;
END;
/
ALTER TABLE risk_follow_up ADD CONSTRAINT ck_risk_follow_status CHECK
 (status IN ('OPEN','COMPLETED','IN_PROGRESS','EVIDENCE_SUBMITTED','VERIFIED','REOPENED','CLOSED'));
