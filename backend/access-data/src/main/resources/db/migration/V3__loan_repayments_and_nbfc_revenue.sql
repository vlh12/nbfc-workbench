-- Synthetic demonstration data only.
--
-- Loan figures are cumulative through 2026-09-30.
-- Interest is a recorded amount, not an annual interest rate.
-- Amount repaid includes principal and interest payments.

-- Confirm the expected starting data before changing the schema.
DECLARE
    reference_count PLS_INTEGER;
    loan_count      PLS_INTEGER;
    matching_count  PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO reference_count
    FROM lending_reference
    WHERE id = 'SYN-REF-20260930-V1'
      AND bank_id = 'SYN-BANK-001'
      AND reporting_date = DATE '2026-09-30'
      AND currency = 'INR'
      AND money_unit = 'RUPEE';

    SELECT COUNT(*)
    INTO loan_count
    FROM lending_reference_loan;

    SELECT COUNT(*)
    INTO matching_count
    FROM lending_reference_loan
    WHERE reference_id = 'SYN-REF-20260930-V1'
      AND (
          (loan_id = 'SYN-LOAN-001'
           AND nbfc_id = 'SYN-NBFC-001'
           AND amount_lent = 1000000000)
       OR (loan_id = 'SYN-LOAN-002'
           AND nbfc_id = 'SYN-NBFC-001'
           AND amount_lent = 250000000)
       OR (loan_id = 'SYN-LOAN-003'
           AND nbfc_id = 'SYN-NBFC-002'
           AND amount_lent = 800000000)
       OR (loan_id = 'SYN-LOAN-004'
           AND nbfc_id = 'SYN-NBFC-003'
           AND amount_lent = 500000000)
       OR (loan_id = 'SYN-LOAN-005'
           AND nbfc_id = 'SYN-NBFC-001'
           AND amount_lent = 150000000)
       OR (loan_id = 'SYN-LOAN-006'
           AND nbfc_id = 'SYN-NBFC-002'
           AND amount_lent = 200000000)
      );

    IF reference_count <> 1
       OR loan_count <> 6
       OR matching_count <> 6 THEN
        RAISE_APPLICATION_ERROR(
            -20001,
            'Expected the original six synthetic reference loans. Check the database before applying V3.'
        );
    END IF;
END;
/

-- Add the two new source amounts.
ALTER TABLE lending_reference_loan ADD (
    interest_amount NUMBER(18,2),
    amount_repaid   NUMBER(18,2)
);

UPDATE lending_reference_loan
SET interest_amount = 60000000.00,
    amount_repaid = 200000000.00
WHERE reference_id = 'SYN-REF-20260930-V1'
  AND loan_id = 'SYN-LOAN-001';

UPDATE lending_reference_loan
SET interest_amount = 15000000.00,
    amount_repaid = 50000000.00
WHERE reference_id = 'SYN-REF-20260930-V1'
  AND loan_id = 'SYN-LOAN-002';

UPDATE lending_reference_loan
SET interest_amount = 40000000.00,
    amount_repaid = 160000000.00
WHERE reference_id = 'SYN-REF-20260930-V1'
  AND loan_id = 'SYN-LOAN-003';

UPDATE lending_reference_loan
SET interest_amount = 25000000.00,
    amount_repaid = 100000000.00
WHERE reference_id = 'SYN-REF-20260930-V1'
  AND loan_id = 'SYN-LOAN-004';

UPDATE lending_reference_loan
SET interest_amount = 6000000.00,
    amount_repaid = 0.00
WHERE reference_id = 'SYN-REF-20260930-V1'
  AND loan_id = 'SYN-LOAN-005';

UPDATE lending_reference_loan
SET interest_amount = 10000000.00,
    amount_repaid = 210000000.00
WHERE reference_id = 'SYN-REF-20260930-V1'
  AND loan_id = 'SYN-LOAN-006';

