-- Students table
CREATE TABLE IF NOT EXISTS students (
    id SERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    course INT NOT NULL,
    group_name VARCHAR(50) NOT NULL,
    subgroup VARCHAR(50) NOT NULL,
    faculty VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Admins table
CREATE TABLE IF NOT EXISTS admins (
    id SERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_authenticated BOOLEAN DEFAULT FALSE,
    session_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Units table (F1, F2, F3)
CREATE TABLE IF NOT EXISTS units (
    id SERIAL PRIMARY KEY,
    name VARCHAR(10) UNIQUE NOT NULL,
    title_uz VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Lessons table
CREATE TABLE IF NOT EXISTS lessons (
    id SERIAL PRIMARY KEY,
    unit_id INT REFERENCES units(id) ON DELETE CASCADE,
    lesson_number INT NOT NULL,
    title_uz VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(unit_id, lesson_number)
);

-- Tests table
CREATE TABLE IF NOT EXISTS tests (
    id SERIAL PRIMARY KEY,
    lesson_id INT REFERENCES lessons(id) ON DELETE CASCADE,
    title_uz VARCHAR(255) NOT NULL DEFAULT 'Test',
    time_limit_minutes INT NOT NULL DEFAULT 30,
    total_points INT NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Questions table
CREATE TABLE IF NOT EXISTS questions (
    id SERIAL PRIMARY KEY,
    test_id INT REFERENCES tests(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    points INT NOT NULL DEFAULT 1,
    order_num INT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Answer options table
CREATE TABLE IF NOT EXISTS answer_options (
    id SERIAL PRIMARY KEY,
    question_id INT REFERENCES questions(id) ON DELETE CASCADE,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN DEFAULT FALSE,
    order_num INT NOT NULL DEFAULT 0
);

-- Test results table
CREATE TABLE IF NOT EXISTS test_results (
    id SERIAL PRIMARY KEY,
    student_id INT REFERENCES students(id) ON DELETE CASCADE,
    test_id INT REFERENCES tests(id) ON DELETE CASCADE,
    score INT NOT NULL DEFAULT 0,
    max_score INT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'in_progress',
    CONSTRAINT valid_status CHECK (status IN ('in_progress', 'completed', 'timeout'))
);

-- Test answers table (student's answers to questions)
CREATE TABLE IF NOT EXISTS test_answers (
    id SERIAL PRIMARY KEY,
    result_id INT REFERENCES test_results(id) ON DELETE CASCADE,
    question_id INT REFERENCES questions(id) ON DELETE CASCADE,
    selected_option_id INT REFERENCES answer_options(id) ON DELETE SET NULL,
    is_correct BOOLEAN,
    answered_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(result_id, question_id)
);

-- Theory materials table
CREATE TABLE IF NOT EXISTS theory_materials (
    id SERIAL PRIMARY KEY,
    lesson_id INT REFERENCES lessons(id) ON DELETE CASCADE,
    title_uz VARCHAR(255) NOT NULL,
    material_type VARCHAR(20) NOT NULL DEFAULT 'material',
    file_path VARCHAR(500),
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT valid_material_type CHECK (material_type IN ('book', 'manual', 'material'))
);

-- Situational tasks table
CREATE TABLE IF NOT EXISTS situational_tasks (
    id SERIAL PRIMARY KEY,
    lesson_id INT REFERENCES lessons(id) ON DELETE CASCADE,
    task_text TEXT NOT NULL,
    time_limit_minutes INT NOT NULL DEFAULT 30,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Situational answers table
CREATE TABLE IF NOT EXISTS situational_answers (
    id SERIAL PRIMARY KEY,
    student_id INT REFERENCES students(id) ON DELETE CASCADE,
    task_id INT REFERENCES situational_tasks(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL,
    submitted_at TIMESTAMP DEFAULT NOW(),
    is_graded BOOLEAN DEFAULT FALSE,
    grade INT,
    feedback TEXT,
    graded_by INT REFERENCES admins(id),
    graded_at TIMESTAMP
);

-- Test retakes table (admin-granted retakes)
CREATE TABLE IF NOT EXISTS test_retakes (
    id SERIAL PRIMARY KEY,
    student_id INT REFERENCES students(id) ON DELETE CASCADE,
    test_id INT REFERENCES tests(id) ON DELETE CASCADE,
    granted_by INT REFERENCES admins(id),
    granted_at TIMESTAMP DEFAULT NOW(),
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP
);

-- User states table (for FSM)
CREATE TABLE IF NOT EXISTS user_states (
    telegram_id BIGINT PRIMARY KEY,
    bot_type VARCHAR(20) NOT NULL DEFAULT 'student',
    state VARCHAR(100) NOT NULL DEFAULT 'idle',
    state_data JSONB DEFAULT '{}',
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT valid_bot_type CHECK (bot_type IN ('student', 'admin'))
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_students_telegram_id ON students(telegram_id);
CREATE INDEX IF NOT EXISTS idx_students_course ON students(course);
CREATE INDEX IF NOT EXISTS idx_students_group ON students(group_name);
CREATE INDEX IF NOT EXISTS idx_students_faculty ON students(faculty);
CREATE INDEX IF NOT EXISTS idx_lessons_unit_id ON lessons(unit_id);
CREATE INDEX IF NOT EXISTS idx_questions_test_id ON questions(test_id);
CREATE INDEX IF NOT EXISTS idx_answer_options_question_id ON answer_options(question_id);
CREATE INDEX IF NOT EXISTS idx_test_results_student_id ON test_results(student_id);
CREATE INDEX IF NOT EXISTS idx_test_results_test_id ON test_results(test_id);
CREATE INDEX IF NOT EXISTS idx_situational_answers_student_id ON situational_answers(student_id);
CREATE INDEX IF NOT EXISTS idx_user_states_telegram_id ON user_states(telegram_id);

-- Insert default units
INSERT INTO units (name, title_uz) VALUES
    ('F1', 'F1 - Birinchi bo''lim'),
    ('F2', 'F2 - Ikkinchi bo''lim'),
    ('F3', 'F3 - Uchinchi bo''lim')
ON CONFLICT (name) DO NOTHING;
