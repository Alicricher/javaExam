-- Seed data for University Dentistry Bot
-- Sample lessons, tests, questions, theory materials, and situational tasks

-- =====================
-- LESSONS FOR F1
-- =====================
INSERT INTO lessons (unit_id, lesson_number, title_uz) VALUES
    (1, 1, 'Tish anatomiyasi va morfologiyasi'),
    (1, 2, 'Og''iz bo''shlig''i anatomiyasi'),
    (1, 3, 'Tish rivojlanishi va o''sishi'),
    (1, 4, 'Tish to''qimalari tuzilishi'),
    (1, 5, 'Parodont anatomiyasi'),
    (1, 6, 'Jag'' suyaklari anatomiyasi'),
    (1, 7, 'Chaynov mushaklari'),
    (1, 8, 'Og''iz bo''shlig''i shilliq qavati'),
    (1, 9, 'So''lak bezlari anatomiyasi'),
    (1, 10, 'Tish turlari va funksiyalari'),
    (1, 11, 'Sut tishlari xususiyatlari'),
    (1, 12, 'Doimiy tishlar xususiyatlari')
ON CONFLICT DO NOTHING;

-- =====================
-- LESSONS FOR F2
-- =====================
INSERT INTO lessons (unit_id, lesson_number, title_uz) VALUES
    (2, 1, 'Kariyes etiologiyasi'),
    (2, 2, 'Kariyes patogenezi'),
    (2, 3, 'Kariyes tasniflashi'),
    (2, 4, 'Kariyes diagnostikasi'),
    (2, 5, 'Kariyes davolash usullari'),
    (2, 6, 'Pulpit etiologiyasi va klinikasi'),
    (2, 7, 'Pulpit davolash'),
    (2, 8, 'Periodontit'),
    (2, 9, 'Endodontik davolash'),
    (2, 10, 'Tish plombalash materiallari'),
    (2, 11, 'Estetik stomatologiya asoslari'),
    (2, 12, 'Profilaktik stomatologiya')
ON CONFLICT DO NOTHING;

-- =====================
-- LESSONS FOR F3
-- =====================
INSERT INTO lessons (unit_id, lesson_number, title_uz) VALUES
    (3, 1, 'Tish olish ko''rsatmalari'),
    (3, 2, 'Tish olish texnikasi'),
    (3, 3, 'Jarrohlik asboblari'),
    (3, 4, 'Mahalliy anesteziya'),
    (3, 5, 'Tish olishdan keyingi asoratlar'),
    (3, 6, 'Og''iz bo''shlig''i yallig''lanishlari'),
    (3, 7, 'Odontogen infeksiyalar'),
    (3, 8, 'Jag'' sinishlari'),
    (3, 9, 'Implantatsiya asoslari'),
    (3, 10, 'Ortopedik stomatologiya kirish'),
    (3, 11, 'Protezlash turlari'),
    (3, 12, 'Ortodontiya asoslari')
ON CONFLICT DO NOTHING;

-- =====================
-- TESTS FOR F1 LESSONS
-- =====================

-- Test for Lesson 1 (F1)
INSERT INTO tests (lesson_id, title_uz, time_limit_minutes, total_points)
SELECT id, 'Tish anatomiyasi testi', 15, 10 FROM lessons WHERE unit_id = 1 AND lesson_number = 1
ON CONFLICT DO NOTHING;

-- Test for Lesson 2 (F1)
INSERT INTO tests (lesson_id, title_uz, time_limit_minutes, total_points)
SELECT id, 'Og''iz bo''shlig''i anatomiyasi testi', 15, 10 FROM lessons WHERE unit_id = 1 AND lesson_number = 2
ON CONFLICT DO NOTHING;

-- Test for Lesson 3 (F1)
INSERT INTO tests (lesson_id, title_uz, time_limit_minutes, total_points)
SELECT id, 'Tish rivojlanishi testi', 15, 10 FROM lessons WHERE unit_id = 1 AND lesson_number = 3
ON CONFLICT DO NOTHING;

-- =====================
-- QUESTIONS FOR F1 LESSON 1 TEST
-- =====================

-- Get test ID for F1 Lesson 1
DO $$
DECLARE
    test_id_var INT;
    question_id_var INT;
