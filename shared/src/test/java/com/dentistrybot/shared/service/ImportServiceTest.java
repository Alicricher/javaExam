package com.dentistrybot.shared.service;

import com.dentistrybot.shared.model.AnswerOption;
import com.dentistrybot.shared.model.Question;
import com.dentistrybot.shared.repository.TestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void importCsvCreatesQuestionOptionsAndUpdatesTotalPoints() throws Exception {
        TestRepository repository = mock(TestRepository.class);
        when(repository.countQuestions(7)).thenReturn(3);
        when(repository.createQuestion(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(99);
            return question;
        });
        Path csv = tempDir.resolve("questions.csv");
        Files.writeString(csv, """
            Savol,Ball,A,B,C,D,E,Correct
            Question one,2,A text,B text,C text,D text,E text,C
            """);

        int imported = new ImportService(repository).importQuestionsFromCsv(csv.toString(), 7);

        assertThat(imported).isEqualTo(1);
        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(repository).createQuestion(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getTestId()).isEqualTo(7);
        assertThat(questionCaptor.getValue().getOrderNum()).isEqualTo(4);
        assertThat(questionCaptor.getValue().getPoints()).isEqualTo(2);

        ArgumentCaptor<AnswerOption> optionCaptor = ArgumentCaptor.forClass(AnswerOption.class);
        verify(repository, times(5)).createAnswerOption(optionCaptor.capture());
        List<AnswerOption> options = optionCaptor.getAllValues();
        assertThat(options).extracting(AnswerOption::getQuestionId).containsOnly(99);
        assertThat(options).filteredOn(AnswerOption::isCorrect)
            .singleElement()
            .extracting(AnswerOption::getOptionText)
            .isEqualTo("C text");
        verify(repository).updateTestTotalPoints(7);
    }

    @Test
    void importCsvRejectsCorrectAnswerThatDoesNotExist() throws Exception {
        TestRepository repository = mock(TestRepository.class);
        Path csv = tempDir.resolve("questions.csv");
        Files.writeString(csv, """
            Savol,Ball,A,B,C,D,E,Correct
            Question one,1,A text,B text,,,,E
            """);

        assertThatThrownBy(() -> new ImportService(repository).importQuestionsFromCsv(csv.toString(), 7))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("correct answer must match an existing option");

        verify(repository, never()).createQuestion(any());
        verify(repository, never()).createAnswerOption(any());
        verify(repository, never()).updateTestTotalPoints(anyInt());
    }
}
