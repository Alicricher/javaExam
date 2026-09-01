package com.dentistrybot.shared.repository;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.StudentFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StudentRepositoryTest {

    @Test
    void getStudentsUsesServerPaginationAndNumericSearchByIds() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        StudentFilter filter = new StudentFilter();
        filter.setFullName("123456");
        filter.setLimit(50);
        filter.setOffset(100);

        new StudentRepository(jdbc).getStudents(filter);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("id = :studentId", "telegram_id = :telegramId", "LIMIT :limit OFFSET :offset");
        assertThat(paramsCaptor.getValue().getValue("fullName")).isEqualTo("%123456%");
        assertThat(paramsCaptor.getValue().getValue("studentId")).isEqualTo(123456);
        assertThat(paramsCaptor.getValue().getValue("telegramId")).isEqualTo(123456L);
        assertThat(paramsCaptor.getValue().getValue("limit")).isEqualTo(50);
        assertThat(paramsCaptor.getValue().getValue("offset")).isEqualTo(100);
    }

    @Test
    void countStudentsUsesSameFiltersWithoutPaginationParams() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(5);
        StudentFilter filter = new StudentFilter();
        filter.setFullName("Ali");
        filter.setCourse(3);
        filter.setGroupName("301");
        filter.setSubgroup("A");
        filter.setFaculty("Dentistry");

        int total = new StudentRepository(jdbc).countStudents(filter);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(sqlCaptor.capture(), paramsCaptor.capture(), eq(Integer.class));
        assertThat(total).isEqualTo(5);
        assertThat(sqlCaptor.getValue()).contains("uz_translit(full_name) LIKE uz_translit(:fullName)", "course = :course", "LOWER(group_name)", "LOWER(subgroup)", "LOWER(faculty)");
        assertThat(paramsCaptor.getValue().hasValue("limit")).isFalse();
        assertThat(paramsCaptor.getValue().hasValue("offset")).isFalse();
    }

    @Test
    void getDistinctFilterValuesIgnoreBlankTextValues() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(anyString(), anyMap(), eq(String.class))).thenReturn(List.of("A", "B"));

        List<String> values = new StudentRepository(jdbc).getDistinctSubgroups();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sqlCaptor.capture(), anyMap(), eq(String.class));
        assertThat(values).containsExactly("A", "B");
        assertThat(sqlCaptor.getValue()).contains("SELECT DISTINCT subgroup", "TRIM(subgroup) <> ''", "ORDER BY subgroup");
    }

    @Test
    void getDistinctCoursesReturnsPositiveCourses() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(anyString(), anyMap(), eq(Integer.class))).thenReturn(List.of(1, 2, 3));

        List<Integer> courses = new StudentRepository(jdbc).getDistinctCourses();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sqlCaptor.capture(), anyMap(), eq(Integer.class));
        assertThat(courses).containsExactly(1, 2, 3);
        assertThat(sqlCaptor.getValue()).contains("course > 0", "ORDER BY course");
    }
}
