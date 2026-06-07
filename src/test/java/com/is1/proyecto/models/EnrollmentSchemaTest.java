package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnrollmentSchemaTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-enrollment-schema.db";

    @BeforeAll
    void setupDatabase() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("PRAGMA foreign_keys = ON");

        Base.exec("CREATE TABLE students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_person INTEGER NOT NULL, " +
                "student_type TEXT NOT NULL" +
                ")");

        Base.exec("CREATE TABLE subjects (" +
                "id_subject INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_name TEXT NOT NULL" +
                ")");

        Base.exec("CREATE TABLE enrollments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL REFERENCES students(id), " +
                "subject_id INTEGER NOT NULL REFERENCES subjects(id_subject), " +
                "period TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'ENROLLED' " +
                "CHECK(status IN('ENROLLED','DROPPED','COMPLETED','CANCELLED')), " +
                "created_at TEXT NOT NULL DEFAULT (datetime('now')), " +
                "UNIQUE(student_id, subject_id, period)" +
                ")");
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("PRAGMA foreign_keys = ON");
    }

    @AfterEach
    void closeConnection() {
        Base.exec("DELETE FROM enrollments");
        Base.exec("DELETE FROM subjects");
        Base.exec("DELETE FROM students");
        Base.close();
    }

    @AfterAll
    void tearDown() {
        new File("target/test-enrollment-schema.db").delete();
    }

    @Test
    void tableExists() {
        Object result = Base.firstCell(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                "enrollments");
        assertEquals("enrollments", result);
    }

    @Test
    void primaryKey_isIdColumn() {
        Object pkCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('enrollments') WHERE pk=1");
        assertEquals("id", pkCol);
    }

    @Test
    void allRequiredColumnsExist() {
        Object idCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('enrollments') WHERE name='id'");
        assertEquals("id", idCol);

        Object studentIdCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('enrollments') WHERE name='student_id'");
        assertEquals("student_id", studentIdCol);

        Object subjectIdCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('enrollments') WHERE name='subject_id'");
        assertEquals("subject_id", subjectIdCol);

        Object periodCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('enrollments') WHERE name='period'");
        assertEquals("period", periodCol);

        Object statusCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('enrollments') WHERE name='status'");
        assertEquals("status", statusCol);

        Object createdAtCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('enrollments') WHERE name='created_at'");
        assertEquals("created_at", createdAtCol);
    }

    @Test
    void notNull_studentId() {
        Base.exec("INSERT INTO students (id, id_person, student_type) VALUES (1, 1, 'REGULAR')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (NULL, 1, '2025-1', 'ENROLLED')"));
    }

    @Test
    void notNull_subjectId() {
        Base.exec("INSERT INTO students (id, id_person, student_type) VALUES (1, 1, 'REGULAR')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, NULL, '2025-1', 'ENROLLED')"));
    }

    @Test
    void notNull_period() {
        Base.exec("INSERT INTO students (id, id_person, student_type) VALUES (1, 1, 'REGULAR')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, 1, NULL, 'ENROLLED')"));
    }

    @Test
    void foreignKey_studentIdReferencesStudents() {
        Object refTable = Base.firstCell(
                "SELECT \"table\" FROM pragma_foreign_key_list('enrollments') WHERE \"from\"='student_id'");
        assertEquals("students", refTable);
    }

    @Test
    void foreignKey_subjectIdReferencesSubjects() {
        Object refTable = Base.firstCell(
                "SELECT \"table\" FROM pragma_foreign_key_list('enrollments') WHERE \"from\"='subject_id'");
        assertEquals("subjects", refTable);
    }

    @Test
    void foreignKey_enforced_studentId() {
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (999, 1, '2025-1', 'ENROLLED')"));
    }

    @Test
    void foreignKey_enforced_subjectId() {
        Base.exec("INSERT INTO students (id, id_person, student_type) VALUES (1, 1, 'REGULAR')");
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, 999, '2025-1', 'ENROLLED')"));
    }

    @Test
    void checkConstraint_allowsValidStatuses() {
        Base.exec("INSERT INTO students (id, id_person, student_type) VALUES (1, 1, 'REGULAR')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");

        assertDoesNotThrow(() ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, 1, '2025-1', 'ENROLLED')"));
        assertDoesNotThrow(() ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, 1, '2025-2', 'DROPPED')"));
        assertDoesNotThrow(() ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, 1, '2025-3', 'COMPLETED')"));
        assertDoesNotThrow(() ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, 1, '2025-4', 'CANCELLED')"));
    }

    @Test
    void checkConstraint_rejectsInvalidStatus() {
        Base.exec("INSERT INTO students (id, id_person, student_type) VALUES (1, 1, 'REGULAR')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");

        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, 1, '2025-1', 'INVALID')"));
    }

    @Test
    void uniqueConstraint_preventsDuplicateEnrollment() {
        Base.exec("INSERT INTO students (id, id_person, student_type) VALUES (1, 1, 'REGULAR')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");

        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, 1, '2025-1', 'ENROLLED')");
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, 1, '2025-1', 'ENROLLED')"));
    }

    @Test
    void insertAndReadValidEnrollment() {
        Base.exec("INSERT INTO students (id, id_person, student_type) VALUES (1, 1, 'REGULAR')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");

        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?, ?, ?, ?)",
                1, 1, "2025-1", "ENROLLED");

        Object idObj = Base.firstCell("SELECT id FROM enrollments WHERE student_id=1 AND subject_id=1 AND period='2025-1'");
        assertNotNull(idObj);
        Long insertedId = ((Number) idObj).longValue();
        String status = Base.firstCell("SELECT status FROM enrollments WHERE id=?", insertedId).toString();
        assertEquals("ENROLLED", status);
    }

    @Test
    void defaultStatusIsEnrolled() {
        Base.exec("INSERT INTO students (id, id_person, student_type) VALUES (1, 1, 'REGULAR')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");

        Base.exec("INSERT INTO enrollments (student_id, subject_id, period) VALUES (1, 1, '2025-1')");

        String status = Base.firstCell(
                "SELECT status FROM enrollments WHERE student_id=1 AND subject_id=1 AND period='2025-1'").toString();
        assertEquals("ENROLLED", status);
    }

    @Test
    void createdAt_hasDefaultValue() {
        Base.exec("INSERT INTO students (id, id_person, student_type) VALUES (1, 1, 'REGULAR')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");

        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (1, 1, '2025-1', 'ENROLLED')");

        String createdAt = Base.firstCell(
                "SELECT created_at FROM enrollments WHERE student_id=1 AND subject_id=1 AND period='2025-1'").toString();
        assertNotNull(createdAt);
        assertFalse(createdAt.isEmpty());
    }

    @Test
    void migration_createTable_idempotent() {
        assertDoesNotThrow(() ->
                Base.exec("CREATE TABLE IF NOT EXISTS enrollments (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "student_id INTEGER NOT NULL REFERENCES students(id), " +
                        "subject_id INTEGER NOT NULL REFERENCES subjects(id_subject), " +
                        "period TEXT NOT NULL, " +
                        "status TEXT NOT NULL DEFAULT 'ENROLLED' " +
                        "CHECK(status IN('ENROLLED','DROPPED','COMPLETED','CANCELLED')), " +
                        "created_at TEXT NOT NULL DEFAULT (datetime('now')), " +
                        "UNIQUE(student_id, subject_id, period))"));
    }

    @Test
    void migration_createIndexes_idempotent() {
        assertDoesNotThrow(() ->
                Base.exec("CREATE INDEX IF NOT EXISTS idx_enrollments_student_id ON enrollments(student_id)"));
        assertDoesNotThrow(() ->
                Base.exec("CREATE INDEX IF NOT EXISTS idx_enrollments_student_id ON enrollments(student_id)"));
        assertDoesNotThrow(() ->
                Base.exec("CREATE INDEX IF NOT EXISTS idx_enrollments_subject_id ON enrollments(subject_id)"));
        assertDoesNotThrow(() ->
                Base.exec("CREATE INDEX IF NOT EXISTS idx_enrollments_subject_id ON enrollments(subject_id)"));
    }
}
