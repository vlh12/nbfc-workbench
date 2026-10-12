# Oracle Database configuration

The intended native development database is Oracle Free/AI Database with the `FREEPDB1` pluggable database.

~~~ eg text
URL:      jdbc:oracle:thin:@//localhost:1521/FREEPDB1
Username: user
Password: vikas123
~~~

The VS Code launch configuration passes these values to the upload services. Risk Governance reads `DB_URL`, `DB_USER` and `DB_PASSWORD` from its environment. Before native startup, confirm that Access & Data and Exposure Analytics honor the same account in their `application.yml`; their checked-in defaults contain legacy values and the environment override must be honored.

## Required privileges

The application account needs `CREATE SESSION`, `CREATE TABLE`, required sequence/index privileges and tablespace quota. Do not drop tables to solve a Flyway error. Inspect the service Flyway history table and reconcile the state first.

## Service-owned migrations

The prototype uses one Oracle instance with logically separated table names and Flyway history tables:

- `access_data_flyway_history`: `exposure_batch`, `exposure_record`, `outbox_event`.
- `exposure_analytics_flyway_history`: normalized exposure projection and processed-event records.
- `risk_governance_flyway_history`: `risk_assessment`, `risk_follow_up`, `risk_audit_event`.

Hibernate runs with `ddl-auto=validate`; Flyway owns schema creation. Baseline-on-migrate at version `0` allows a non-empty development schema to be inspected before versioned changes are applied.

## Local checks

On Windows, start the Oracle service and listener, then open `FREEPDB1` if necessary. Verify the account with SQL*Plus or SQL Developer before starting Spring Boot. `ORA-01017` usually means the username, password or service name is wrong. `ORA-00942` or schema-validation errors require migration/table review.

No startup script changes passwords, creates users or deletes tables.

