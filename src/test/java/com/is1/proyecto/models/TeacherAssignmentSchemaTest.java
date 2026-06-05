package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeacherAssignmentSchemaTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-teacher-assignment-schema.db";

    @BeforeAll
    void setupDatabase() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("PRAGMA foreign_keys = ON");

        Base.exec("CREATE TABLE subjects (" +
                "id_subject INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_name TEXT NOT NULL" +
                ")");

        Base.exec("CREATE TABLE teachers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nroLegajo VARCHAR(30) NOT NULL UNIQUE" +
                ")");

        Base.exec("CREATE TABLE teacher_assignments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "teacher_id INTEGER NOT NULL REFERENCES teachers(id), " +
                "subject_id INTEGER NOT NULL REFERENCES subjects(id_subject), " +
                "period VARCHAR(10) NOT NULL, " +
                "role VARCHAR(20) NOT NULL, " +
                "CHECK(role IN ('RESPONSABLE', 'JTP', 'AYUDANTE')), " +
                "UNIQUE(teacher_id, subject_id, period, role)" +
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
        Base.exec("DELETE FROM teacher_assignments");
        Base.exec("DELETE FROM teachers");
        Base.exec("DELETE FROM subjects");
        Base.close();
    }

    @AfterAll
    void tearDown() {
        new File("target/test-teacher-assignment-schema.db").delete();
    }

    @Test
    void tableExists() {
        Object result = Base.firstCell(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                "teacher_assignments");
        assertEquals("teacher_assignments", result);
    }

    @Test
    void primaryKey_isIdColumn() {
        Object pkCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('teacher_assignments') WHERE pk=1");
        assertEquals("id", pkCol);
    }

    @Test
    void allRequiredColumnsExist() {
        Object idCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('teacher_assignments') WHERE name='id'");
        assertEquals("id", idCol);

        Object teacherIdCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('teacher_assignments') WHERE name='teacher_id'");
        assertEquals("teacher_id", teacherIdCol);

        Object subjectIdCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('teacher_assignments') WHERE name='subject_id'");
        assertEquals("subject_id", subjectIdCol);

        Object periodCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('teacher_assignments') WHERE name='period'");
        assertEquals("period", periodCol);

        Object roleCol = Base.firstCell(
                "SELECT name FROM pragma_table_info('teacher_assignments') WHERE name='role'");
        assertEquals("role", roleCol);
    }

    @Test
    void notNull_teacherId() {
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (NULL, 1, '2025-1', 'RESPONSABLE')"));
    }

    @Test
    void notNull_subjectId() {
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, NULL, '2025-1', 'RESPONSABLE')"));
    }

    @Test
    void notNull_period() {
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, NULL, 'RESPONSABLE')"));
    }

    @Test
    void notNull_role() {
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, '2025-1', NULL)"));
    }

    @Test
    void foreignKey_teacherIdReferencesTeachers() {
        Object refTable = Base.firstCell(
                "SELECT \"table\" FROM pragma_foreign_key_list('teacher_assignments') WHERE \"from\"='teacher_id'");
        assertEquals("teachers", refTable);
    }

    @Test
    void foreignKey_subjectIdReferencesSubjects() {
        Object refTable = Base.firstCell(
                "SELECT \"table\" FROM pragma_foreign_key_list('teacher_assignments') WHERE \"from\"='subject_id'");
        assertEquals("subjects", refTable);
    }

    @Test
    void foreignKey_enforced_teacherId() {
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (999, 1, '2025-1', 'RESPONSABLE')"));
    }

    @Test
    void foreignKey_enforced_subjectId() {
        Base.exec("INSERT INTO teachers (id, nroLegajo) VALUES (1, 'LEG-001')");
        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 999, '2025-1', 'RESPONSABLE')"));
    }

    @Test
    void checkConstraint_allowsValidRoles() {
        Base.exec("INSERT INTO teachers (id, nroLegajo) VALUES (1, 'LEG-001')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");

        assertDoesNotThrow(() ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, '2025-1', 'RESPONSABLE')"));
        assertDoesNotThrow(() ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, '2025-1', 'JTP')"));
        assertDoesNotThrow(() ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, '2025-1', 'AYUDANTE')"));
    }

    @Test
    void checkConstraint_rejectsInvalidRole() {
        Base.exec("INSERT INTO teachers (id, nroLegajo) VALUES (1, 'LEG-001')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");

        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, '2025-1', 'INVALID_ROLE')"));
    }

    @Test
    void multipleRoles_allowedForSameTeacherSubjectPeriod() {
        Base.exec("INSERT INTO teachers (id, nroLegajo) VALUES (1, 'LEG-001')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");

        assertDoesNotThrow(() ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, '2025-1', 'RESPONSABLE')"));
        assertDoesNotThrow(() ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, '2025-1', 'JTP')"));
        assertDoesNotThrow(() ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, '2025-1', 'AYUDANTE')"));
    }

    @Test
    void duplicateExact_throwsError() {
        Base.exec("INSERT INTO teachers (id, nroLegajo) VALUES (1, 'LEG-001')");
        Base.exec("INSERT INTO subjects (id_subject, subject_name) VALUES (1, 'Math')");
        Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, '2025-1', 'RESPONSABLE')");

        assertThrows(Exception.class, () ->
                Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, period, role) VALUES (1, 1, '2025-1', 'RESPONSABLE')"));
    }

    @Test
    void migration_createTable_idempotent() {
        assertDoesNotThrow(() ->
                Base.exec("CREATE TABLE IF NOT EXISTS teacher_assignments (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "teacher_id INTEGER NOT NULL REFERENCES teachers(id), " +
                        "subject_id INTEGER NOT NULL REFERENCES subjects(id_subject), " +
                        "period VARCHAR(10) NOT NULL, " +
                        "role VARCHAR(20) NOT NULL, " +
                        "CHECK(role IN ('RESPONSABLE', 'JTP', 'AYUDANTE')), " +
                        "UNIQUE(teacher_id, subject_id, period, role))"));
    }

    @Test
    void migration_createIndexes_idempotent() {
        assertDoesNotThrow(() ->
                Base.exec("CREATE INDEX IF NOT EXISTS idx_teacher_assignments_teacher_id ON teacher_assignments(teacher_id)"));
        assertDoesNotThrow(() ->
                Base.exec("CREATE INDEX IF NOT EXISTS idx_teacher_assignments_teacher_id ON teacher_assignments(teacher_id)"));
        assertDoesNotThrow(() ->
                Base.exec("CREATE INDEX IF NOT EXISTS idx_teacher_assignments_subject_id ON teacher_assignments(subject_id)"));
        assertDoesNotThrow(() ->
                Base.exec("CREATE INDEX IF NOT EXISTS idx_teacher_assignments_subject_id ON teacher_assignments(subject_id)"));
    }
}
