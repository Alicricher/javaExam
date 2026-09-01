package com.dentistrybot.student.localization;

public final class Lang {
    private Lang() {}

    public static String msg(String lang, String uz, String ru) {
        return "ru".equals(lang) ? ru : uz;
    }

    // Main menu buttons
    public static String btnStartLearning(String lang) { return msg(lang, "📚 O'qishni boshlash", "📚 Начать обучение"); }
    public static String btnProfile(String lang)       { return msg(lang, "👤 Profil", "👤 Профиль"); }

    // Profile edit buttons
    public static String btnEditCourse(String lang)    { return msg(lang, "Kursni o'zgartirish", "Изменить курс"); }
    public static String btnEditGroup(String lang)     { return msg(lang, "Guruhni o'zgartirish", "Изменить группу"); }
    public static String btnEditSubgroup(String lang)  { return msg(lang, "Kichik guruhni o'zgartirish", "Изменить подгруппу"); }
    public static String btnEditFaculty(String lang)   { return msg(lang, "Fakultetni o'zgartirish", "Изменить факультет"); }
    public static String btnEditLang(String lang)      { return msg(lang, "🌐 Tilni o'zgartirish", "🌐 Изменить язык"); }
    public static String btnBack(String lang)          { return msg(lang, "⬅️ Orqaga", "⬅️ Назад"); }
    public static String btnCancel(String lang)        { return msg(lang, "❌ Bekor qilish", "❌ Отмена"); }

    // Registration messages
    public static String msgSelectLanguage()           { return "Tilni tanlang / Выберите язык:"; }
    public static String msgWelcomeBack(String lang, String name) {
        return msg(lang, "Qaytganingizdan xursandmiz, " + name + "!", "Рады видеть вас снова, " + name + "!");
    }
    public static String msgRegisterStart(String lang) {
        return msg(lang,
            "Ro'yxatdan o'tish uchun quyidagi ma'lumotlarni kiriting.",
            "Для регистрации введите следующие данные.");
    }
    public static String msgEnterFullName(String lang) {
        return msg(lang, "To'liq ismingizni kiriting (F.I.O.):", "Введите ваше полное имя (Ф.И.О.):");
    }
    public static String msgEnterCourse(String lang) {
        return msg(lang, "Kursingizni tanlang:", "Выберите ваш курс:");
    }
    public static String msgEnterGroup(String lang) {
        return msg(lang, "Guruhingizni kiriting:", "Введите вашу группу:");
    }
    public static String msgEnterSubgroup(String lang) {
        return msg(lang,
            "⚠️ DIQQAT: Faqat INGLIZ klaviaturasida kiriting!\n\nKichik guruhingizni kiriting:",
            "⚠️ ВНИМАНИЕ: Вводите только на АНГЛИЙСКОЙ клавиатуре!\n\nВведите вашу подгруппу:");
    }
    public static String msgEnterFaculty(String lang) {
        return msg(lang, "Fakultetingizni kiriting:", "Введите ваш факультет:");
    }
    public static String msgRegisterComplete(String lang, String name, int course, String group, String subgroup, String faculty) {
        return msg(lang,
            String.format("Ro'yxatdan o'tish muvaffaqiyatli yakunlandi!\n\nIsm: %s\nKurs: %d\nGuruh: %s\nKichik guruh: %s\nFakultet: %s",
                name, course, group, subgroup, faculty),
            String.format("Регистрация успешно завершена!\n\nИмя: %s\nКурс: %d\nГруппа: %s\nПодгруппа: %s\nФакультет: %s",
                name, course, group, subgroup, faculty));
    }

    // Profile display
    public static String msgProfile(String lang, String name, int course, String group, String subgroup, String faculty) {
        return msg(lang,
            String.format("Sizning profilingiz:\n\nIsm: %s\nKurs: %d\nGuruh: %s\nKichik guruh: %s\nFakultet: %s",
                name, course, group, subgroup, faculty),
            String.format("Ваш профиль:\n\nИмя: %s\nКурс: %d\nГруппа: %s\nПодгруппа: %s\nФакультет: %s",
                name, course, group, subgroup, faculty));
    }
    public static String msgProfileUpdated(String lang) {
        return msg(lang, "Profil muvaffaqiyatli yangilandi!", "Профиль успешно обновлён!");
    }
    public static String msgLangChanged(String lang) {
        return msg(lang, "Til o'zgartirildi!", "Язык изменён!");
    }

