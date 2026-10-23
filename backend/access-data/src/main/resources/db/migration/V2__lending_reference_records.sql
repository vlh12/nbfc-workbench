-- Fictional bank lending records.
-- Amount means money lent, expressed in INR rupees.

CREATE TABLE lending_reference (
    id VARCHAR2(36 CHAR) PRIMARY KEY,
    bank_id VARCHAR2(44 CHAR) NOT NULL,
    bank_name VARCHAR2(120 CHAR) NOT NULL,
    reporting_date DATE NOT NULL,
    reference_version NUMBER(10) NOT NULL
        CHECK (reference_version > 0),
    amount_basis VARCHAR2(30 CHAR) NOT NULL
        CHECK (amount_basis = 'LOAN_AMOUNT_LENT'),
    currency VARCHAR2(3 CHAR) NOT NULL
        CHECK (currency = 'INR'),
    money_unit VARCHAR2(10 CHAR) NOT NULL
        CHECK (money_unit = 'RUPEE'),
    CONSTRAINT uq_lending_ref_scope
        UNIQUE (bank_id, reporting_date, reference_version)
);

CREATE TABLE lending_reference_loan (
    id VARCHAR2(36 CHAR) PRIMARY KEY,
    reference_id VARCHAR2(36 CHAR) NOT NULL,
    loan_id VARCHAR2(64 CHAR) NOT NULL,
    nbfc_id VARCHAR2(44 CHAR) NOT NULL,
    nbfc_name VARCHAR2(120 CHAR) NOT NULL,
    amount_lent NUMBER(18,2) NOT NULL
        CHECK (amount_lent > 0),
    CONSTRAINT fk_lending_ref_loan
        FOREIGN KEY (reference_id) REFERENCES lending_reference(id),
    CONSTRAINT uq_lending_ref_loan
        UNIQUE (reference_id, loan_id)
);

INSERT INTO lending_reference (
    id, bank_id, bank_name, reporting_date,
    reference_version, amount_basis, currency, money_unit
) VALUES (
    'SYN-REF-20260930-V1',
    'SYN-BANK-001',
    'Synthetic Demo Bank',
    DATE '2026-09-30',
    1,
    'LOAN_AMOUNT_LENT',
    'INR',
    'RUPEE'
);

INSERT INTO lending_reference_loan
    (id, reference_id, loan_id, nbfc_id, nbfc_name, amount_lent)
VALUES
    ('SYN-REF-SEP-001', 'SYN-REF-20260930-V1',
     'SYN-LOAN-001', 'SYN-NBFC-001',
     'Synthetic NBFC A', 1000000000.00);

INSERT INTO lending_reference_loan
    (id, reference_id, loan_id, nbfc_id, nbfc_name, amount_lent)
VALUES
    ('SYN-REF-SEP-002', 'SYN-REF-20260930-V1',
     'SYN-LOAN-002', 'SYN-NBFC-001',
     'Synthetic NBFC A', 250000000.00);

INSERT INTO lending_reference_loan
    (id, reference_id, loan_id, nbfc_id, nbfc_name, amount_lent)
VALUES
    ('SYN-REF-SEP-003', 'SYN-REF-20260930-V1',
     'SYN-LOAN-003', 'SYN-NBFC-002',
     'Synthetic NBFC B', 800000000.00);

INSERT INTO lending_reference_loan
    (id, reference_id, loan_id, nbfc_id, nbfc_name, amount_lent)
VALUES
    ('SYN-REF-SEP-004', 'SYN-REF-20260930-V1',
     'SYN-LOAN-004', 'SYN-NBFC-003',
     'Synthetic NBFC C', 500000000.00);

INSERT INTO lending_reference_loan
    (id, reference_id, loan_id, nbfc_id, nbfc_name, amount_lent)
VALUES
    ('SYN-REF-SEP-005', 'SYN-REF-20260930-V1',
     'SYN-LOAN-005', 'SYN-NBFC-001',
     'Synthetic NBFC A', 150000000.00);

INSERT INTO lending_reference_loan
    (id, reference_id, loan_id, nbfc_id, nbfc_name, amount_lent)
VALUES
    ('SYN-REF-SEP-006', 'SYN-REF-20260930-V1',
     'SYN-LOAN-006', 'SYN-NBFC-002',
     'Synthetic NBFC B', 200000000.00);