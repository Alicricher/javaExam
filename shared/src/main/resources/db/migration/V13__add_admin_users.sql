CREATE TABLE IF NOT EXISTS admin_users (
    id          SERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    role        VARCHAR(50)  NOT NULL DEFAULT 'PROFESSOR',
    full_name   VARCHAR(200),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