    // Main menu
    public static String msgMainMenu(String lang) {
        return msg(lang, "Asosiy menyu", "Главное меню");
    }

    // Misc
    public static String msgError(String lang) {
        return msg(lang, "Xatolik yuz berdi. Iltimos, qaytadan urinib ko'ring.", "Произошла ошибка. Пожалуйста, попробуйте ещё раз.");
    }

    public static String msgWelcome(String lang) {
        return msg(lang,
            "Xush kelibsiz! Stomatologiya fakulteti ta'lim botiga xush kelibsiz.",
            "Добро пожаловать в образовательного бота стоматологического факультета!");
    }
    public static String msgNotFound(String lang) { return msg(lang, "Topilmadi", "Не найдено"); }
    public static String msgTestInProgress(String lang) {
        return msg(lang, "Test jarayonida. Iltimos, testni yakunlang.", "Тест уже идёт. Пожалуйста, завершите тест.");
    }
    public static String msgOnlyTextAnswer(String lang) {
        return msg(lang, "Faqat matn javobini yuboring. Rasm qabul qilinmaydi.", "Отправьте ответ только текстом. Изображения не принимаются.");
    }
    public static String msgPleaseRegisterFirst(String lang) {
        return msg(lang, "Iltimos, avval ro'yxatdan o'ting.", "Пожалуйста, сначала зарегистрируйтесь.");
    }

    // Buttons: navigation / content
    public static String btnTest(String lang)        { return msg(lang, "Test", "Тест"); }
    public static String btnTheory(String lang)       { return msg(lang, "Nazariya", "Теория"); }
    public static String btnSituational(String lang)  { return msg(lang, "Vaziyatli masala", "Ситуационная задача"); }
    public static String btnNext(String lang)         { return msg(lang, "Keyingi >", "Следующая >"); }
    public static String btnPrevious(String lang)     { return msg(lang, "< Oldingi", "< Предыдущая"); }
    public static String btnCourse(String lang, int n) { return msg(lang, n + "-kurs", n + " курс"); }

    // Units / Lessons
    public static String msgSelectUnit(String lang)   { return msg(lang, "Bo'limni tanlang:", "Выберите раздел:"); }
    public static String msgSelectLesson(String lang) { return msg(lang, "Darsni tanlang:", "Выберите урок:"); }
    public static String msgLessonMenu(String lang, String unitTitle, String lessonTitle) {
        return msg(lang,
            unitTitle + " - " + lessonTitle + "\n\nQuyidagilardan birini tanlang:",
            unitTitle + " - " + lessonTitle + "\n\nВыберите один из вариантов:");
    }

    // Tests
    public static String msgTestStart(String lang, String title, int qCount, int timeLimit, int totalPoints) {
        return msg(lang,
            String.format("Test: %s\n\nSavollar soni: %d\nVaqt: %d daqiqa\nJami ball: %d\n\nTestni boshlashni tasdiqlaysizmi?",
                title, qCount, timeLimit, totalPoints),
            String.format("Тест: %s\n\nКоличество вопросов: %d\nВремя: %d мин\nВсего баллов: %d\n\nНачать тест?",
                title, qCount, timeLimit, totalPoints));
    }
    public static String msgStartTest(String lang)  { return msg(lang, "Testni boshlash", "Начать тест"); }
    public static String msgTestQuestion(String lang, int current, int total, String questionText) {
        return msg(lang,
            String.format("Savol %d/%d\n\n%s", current, total, questionText),
            String.format("Вопрос %d/%d\n\n%s", current, total, questionText));
    }
    public static String msgTestTimeWarning(String lang) {
        return msg(lang, "Diqqat! Test tugashiga 30 soniya qoldi!", "Внимание! До конца теста осталось 30 секунд!");
    }
    public static String msgTestTimeUp(String lang) {
        return msg(lang, "Vaqt tugadi! Javoblaringiz saqlandi.", "Время вышло! Ваши ответы сохранены.");
    }
    public static String msgTestCompleted(String lang, int score, int maxScore, int correct, int totalQuestions) {
        return msg(lang,
            String.format("Test yakunlandi!\n\nNatija: %d/%d ball\nTo'g'ri javoblar: %d/%d", score, maxScore, correct, totalQuestions),
            String.format("Тест завершён!\n\nРезультат: %d/%d баллов\nПравильные ответы: %d/%d", score, maxScore, correct, totalQuestions));
    }
    public static String msgAlreadyTookTest(String lang) {
        return msg(lang, "Siz bu testni allaqachon topshirgansiz.", "Вы уже сдавали этот тест.");
    }
    public static String msgNoRetakeAvailable(String lang) {
        return msg(lang, "Qayta topshirish imkoniyati yo'q. Administrator bilan bog'laning.",
            "Возможность пересдачи недоступна. Свяжитесь с администратором.");
    }
    public static String msgRetakeAvailable(String lang) {
        return msg(lang, "Sizda qayta topshirish imkoniyati mavjud.", "У вас есть возможность пересдачи.");
    }
    public static String msgNoTestAvailable(String lang) {
        return msg(lang, "Bu dars uchun test mavjud emas.", "Для этого урока тест недоступен.");
    }
    public static String msgUrinish(String lang, int attemptNumber) {
        return msg(lang, attemptNumber + "-urinish", attemptNumber + "-я попытка");
    }
    public static String msgUrinishNatijasi(String lang, int attemptNumber) {
        return msg(lang, attemptNumber + "-urinish natijasi", attemptNumber + "-я попытка, результат");
    }
    public static String msgTimeUpShort(String lang)  { return msg(lang, "⏰ Vaqt tugadi!", "⏰ Время вышло!"); }
    public static String msgTimeUpAutoCompleted(String lang) {
        return msg(lang, "⏰ Vaqt tugadi! Test avtomatik yakunlandi.", "⏰ Время вышло! Тест завершён автоматически.");
    }
    public static String msgRemainingTimeLabel(String lang) { return msg(lang, "Qolgan vaqt: ", "Осталось времени: "); }

