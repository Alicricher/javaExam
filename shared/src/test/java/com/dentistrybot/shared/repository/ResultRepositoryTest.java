package com.dentistrybot.shared.repository;

import com.dentistrybot.shared.model.SituationalAnswerWithStudent;
import com.dentistrybot.shared.model.TestResultWithStudent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResultRepositoryTest {

    @Test
    void testResultsFilterIncludesStudentGroupLessonStatusAndPagination() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        new ResultRepository(jdbc, mock(TransactionTemplate.class))
            .getTestResultsFiltered("301", "A", "Ali", "Implant", "completed", 2, 9, 14, 20, 40);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains(
            "LOWER(s.group_name)",
            "LOWER(s.subgroup)",
            "uz_translit(s.full_name) LIKE uz_translit(:studentName)",
            "LOWER(t.title_uz)",
            "tr.status = :status",
            "u.id = :unitId",
            "l.id = :lessonId",
            "tr.test_id = :testId",
            "LIMIT :limit OFFSET :offset"
        );
        assertThat(paramsCaptor.getValue().getValue("groupName")).isEqualTo("%301%");
        assertThat(paramsCaptor.getValue().getValue("subgroupName")).isEqualTo("%A%");
        assertThat(paramsCaptor.getValue().getValue("studentName")).isEqualTo("%Ali%");
        assertThat(paramsCaptor.getValue().getValue("testName")).isEqualTo("%Implant%");
        assertThat(paramsCaptor.getValue().getValue("status")).isEqualTo("completed");
        assertThat(paramsCaptor.getValue().getValue("unitId")).isEqualTo(2);
        assertThat(paramsCaptor.getValue().getValue("lessonId")).isEqualTo(9);
        assertThat(paramsCaptor.getValue().getValue("testId")).isEqualTo(14);
        assertThat(paramsCaptor.getValue().getValue("limit")).isEqualTo(20);
        assertThat(paramsCaptor.getValue().getValue("offset")).isEqualTo(40);
    }

    @Test
    void situationalFilterIncludesGradedLessonTaskAndPagination() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        new ResultRepository(jdbc, mock(TransactionTemplate.class))
            .getSituationalAnswersFiltered("301", "A", "Ali", "false", 2, 9, 14, 20, 40);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains(
            "LOWER(s.group_name)",
            "LOWER(s.subgroup)",
            "uz_translit(s.full_name) LIKE uz_translit(:studentName)",
            "sa.is_graded = :graded",
            "u.id = :unitId",
            "l.id = :lessonId",
            "sa.task_id = :taskId",
            "LIMIT :limit OFFSET :offset"
        );
        assertThat(paramsCaptor.getValue().getValue("graded")).isEqualTo(false);
        assertThat(paramsCaptor.getValue().getValue("unitId")).isEqualTo(2);
        assertThat(paramsCaptor.getValue().getValue("lessonId")).isEqualTo(9);
        assertThat(paramsCaptor.getValue().getValue("taskId")).isEqualTo(14);
        assertThat(paramsCaptor.getValue().getValue("limit")).isEqualTo(20);
        assertThat(paramsCaptor.getValue().getValue("offset")).isEqualTo(40);
    }
}
