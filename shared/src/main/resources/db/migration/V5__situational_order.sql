ALTER TABLE situational_tasks ADD COLUMN IF NOT EXISTS order_num INT NOT NULL DEFAULT 1;

UPDATE situational_tasks t
SET order_num = sub.rn
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lesson_id ORDER BY created_at) AS rn
    FROM situational_tasks
) sub
WHERE t.id = sub.id;

CREATE INDEX IF NOT EXISTS idx_situational_tasks_lesson_order 
ON situational_tasks(lesson_id, order_num);