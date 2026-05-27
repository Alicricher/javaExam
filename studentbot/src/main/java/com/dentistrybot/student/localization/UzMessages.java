package com.dentistrybot.student.localization;

public final class UzMessages {
    private UzMessages() {}

    // General
    public static final String MSG_WELCOME       = "Xush kelibsiz! Stomatologiya fakulteti ta'lim botiga xush kelibsiz.";
    public static final String MSG_WELCOME_BACK  = "Qaytganingizdan xursandmiz, %s!";
    public static final String MSG_MAIN_MENU     = "Asosiy menyu";
    public static final String MSG_SELECT_OPTION = "Variantni tanlang:";
    public static final String MSG_CANCEL        = "Bekor qilindi";
    public static final String MSG_ERROR         = "Xatolik yuz berdi. Iltimos, qaytadan urinib ko'ring.";
    public static final String MSG_NOT_FOUND     = "Topilmadi";
    public static final String MSG_SUCCESS       = "Muvaffaqiyatli!";
    public static final String MSG_BACK          = "Orqaga";
    public static final String MSG_CONFIRM       = "Tasdiqlash";
    public static final String MSG_YES           = "Ha";
    public static final String MSG_NO            = "Yo'q";

    // Registration
    public static final String MSG_REGISTER_START    = "Ro'yxatdan o'tish uchun quyidagi ma'lumotlarni kiriting.";
    public static final String MSG_ENTER_FULL_NAME   = "To'liq ismingizni kiriting (F.I.O.):";
    public static final String MSG_ENTER_COURSE      = "Kursingizni tanlang:";
    public static final String MSG_ENTER_GROUP       = "Guruhingizni kiriting:";
    public static final String MSG_ENTER_SUBGROUP    = "⚠️ DIQQAT: Faqat INGLIZ klaviaturasida kiriting!\n\nKichik guruhingizni kiriting:";
    public static final String MSG_ENTER_FACULTY     = "Fakultetingizni kiriting:";
    public static final String MSG_REGISTER_COMPLETE = "Ro'yxatdan o'tish muvaffaqiyatli yakunlandi!\n\nIsm: %s\nKurs: %d\nGuruh: %s\nKichik guruh: %s\nFakultet: %s";
    public static final String MSG_INVALID_COURSE    = "Noto'g'ri kurs. Iltimos, 1 dan 6 gacha son kiriting.";

    // Profile
    public static final String MSG_PROFILE         = "Sizning profilingiz:\n\nIsm: %s\nKurs: %d\nGuruh: %s\nKichik guruh: %s\nFakultet: %s";
    public static final String MSG_EDIT_PROFILE    = "Profilni tahrirlash";
    public static final String MSG_EDIT_COURSE     = "Kursni o'zgartirish";
    public static final String MSG_EDIT_GROUP      = "Guruhni o'zgartirish";
    public static final String MSG_EDIT_SUBGROUP   = "Kichik guruhni o'zgartirish";
    public static final String MSG_EDIT_FACULTY    = "Fakultetni o'zgartirish";
    public static final String MSG_PROFILE_UPDATED = "Profil muvaffaqiyatli yangilandi!";
    public static final String MSG_CANNOT_EDIT_NAME = "Ismni faqat administrator o'zgartirishi mumkin.";

    // Units / Lessons
    public static final String MSG_SELECT_UNIT   = "Bo'limni tanlang:";
    public static final String MSG_SELECT_LESSON = "Darsni tanlang:";
    public static final String MSG_LESSON_MENU   = "%s - %s\n\nQuyidagilardan birini tanlang:";