ALTER TABLE lending_reference_loan MODIFY (
    interest_amount NOT NULL,
    amount_repaid NOT NULL
);

ALTER TABLE lending_reference_loan ADD (
    CONSTRAINT ck_lrl_interest_nonneg
        CHECK (interest_amount >= 0),
    CONSTRAINT ck_lrl_repaid_nonneg
        CHECK (amount_repaid >= 0),
    CONSTRAINT ck_lrl_repaid_limit
        CHECK (amount_repaid <= amount_lent + interest_amount)
);

-- Revenue belongs to the NBFC and reporting period.
-- It is not repeated for each loan.
-- Currency and unit come from the parent lending reference.
CREATE TABLE nbfc_revenue_reference (
    id             VARCHAR2(36 CHAR) PRIMARY KEY,
    reference_id   VARCHAR2(36 CHAR) NOT NULL,
    nbfc_id        VARCHAR2(44 CHAR) NOT NULL,
    period_start   DATE NOT NULL,
    period_end     DATE NOT NULL,
    revenue_amount NUMBER(18,2) NOT NULL,

    CONSTRAINT fk_nbfc_rev_reference
        FOREIGN KEY (reference_id)
        REFERENCES lending_reference(id),

    CONSTRAINT uq_nbfc_rev_period
        UNIQUE (reference_id, nbfc_id, period_start, period_end),

    CONSTRAINT ck_nbfc_rev_dates
        CHECK (period_start <= period_end),

    CONSTRAINT ck_nbfc_rev_amount
        CHECK (revenue_amount >= 0)
);

-- Nine synthetic revenue records:
-- three NBFCs, each with three completed reporting periods.
-- These amounts represent revenue before expenses, not profit.
INSERT INTO nbfc_revenue_reference (
    id,
    reference_id,
    nbfc_id,
    period_start,
    period_end,
    revenue_amount
)
SELECT
    id,
    'SYN-REF-20260930-V1',
    nbfc_id,
    period_start,
    period_end,
    revenue_amount
FROM (
    SELECT
        'SYN-REV-A-3M-20260930' AS id,
        'SYN-NBFC-001' AS nbfc_id,
        DATE '2026-07-01' AS period_start,
        DATE '2026-09-30' AS period_end,
        180000000.00 AS revenue_amount
    FROM dual

    UNION ALL
    SELECT 'SYN-REV-A-6M-20260930', 'SYN-NBFC-001',
           DATE '2026-04-01', DATE '2026-09-30', 345000000.00
    FROM dual

    UNION ALL
    SELECT 'SYN-REV-A-12M-20260331', 'SYN-NBFC-001',
           DATE '2025-04-01', DATE '2026-03-31', 650000000.00
    FROM dual

    UNION ALL
    SELECT 'SYN-REV-B-3M-20260930', 'SYN-NBFC-002',
           DATE '2026-07-01', DATE '2026-09-30', 130000000.00
    FROM dual

    UNION ALL
    SELECT 'SYN-REV-B-6M-20260930', 'SYN-NBFC-002',
           DATE '2026-04-01', DATE '2026-09-30', 250000000.00
    FROM dual

    UNION ALL
    SELECT 'SYN-REV-B-12M-20260331', 'SYN-NBFC-002',
           DATE '2025-04-01', DATE '2026-03-31', 480000000.00
    FROM dual

    UNION ALL
    SELECT 'SYN-REV-C-3M-20260930', 'SYN-NBFC-003',
           DATE '2026-07-01', DATE '2026-09-30', 75000000.00
    FROM dual

    UNION ALL
    SELECT 'SYN-REV-C-6M-20260930', 'SYN-NBFC-003',
           DATE '2026-04-01', DATE '2026-09-30', 145000000.00
    FROM dual

    UNION ALL
    SELECT 'SYN-REV-C-12M-20260331', 'SYN-NBFC-003',
           DATE '2025-04-01', DATE '2026-03-31', 280000000.00
    FROM dual
);