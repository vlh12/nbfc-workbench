CREATE TABLE lending_report (
    id                   VARCHAR2(36 CHAR) PRIMARY KEY,
    reference_id         VARCHAR2(36 CHAR) NOT NULL,
    bank_id              VARCHAR2(44 CHAR) NOT NULL,
    nbfc_id              VARCHAR2(44 CHAR) NOT NULL,
    nbfc_name            VARCHAR2(120 CHAR) NOT NULL,
    reporting_date       DATE NOT NULL,
    revenue_period_start DATE NOT NULL,
    revenue_period_end   DATE NOT NULL,
    revenue_amount       NUMBER(18,2) NOT NULL,
    currency             VARCHAR2(3 CHAR) NOT NULL,
    money_unit           VARCHAR2(10 CHAR) NOT NULL,
    row_count            NUMBER(10) NOT NULL,
    status               VARCHAR2(30 CHAR) DEFAULT 'VALIDATED' NOT NULL,
    original_filename    VARCHAR2(255 CHAR) NOT NULL,
    submitted_by         VARCHAR2(255 CHAR) NOT NULL,
    created_at           TIMESTAMP(6) WITH TIME ZONE
                         DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_lr_reference
        FOREIGN KEY (reference_id)
        REFERENCES lending_reference(id),

    CONSTRAINT ck_lr_revenue
        CHECK (revenue_amount >= 0),

    CONSTRAINT ck_lr_dates
        CHECK (
            revenue_period_start <= revenue_period_end
            AND revenue_period_end <= reporting_date
        ),

    CONSTRAINT ck_lr_currency
        CHECK (currency = 'INR'),

    CONSTRAINT ck_lr_unit
        CHECK (money_unit = 'RUPEE'),

    CONSTRAINT ck_lr_row_count
        CHECK (row_count > 0 AND row_count <= 10000),

    CONSTRAINT ck_lr_status
        CHECK (status = 'VALIDATED')
);

CREATE TABLE lending_report_loan (
    id              VARCHAR2(36 CHAR) PRIMARY KEY,
    report_id       VARCHAR2(36 CHAR) NOT NULL,
    loan_id         VARCHAR2(64 CHAR) NOT NULL,
    amount_lent     NUMBER(18,2) NOT NULL,
    interest_amount NUMBER(18,2) NOT NULL,
    amount_repaid   NUMBER(18,2) NOT NULL,

    CONSTRAINT fk_lrl_report
        FOREIGN KEY (report_id)
        REFERENCES lending_report(id),

    CONSTRAINT uq_lrl_report_loan
        UNIQUE (report_id, loan_id),

    CONSTRAINT ck_lrl_lent
        CHECK (amount_lent > 0),

    CONSTRAINT ck_lrl_interest
        CHECK (interest_amount >= 0),

    CONSTRAINT ck_lrl_repaid
        CHECK (
            amount_repaid >= 0
            AND amount_repaid <= amount_lent + interest_amount
        )
);

CREATE INDEX ix_lending_report_history
    ON lending_report(bank_id, submitted_by, created_at);