BEGIN
    -- Get test for F1 Lesson 1
    SELECT t.id INTO test_id_var
    FROM tests t
    JOIN lessons l ON t.lesson_id = l.id
    WHERE l.unit_id = 1 AND l.lesson_number = 1
    LIMIT 1;

    IF test_id_var IS NOT NULL THEN
        -- Question 1
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Odamda nechta doimiy tish bo''ladi?', 1, 1)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, '28 ta', FALSE, 1),
            (question_id_var, '32 ta', TRUE, 2),
            (question_id_var, '30 ta', FALSE, 3),
            (question_id_var, '26 ta', FALSE, 4);

        -- Question 2
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Tish tojining tashqi qavati qanday nomlanadi?', 1, 2)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Dentin', FALSE, 1),
            (question_id_var, 'Emal', TRUE, 2),
            (question_id_var, 'Sement', FALSE, 3),
            (question_id_var, 'Pulpa', FALSE, 4);

        -- Question 3
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Tish ildizini qoplab turgan qattiq to''qima nima deyiladi?', 1, 3)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Emal', FALSE, 1),
            (question_id_var, 'Dentin', FALSE, 2),
            (question_id_var, 'Sement', TRUE, 3),
            (question_id_var, 'Pulpa', FALSE, 4);

        -- Question 4
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Tish pulpasi qayerda joylashgan?', 1, 4)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Tish tojida', FALSE, 1),
            (question_id_var, 'Tish bo''shlig''ida', TRUE, 2),
            (question_id_var, 'Tish ildizida', FALSE, 3),
            (question_id_var, 'Tish bo''ynida', FALSE, 4);

        -- Question 5
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Kesuvchi tishlar necha bo''lakdan iborat?', 1, 5)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, '1 ta ildiz', TRUE, 1),
            (question_id_var, '2 ta ildiz', FALSE, 2),
            (question_id_var, '3 ta ildiz', FALSE, 3),
            (question_id_var, '4 ta ildiz', FALSE, 4);

        -- Question 6
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Oziq tishlar (katta oziq) qaysi joyda joylashgan?', 1, 6)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Jag''ning oldingi qismida', FALSE, 1),
            (question_id_var, 'Jag''ning orqa qismida', TRUE, 2),
            (question_id_var, 'Jag''ning o''rta qismida', FALSE, 3),
            (question_id_var, 'Faqat yuqori jag''da', FALSE, 4);

        -- Question 7
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Tishning qaysi qismi milkda ko''rinadi?', 1, 7)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Ildiz', FALSE, 1),
            (question_id_var, 'Toj', TRUE, 2),
            (question_id_var, 'Bo''yin', FALSE, 3),
            (question_id_var, 'Pulpa', FALSE, 4);

        -- Question 8
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Dentin nima?', 1, 8)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Tishning eng qattiq qismi', FALSE, 1),
            (question_id_var, 'Tishning asosiy massasini tashkil etuvchi to''qima', TRUE, 2),
            (question_id_var, 'Tish ildizining qoplamasi', FALSE, 3),
            (question_id_var, 'Tish ichidagi yumshoq to''qima', FALSE, 4);

        -- Question 9
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'So''yloq tishlar nechta?', 1, 9)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, '2 ta', FALSE, 1),
            (question_id_var, '4 ta', TRUE, 2),
            (question_id_var, '6 ta', FALSE, 3),
            (question_id_var, '8 ta', FALSE, 4);

        -- Question 10
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Emal qaysi moddalardan tashkil topgan?', 1, 10)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Organik moddalar', FALSE, 1),
            (question_id_var, 'Gidroksilapatit kristallari', TRUE, 2),
            (question_id_var, 'Kollagen tolalar', FALSE, 3),
            (question_id_var, 'Nerv tolalari', FALSE, 4);

        -- Update total points
        UPDATE tests SET total_points = 10 WHERE id = test_id_var;
    END IF;
END $$;

