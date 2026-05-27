package com.dentistrybot.student.keyboard;

import com.dentistrybot.shared.model.AnswerOption;
import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.model.SituationalTask;
import com.dentistrybot.shared.model.TheoryMaterial;
import com.dentistrybot.shared.model.Unit;
import com.dentistrybot.student.localization.UzMessages;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class StudentKeyboards {

    private StudentKeyboards() {}

    // Callback prefixes
    public static final String CB_UNIT         = "unit:";
    public static final String CB_LESSON       = "lesson:";
    public static final String CB_TEST         = "test:";
    public static final String CB_THEORY       = "theory:";
    public static final String CB_SIT          = "sit:";
    public static final String CB_SIT_TASK     = "sit_task:";
    public static final String CB_ANSWER       = "ans:";
    public static final String CB_CONFIRM      = "confirm:";
    public static final String CB_CANCEL       = "cancel";
    public static final String CB_BACK         = "back:";
    public static final String CB_MATERIAL     = "mat:";
    public static final String CB_MAT_TYPE     = "mat_type:";
    public static final String CB_COURSE       = "course:";
    public static final String CB_GROUP        = "group:";
    public static final String CB_SUBGROUP     = "subgroup:";
    public static final String CB_FACULTY      = "faculty:";
    public static final String CB_EDIT_PROFILE = "edit_profile:";

    public static ReplyKeyboardMarkup mainMenu() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(UzMessages.BTN_START_LEARNING));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(UzMessages.BTN_PROFILE));
        return ReplyKeyboardMarkup.builder()
            .keyboard(List.of(row1, row2))
            .resizeKeyboard(true)
            .build();
    }

    public static ReplyKeyboardRemove removeKeyboard() {
        return ReplyKeyboardRemove.builder().removeKeyboard(true).build();
    }

    public static InlineKeyboardMarkup courseSelection() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                btn(UzMessages.BTN_COURSE_1, CB_COURSE + "1"),
                btn(UzMessages.BTN_COURSE_2, CB_COURSE + "2"),
                btn(UzMessages.BTN_COURSE_3, CB_COURSE + "3")))
            .keyboardRow(new InlineKeyboardRow(
                btn(UzMessages.BTN_COURSE_4, CB_COURSE + "4"),
                btn(UzMessages.BTN_COURSE_5, CB_COURSE + "5"),
                btn(UzMessages.BTN_COURSE_6, CB_COURSE + "6")))
            .build();
    }

    public static InlineKeyboardMarkup groupSelection(int course) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int i = 1; i <= 5; i++) {
            String g = String.valueOf(course * 100 + i);
            row.add(btn(g, CB_GROUP + g));
            if (row.size() == 3) { rows.add(new InlineKeyboardRow(row)); row.clear(); }
        }
        if (!row.isEmpty()) rows.add(new InlineKeyboardRow(row));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup subgroupSelection() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                btn("A", CB_SUBGROUP + "A"),
                btn("B", CB_SUBGROUP + "B"),
                btn("C", CB_SUBGROUP + "C"),
                btn("D", CB_SUBGROUP + "D")))
            .build();
    }

    public static InlineKeyboardMarkup facultySelection() {
        String[] faculties = {"Stomatologiya", "Davolash", "Pediatriya", "Tibbiy profilaktika", "Farmasevtika"};
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (String f : faculties) rows.add(new InlineKeyboardRow(btn(f, CB_FACULTY + f)));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup units(List<Unit> units) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Unit u : units) {
            rows.add(new InlineKeyboardRow(btn(u.getName() + " - " + u.getTitleUz(), CB_UNIT + u.getId())));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup lessons(List<Lesson> lessons, int unitId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < lessons.size(); i += 2) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            Lesson l1 = lessons.get(i);
            row.add(btn(l1.getLessonNumber() + "-dars: " + l1.getTitleUz(), CB_LESSON + l1.getId()));
            if (i + 1 < lessons.size()) {
                Lesson l2 = lessons.get(i + 1);
                row.add(btn(l2.getLessonNumber() + "-dars: " + l2.getTitleUz(), CB_LESSON + l2.getId()));
            }
            rows.add(row);
        }
        rows.add(new InlineKeyboardRow(btn(UzMessages.BTN_BACK, CB_BACK + "units")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup lessonMenu(int lessonId, int unitId) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.BTN_TEST, CB_TEST + lessonId)))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.BTN_THEORY, CB_THEORY + lessonId)))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.BTN_SITUATIONAL, CB_SIT + lessonId)))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.BTN_BACK, CB_BACK + "lessons:" + unitId)))
            .build();
    }

    public static InlineKeyboardMarkup testConfirm(int testId) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.MSG_START_TEST, CB_CONFIRM + "test:" + testId)))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.BTN_CANCEL, CB_CANCEL)))
            .build();
    }

    public static InlineKeyboardMarkup answerOptions(List<AnswerOption> options) {
        String[] letters = {"A", "B", "C", "D", "E", "F"};
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            AnswerOption opt = options.get(i);
            String letter = letters[i % letters.length];
            rows.add(new InlineKeyboardRow(btn(letter + ") " + opt.getOptionText(), CB_ANSWER + opt.getId())));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup situationalTaskList(List<SituationalTask> tasks, Set<Integer> answeredIds, int lessonId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int i = 0; i < tasks.size(); i++) {
            SituationalTask t = tasks.get(i);
            String label = answeredIds.contains(t.getId())
                ? "✅ " + t.getOrderNum() + "-masala"
                : t.getOrderNum() + "-masala";
            row.add(btn(label, CB_SIT_TASK + t.getId()));
            if (row.size() == 3 || i == tasks.size() - 1) {
                rows.add(new InlineKeyboardRow(row));
                row.clear();
            }
        }
        rows.add(new InlineKeyboardRow(btn(UzMessages.BTN_BACK, CB_BACK + "lesson:" + lessonId)));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup situationalConfirm(int taskId) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.MSG_START_SITUATIONAL, CB_CONFIRM + "sit:" + taskId)))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.BTN_CANCEL, CB_CANCEL)))
            .build();
    }

    public static InlineKeyboardMarkup situationalAnswerConfirm() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                btn("✅ Yuborish", "confirm_situational_answer"),
                btn("✏️ Tahrirlash", "edit_situational_answer")))
            .build();
    }

    public static InlineKeyboardMarkup theoryTypes(int lessonId) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.MSG_BOOKS, CB_MAT_TYPE + lessonId + ":book")))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.MSG_MANUALS, CB_MAT_TYPE + lessonId + ":manual")))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.MSG_MATERIALS, CB_MAT_TYPE + lessonId + ":material")))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.BTN_BACK, CB_BACK + "lesson:" + lessonId)))
            .build();
    }

    public static InlineKeyboardMarkup materialList(List<TheoryMaterial> materials, String type, int lessonId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (TheoryMaterial m : materials) {
            if (type.equals(m.getMaterialType())) {
                rows.add(new InlineKeyboardRow(btn(m.getTitleUz(), CB_MATERIAL + m.getId())));
            }
        }
        rows.add(new InlineKeyboardRow(btn(UzMessages.BTN_BACK, CB_THEORY + lessonId)));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup profileEdit() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.MSG_EDIT_COURSE, CB_EDIT_PROFILE + "course")))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.MSG_EDIT_GROUP, CB_EDIT_PROFILE + "group")))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.MSG_EDIT_SUBGROUP, CB_EDIT_PROFILE + "subgroup")))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.MSG_EDIT_FACULTY, CB_EDIT_PROFILE + "faculty")))
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.BTN_BACK, CB_BACK + "main")))
            .build();
    }

    public static InlineKeyboardMarkup cancelOnly() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(btn(UzMessages.BTN_CANCEL, CB_CANCEL)))
            .build();
    }

    public static InlineKeyboardMarkup pagination(int current, int total, String prefix) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        if (current > 1) row.add(btn(UzMessages.BTN_PREVIOUS, prefix + (current - 1)));
        row.add(btn(String.format(UzMessages.MSG_PAGE, current, total), "noop"));
        if (current < total) row.add(btn(UzMessages.BTN_NEXT, prefix + (current + 1)));
        return InlineKeyboardMarkup.builder().keyboardRow(row).build();
    }

    private static InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }
}
