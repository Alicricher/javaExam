package com.dentistrybot.admin.keyboard;

import com.dentistrybot.admin.localization.AdminMessages;
import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.model.Question;
import com.dentistrybot.shared.model.SituationalTask;
import com.dentistrybot.shared.model.TheoryMaterial;
import com.dentistrybot.shared.model.Unit;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

public final class AdminKeyboards {

    private AdminKeyboards() {}

    // Callback prefixes shared with student dispatcher
    public static final String CB_ADMIN_MENU = "admin_menu:";
    public static final String CB_BACK       = "back:";
    public static final String CB_FILTER     = "filter:";
    public static final String CB_STUDENT    = "student:";
    public static final String CB_RETAKE     = "retake:";
    public static final String CB_MANAGE     = "manage:";
    public static final String CB_PAGE       = "page:";
    public static final String CB_CANCEL     = "cancel";

    public static InlineKeyboardMarkup adminMenu() {
        return build(
            row(btn(AdminMessages.MSG_STUDENT_LIST, CB_ADMIN_MENU + "students")),
            row(btn(AdminMessages.MSG_TEST_RESULTS, CB_ADMIN_MENU + "test_results")),
            row(btn(AdminMessages.MSG_SITUATIONAL_ANSWERS, CB_ADMIN_MENU + "situational")),
            row(btn(AdminMessages.MSG_CONTENT_MANAGEMENT, CB_ADMIN_MENU + "content")),
            row(btn(AdminMessages.MSG_LOGOUT, CB_ADMIN_MENU + "logout"))
        );
    }

    public static InlineKeyboardMarkup studentFilter() {
        return build(
            row(btn(AdminMessages.MSG_BY_NAME, CB_FILTER + "name"),
                btn(AdminMessages.MSG_BY_COURSE, CB_FILTER + "course")),
            row(btn("🔍 Guruh+Kichik guruh", CB_FILTER + "combined"),
                btn(AdminMessages.MSG_BY_FACULTY, CB_FILTER + "faculty")),
            row(btn(AdminMessages.MSG_BACK, CB_BACK + "admin_menu"))
        );
    }

    public static InlineKeyboardMarkup studentActions(int studentId) {
        return build(
            row(btn("📊 Test natijalari", "student_results:" + studentId)),
            row(btn(AdminMessages.MSG_EDIT_STUDENT_NAME, CB_STUDENT + "edit_name:" + studentId)),
            row(btn(AdminMessages.MSG_GRANT_RETAKE, CB_STUDENT + "retake:" + studentId)),
            row(btn("🔄 Vaziyatli masalaga qayta topshirish", CB_STUDENT + "sit_retake:" + studentId)),
            row(btn(AdminMessages.MSG_BACK, CB_BACK + "students"))
        );
    }

    public static InlineKeyboardMarkup contentManagement() {
        return build(
            row(btn("📚 Fanlar", CB_MANAGE + "units")),
            row(btn(AdminMessages.MSG_MANAGE_LESSONS, CB_MANAGE + "lessons")),
            row(btn(AdminMessages.MSG_MANAGE_TESTS, CB_MANAGE + "tests")),
            row(btn(AdminMessages.MSG_MANAGE_THEORY, CB_MANAGE + "theory")),
            row(btn(AdminMessages.MSG_MANAGE_TASKS, CB_MANAGE + "tasks")),
            row(btn(AdminMessages.MSG_BACK, CB_BACK + "admin_menu"))
        );
    }

    public static InlineKeyboardMarkup cancelOnly() {
        return build(row(btn(AdminMessages.MSG_CANCEL, CB_CANCEL)));
    }

    public static InlineKeyboardMarkup yesNo(String yesCallback, String noCallback) {
        return build(row(btn(AdminMessages.MSG_BACK, yesCallback), btn("Yo'q", noCallback)));
    }

