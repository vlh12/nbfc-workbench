CREATE TABLE wb_login_challenge (
    id VARCHAR2(36 CHAR) NOT NULL,
    user_id VARCHAR2(36 CHAR) NOT NULL,
    binding_hash VARCHAR2(64 CHAR) NOT NULL,
    code_hash VARCHAR2(64 CHAR) NOT NULL,
    account_version NUMBER(19, 0) NOT NULL,
    role_name VARCHAR2(16 CHAR) NOT NULL,
    attempts_remaining NUMBER(1, 0) DEFAULT 5 NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE
        DEFAULT SYSTIMESTAMP NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT pk_wb_login_challenge PRIMARY KEY (id),

    CONSTRAINT fk_wb_login_challenge_user
        FOREIGN KEY (user_id) REFERENCES wb_app_user(id),

    CONSTRAINT ck_wb_login_challenge_role
        CHECK (role_name IN (
            'CONTRIBUTOR', 'ANALYST', 'SUPERVISOR'
        )),

    CONSTRAINT ck_wb_login_challenge_tries
        CHECK (attempts_remaining BETWEEN 0 AND 5)
);

CREATE INDEX ix_wb_login_challenge_user
    ON wb_login_challenge(user_id);

CREATE INDEX ix_wb_login_challenge_expiry
    ON wb_login_challenge(expires_at);