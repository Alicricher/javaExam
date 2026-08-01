package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.SituationalAnswer;
import com.dentistrybot.shared.model.TestResultWithStudent;
import com.dentistrybot.shared.repository.ResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ResultControllerTest {

    @Test
    void testResultsAppliesPaginationAndFilters() {
        ResultRepository repo = mock(ResultRepository.class);
        when(repo.getTestResultsFiltered("301", "A", "Ali", "Test", "passed", 1, 2, 3, 20, 20))
            .thenReturn(List.of(new TestResultWithStudent()));
        when(repo.countTestResultsFiltered("301", "A", "Ali", "Test", "passed", 1, 2, 3)).thenReturn(21);

        var response = new ResultController(repo)
            .testResults("Ali", "301", "A", "Test", "passed", 1, 2, 3, 2, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("total", 21).containsEntry("page", 2).containsEntry("size", 20);
    }

    @Test
    void situationalResultsAppliesPaginationAndFilters() {
        ResultRepository repo = mock(ResultRepository.class);
        when(repo.getSituationalAnswersFiltered("301", "A", "Ali", "true", 1, 2, 3, 20, 0))
            .thenReturn(List.of());
        when(repo.countSituationalAnswersFiltered("301", "A", "Ali", "true", 1, 2, 3)).thenReturn(0);

        var response = new ResultController(repo)
            .situationalResults("Ali", "301", "A", "true", 1, 2, 3, 1, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("total", 0);
    }

    @Test
    void getSituationalAnswerReturnsNotFoundWhenMissing() {
        ResultRepository repo = mock(ResultRepository.class);
        when(repo.getSituationalAnswerById(1)).thenReturn(null);

        var response = new ResultController(repo).getSituationalAnswer(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getSituationalAnswerReturnsAnswerWhenFound() {
        ResultRepository repo = mock(ResultRepository.class);
        SituationalAnswer answer = new SituationalAnswer();
        answer.setId(1);
        when(repo.getSituationalAnswerById(1)).thenReturn(answer);

        var response = new ResultController(repo).getSituationalAnswer(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(answer);
    }
}
