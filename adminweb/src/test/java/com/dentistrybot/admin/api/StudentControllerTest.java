package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.StudentFilter;
import com.dentistrybot.shared.model.TestResultWithStudent;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StudentControllerTest {

    @Test
    void listPassesFiltersAndPaginationToRepository() {
        StudentRepository studentRepository = mock(StudentRepository.class);
        ResultRepository resultRepository = mock(ResultRepository.class);
        when(studentRepository.getStudents(any(StudentFilter.class))).thenReturn(List.of(new Student()));
        when(studentRepository.countStudents(any(StudentFilter.class))).thenReturn(42);

        var response = new StudentController(studentRepository, resultRepository)
            .list("Ali", 3, "301", "A", "Stomatologiya", 2, 50);

        ArgumentCaptor<StudentFilter> filterCaptor = ArgumentCaptor.forClass(StudentFilter.class);
        verify(studentRepository).getStudents(filterCaptor.capture());
        StudentFilter filter = filterCaptor.getValue();
        assertThat(filter.getFullName()).isEqualTo("Ali");
        assertThat(filter.getCourse()).isEqualTo(3);
        assertThat(filter.getGroupName()).isEqualTo("301");
        assertThat(filter.getSubgroup()).isEqualTo("A");
        assertThat(filter.getFaculty()).isEqualTo("Stomatologiya");
        assertThat(filter.getLimit()).isEqualTo(50);
        assertThat(filter.getOffset()).isEqualTo(50);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("total", 42);
    }

    @Test
    void resultsReturnsTotalForStudentResultPagination() {
        StudentRepository studentRepository = mock(StudentRepository.class);
        ResultRepository resultRepository = mock(ResultRepository.class);
        when(resultRepository.getTestResultsByStudentId(7, 10, 20)).thenReturn(List.of(new TestResultWithStudent()));
        when(resultRepository.countTestResultsByStudentId(7)).thenReturn(31);

        var response = new StudentController(studentRepository, resultRepository).results(7, 3, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("total", 31).containsEntry("page", 3).containsEntry("size", 10);
    }

    @Test
    void filterOptionsReturnsDistinctStudentFilters() {
        StudentRepository studentRepository = mock(StudentRepository.class);
        ResultRepository resultRepository = mock(ResultRepository.class);
        when(studentRepository.getDistinctCourses()).thenReturn(List.of(1, 2, 3));
        when(studentRepository.getDistinctGroups()).thenReturn(List.of("301", "302"));
        when(studentRepository.getDistinctSubgroups()).thenReturn(List.of("A", "B"));
        when(studentRepository.getDistinctFaculties()).thenReturn(List.of("Stomatologiya"));

        var response = new StudentController(studentRepository, resultRepository).filterOptions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body)
            .containsEntry("courses", List.of(1, 2, 3))
            .containsEntry("groups", List.of("301", "302"))
            .containsEntry("subgroups", List.of("A", "B"))
            .containsEntry("faculties", List.of("Stomatologiya"));
    }

    @Test
    void updateNameRejectsBlankName() {
        var response = new StudentController(mock(StudentRepository.class), mock(ResultRepository.class))
            .updateName(1, Map.of("name", " "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
