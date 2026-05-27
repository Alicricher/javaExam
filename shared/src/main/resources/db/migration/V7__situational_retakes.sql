-- Migration: Add situational task retake permissions table
CREATE TABLE IF NOT EXISTS situational_retakes (
    id SERIAL PRIMARY KEY,
    student_id INT REFERENCES students(id) ON DELETE CASCADE,
    task_id INT REFERENCES situational_tasks(id) ON DELETE CASCADE,
    granted_by INT REFERENCES admins(id),
    granted_at TIMESTAMP DEFAULT NOW(),
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,
    UNIQUE (student_id, task_id)
);