-- =====================
-- QUESTIONS FOR F1 LESSON 2 TEST (Og'iz bo'shlig'i anatomiyasi)
-- =====================

DO $$
DECLARE
    test_id_var INT;
    question_id_var INT;
BEGIN
    SELECT t.id INTO test_id_var
    FROM tests t
    JOIN lessons l ON t.lesson_id = l.id
    WHERE l.unit_id = 1 AND l.lesson_number = 2
    LIMIT 1;

    IF test_id_var IS NOT NULL THEN
        -- Question 1
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Og''iz bo''shlig''i qaysi strukturalar bilan chegaralangan?', 1, 1)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Faqat tishlar', FALSE, 1),
            (question_id_var, 'Lablar, yonoqlar, tanglayva til', TRUE, 2),
            (question_id_var, 'Faqat jag'' suyaklari', FALSE, 3),
            (question_id_var, 'Faqat til', FALSE, 4);

        -- Question 2
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Til mushaklari qanday xususiyatga ega?', 1, 2)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Faqat uzunasiga joylashgan', FALSE, 1),
            (question_id_var, 'Uch yo''nalishda joylashgan', TRUE, 2),
            (question_id_var, 'Faqat ko''ndalangiga joylashgan', FALSE, 3),
            (question_id_var, 'Mushaklar yo''q', FALSE, 4);

        -- Question 3
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Qattiq tanglay nima bilan qoplangan?', 1, 3)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Teri', FALSE, 1),
            (question_id_var, 'Shilliq qavat', TRUE, 2),
            (question_id_var, 'Mushak', FALSE, 3),
            (question_id_var, 'Tog''ay', FALSE, 4);

        -- Question 4
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Yumshoq tanglay qayerda joylashgan?', 1, 4)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Og''iz bo''shlig''ining oldingi qismida', FALSE, 1),
            (question_id_var, 'Og''iz bo''shlig''ining orqa qismida', TRUE, 2),
            (question_id_var, 'Til ostida', FALSE, 3),
            (question_id_var, 'Yonoq ichida', FALSE, 4);

        -- Question 5
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Og''iz bo''shlig''ida nechta asosiy so''lak bezi bor?', 1, 5)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, '2 juft', FALSE, 1),
            (question_id_var, '3 juft', TRUE, 2),
            (question_id_var, '4 juft', FALSE, 3),
            (question_id_var, '1 juft', FALSE, 4);

        UPDATE tests SET total_points = 5 WHERE id = test_id_var;
    END IF;
END $$;

-- =====================
-- TEST FOR F2 LESSON 1 (Kariyes)
-- =====================

INSERT INTO tests (lesson_id, title_uz, time_limit_minutes, total_points)
SELECT id, 'Kariyes etiologiyasi testi', 20, 10 FROM lessons WHERE unit_id = 2 AND lesson_number = 1
ON CONFLICT DO NOTHING;

DO $$
DECLARE
    test_id_var INT;
    question_id_var INT;
BEGIN
    SELECT t.id INTO test_id_var
    FROM tests t
    JOIN lessons l ON t.lesson_id = l.id
    WHERE l.unit_id = 2 AND l.lesson_number = 1
    LIMIT 1;

    IF test_id_var IS NOT NULL THEN
        -- Question 1
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Kariyes asosiy sababi nima?', 1, 1)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Vitamin yetishmovchiligi', FALSE, 1),
            (question_id_var, 'Bakteriyalar va karbohidratlar ta''siri', TRUE, 2),
            (question_id_var, 'Genetik omillar', FALSE, 3),
            (question_id_var, 'Suv yetishmovchiligi', FALSE, 4);

        -- Question 2
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Streptococcus mutans nima?', 1, 2)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Virus', FALSE, 1),
            (question_id_var, 'Kariyes keltirib chiqaruvchi bakteriya', TRUE, 2),
            (question_id_var, 'Zamburug''', FALSE, 3),
            (question_id_var, 'Parazit', FALSE, 4);

        -- Question 3
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Tish plagi nima?', 1, 3)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Tish kasalligi', FALSE, 1),
            (question_id_var, 'Bakteriyalar to''plami', TRUE, 2),
            (question_id_var, 'Tish toshi', FALSE, 3),
            (question_id_var, 'Ovqat qoldiqlari', FALSE, 4);

        -- Question 4
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Kariyes rivojlanishida pH qanday o''zgaradi?', 1, 4)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'pH oshadi', FALSE, 1),
            (question_id_var, 'pH pasayadi (kislotali muhit)', TRUE, 2),
            (question_id_var, 'pH o''zgarmaydi', FALSE, 3),
            (question_id_var, 'pH neytral bo''ladi', FALSE, 4);

        -- Question 5
        INSERT INTO questions (test_id, question_text, points, order_num)
        VALUES (test_id_var, 'Kariyes profilaktikasida eng muhim omil?', 1, 5)
        RETURNING id INTO question_id_var;

        INSERT INTO answer_options (question_id, option_text, is_correct, order_num) VALUES
            (question_id_var, 'Antibiotiklar qabul qilish', FALSE, 1),
            (question_id_var, 'Og''iz bo''shlig''i gigienasi', TRUE, 2),
            (question_id_var, 'Vitaminlar qabul qilish', FALSE, 3),
            (question_id_var, 'Ko''p suv ichish', FALSE, 4);

        UPDATE tests SET total_points = 5 WHERE id = test_id_var;
    END IF;
END $$;

-- =====================
-- THEORY MATERIALS
-- =====================

