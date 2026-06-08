package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Evaluation;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for EvaluationRepository.
 * Uses a separate SQLite database file (target/test-evaluation-repo.db).
 * Tests CRUD operations and student/subject grade queries.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EvaluationRepositoryTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-evaluation-repo.db";
    private EvaluationRepository repo;

    private int eval1Id;
    private int eval2Id;
    private int enrollmentId;
    private int otherEnrollmentId;
    private int studentId;
    private int subjectId;

    @BeforeAll
    void setupDatabase() {
        new File("./target/test-evaluation-repo.db").delete();
        System.setProperty("db.url", JDBC_URL);
        repo = new EvaluationRepository();
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE IF NOT EXISTS students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_person INTEGER NOT NULL, " +
                "student_type TEXT NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS subjects (" +
                "id_subject INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_name TEXT NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS enrollments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL, " +
                "period TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'ENROLLED')");
        Base.exec("CREATE TABLE IF NOT EXISTS evaluations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "evaluation_date DATE, " +
                "grade DOUBLE, " +
                "enrollment_id INTEGER)");
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        try { Base.close(); } catch (Exception ignored) { }
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
    }

    @BeforeEach
    void insertTestData() {
        Base.exec("INSERT INTO students (id_person, student_type) VALUES (?,?)", 1, "REGULAR");
        studentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Matematica");
        subjectId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Historia");

        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?,?,?,?)",
                studentId, subjectId, "2024-01", "COMPLETED");
        enrollmentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        // Other student for subject — tests findBySubject
        Base.exec("INSERT INTO students (id_person, student_type) VALUES (?,?)", 2, "REGULAR");
        int otherStudentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();
        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?,?,?,?)",
                otherStudentId, subjectId, "2024-01", "COMPLETED");
        otherEnrollmentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        // Evaluations
        Base.exec("INSERT INTO evaluations (evaluation_date, grade, enrollment_id) VALUES (?,?,?)",
                Date.valueOf("2024-02-01"), 7.5, enrollmentId);
        eval1Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        Base.exec("INSERT INTO evaluations (evaluation_date, grade, enrollment_id) VALUES (?,?,?)",
                Date.valueOf("2024-02-15"), 4.0, otherEnrollmentId);
        eval2Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM evaluations");
        Base.exec("DELETE FROM enrollments");
        Base.exec("DELETE FROM subjects");
        Base.exec("DELETE FROM students");
        Base.close();
    }

    @AfterAll
    void teardown() {
        try { Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
            Base.exec("DROP TABLE IF EXISTS evaluations");
            Base.exec("DROP TABLE IF EXISTS enrollments");
            Base.exec("DROP TABLE IF EXISTS subjects");
            Base.exec("DROP TABLE IF EXISTS students");
            Base.close();
        } catch (Exception ignored) { }
        new File("./target/test-evaluation-repo.db").delete();
    }

    // =============================
    // findById
    // =============================
    @Test
    void findById_existingId_returnsEvaluation() {
        Evaluation result = repo.findById(eval1Id);
        assertNotNull(result);
        assertEquals(7.5, result.getEvaluationGrade(), 0.001);
    }

    @Test
    void findById_unknownId_returnsNull() {
        assertNull(repo.findById(9999));
    }

    // =============================
    // findByEnrollmentId
    // =============================
    @Test
    void findByEnrollmentId_existingEnrollment_returnsEvaluation() {
        Evaluation result = repo.findByEnrollmentId(enrollmentId);
        assertNotNull(result);
        assertEquals(7.5, result.getEvaluationGrade(), 0.001);
    }

    @Test
    void findByEnrollmentId_unknownEnrollment_returnsNull() {
        assertNull(repo.findByEnrollmentId(9999));
    }

    // =============================
    // createEvaluation
    // =============================
    @Test
    void createEvaluation_newEval_savesAndReturns() {
        Evaluation newEval = new Evaluation();
        newEval.setEvaluationGrade(8.0);
        newEval.setEvaluationEnrollementId(enrollmentId);
        newEval.setEvaluationDate(Date.valueOf("2024-03-01"));

        Evaluation result = repo.createEvaluation(newEval);

        assertNotNull(result);
        assertNotNull(result.getEvaluationId());
        assertEquals(8.0, result.getEvaluationGrade(), 0.001);

        Evaluation loaded = repo.findById(result.getEvaluationId());
        assertNotNull(loaded);
        assertEquals(8.0, loaded.getEvaluationGrade(), 0.001);
    }

    // =============================
    // findByStudent
    // =============================
    @Test
    void findByStudent_studentWithEvals_returnsList() {
        List<Evaluation> result = repo.findByStudent(studentId);
        assertEquals(1, result.size());
        assertEquals(7.5, result.get(0).getEvaluationGrade(), 0.001);
    }

    @Test
    void findByStudent_unknownStudent_returnsEmptyList() {
        List<Evaluation> result = repo.findByStudent(9999);
        assertTrue(result.isEmpty());
    }

    // =============================
    // findBySubject
    // =============================
    @Test
    void findBySubject_subjectWithEvals_returnsList() {
        List<Evaluation> result = repo.findBySubject(subjectId);
        assertEquals(2, result.size());
    }

    @Test
    void findBySubject_unknownSubject_returnsEmptyList() {
        List<Evaluation> result = repo.findBySubject(9999);
        assertTrue(result.isEmpty());
    }

    // =============================
    // findAll
    // =============================
    @Test
    void findAll_returnsAllEvaluations() {
        List<Evaluation> result = repo.findAll();
        assertEquals(2, result.size());
    }
}
