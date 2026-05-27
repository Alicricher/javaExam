-- Migration: Fix order_num to be sequential per lesson (1,2,3... per lesson, not globally)
UPDATE situational_tasks t
SET order_num = sub.rn
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lesson_id ORDER BY order_num, id) AS rn
    FROM situational_tasks
    WHERE is_active = TRUE
) sub
WHERE t.id = sub.id;