    // Theory
    public static String msgBooks(String lang)     { return msg(lang, "Kitoblar", "Книги"); }
    public static String msgManuals(String lang)   { return msg(lang, "Qo'llanmalar", "Пособия"); }
    public static String msgMaterials(String lang) { return msg(lang, "O'quv materiallari", "Учебные материалы"); }
    public static String msgNoTheory(String lang) {
        return msg(lang, "Bu dars uchun nazariya materiallari mavjud emas.", "Для этого урока теоретические материалы недоступны.");
    }
    public static String msgTheoryMaterialsHeader(String lang) { return msg(lang, "📚 Nazariy materiallar:", "📚 Теоретические материалы:"); }
    public static String msgFileUploadError(String lang, String title) {
        return msg(lang, "❌ " + title + " - faylni yuklashda xatolik", "❌ " + title + " - ошибка при загрузке файла");
    }
    public static String msgBackToLessonHint(String lang) {
        return msg(lang, "⬆️ Darsga qaytish uchun tugmani bosing.", "⬆️ Нажмите кнопку, чтобы вернуться к уроку.");
    }
    public static String msgTypeNotAvailable(String lang, String typeName) {
        return msg(lang, typeName + " mavjud emas.", typeName + " недоступны.");
    }
    public static String msgMaterialNotFound(String lang) { return msg(lang, "Material topilmadi.", "Материал не найден."); }
    public static String msgMaterialNotAvailable(String lang) { return msg(lang, "Material mavjud emas.", "Материал недоступен."); }

