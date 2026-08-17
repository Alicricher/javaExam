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
}
