-- Migration: Add unique constraint to prevent duplicate situational task submissions
-- This ensures a student can only have one answer per task unless admin allows resubmission

-- First, remove any duplicate entries (keep the latest one)
DELETE FROM situational_answers a
USING situational_answers b
WHERE a.id < b.id
  AND a.student_id = b.student_id
  AND a.task_id = b.task_id;

-- Add unique constraint
ALTER TABLE situational_answers
ADD CONSTRAINT unique_student_task UNIQUE (student_id, task_id);
