package com.is1.proyecto.student;

import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.dto.EnrollmentDTO;
import com.is1.proyecto.dto.GradeDTO;
import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.models.Enrollment;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.repositories.StudentRepository;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StudentRepository (Phases 2 & 3).
 * Uses Mockito + MockedStatic for ActiveJDBC static methods.
 * Pattern matches PersonRepositoryTest and ConditionRepositoryTest.
 */
@ExtendWith(MockitoExtension.class)
public class StudentRepositoryTest {

    @Mock
    private DBConfigSingleton dbMock;

    @InjectMocks
    private StudentRepository repository;

    @BeforeEach
    void setUp() {
        // No global stubs — each test declares only what it needs
    }

    // ========================================================================
    // Phase 2: findByDni
    // ========================================================================

    @Test
    void testFindByDni_found() {
        try (MockedStatic<Person> personStatic = Mockito.mockStatic(Person.class);
             MockedStatic<Student> studentStatic = Mockito.mockStatic(Student.class)) {

            Person personMock = mock(Person.class);
            when(personMock.getId()).thenReturn(42L);

            Student studentMock = mock(Student.class);

            personStatic.when(() -> Person.findFirst("dni = ?", "12345678"))
                    .thenReturn(personMock);
            studentStatic.when(() -> Student.findFirst("id_person = ?", 42L))
                    .thenReturn(studentMock);

            Student result = repository.findByDni("12345678");

            assertNotNull(result);
            assertSame(studentMock, result);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testFindByDni_personNotFound() {
        try (MockedStatic<Person> personStatic = Mockito.mockStatic(Person.class)) {

            personStatic.when(() -> Person.findFirst("dni = ?", "nonexistent"))
                    .thenReturn(null);

            Student result = repository.findByDni("nonexistent");

            assertNull(result);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testFindByDni_studentNotFound() {
        try (MockedStatic<Person> personStatic = Mockito.mockStatic(Person.class);
             MockedStatic<Student> studentStatic = Mockito.mockStatic(Student.class)) {

            Person personMock = mock(Person.class);
            when(personMock.getId()).thenReturn(99L);

            personStatic.when(() -> Person.findFirst("dni = ?", "99999999"))
                    .thenReturn(personMock);
            studentStatic.when(() -> Student.findFirst("id_person = ?", 99L))
                    .thenReturn(null);

            Student result = repository.findByDni("99999999");

            assertNull(result);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testFindByDni_nullDni() {
        Student result = repository.findByDni(null);
        assertNull(result);
    }

    // ========================================================================
    // Phase 2: findByPersonId
    // ========================================================================

    @Test
    void testFindByPersonId_found() {
        try (MockedStatic<Student> studentStatic = Mockito.mockStatic(Student.class)) {

            Student studentMock = mock(Student.class);
            studentStatic.when(() -> Student.findFirst("id_person = ?", 1L))
                    .thenReturn(studentMock);

            Student result = repository.findByPersonId(1L);

            assertNotNull(result);
            assertSame(studentMock, result);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testFindByPersonId_notFound() {
        try (MockedStatic<Student> studentStatic = Mockito.mockStatic(Student.class)) {

            studentStatic.when(() -> Student.findFirst("id_person = ?", 999L))
                    .thenReturn(null);

            Student result = repository.findByPersonId(999L);

            assertNull(result);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testFindByPersonId_nullId() {
        Student result = repository.findByPersonId(null);
        assertNull(result);
    }

    // ========================================================================
    // Phase 2: getCurrentEnrollments
    // ========================================================================

    @Test
    void testGetCurrentEnrollments_withResults() {
        try (MockedStatic<Enrollment> enrollmentStatic = Mockito.mockStatic(Enrollment.class)) {

            Enrollment enroll1 = mock(Enrollment.class);
            when(enroll1.getSubjectId()).thenReturn(10L);
            when(enroll1.getStatus()).thenReturn("ENROLLED");
            when(enroll1.getPeriod()).thenReturn("2024-01");

            Enrollment enroll2 = mock(Enrollment.class);
            when(enroll2.getSubjectId()).thenReturn(20L);
            when(enroll2.getStatus()).thenReturn("ENROLLED");
            when(enroll2.getPeriod()).thenReturn("2024-01");

            LazyList<Enrollment> mockList = mock(LazyList.class);
            when(mockList.iterator()).thenReturn(Arrays.asList(enroll1, enroll2).iterator());

            enrollmentStatic.when(() -> Enrollment.where(
                    "student_id = ? AND status = 'ENROLLED'", 1L))
                    .thenReturn(mockList);

            List<EnrollmentDTO> results = repository.getCurrentEnrollments(1L);

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals(10L, results.get(0).getSubjectId());
            assertEquals("ENROLLED", results.get(0).getStatus());
            assertNull(results.get(0).getGrade());
            assertEquals("2024-01", results.get(0).getPeriod());
            assertEquals(20L, results.get(1).getSubjectId());

            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testGetCurrentEnrollments_empty() {
        try (MockedStatic<Enrollment> enrollmentStatic = Mockito.mockStatic(Enrollment.class)) {

            LazyList<Enrollment> emptyList = mock(LazyList.class);
            when(emptyList.iterator()).thenReturn(Collections.emptyIterator());

            enrollmentStatic.when(() -> Enrollment.where(
                    "student_id = ? AND status = 'ENROLLED'", 999L))
                    .thenReturn(emptyList);

            List<EnrollmentDTO> results = repository.getCurrentEnrollments(999L);

            assertNotNull(results);
            assertTrue(results.isEmpty());

            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testGetCurrentEnrollments_nullId() {
        List<EnrollmentDTO> result = repository.getCurrentEnrollments(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========================================================================
    // Phase 3: getAcademicHistory
    // ========================================================================

    @Test
    void testGetAcademicHistory_withResults() {
        try (MockedStatic<Base> baseStatic = Mockito.mockStatic(Base.class)) {

            Map<String, Object> row1 = new HashMap<>();
            row1.put("id_subject", 1L);
            row1.put("subject_name", "Matematica");
            row1.put("status", "COMPLETED");
            row1.put("grade", 7.5);
            row1.put("period", "2024-01");

            Map<String, Object> row2 = new HashMap<>();
            row2.put("id_subject", 2L);
            row2.put("subject_name", "Fisica");
            row2.put("status", "ENROLLED");
            row2.put("grade", null);
            row2.put("period", "2024-02");

            List<Map<String, Object>> rows = Arrays.asList(row1, row2);
            String expectedSql = "SELECT s.id_subject, s.subject_name, e.status, ev.grade, e.period "
                    + "FROM enrollments e "
                    + "JOIN subjects s ON e.subject_id = s.id_subject "
                    + "LEFT JOIN evaluations ev ON ev.enrollment_id = e.id "
                    + "WHERE e.student_id = ? "
                    + "ORDER BY e.period DESC";

            baseStatic.when(() -> Base.findAll(eq(expectedSql), eq(1L)))
                    .thenReturn(rows);

            List<SubjectDTO> results = repository.getAcademicHistory(1L);

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals(1L, results.get(0).getSubjectId());
            assertEquals("Matematica", results.get(0).getSubjectName());
            assertEquals("COMPLETED", results.get(0).getStatus());
            assertEquals(7.5, results.get(0).getGrade());
            assertEquals("2024-01", results.get(0).getPeriod());

            assertEquals(2L, results.get(1).getSubjectId());
            assertEquals("Fisica", results.get(1).getSubjectName());
            assertEquals("ENROLLED", results.get(1).getStatus());
            assertNull(results.get(1).getGrade());
            assertEquals("2024-02", results.get(1).getPeriod());

            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testGetAcademicHistory_empty() {
        try (MockedStatic<Base> baseStatic = Mockito.mockStatic(Base.class)) {

            String expectedSql = "SELECT s.id_subject, s.subject_name, e.status, ev.grade, e.period "
                    + "FROM enrollments e "
                    + "JOIN subjects s ON e.subject_id = s.id_subject "
                    + "LEFT JOIN evaluations ev ON ev.enrollment_id = e.id "
                    + "WHERE e.student_id = ? "
                    + "ORDER BY e.period DESC";

            baseStatic.when(() -> Base.findAll(eq(expectedSql), eq(999L)))
                    .thenReturn(Collections.emptyList());

            List<SubjectDTO> results = repository.getAcademicHistory(999L);

            assertNotNull(results);
            assertTrue(results.isEmpty());

            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    // ========================================================================
    // Phase 3: getApprovedSubjects
    // ========================================================================

    @Test
    void testGetApprovedSubjects_withResults() {
        try (MockedStatic<Base> baseStatic = Mockito.mockStatic(Base.class)) {

            Map<String, Object> row1 = new HashMap<>();
            row1.put("id_subject", 1L);
            row1.put("subject_name", "Matematica");
            row1.put("status", "COMPLETED");
            row1.put("grade", 7.5);
            row1.put("period", "2024-01");

            Map<String, Object> row2 = new HashMap<>();
            row2.put("id_subject", 2L);
            row2.put("subject_name", "Fisica");
            row2.put("status", "COMPLETED");
            row2.put("grade", 4.0);
            row2.put("period", "2024-01");

            List<Map<String, Object>> rows = Arrays.asList(row1, row2);
            String expectedSql = "SELECT s.id_subject, s.subject_name, e.status, ev.grade, e.period "
                    + "FROM enrollments e "
                    + "JOIN subjects s ON e.subject_id = s.id_subject "
                    + "JOIN evaluations ev ON ev.enrollment_id = e.id "
                    + "WHERE e.student_id = ? AND ev.grade >= 4.0 "
                    + "ORDER BY e.period DESC";

            baseStatic.when(() -> Base.findAll(eq(expectedSql), eq(1L)))
                    .thenReturn(rows);

            List<SubjectDTO> results = repository.getApprovedSubjects(1L);

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals(1L, results.get(0).getSubjectId());
            assertEquals("Matematica", results.get(0).getSubjectName());
            assertEquals(7.5, results.get(0).getGrade());

            assertEquals(2L, results.get(1).getSubjectId());
            assertEquals("Fisica", results.get(1).getSubjectName());
            assertEquals(4.0, results.get(1).getGrade());

            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testGetApprovedSubjects_empty() {
        try (MockedStatic<Base> baseStatic = Mockito.mockStatic(Base.class)) {

            String expectedSql = "SELECT s.id_subject, s.subject_name, e.status, ev.grade, e.period "
                    + "FROM enrollments e "
                    + "JOIN subjects s ON e.subject_id = s.id_subject "
                    + "JOIN evaluations ev ON ev.enrollment_id = e.id "
                    + "WHERE e.student_id = ? AND ev.grade >= 4.0 "
                    + "ORDER BY e.period DESC";

            baseStatic.when(() -> Base.findAll(eq(expectedSql), eq(999L)))
                    .thenReturn(Collections.emptyList());

            List<SubjectDTO> results = repository.getApprovedSubjects(999L);

            assertNotNull(results);
            assertTrue(results.isEmpty());

            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    // ========================================================================
    // Phase 3: getGrades
    // ========================================================================

    @Test
    void testGetGrades_withResults() {
        try (MockedStatic<Base> baseStatic = Mockito.mockStatic(Base.class)) {

            Map<String, Object> row1 = new HashMap<>();
            row1.put("subject_name", "Matematica");
            row1.put("grade", 8.0);
            row1.put("evaluation_date", "2024-06-15");

            Map<String, Object> row2 = new HashMap<>();
            row2.put("subject_name", "Fisica");
            row2.put("grade", 7.0);
            row2.put("evaluation_date", "2024-07-01");

            List<Map<String, Object>> rows = Arrays.asList(row1, row2);
            String expectedSql = "SELECT s.subject_name, ev.grade, ev.evaluation_date "
                    + "FROM evaluations ev "
                    + "JOIN enrollments e ON ev.enrollment_id = e.id "
                    + "JOIN subjects s ON e.subject_id = s.id_subject "
                    + "WHERE e.student_id = ? "
                    + "ORDER BY ev.evaluation_date DESC";

            baseStatic.when(() -> Base.findAll(eq(expectedSql), eq(1L)))
                    .thenReturn(rows);

            List<GradeDTO> results = repository.getGrades(1L);

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("Matematica", results.get(0).getSubjectName());
            assertEquals(8.0, results.get(0).getGrade());
            assertEquals("2024-06-15", results.get(0).getDate());

            assertEquals("Fisica", results.get(1).getSubjectName());
            assertEquals(7.0, results.get(1).getGrade());
            assertEquals("2024-07-01", results.get(1).getDate());

            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testGetGrades_empty() {
        try (MockedStatic<Base> baseStatic = Mockito.mockStatic(Base.class)) {

            String expectedSql = "SELECT s.subject_name, ev.grade, ev.evaluation_date "
                    + "FROM evaluations ev "
                    + "JOIN enrollments e ON ev.enrollment_id = e.id "
                    + "JOIN subjects s ON e.subject_id = s.id_subject "
                    + "WHERE e.student_id = ? "
                    + "ORDER BY ev.evaluation_date DESC";

            baseStatic.when(() -> Base.findAll(eq(expectedSql), eq(999L)))
                    .thenReturn(Collections.emptyList());

            List<GradeDTO> results = repository.getGrades(999L);

            assertNotNull(results);
            assertTrue(results.isEmpty());

            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }
}
