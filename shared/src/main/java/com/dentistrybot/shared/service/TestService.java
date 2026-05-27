package com.dentistrybot.shared.service;

import com.dentistrybot.shared.model.*;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.TestRepository;
import com.dentistrybot.shared.state.CachedOptionData;
import com.dentistrybot.shared.state.CachedQuestionData;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TestService {

    private final TestRepository testRepository;
    private final ResultRepository resultRepository;

    public TestService(TestRepository testRepository, ResultRepository resultRepository) {
        this.testRepository = testRepository;
        this.resultRepository = resultRepository;
    }

    // Returns: [0]=canTake, [1]=attemptType ("first_attempt"|"retake"|"no_retake")
    public String[] canTakeTest(int studentId, int testId) {
        boolean completed = resultRepository.hasCompletedTest(studentId, testId);
        if (completed) {
            boolean hasRetake = resultRepository.hasAvailableRetake(studentId, testId);
            if (!hasRetake) return new String[]{"false", "no_retake"};
            return new String[]{"true", "retake"};
        }
        return new String[]{"true", "first_attempt"};
    }

    public TestResult startTest(int studentId, int testId, boolean isRetake) {
        Test test = testRepository.getTestById(testId);
        if (test == null) throw new IllegalArgumentException("Test not found: " + testId);
        return resultRepository.startTestAtomically(studentId, testId, isRetake, test.getTotalPoints());
    }

    public QuestionWithOptions getQuestionForResult(int testId, int questionIndex) {
        List<Question> questions = testRepository.getQuestionsByTestId(testId);
        if (questionIndex >= questions.size()) return null;
        return testRepository.getQuestionWithOptions(questions.get(questionIndex).getId());
    }

    public int getTotalQuestionsCount(int testId) {
        return testRepository.countQuestions(testId);
    }

    // Submit answer using live DB correctness (for non-cached tests)
    public boolean submitAnswer(int resultId, int questionId, int optionId) {
        AnswerOption option = testRepository.getAnswerOptionById(optionId);
        if (option == null) throw new IllegalArgumentException("Option not found: " + optionId);
        TestAnswer answer = new TestAnswer();
        answer.setResultId(resultId);
        answer.setQuestionId(questionId);
        answer.setSelectedOptionId(optionId);
        answer.setCorrect(option.isCorrect());
        answer.setAnsweredAt(LocalDateTime.now());
        resultRepository.createTestAnswer(answer);
        return option.isCorrect();
    }

    // Submit answer using cached correctness (prevents mid-test admin edits from affecting student)
    public void submitAnswerWithCachedCorrectness(int resultId, int questionId, int optionId, boolean isCorrect) {
        TestAnswer answer = new TestAnswer();
        answer.setResultId(resultId);
        answer.setQuestionId(questionId);
        answer.setSelectedOptionId(optionId);
        answer.setCorrect(isCorrect);
        answer.setAnsweredAt(LocalDateTime.now());
        resultRepository.createTestAnswer(answer);
    }

    public TestResult completeTest(int resultId, String status) {
        int score = resultRepository.calculateTestScore(resultId);
        TestResult result = resultRepository.getTestResultById(resultId);
        if (result == null) throw new IllegalArgumentException("TestResult not found: " + resultId);
        result.setScore(score);
        result.setCompletedAt(LocalDateTime.now());
        result.setStatus(status);
        resultRepository.updateTestResult(result);
        return result;
    }

    public Duration getRemainingTime(Instant startedAt, int timeLimitMinutes) {
        Instant endTime = startedAt.plusSeconds((long) timeLimitMinutes * 60);
        Duration remaining = Duration.between(Instant.now(), endTime);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public boolean isTimeUp(Instant startedAt, int timeLimitMinutes) {
        return getRemainingTime(startedAt, timeLimitMinutes).isZero();
    }

    public boolean shouldShowWarning(Instant startedAt, int timeLimitMinutes) {
        Duration remaining = getRemainingTime(startedAt, timeLimitMinutes);
        return !remaining.isZero() && remaining.getSeconds() <= 30;
    }

    public List<CachedQuestionData> loadQuestionsForCaching(int testId) {
        List<Question> questions = testRepository.getQuestionsByTestId(testId);
        List<CachedQuestionData> cached = new ArrayList<>();
        for (Question q : questions) {
            QuestionWithOptions qwo = testRepository.getQuestionWithOptions(q.getId());
            if (qwo == null) continue;
            CachedQuestionData cq = new CachedQuestionData();
            cq.setQuestionId(q.getId());
            cq.setQuestionText(q.getQuestionText());
            cq.setPoints(q.getPoints());
            List<CachedOptionData> options = new ArrayList<>();
            for (AnswerOption opt : qwo.getOptions()) {
                CachedOptionData co = new CachedOptionData();
                co.setOptionId(opt.getId());
                co.setOptionText(opt.getOptionText());
                co.setCorrect(opt.isCorrect());
                options.add(co);
            }
            cq.setOptions(options);
            cached.add(cq);
        }
        return cached;
    }
}
