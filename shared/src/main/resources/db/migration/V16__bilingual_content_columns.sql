-- Adds optional Russian-language variants of lesson/test/question/task content.
-- The existing columns keep their historical meaning (Uzbek for most units);
-- these new columns hold the Russian translation when one exists, so a
-- student's chosen language can be honored with a fallback to the base
-- column when no translation is available for that language.

ALTER TABLE lessons ADD COLUMN title_ru VARCHAR(255);
ALTER TABLE tests ADD COLUMN title_ru VARCHAR(255);
ALTER TABLE questions ADD COLUMN question_text_ru TEXT;
ALTER TABLE answer_options ADD COLUMN option_text_ru TEXT;
ALTER TABLE situational_tasks ADD COLUMN task_text_ru TEXT;
