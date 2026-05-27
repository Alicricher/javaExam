package com.dentistrybot.shared.state;

public final class StateConstants {
    private StateConstants() {}

    public static final String IDLE = "idle";

    // Student registration
    public static final String REGISTER_FULL_NAME = "register_full_name";
    public static final String REGISTER_COURSE    = "register_course";
    public static final String REGISTER_GROUP     = "register_group";
    public static final String REGISTER_SUBGROUP  = "register_subgroup";
    public static final String REGISTER_FACULTY   = "register_faculty";

    // Student profile edit
    public static final String EDIT_COURSE   = "edit_course";
    public static final String EDIT_GROUP    = "edit_group";
    public static final String EDIT_SUBGROUP = "edit_subgroup";
    public static final String EDIT_FACULTY  = "edit_faculty";

    // Learning navigation
    public static final String SELECT_UNIT   = "select_unit";
    public static final String SELECT_LESSON = "select_lesson";
    public static final String LESSON_MENU   = "lesson_menu";
    public static final String VIEW_THEORY   = "view_theory";

    // Test flow
    public static final String TEST_CONFIRM   = "test_confirm";
    public static final String IN_TEST        = "in_test";
    public static final String TEST_COMPLETED = "test_completed";

    // Situational task flow
    public static final String SITUATIONAL_CONFIRM        = "situational_confirm";
    public static final String IN_SITUATIONAL             = "in_situational";
    public static final String SITUATIONAL_CONFIRM_ANSWER = "situational_confirm_answer";

    // Admin states
    public static final String ADMIN_LOGIN    = "admin_login";
    public static final String ADMIN_MENU     = "admin_menu";
    public static final String VIEW_STUDENTS  = "view_students";
    public static final String VIEW_RESULTS   = "view_results";
    public static final String MANAGE_CONTENT = "manage_content";

    // Admin student management
    public static final String EDIT_STUDENT_NAME    = "edit_student_name";
    public static final String GRANT_RETAKE         = "grant_retake";
    public static final String ADMIN_FILTER_NAME    = "admin_filter_name";
    public static final String ADMIN_FILTER_COURSE  = "admin_filter_course";
    public static final String ADMIN_FILTER_GROUP   = "admin_filter_group";
    public static final String ADMIN_FILTER_FACULTY = "admin_filter_faculty";

    // Admin content management
    public static final String MANAGE_UNITS   = "manage_units";
    public static final String MANAGE_LESSONS = "manage_lessons";
    public static final String MANAGE_TESTS   = "manage_tests";
    public static final String MANAGE_THEORY  = "manage_theory";
    public static final String MANAGE_TASKS   = "manage_tasks";

    // Admin grading
    public static final String ADMIN_GRADING                = "admin_grading";
    public static final String ADMIN_CONFIRM_GRADE          = "admin_confirm_grade";
    public static final String ADMIN_MANUAL_GRADE           = "admin_manual_grade";
    public static final String ADMIN_MANUAL_GRADE_FEEDBACK  = "admin_manual_grade_feedback";

    public static final int MANUAL_GRADE_PASS_THRESHOLD = 60;
}
