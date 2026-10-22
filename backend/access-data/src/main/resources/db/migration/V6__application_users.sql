CREATE TABLE wb_app_user (
    id                  VARCHAR2(36 CHAR) NOT NULL,
    username            VARCHAR2(64 CHAR) NOT NULL,
    password_hash       VARCHAR2(100 CHAR) NOT NULL,
    role_name           VARCHAR2(16 CHAR) NOT NULL,
    status              VARCHAR2(16 CHAR) NOT NULL,
    created_by          VARCHAR2(36 CHAR) NOT NULL,
    created_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    password_changed_at TIMESTAMP(6) WITH TIME ZONE,
    version             NUMBER(19,0) DEFAULT 0 NOT NULL,

    CONSTRAINT pk_wb_app_user PRIMARY KEY (id),
    CONSTRAINT uq_wb_app_user_username UNIQUE (username),

    CONSTRAINT ck_wb_app_user_role CHECK (
        role_name IN ('CONTRIBUTOR', 'ANALYST', 'SUPERVISOR')
    ),

    CONSTRAINT ck_wb_app_user_status CHECK (
        status IN ('ACTIVE', 'DISABLED')
    ),

    CONSTRAINT ck_wb_app_user_username CHECK (
        username = LOWER(username)
    )
);