    public static InlineKeyboardMarkup pagination(int current, int total, String prefix) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        if (current > 1) row.add(btn(AdminMessages.BTN_PREVIOUS, prefix + (current - 1)));
        row.add(btn(String.format(AdminMessages.MSG_PAGE, current, total), "noop"));
        if (current < total) row.add(btn(AdminMessages.BTN_NEXT, prefix + (current + 1)));
        return InlineKeyboardMarkup.builder().keyboardRow(row).build();
    }

    // Unit management
    public static InlineKeyboardMarkup unitList(List<Unit> units) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Unit u : units) {
            rows.add(row(btn(u.getName() + " - " + u.getTitleUz(), "edit_unit:" + u.getId())));
        }
        rows.add(row(btn(AdminMessages.MSG_ADD_NEW, "add_unit")));
        rows.add(row(btn(AdminMessages.MSG_BACK, CB_BACK + "admin_menu")));
        return build(rows);
    }

    public static InlineKeyboardMarkup unitEdit(Unit unit) {
        return build(
            row(btn("✏️ Nomini o'zgartirish", "edit_unit_name:" + unit.getId())),
            row(btn("✏️ Sarlavhasini o'zgartirish", "edit_unit_title:" + unit.getId())),
            row(btn("📋 Darslarini ko'rish", "view_unit_lessons:" + unit.getId())),
            row(btn("📝 Testlarini ko'rish", "view_unit_tests:" + unit.getId())),
            row(btn("📚 Nazariyalarni ko'rish", "view_unit_theory:" + unit.getId())),
            row(btn("📌 Vaziyatli masalalarni ko'rish", "view_unit_tasks:" + unit.getId())),
            row(btn("🗑 O'chirish", "delete_unit_confirm:" + unit.getId())),
            row(btn(AdminMessages.MSG_BACK, CB_MANAGE + "units"))
        );
    }

    // Lesson management
    public static InlineKeyboardMarkup lessonList(List<Lesson> lessons, int unitId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Lesson l : lessons) {
            rows.add(row(btn(l.getLessonNumber() + "-dars: " + l.getTitleUz(), "edit_lesson:" + l.getId())));
        }
        rows.add(row(btn(AdminMessages.MSG_ADD_NEW + " (bu fanga)", "add_lesson_unit:" + unitId)));
        rows.add(row(btn(AdminMessages.MSG_BACK, "edit_unit:" + unitId)));
        return build(rows);
    }

    public static InlineKeyboardMarkup lessonEdit(Lesson lesson) {
        return build(
            row(btn("✏️ Sarlavhasini o'zgartirish", "edit_lesson_title:" + lesson.getId())),
            row(btn("🗑 O'chirish", "delete_lesson:" + lesson.getId())),
            row(btn(AdminMessages.MSG_BACK, CB_BACK + "admin_menu"))
        );
    }

    // Test management
    public static InlineKeyboardMarkup manageTest(int lessonId, boolean hasTest) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        if (hasTest) {
            rows.add(row(btn("📋 Savollar ro'yxati", "list_questions:" + lessonId)));
            rows.add(row(btn("⏱ Vaqt limitini o'zgartirish", "edit_test_time:" + lessonId)));
            rows.add(row(btn("📥 Savollarni import qilish", "import_questions:" + lessonId)));
            rows.add(row(btn("🗑 Savollarni tozalash", "clear_questions:" + lessonId)));
            rows.add(row(btn("❌ Testni o'chirish", "delete_test:" + lessonId)));
        } else {
            rows.add(row(btn("➕ Test yaratish", "create_test:" + lessonId)));
            rows.add(row(btn("📥 Fayldan import", "import_new_test:" + lessonId)));
        }
        rows.add(row(btn(AdminMessages.MSG_BACK, CB_BACK + "admin_menu")));
        return build(rows);
    }

    public static InlineKeyboardMarkup questionList(List<Question> questions, int testId, int page, int totalPages) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Question q : questions) {
            String label = q.getOrderNum() + ". " + truncate(q.getQuestionText(), 40);
            rows.add(row(btn(label, "view_question:" + q.getId())));
        }
        if (totalPages > 1) {
            InlineKeyboardRow pagRow = new InlineKeyboardRow();
            if (page > 1) pagRow.add(btn(AdminMessages.BTN_PREVIOUS, "questions_page:" + (page - 1)));
            pagRow.add(btn(String.format(AdminMessages.MSG_PAGE, page, totalPages), "noop"));
            if (page < totalPages) pagRow.add(btn(AdminMessages.BTN_NEXT, "questions_page:" + (page + 1)));
            rows.add(pagRow);
        }
        rows.add(row(btn("📥 Yangi import", "import_questions:" + testId),
                     btn("📥 Shablon", "download_template:" + testId)));
        rows.add(row(btn(AdminMessages.MSG_BACK, CB_BACK + "admin_menu")));
        return build(rows);
    }

    public static InlineKeyboardMarkup questionView(Question q) {
        return build(
            row(btn("✏️ Savolni tahrirlash", "edit_q_text:" + q.getId())),
            row(btn("🔢 Ballni tahrirlash", "edit_q_points:" + q.getId())),
            row(btn("📝 Javob variantlarini tahrirlash", "edit_answers:" + q.getId())),
            row(btn("⬆️ Yuqoriga", "move_q_up:" + q.getId()),
                btn("⬇️ Pastga", "move_q_down:" + q.getId())),
            row(btn("🗑 O'chirish", "delete_question:" + q.getId())),
            row(btn(AdminMessages.MSG_BACK, "list_questions:" + q.getTestId()))
        );
    }

    // Theory management
    public static InlineKeyboardMarkup theoryList(List<TheoryMaterial> materials, int lessonId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (TheoryMaterial m : materials) {
            rows.add(row(btn("📄 " + m.getTitleUz(), "view_theory_material:" + m.getId())));
        }
        rows.add(row(btn("📤 Fayl yuklash", "upload_theory:" + lessonId)));
        rows.add(row(btn(AdminMessages.MSG_BACK, CB_BACK + "admin_menu")));
        return build(rows);
    }

    public static InlineKeyboardMarkup theoryMaterialView(TheoryMaterial m) {
        return build(
            row(btn("✏️ Sarlavhani tahrirlash", "edit_theory_title:" + m.getId())),
            row(btn("✏️ Tavsifni tahrirlash", "edit_theory_text:" + m.getId())),
            row(btn("🗑 O'chirish", "delete_theory_material:" + m.getId())),
            row(btn(AdminMessages.MSG_BACK, "manage_theory:" + m.getLessonId()))
        );
    }

    // Situational tasks management
    public static InlineKeyboardMarkup taskList(List<SituationalTask> tasks, int lessonId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (SituationalTask t : tasks) {
            rows.add(row(btn(t.getOrderNum() + ". " + truncate(t.getTaskText(), 40), "edit_task:" + t.getId())));
        }
        rows.add(row(btn("➕ Yangi masala qo'shish", "create_task:" + lessonId)));
        rows.add(row(btn(AdminMessages.MSG_BACK, CB_BACK + "admin_menu")));
        return build(rows);
    }

    public static InlineKeyboardMarkup taskEdit(SituationalTask t) {
        return build(
            row(btn("✏️ Matnni tahrirlash", "edit_task_text:" + t.getId())),
            row(btn("⏱ Vaqtni tahrirlash", "edit_task_time:" + t.getId())),
            row(btn("🗑 O'chirish", "delete_task:" + t.getId())),
            row(btn(AdminMessages.MSG_BACK, CB_BACK + "admin_menu"))
        );
    }

    // Grading
    public static InlineKeyboardMarkup gradingOptions(int answerId) {
        return build(
            row(btn("🤖 AI bilan baholash", "grade_situational:" + answerId)),
            row(btn("✍️ Qo'lda baholash", "manual_grade:" + answerId)),
            row(btn(AdminMessages.MSG_BACK, CB_CANCEL))
        );
    }

    public static InlineKeyboardMarkup confirmGrade(int answerId, int grade) {
        return build(
            row(btn("✅ Tasdiqlash (" + grade + " ball)", "confirm_grade:" + answerId)),
            row(btn("❌ Bekor", "cancel_grade"))
        );
    }

    // Results filter
    public static InlineKeyboardMarkup resultsFilter() {
        return build(
            row(btn("👤 Ism bo'yicha", "filter_results:name")),
            row(btn("👥 Guruh+Kichik guruh bo'yicha", "filter_results:group")),
            row(btn(AdminMessages.MSG_BACK, CB_CANCEL))
        );
    }

    public static InlineKeyboardMarkup situationalFilter() {
        return build(
            row(btn("👤 Ism bo'yicha", "filter_situational:name")),
            row(btn("👥 Guruh+Kichik guruh bo'yicha", "filter_situational:group")),
            row(btn(AdminMessages.MSG_BACK, CB_CANCEL))
        );
    }

    // Helpers
    private static InlineKeyboardRow row(InlineKeyboardButton... btns) {
        return new InlineKeyboardRow(btns);
    }

    @SafeVarargs
    private static InlineKeyboardMarkup build(InlineKeyboardRow... rows) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(rows)).build();
    }

    private static InlineKeyboardMarkup build(List<InlineKeyboardRow> rows) {
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
