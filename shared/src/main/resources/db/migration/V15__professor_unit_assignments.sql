-- Зав.кафедрой назначает профессорам предметы (units), которыми они могут управлять.
-- SUPER_ADMIN и ZAV_KAFEDRA не нуждаются в записях здесь — им разрешено всё на уровне
-- кода (см. AccessControlService), таблица используется только для роли PROFESSOR.
CREATE TABLE IF NOT EXISTS professor_unit_assignments (
    id             SERIAL PRIMARY KEY,
    admin_user_id  INT NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    unit_id        INT NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    assigned_by    INT REFERENCES admin_users(id),
    assigned_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (admin_user_id, unit_id)
);

CREATE INDEX IF NOT EXISTS idx_professor_unit_assignments_admin_user
    ON professor_unit_assignments(admin_user_id);
