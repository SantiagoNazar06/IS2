package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Enrollment;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnrollmentRepositoryTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-enrollment-repo.db";
    private EnrollmentRepository repo;

    private long studentId;
    private long otherStudentId;
    private long subjectId;
    private long otherSubjectId;

    @BeforeAll
    void setupDatabase() {
        new File("./target/test-enrollment-repo.db").delete();
        System.setProperty("db.url", JDBC_URL);
        repo = new EnrollmentRepository();
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("PRAGMA foreign_keys = ON");
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
                "status TEXT NOT NULL DEFAULT 'ENROLLED', " +
                "created_at TEXT NOT NULL DEFAULT (datetime('now')), " +
                "UNIQUE(student_id, subject_id, period))");
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
        studentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO students (id_person, student_type) VALUES (?,?)", 2, "REGULAR");
        otherStudentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Matematica");
        subjectId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Historia");
        otherSubjectId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?,?,?,?)",
                studentId, subjectId, "2025-1", "ENROLLED");
        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?,?,?,?)",
                studentId, otherSubjectId, "2025-1", "ENROLLED");
        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?,?,?,?)",
                studentId, subjectId, "2024-2", "COMPLETED");
        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?,?,?,?)",
                otherStudentId, subjectId, "2025-1", "ENROLLED");
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM enrollments");
        Base.exec("DELETE FROM subjects");
        Base.exec("DELETE FROM students");
    }

    @AfterAll
    void tearDown() {
        new File("./target/test-enrollment-repo.db").delete();
    }

    @Test
    void findByStudentAndPeriod_returnsMatchingEnrollments() {
        List<Enrollment> results = repo.findByStudentAndPeriod(studentId, "2025-1");

        assertEquals(2, results.size());
        for (Enrollment e : results) {
            assertEquals(studentId, e.getStudentId());
            assertEquals("2025-1", e.getPeriod());
        }
    }

    @Test
    void findByStudentAndPeriod_returnsEmpty_whenNoMatch() {
        List<Enrollment> results = repo.findByStudentAndPeriod(studentId, "2099-1");
        assertTrue(results.isEmpty());
    }

    @Test
    void findByStudentAndPeriod_doesNotReturnOtherStudents() {
        List<Enrollment> results = repo.findByStudentAndPeriod(otherStudentId, "2025-1");

        assertEquals(1, results.size());
        assertEquals(otherStudentId, results.get(0).getStudentId());
    }

    @Test
    void findById_returnsEnrollment() {
        Object idObj = Base.firstCell("SELECT id FROM enrollments WHERE student_id=? AND subject_id=? AND period=?",
                studentId, subjectId, "2025-1");
        assertNotNull(idObj);
        Long firstId = ((Number) idObj).longValue();

        Enrollment result = repo.findById(firstId.intValue());

        assertNotNull(result);
        assertEquals(studentId, result.getStudentId());
        assertEquals(subjectId, result.getSubjectId());
    }

    @Test
    void findById_returnsNull_whenNotFound() {
        Enrollment result = repo.findById(99999);
        assertNull(result);
    }

    @Test
    void findByStudent_returnsAllForStudent() {
        List<Enrollment> results = repo.findByStudent(studentId);

        assertEquals(3, results.size());
        for (Enrollment e : results) {
            assertEquals(studentId, e.getStudentId());
        }
    }

    @Test
    void findBySubject_returnsAllForSubject() {
        List<Enrollment> results = repo.findBySubject(subjectId);

        assertEquals(3, results.size());
        for (Enrollment e : results) {
            assertEquals(subjectId, e.getSubjectId());
        }
    }

    @Test
    void create_persistsEnrollment() {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(otherStudentId);
        enrollment.setSubjectId(otherSubjectId);
        enrollment.setPeriod("2026-1");
        enrollment.setStatus("ENROLLED");

        Enrollment created = repo.create(enrollment);

        assertNotNull(created.getId());
        assertEquals("ENROLLED", created.getStatus());

        Enrollment fetched = Enrollment.findFirst("id = ?", created.getId());
        assertNotNull(fetched);
        assertEquals(otherStudentId, fetched.getStudentId());
    }
}