-- F1 Lesson 1 Theory
INSERT INTO theory_materials (lesson_id, title_uz, material_type, description)
SELECT id, 'Tish anatomiyasi asoslari', 'book',
'Tish anatomiyasi - stomatologiyaning asosiy bo''limi bo''lib, tishlarning tuzilishi, shakli va funksiyalarini o''rganadi.

TISH TUZILISHI:
1. Toj (Corona) - tishning og''iz bo''shlig''ida ko''rinadigan qismi
2. Bo''yin (Cervix) - toj va ildiz orasidagi qism
3. Ildiz (Radix) - jag'' suyagiga kirib turgan qism

TISH TO''QIMALARI:
- Emal - eng qattiq to''qima, toj qismini qoplaydi
- Dentin - tishning asosiy massasini tashkil etadi
- Sement - ildiz qismini qoplaydi
- Pulpa - qon tomirlari va nervlardan iborat yumshoq to''qima'
FROM lessons WHERE unit_id = 1 AND lesson_number = 1
ON CONFLICT DO NOTHING;

INSERT INTO theory_materials (lesson_id, title_uz, material_type, description)
SELECT id, 'Tish turlari haqida', 'manual',
'TISH TURLARI:

1. KESUVCHI TISHLAR (Incisivi)
- Soni: 8 ta (har bir jag''da 4 ta)
- Funksiyasi: ovqatni kesish
- Xususiyati: yassi, keskir qirrali

2. SO''YLOQ TISHLAR (Canini)
- Soni: 4 ta (har bir jag''da 2 ta)
- Funksiyasi: ovqatni yirtish
- Xususiyati: uchli, kuchli ildizli

3. KICHIK OZIQ TISHLAR (Premolares)
- Soni: 8 ta (har bir jag''da 4 ta)
- Funksiyasi: ovqatni maydalash
- Xususiyati: 2 ta do''nglikli

4. KATTA OZIQ TISHLAR (Molares)
- Soni: 12 ta (aql tishlari bilan)
- Funksiyasi: ovqatni chaynash
- Xususiyati: 4-5 ta do''nglikli'
FROM lessons WHERE unit_id = 1 AND lesson_number = 1
ON CONFLICT DO NOTHING;

-- F1 Lesson 2 Theory
INSERT INTO theory_materials (lesson_id, title_uz, material_type, description)
SELECT id, 'Og''iz bo''shlig''i tuzilishi', 'book',
'OG''IZ BO''SHLIG''I ANATOMIYASI

Og''iz bo''shlig''i - hazm sistemasining boshlang''ich qismi.

CHEGARALARI:
- Yuqoridan: qattiq va yumshoq tanglay
- Pastdan: og''iz tubidagi muskullar
- Yon tomonlardan: yonoqlar
- Oldidan: lablar

ASOSIY TUZILMALAR:
1. Til - ta''m bilish va nutq organei
2. Tishlar - chaynash organi
3. So''lak bezlari - so''lak ishlab chiqaradi
4. Tanglay - og''iz va burun bo''shlig''ini ajratadi'
FROM lessons WHERE unit_id = 1 AND lesson_number = 2
ON CONFLICT DO NOTHING;

-- F2 Lesson 1 Theory
INSERT INTO theory_materials (lesson_id, title_uz, material_type, description)
SELECT id, 'Kariyes haqida umumiy ma''lumot', 'book',
'KARIYES - tish qattiq to''qimalarining demineralizatsiyasi va parchalanishi.

KARIYES RIVOJLANISH BOSQICHLARI:
1. Boshlang''ich kariyes (dog'') - emalda oq dog'' paydo bo''ladi
2. Yuzaki kariyes - emal buziladi
3. O''rta kariyes - dentin zararlandi
4. Chuqur kariyes - pulpaga yaqin

SABABLARI:
- Mikroorganizmlar (Streptococcus mutans)
- Karbohidratlar (shakar)
- Vaqt omili
- Tish sirt xususiyatlari

PROFILAKTIKA:
- Kuniga 2 marta tish tozalash
- Ftorli pasta ishlatish
- Shakarli ovqatlarni kamaytirish
- Muntazam stomatolog ko''rigi'
FROM lessons WHERE unit_id = 2 AND lesson_number = 1
ON CONFLICT DO NOTHING;

-- F3 Lesson 1 Theory
INSERT INTO theory_materials (lesson_id, title_uz, material_type, description)
SELECT id, 'Tish olish ko''rsatmalari', 'book',
'TISH OLISH KO''RSATMALARI

