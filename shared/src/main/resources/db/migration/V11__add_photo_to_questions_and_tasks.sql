ALTER TABLE questions ADD COLUMN IF NOT EXISTS photo_file_path VARCHAR(500);
ALTER TABLE situational_tasks ADD COLUMN IF NOT EXISTS photo_file_path VARCHAR(500);
