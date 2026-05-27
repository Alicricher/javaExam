-- Add photo_file_id column to situational_answers table
ALTER TABLE situational_answers ADD COLUMN IF NOT EXISTS photo_file_id TEXT DEFAULT '';
