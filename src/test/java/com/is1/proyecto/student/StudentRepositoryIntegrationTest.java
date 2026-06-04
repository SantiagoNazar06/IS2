package com.is1.proyecto.student;

import com.is1.proyecto.dto.EnrollmentDTO;
import com.is1.proyecto.dto.GradeDTO;
import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.repositories.StudentRepository;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for StudentRepository.
 * Uses a separate SQLite database file (target/test-student.db).
 * Creates all needed tables, seeds test data, and verifies
 * all 6 repository methods against real database records.
 * <p>
 * Pattern follows PersonRepositoryIntegrationTest.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StudentRepositoryIntegrationTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-student.db";

    private StudentRepository repository;

    // IDs of seeded test records — reused across tests
    private Long studentId;
    private Long otherStudentId;
    private Long personId;
    private Long otherPersonId;

    @BeforeAll
    void setupDatabase() {
        System.setProperty("db.url", JDBC_URL);

        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");

        // Create tables matching the project schema
        Base.exec("CREATE TABLE IF NOT EXISTS persons (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "dni TEXT NOT NULL UNIQUE, " +
                "firstName TEXT NOT NULL, " +
                "lastName TEXT NOT NULL, " +
                "phone TEXT NOT NULL, " +
                "email TEXT NOT NULL" +
                ")");

        Base.exec("CREATE TABLE IF NOT EXISTS students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_person INTEGER NOT NULL, " +
                "student_type TEXT NOT NULL, " +
                "FOREIGN KEY (id_person) REFERENCES persons(id)" +
                ")");

        Base.exec("CREATE TABLE IF NOT EXISTS subjects (" +
                "id_subject INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_name TEXT NOT NULL" +
                ")");

        Base.exec("CREATE TABLE IF NOT EXISTS enrollments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL, " +
                "period TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'ENROLLED', " +
                "created_at TEXT NOT NULL, " +
                "FOREIGN KEY (student_id) REFERENCES students(id), " +
                "FOREIGN KEY (subject_id) REFERENCES subjects(id_subject), " +
                "UNIQUE(student_id, subject_id, period)" +
                ")");

        Base.exec("CREATE TABLE IF NOT EXISTS evaluations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "enrollment_id INTEGER NOT NULL UNIQUE, " +
                "grade DECIMAL(4,2) NOT NULL, " +
                "evaluation_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (enrollment_id) REFERENCES enrollments(id)" +
                ")");

        Base.close();
    }

    /**
     * Inserts a row and returns the auto-generated ID.
     * Uses parametrized query (with ? placeholders) because
     * Base.exec(String) and Base.exec(String, Object...) are different methods.
     */
    private long insertAndGetId(String sql, Object... params) {
        Base.exec(sql, params);
        List<Map> rows = Base.findAll("SELECT last_insert_rowid() AS id");
        return ((Number) rows.get(0).get("id")).longValue();
    }

    @BeforeEach
    void setUp() {
        // Close any leftover connection from a previous failed test
        try {
            Base.close();
        } catch (Exception ignored) {
        }

        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        repository = new StudentRepository();

        // Persons — use placeholder-style queries
        personId = insertAndGetId(
                "INSERT INTO persons (dni, firstName, lastName, phone, email) " +
                "VALUES (?, ?, ?, ?, ?)",
                "12345678", "Juan", "Perez", "3510000001", "juan@test.com");

        otherPersonId = insertAndGetId(
                "INSERT INTO persons (dni, firstName, lastName, phone, email) " +
                "VALUES (?, ?, ?, ?, ?)",
                "87654321", "Maria", "Gomez", "3510000002", "maria@test.com");

        // Students
        studentId = insertAndGetId(
                "INSERT INTO students (id_person, student_type) VALUES (?, ?)", personId, "REGULAR");

        otherStudentId = insertAndGetId(
                "INSERT INTO students (id_person, student_type) VALUES (?, ?)", otherPersonId, "REGULAR");

        // Subjects
        Long mathId = insertAndGetId(
                "INSERT INTO subjects (subject_name) VALUES (?)", "Matematica");
        Long physicsId = insertAndGetId(
                "INSERT INTO subjects (subject_name) VALUES (?)", "Fisica");
        Long historyId = insertAndGetId(
                "INSERT INTO subjects (subject_name) VALUES (?)", "Historia");

        // Enrollments for studentId:
        Long enrollMathId = insertAndGetId(
                "INSERT INTO enrollments (student_id, subject_id, period, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?)",
                studentId, mathId, "2024-01", "COMPLETED", "2024-01-10");

        insertAndGetId(
                "INSERT INTO enrollments (student_id, subject_id, period, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?)",
                studentId, physicsId, "2024-02", "ENROLLED", "2024-02-15");

        insertAndGetId(
                "INSERT INTO enrollments (student_id, subject_id, period, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?)",
                studentId, historyId, "2024-01", "DROPPED", "2024-01-20");

        Long enrollMath2Id = insertAndGetId(
                "INSERT INTO enrollments (student_id, subject_id, period, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?)",
                studentId, mathId, "2023-02", "COMPLETED", "2023-02-10");

        // Evaluations
        insertAndGetId(
                "INSERT INTO evaluations (enrollment_id, grade, evaluation_date) VALUES (?, ?, ?)",
                enrollMathId, 7.5, "2024-02-01");

        insertAndGetId(
                "INSERT INTO evaluations (enrollment_id, grade, evaluation_date) VALUES (?, ?, ?)",
                enrollMath2Id, 3.0, "2024-01-15");

        // One enrollment for otherStudentId (no evaluations)
        insertAndGetId(
                "INSERT INTO enrollments (student_id, subject_id, period, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?)",
                otherStudentId, mathId, "2024-01", "ENROLLED", "2024-01-05");
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM evaluations");
        Base.exec("DELETE FROM enrollments");
        Base.exec("DELETE FROM subjects");
        Base.exec("DELETE FROM students");
        Base.exec("DELETE FROM persons");
        Base.close();
    }

    @AfterAll
    void teardown() {
        try { Base.close(); } catch (Exception ignored) { }
        new File("target/test-student.db").delete();
    }

    // ========================================================================
    // Tests
    // ========================================================================

    @Test
    void testFindByDni_found() {
        Student result = repository.findByDni("12345678");
        assertNotNull(result);
        assertEquals(personId, result.getLong("id_person"));
    }

    @Test
    void testFindByDni_notFound() {
        Student result = repository.findByDni("NONEXISTENT");
        assertNull(result);
    }

    @Test
    void testFindByPersonId_found() {
        Student result = repository.findByPersonId(personId);
        assertNotNull(result);
    }

    @Test
    void testFindByPersonId_notFound() {
        Student result = repository.findByPersonId(99999L);
        assertNull(result);
    }

    @Test
    void testGetCurrentEnrollments_withActive() {
        List<EnrollmentDTO> results = repository.getCurrentEnrollments(studentId);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("ENROLLED", results.get(0).getStatus());
        assertNull(results.get(0).getGrade());
    }

    @Test
    void testGetCurrentEnrollments_emptyForUnknownStudent() {
        List<EnrollmentDTO> results = repository.getCurrentEnrollments(99999L);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetAcademicHistory_withHistory() {
        List<SubjectDTO> results = repository.getAcademicHistory(studentId);

        assertNotNull(results);
        assertEquals(4, results.size());

        for (SubjectDTO dto : results) {
            assertNotNull(dto.getSubjectName());
            assertNotNull(dto.getPeriod());
        }

        // Should be ordered by period DESC
        assertTrue(results.get(0).getPeriod().compareTo(results.get(3).getPeriod()) >= 0);
    }

    @Test
    void testGetAcademicHistory_noHistory() {
        List<SubjectDTO> results = repository.getAcademicHistory(99999L);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetApprovedSubjects_withApproved() {
        List<SubjectDTO> results = repository.getApprovedSubjects(studentId);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(7.5, results.get(0).getGrade(), 0.001);
    }

    @Test
    void testGetApprovedSubjects_noApproved() {
        List<SubjectDTO> results = repository.getApprovedSubjects(otherStudentId);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetGrades_withGrades() {
        List<GradeDTO> results = repository.getGrades(studentId);

        assertNotNull(results);
        assertEquals(2, results.size());

        for (GradeDTO dto : results) {
            assertNotNull(dto.getSubjectName());
            assertNotNull(dto.getGrade());
            assertNotNull(dto.getDate());
        }
    }

    @Test
    void testGetGrades_noGrades() {
        List<GradeDTO> results = repository.getGrades(otherStudentId);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testEdgeCases_nullInputs() {
        assertNull(repository.findByDni(null));
        assertNull(repository.findByPersonId(null));
        assertTrue(repository.getCurrentEnrollments(null).isEmpty());
        assertTrue(repository.getAcademicHistory(null).isEmpty());
        assertTrue(repository.getApprovedSubjects(null).isEmpty());
        assertTrue(repository.getGrades(null).isEmpty());
    }
}
