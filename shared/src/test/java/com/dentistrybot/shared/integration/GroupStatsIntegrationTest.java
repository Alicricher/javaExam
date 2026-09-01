package com.dentistrybot.shared.integration;

import com.dentistrybot.shared.model.AnswerOption;
import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.model.Question;
import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.TestResult;
import com.dentistrybot.shared.model.Unit;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.repository.TestRepository;
import com.dentistrybot.shared.service.TestService;
import com.dentistrybot.shared.state.CachedQuestionData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResultRepository.getGroupStatsByTestId/getStudentResultDetailsByTestId back the admin
 * "group pass-rate" dashboard and its PDF export. The SQL does a non-trivial
 * DISTINCT ON (best attempt per student, completed-only) - exactly the kind of query a
 * mocked unit test can't verify. This proves it against real Postgres, including that
 * the aggregate counts and the per-student rows always agree with each other.
 */
class GroupStatsIntegrationTest extends AbstractIntegrationTest {

    private final StudentRepository studentRepository = new StudentRepository(jdbc);
    private final LessonRepository lessonRepository = new LessonRepository(jdbc);
    private final TestRepository testRepository = new TestRepository(jdbc);
    private final ResultRepository resultRepository = new ResultRepository(jdbc, tx);
    private final TestService testService = new TestService(testRepository, resultRepository);

    @Test
    void groupStatsAndStudentDetailsAgreeOnPassFailCounts() {
        int testId = createTestWithOneQuestionWorthOnePoint();

        Student passer = createStudent(1L, "Ахрор Каримов", "301", "A");
        Student failer = createStudent(2L, "Botir Yusupov", "301", "A");
        Student otherGroup = createStudent(3L, "Dilnoza Rashidova", "302", "B");

        takeAndFinish(passer, testId, true);   // scores 1/1 -> passes (100% >= 60%)
        takeAndFinish(failer, testId, false);  // scores 0/1 -> fails
        takeAndFinish(otherGroup, testId, true);

        List<Map<String, Object>> stats = resultRepository.getGroupStatsByTestId(testId);
        List<Map<String, Object>> details = resultRepository.getStudentResultDetailsByTestId(testId);

        assertThat(details).hasSize(3);
        assertThat(details).filteredOn(r -> "Ахрор Каримов".equals(r.get("full_name")))
            .singleElement().satisfies(r -> assertThat(r.get("passed")).isEqualTo(true));
        assertThat(details).filteredOn(r -> "Botir Yusupov".equals(r.get("full_name")))
            .singleElement().satisfies(r -> assertThat(r.get("passed")).isEqualTo(false));

        Map<String, Object> group301 = stats.stream()
            .filter(r -> "301".equals(r.get("group_name"))).findFirst().orElseThrow();
        assertThat(group301.get("student_count")).isEqualTo(2L);
        assertThat(group301.get("passed_count")).isEqualTo(1L);

        Map<String, Object> group302 = stats.stream()
            .filter(r -> "302".equals(r.get("group_name"))).findFirst().orElseThrow();
        assertThat(group302.get("student_count")).isEqualTo(1L);
        assertThat(group302.get("passed_count")).isEqualTo(1L);
    }

    @Test
    void bestAttemptWinsWhenStudentRetakesAfterFailing() {
        int testId = createTestWithOneQuestionWorthOnePoint();
        Student student = createStudent(10L, "Sardor Nazarov", "201", "A");

        takeAndFinish(student, testId, false); // first attempt: fails

        var retake = new com.dentistrybot.shared.model.TestRetake();
        retake.setStudentId(student.getId());
        retake.setTestId(testId);
        resultRepository.createTestRetake(retake);
        takeAndFinish(student, testId, true, true); // retake: passes

        List<Map<String, Object>> details = resultRepository.getStudentResultDetailsByTestId(testId);

        assertThat(details).hasSize(1); // one row per student, not one per attempt
        assertThat(details.get(0).get("passed")).isEqualTo(true); // the *better* attempt wins
    }

    private void takeAndFinish(Student student, int testId, boolean correct) {
        takeAndFinish(student, testId, correct, false);
    }

    private void takeAndFinish(Student student, int testId, boolean correct, boolean isRetake) {
        TestResult result = testService.startTest(student.getId(), testId, isRetake);
        List<CachedQuestionData> questions = testService.loadQuestionsForCaching(testId);
        CachedQuestionData q = questions.get(0);
        int optionId = q.getOptions().stream()
            .filter(o -> o.isCorrect() == correct).findFirst().orElseThrow().getOptionId();
        testService.submitAnswerWithCachedCorrectness(result.getId(), q.getQuestionId(), optionId, correct);
        testService.completeTest(result.getId(), "completed");
    }

    private Student createStudent(long telegramId, String fullName, String group, String subgroup) {
        Student s = new Student();
        s.setTelegramId(telegramId);
        s.setFullName(fullName);
        s.setCourse(2);
        s.setGroupName(group);
        s.setSubgroup(subgroup);
        s.setFaculty("Dentistry");
        return studentRepository.createStudent(s);
    }

    private int createTestWithOneQuestionWorthOnePoint() {
        Unit unit = new Unit();
        unit.setName("GS-U1");
        unit.setTitleUz("Group stats unit");
        unit = lessonRepository.createUnit(unit);

        Lesson lesson = new Lesson();
        lesson.setUnitId(unit.getId());
        lesson.setLessonNumber(1);
        lesson.setTitleUz("Group stats lesson");
        lesson = lessonRepository.createLesson(lesson);

        com.dentistrybot.shared.model.Test test = new com.dentistrybot.shared.model.Test();
        test.setLessonId(lesson.getId());
        test.setTitleUz("Group stats test");
        test.setTimeLimitMinutes(15);
        test.setTotalPoints(0);
        test = testRepository.createTest(test);

        Question q = new Question();
        q.setTestId(test.getId());
        q.setQuestionText("Only question");
        q.setPoints(1);
        q.setOrderNum(1);
        q = testRepository.createQuestion(q);

        AnswerOption correct = new AnswerOption();
        correct.setQuestionId(q.getId());
        correct.setOptionText("Correct");
        correct.setCorrect(true);
        correct.setOrderNum(1);
        testRepository.createAnswerOption(correct);

        AnswerOption wrong = new AnswerOption();
        wrong.setQuestionId(q.getId());
        wrong.setOptionText("Wrong");
        wrong.setCorrect(false);
        wrong.setOrderNum(2);
        testRepository.createAnswerOption(wrong);

        return test.getId();
    }
}
