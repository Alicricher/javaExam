package com.dentistrybot.shared.integration;

import com.dentistrybot.shared.model.AnswerOption;
import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.model.Question;
import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.TestResult;
import com.dentistrybot.shared.model.TestRetake;
import com.dentistrybot.shared.model.Unit;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.repository.TestRepository;
import com.dentistrybot.shared.service.TestService;
import com.dentistrybot.shared.state.CachedQuestionData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the whole test-taking lifecycle end to end against a real Postgres —
 * student registration, first attempt, completion, an admin-granted retake, and a
 * second attempt — through the real TestService/ResultRepository/TestRepository,
 * with no mocks anywhere. This is exactly the state machine that TestHandler drives
 * in production (see TestHandler.handleTestCallback / handleTestConfirm), and it
 * pins down the retake/attempt-numbering behavior a prior refactor silently dropped
 * (StudentTest for that regression only asserts against a mocked ResultRepository —
 * this proves the real SQL behind canTakeTest/getAllTestAttempts/startTestAtomically
 * actually produces that behavior).
 */
class TestTakingFlowIntegrationTest extends AbstractIntegrationTest {

    private final StudentRepository studentRepository = new StudentRepository(jdbc);
    private final LessonRepository lessonRepository = new LessonRepository(jdbc);
    private final TestRepository testRepository = new TestRepository(jdbc);
    private final ResultRepository resultRepository = new ResultRepository(jdbc, tx);
    private final TestService testService = new TestService(testRepository, resultRepository);

    @Test
    void firstAttemptThenAdminGrantedRetakeProducesCorrectAttemptNumbersAndScores() {
        Student student = createStudent();
        int testId = createTestWithOneCorrectAndOneWrongQuestion();

        // ---- first attempt: never taken before ----
        String[] canTakeBefore = testService.canTakeTest(student.getId(), testId);
        assertThat(canTakeBefore).containsExactly("true", "first_attempt");

        TestResult firstResult = testService.startTest(student.getId(), testId, false);
        List<CachedQuestionData> questions = testService.loadQuestionsForCaching(testId);
        assertThat(questions).hasSize(2);

        answerAllCorrectly(firstResult.getId(), questions);
        TestResult completedFirst = testService.completeTest(firstResult.getId(), "completed");

        assertThat(completedFirst.getScore()).isEqualTo(2); // 1 point per question, both correct
        assertThat(completedFirst.getStatus()).isEqualTo("completed");
        assertThat(resultRepository.getAllTestAttempts(student.getId(), testId)).hasSize(1);

        // ---- no retake granted yet: student is blocked from retaking ----
        String[] canTakeAfterFirst = testService.canTakeTest(student.getId(), testId);
        assertThat(canTakeAfterFirst).containsExactly("false", "no_retake");

        // ---- admin grants a retake ----
        TestRetake retake = new TestRetake();
        retake.setStudentId(student.getId());
        retake.setTestId(testId);
        resultRepository.createTestRetake(retake);

        String[] canTakeWithRetake = testService.canTakeTest(student.getId(), testId);
        assertThat(canTakeWithRetake).containsExactly("true", "retake");

        // ---- second attempt: this is the exact atomic path TestHandler.handleTestConfirm
        //      uses (isRetake=true consumes the granted retake row) ----
        int attemptNumberBeforeSecond =
            resultRepository.getAllTestAttempts(student.getId(), testId).size() + 1;
        assertThat(attemptNumberBeforeSecond).isEqualTo(2);

        TestResult secondResult = testService.startTest(student.getId(), testId, true);
        List<CachedQuestionData> questionsAgain = testService.loadQuestionsForCaching(testId);
        answerAllWrong(secondResult.getId(), questionsAgain);
        TestResult completedSecond = testService.completeTest(secondResult.getId(), "completed");

        assertThat(completedSecond.getScore()).isZero();
        assertThat(resultRepository.getAllTestAttempts(student.getId(), testId)).hasSize(2);

        // ---- the retake was consumed: a third attempt is blocked again ----
        String[] canTakeAfterRetakeUsed = testService.canTakeTest(student.getId(), testId);
        assertThat(canTakeAfterRetakeUsed).containsExactly("false", "no_retake");
    }

    @Test
    void startingARetakeWithoutOneGrantedIsRejected() {
        Student student = createStudent();
        int testId = createTestWithOneCorrectAndOneWrongQuestion();

        assertThatStartTestThrowsForUngrantedRetake(student.getId(), testId);
    }

    private void assertThatStartTestThrowsForUngrantedRetake(int studentId, int testId) {
        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> testService.startTest(studentId, testId, true)
        ).isInstanceOf(IllegalStateException.class);
    }

    private void answerAllCorrectly(int resultId, List<CachedQuestionData> questions) {
        for (CachedQuestionData q : questions) {
            int correctOptionId = q.getOptions().stream()
                .filter(o -> o.isCorrect()).findFirst().orElseThrow().getOptionId();
            testService.submitAnswerWithCachedCorrectness(resultId, q.getQuestionId(), correctOptionId, true);
        }
    }

    private void answerAllWrong(int resultId, List<CachedQuestionData> questions) {
        for (CachedQuestionData q : questions) {
            int wrongOptionId = q.getOptions().stream()
                .filter(o -> !o.isCorrect()).findFirst().orElseThrow().getOptionId();
            testService.submitAnswerWithCachedCorrectness(resultId, q.getQuestionId(), wrongOptionId, false);
        }
    }

    private Student createStudent() {
        Student s = new Student();
        s.setTelegramId(555L);
        s.setFullName("Test Student");
        s.setCourse(2);
        s.setGroupName("201");
        s.setSubgroup("A");
        s.setFaculty("Dentistry");
        return studentRepository.createStudent(s);
    }

    private int createTestWithOneCorrectAndOneWrongQuestion() {
        Unit unit = new Unit();
        unit.setName("IT-U1");
        unit.setTitleUz("Integration unit");
        unit = lessonRepository.createUnit(unit);

        Lesson lesson = new Lesson();
        lesson.setUnitId(unit.getId());
        lesson.setLessonNumber(1);
        lesson.setTitleUz("Integration lesson");
        lesson = lessonRepository.createLesson(lesson);

        com.dentistrybot.shared.model.Test test = new com.dentistrybot.shared.model.Test();
        test.setLessonId(lesson.getId());
        test.setTitleUz("Integration test");
        test.setTimeLimitMinutes(15);
        test.setTotalPoints(0);
        test = testRepository.createTest(test);

        for (int i = 1; i <= 2; i++) {
            Question q = new Question();
            q.setTestId(test.getId());
            q.setQuestionText("Question " + i);
            q.setPoints(1);
            q.setOrderNum(i);
            q = testRepository.createQuestion(q);

            AnswerOption correct = new AnswerOption();
            correct.setQuestionId(q.getId());
            correct.setOptionText("Correct " + i);
            correct.setCorrect(true);
            correct.setOrderNum(1);
            testRepository.createAnswerOption(correct);

            AnswerOption wrong = new AnswerOption();
            wrong.setQuestionId(q.getId());
            wrong.setOptionText("Wrong " + i);
            wrong.setCorrect(false);
            wrong.setOrderNum(2);
            testRepository.createAnswerOption(wrong);
        }

        return test.getId();
    }
}