    // Situational tasks
    public static String msgSituationalStart(String lang, int timeLimit) {
        return msg(lang,
            String.format("Vaziyatli masala\n\nVaqt: %d daqiqa\n\nBoshlashni tasdiqlaysizmi?", timeLimit),
            String.format("Ситуационная задача\n\nВремя: %d мин\n\nНачать?", timeLimit));
    }
    public static String msgStartSituational(String lang) { return msg(lang, "Boshlash", "Начать"); }
    public static String msgSituationalTask(String lang, String taskText) {
        return msg(lang,
            "Vaziyatli masala:\n\n" + taskText + "\n\nJavobingizni yozing:",
            "Ситуационная задача:\n\n" + taskText + "\n\nНапишите ваш ответ:");
    }
    public static String msgSituationalTimeWarning(String lang) {
        return msg(lang, "Diqqat! Vaziyatli masala tugashiga 30 soniya qoldi!", "Внимание! До конца задачи осталось 30 секунд!");
    }
    public static String msgSituationalTimeUp(String lang) {
        return msg(lang, "Vaqt tugadi! Javobingiz saqlandi.", "Время вышло! Ваш ответ сохранён.");
    }
    public static String msgAlreadySubmitted(String lang) {
        return msg(lang, "Siz bu vaziyatli masalaga allaqachon javob bergansiz.", "Вы уже отвечали на эту ситуационную задачу.");
    }
    public static String msgAlreadySubmittedShort(String lang, int orderNum) {
        return msg(lang,
            "✅ " + orderNum + "-masala\n\nSiz bu masalaga allaqachon javob bergansiz.\nQayta topshirish uchun administrator bilan bog'laning.",
            "✅ Задача " + orderNum + "\n\nВы уже отвечали на эту задачу.\nДля пересдачи свяжитесь с администратором.");
    }
    public static String msgNoTaskAvailable(String lang) {
        return msg(lang, "Bu dars uchun vaziyatli masala mavjud emas.", "Для этого урока ситуационная задача недоступна.");
    }
    public static String msgSituationalSubmitted(String lang) {
        return msg(lang,
            "Javobingiz muvaffaqiyatli yuborildi. Natijalar admin tomonidan tekshirilgandan so'ng sizga xabar beriladi.",
            "Ваш ответ успешно отправлен. Результат придёт после проверки администратором.");
    }
    public static String msgConfirmSituationalAnswer(String lang) { return msg(lang, "✅ Yuborish", "✅ Отправить"); }
    public static String msgEditSituationalAnswer(String lang)    { return msg(lang, "✏️ Tahrirlash", "✏️ Редактировать"); }
    public static String msgSituationalTaskListHeader(String lang, int total, int answered) {
        return msg(lang,
            String.format("📋 Vaziyatli masalalar (%d ta)\nBajarilgan: %d/%d\n\nMasalani tanlang:", total, answered, total),
            String.format("📋 Ситуационные задачи (%d шт.)\nВыполнено: %d/%d\n\nВыберите задачу:", total, answered, total));
    }
    public static String msgPleaseSendTextAnswer(String lang) {
        return msg(lang, "Iltimos, javobingizni matn yoki rasm sifatida yuboring.", "Пожалуйста, отправьте ваш ответ текстом.");
    }
    public static String msgAnswerNotSavedError(String lang) {
        return msg(lang, "Xatolik yuz berdi. Javobingiz saqlanmadi.", "Произошла ошибка. Ваш ответ не сохранён.");
    }
    public static String msgAnswerAccepted(String lang, int taskNumber) {
        return msg(lang, "✅ " + taskNumber + "-masala javob qabul qilindi!", "✅ Задача " + taskNumber + " — ответ принят!");
    }
    public static String msgGoToNextTask(String lang, int nextTaskNumber) {
        return msg(lang, "➡️ " + nextTaskNumber + "-masalaga o'tish", "➡️ Перейти к задаче " + nextTaskNumber);
    }
    public static String msgAllTasksCompleted(String lang) {
        return msg(lang, "🎉 Barcha vaziyatli masalalar bajarildi!", "🎉 Все ситуационные задачи выполнены!");
    }
    public static String msgAnswerSaved(String lang) { return msg(lang, "Javobingiz saqlandi.", "Ваш ответ сохранён."); }
    public static String msgSituationalTimeoutNotify(String lang) {
        return msg(lang, "⏰ Vaziyatli masala vaqti tugadi!", "⏰ Время на ситуационную задачу истекло!");
    }
    public static String msgSituationalTimeoutNotifySaved(String lang) {
        return msg(lang, "⏰ Vaziyatli masala vaqti tugadi! Javobingiz saqlandi.", "⏰ Время на ситуационную задачу истекло! Ваш ответ сохранён.");
    }
    public static String msgConfirmYourAnswer(String lang) { return msg(lang, "Javobingizni tasdiqlaysizmi?\n\n", "Подтвердить ваш ответ?\n\n"); }
    public static String msgPhotoUploadedLabel(String lang) { return msg(lang, "📷 Rasm: Yuklangan\n\n", "📷 Изображение: загружено\n\n"); }
    public static String msgAnswerLabel(String lang) { return msg(lang, "Javob: ", "Ответ: "); }
    public static String msgCannotEditAfterConfirm(String lang) {
        return msg(lang, "\n\n⚠️ Tasdiqlangandan keyin javobni o'zgartirish mumkin emas!",
            "\n\n⚠️ После подтверждения изменить ответ будет нельзя!");
    }

    // Pagination
    public static String msgPage(String lang, int current, int total) {
        return msg(lang, String.format("Sahifa %d/%d", current, total), String.format("Страница %d/%d", current, total));
    }
}