    // Tests
    public static final String MSG_TEST_START          = "Test: %s\n\nSavollar soni: %d\nVaqt: %d daqiqa\nJami ball: %d\n\nTestni boshlashni tasdiqlaysizmi?";
    public static final String MSG_START_TEST          = "Testni boshlash";
    public static final String MSG_TEST_QUESTION       = "Savol %d/%d\n\n%s";
    public static final String MSG_TEST_TIME_WARNING   = "Diqqat! Test tugashiga 30 soniya qoldi!";
    public static final String MSG_TEST_TIME_UP        = "Vaqt tugadi! Javoblaringiz saqlandi.";
    public static final String MSG_TEST_COMPLETED      = "Test yakunlandi!\n\nNatija: %d/%d ball\nTo'g'ri javoblar: %d/%d";
    public static final String MSG_ALREADY_TOOK_TEST   = "Siz bu testni allaqachon topshirgansiz.";
    public static final String MSG_NO_RETAKE_AVAILABLE = "Qayta topshirish imkoniyati yo'q. Administrator bilan bog'laning.";
    public static final String MSG_RETAKE_AVAILABLE    = "Sizda qayta topshirish imkoniyati mavjud.";
    public static final String MSG_NO_TEST_AVAILABLE   = "Bu dars uchun test mavjud emas.";

    // Theory
    public static final String MSG_THEORY         = "Nazariya materiallari:";
    public static final String MSG_NO_THEORY      = "Bu dars uchun nazariya materiallari mavjud emas.";
    public static final String MSG_MATERIAL_TYPES = "Material turini tanlang:";
    public static final String MSG_BOOKS          = "Kitoblar";
    public static final String MSG_MANUALS        = "Qo'llanmalar";
    public static final String MSG_MATERIALS      = "O'quv materiallari";
    public static final String MSG_DOWNLOAD_FILE  = "Faylni yuklab olish";

    // Situational tasks
    public static final String MSG_SITUATIONAL_START        = "Vaziyatli masala\n\nVaqt: %d daqiqa\n\nBoshlashni tasdiqlaysizmi?";
    public static final String MSG_START_SITUATIONAL        = "Boshlash";
    public static final String MSG_SITUATIONAL_TASK         = "Vaziyatli masala:\n\n%s\n\nJavobingizni yozing:";
    public static final String MSG_SITUATIONAL_TIME_WARNING = "Diqqat! Vaziyatli masala tugashiga 30 soniya qoldi!";
    public static final String MSG_SITUATIONAL_TIME_UP      = "Vaqt tugadi! Javobingiz saqlandi.";
    public static final String MSG_SITUATIONAL_SUBMITTED    = "Javobingiz muvaffaqiyatli yuborildi. Natijalar admin tomonidan tekshirilgandan so'ng sizga xabar beriladi.";
    public static final String MSG_ALREADY_SUBMITTED        = "Siz bu vaziyatli masalaga allaqachon javob bergansiz.";
    public static final String MSG_NO_TASK_AVAILABLE        = "Bu dars uchun vaziyatli masala mavjud emas.";
    public static final String MSG_TASK_GRADED              = "Vaziyatli masalangiz baholandi!\n\nMasala: %s\nBaho: %d\nIzoh: %s";

    // Buttons
    public static final String BTN_MAIN_MENU      = "Asosiy menyu";
    public static final String BTN_PROFILE        = "Profil";
    public static final String BTN_START_LEARNING = "O'qishni boshlash";
    public static final String BTN_TEST           = "Test";
    public static final String BTN_THEORY         = "Nazariya";
    public static final String BTN_SITUATIONAL    = "Vaziyatli masala";
    public static final String BTN_BACK           = "Orqaga";
    public static final String BTN_CANCEL         = "Bekor qilish";
    public static final String BTN_CONFIRM        = "Tasdiqlash";
    public static final String BTN_SUBMIT         = "Yuborish";

    // Course buttons
    public static final String BTN_COURSE_1 = "1-kurs";
    public static final String BTN_COURSE_2 = "2-kurs";
    public static final String BTN_COURSE_3 = "3-kurs";
    public static final String BTN_COURSE_4 = "4-kurs";
    public static final String BTN_COURSE_5 = "5-kurs";
    public static final String BTN_COURSE_6 = "6-kurs";

    // Pagination
    public static final String MSG_PAGE     = "Sahifa %d/%d";
    public static final String BTN_NEXT     = "Keyingi >";
    public static final String BTN_PREVIOUS = "< Oldingi";
}