MUTLAQ KO''RSATMALAR:
1. Davolab bo''lmaydigan tishlar
2. Jag'' sinishida interferensiya qiluvchi tishlar
3. O''sma atrofidagi tishlar
4. Ortodontik ko''rsatmalar

NISBIY KO''RSATMALAR:
1. Surunkali periodontit
2. Haddan tashqari harakatchan tishlar
3. Estetik ko''rsatmalar

QARSHI KO''RSATMALAR:
1. O''tkir yurak kasalliklari
2. Qon ivish buzilishlari
3. O''tkir infeksion kasalliklar
4. Homiladorlikning birinchi va oxirgi uch oyligi'
FROM lessons WHERE unit_id = 3 AND lesson_number = 1
ON CONFLICT DO NOTHING;

-- =====================
-- SITUATIONAL TASKS
-- =====================

-- F1 Lesson 1 Situational Task
INSERT INTO situational_tasks (lesson_id, task_text, time_limit_minutes)
SELECT id,
'VAZIYATLI MASALA:

Bemor 25 yoshda, yuqori jag''ning o''ng tomonidagi 6-tishda og''riqdan shikoyat qilmoqda.
Ko''rikda: tish tojida katta karioz bo''shliq aniqlanadi, zondlashda og''riq bor,
perkussiya salbiy.

SAVOLLAR:
1. Tishning qaysi qatlamlari zararlangan deb o''ylaysiz?
2. Qanday diagnostik usullarni qo''llagan bo''lar edingiz?
3. Davolash rejasini tuzing.
4. Prognoz qanday?',
30
FROM lessons WHERE unit_id = 1 AND lesson_number = 1
ON CONFLICT DO NOTHING;

-- F1 Lesson 2 Situational Task
INSERT INTO situational_tasks (lesson_id, task_text, time_limit_minutes)
SELECT id,
'VAZIYATLI MASALA:

Bemor 18 yoshda, pastki jag''ning chap tomonida milkdan qon ketishidan shikoyat qilmoqda.
Ko''rikda: milklar shishgan, qizargan, tish tozalashda qon ketadi.
Og''iz gigienasi qoniqarsiz.

SAVOLLAR:
1. Dastlabki tashxisingiz nima?
2. Qaysi anatomik strukturalar zararlangan?
3. Kasallik sabablarini ayting.
4. Davolash va profilaktika choralarini tavsiya qiling.',
25
FROM lessons WHERE unit_id = 1 AND lesson_number = 2
ON CONFLICT DO NOTHING;

-- F2 Lesson 1 Situational Task
INSERT INTO situational_tasks (lesson_id, task_text, time_limit_minutes)
SELECT id,
'VAZIYATLI MASALA:

Bemor 30 yoshda, bir necha tishlarida sovuq va issiqqa sezuvchanlik borligidan shikoyat qilmoqda.
Anamnez: kuniga bir marta tish tozalaydi, shirinlikni yaxshi ko''radi.
Ko''rikda: bir necha tishlarda emal rangi o''zgargan oq dog''lar aniqlanadi.

SAVOLLAR:
1. Qanday tashxis qo''yasiz?
2. Kasallik qaysi bosqichda?
3. Etiologik omillarni sanab bering.
4. Davolash va profilaktika rejasini tuzing.',
30
FROM lessons WHERE unit_id = 2 AND lesson_number = 1
ON CONFLICT DO NOTHING;

-- F3 Lesson 1 Situational Task
INSERT INTO situational_tasks (lesson_id, task_text, time_limit_minutes)
SELECT id,
'VAZIYATLI MASALA:

Bemor 45 yoshda, pastki jag''ning o''ng tomonidagi 8-tishni (aql tishi) olib tashlash uchun murojaat qildi.
Anamnez: tish bir necha marta yallig''langan, antibiotiklar bilan davolangan.
Ko''rikda: tish qisman chiqqan, atrofida yallig''lanish belgilari bor.
Rentgenda: tish gorizontal holatda joylashgan.

SAVOLLAR:
1. Tishni olish ko''rsatmasi bormi? Asoslang.
2. Qanday anesteziya qo''llaysiz?
3. Operatsiya texnikasini tasvirlang.
4. Mumkin bo''lgan asoratlar va ularning oldini olish choralarini ayting.',
35
FROM lessons WHERE unit_id = 3 AND lesson_number = 1
ON CONFLICT DO NOTHING;

-- Update all test total points based on questions
UPDATE tests t SET total_points = (
    SELECT COALESCE(SUM(q.points), 0)
    FROM questions q
    WHERE q.test_id = t.id
);
