-- Same bilingual pattern as V16, applied to units (F1/F2/F3 subject labels),
-- which were missed from V16 because their button text is built separately
-- in StudentKeyboards and wasn't caught until QA found the unit-selection
-- button still showing Uzbek regardless of the student's chosen language.

ALTER TABLE units ADD COLUMN title_ru VARCHAR(255);
