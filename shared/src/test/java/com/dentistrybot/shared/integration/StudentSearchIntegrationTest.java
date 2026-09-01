package com.dentistrybot.shared.integration;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.StudentFilter;
import com.dentistrybot.shared.repository.StudentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The unit tests for StudentRepository only assert that the built SQL string CONTAINS
 * "uz_translit(full_name) LIKE uz_translit(:fullName)" — they never actually run it.
 * These tests execute the real query against real Postgres with the real V10
 * uz_translit() plpgsql function, proving the Cyrillic/Latin search students actually
 * see really works both directions, and that all filters really AND together.
 */
class StudentSearchIntegrationTest extends AbstractIntegrationTest {

    private final StudentRepository repository = new StudentRepository(jdbc);

    private Student student(long telegramId, String fullName, int course,
                             String group, String subgroup, String faculty) {
        Student s = new Student();
        s.setTelegramId(telegramId);
        s.setFullName(fullName);
        s.setCourse(course);
        s.setGroupName(group);
        s.setSubgroup(subgroup);
        s.setFaculty(faculty);
        return repository.createStudent(s);
    }

    @Test
    void latinQueryFindsStudentRegisteredWithCyrillicName() {
        // uz_translit maps Cyrillic 'х' -> 'x', the correct Uzbek Latin form of "Ахрор".
        student(1L, "Ахрор Таиров", 3, "301", "A", "Dentistry");

        List<Student> found = repository.getStudents(filterFor("Axror"));

        assertThat(found).extracting(Student::getFullName).containsExactly("Ахрор Таиров");
    }

    @Test
    void latinQueryWithCommonHXConfusionStillFindsCyrillicName() {
        // V14: users routinely type "h" where the correct spelling uses "x" (or vice
        // versa) — uz_translit collapses both to 'x' for search so "Ahror" still finds
        // "Ахрор" even though the "textbook correct" Latin spelling is "Axror".
        student(1L, "Ахрор Таиров", 3, "301", "A", "Dentistry");

        List<Student> found = repository.getStudents(filterFor("Ahror"));

        assertThat(found).extracting(Student::getFullName).containsExactly("Ахрор Таиров");
    }

    @Test
    void cyrillicQueryFindsStudentRegisteredWithLatinName() {
        student(2L, "Jasur Yusupov", 2, "205", "B", "Dentistry");

        List<Student> found = repository.getStudents(filterFor("Жасур"));

        assertThat(found).extracting(Student::getFullName).containsExactly("Jasur Yusupov");
    }

    @Test
    void queryDoesNotMatchUnrelatedName() {
        student(3L, "Ахрор Таиров", 3, "301", "A", "Dentistry");

        List<Student> found = repository.getStudents(filterFor("Karimov"));

        assertThat(found).isEmpty();
    }

    @Test
    void combinedFiltersAllApplyTogetherAgainstRealRows() {
        student(10L, "Ахрор Таиров", 3, "301", "A", "Dentistry");
        // same name and course, different group -> must be excluded by the group filter
        student(11L, "Ахрор Тошматов", 3, "302", "A", "Dentistry");
        // same name and group, different course -> must be excluded by the course filter
        student(12L, "Ахрор Каримов", 2, "301", "A", "Dentistry");

        StudentFilter filter = filterFor("Axror");
        filter.setCourse(3);
        filter.setGroupName("301");

        List<Student> found = repository.getStudents(filter);

        assertThat(found).extracting(Student::getFullName).containsExactly("Ахрор Таиров");
    }

    private StudentFilter filterFor(String fullName) {
        StudentFilter filter = new StudentFilter();
        filter.setFullName(fullName);
        return filter;
    }
}
