package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para el modelo TeacherAssignment.
 * Verifica getters/setters y ciclo save/load contra SQLite.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeacherAssignmentTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-teacherassignment-model.db";

    @BeforeAll
    void setupDatabase() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE teachers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_persona INTEGER NOT NULL, " +
                "nroLegajo VARCHAR(30) NOT NULL UNIQUE)");
        Base.exec("CREATE TABLE subjects (" +
                "id_subject INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "code TEXT NOT NULL UNIQUE, " +
                "subject_name TEXT NOT NULL)");
        Base.exec("CREATE TABLE teacher_assignments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "teacher_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL, " +
                "role VARCHAR(30), " +
                "period TEXT)");
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
    }

    @AfterEach
    void closeConnection() {
        Base.close();
    }

    @Test
    void setAndGetTeacherId() {
        TeacherAssignment ta = new TeacherAssignment();
        ta.setTeacherId(1L);
        assertEquals(Long.valueOf(1L), ta.getTeacherId());
    }

    @Test
    void setAndGetSubjectId() {
        TeacherAssignment ta = new TeacherAssignment();
        ta.setSubjectId(2L);
        assertEquals(Long.valueOf(2L), ta.getSubjectId());
    }

    @Test
    void setAndGetRole() {
        TeacherAssignment ta = new TeacherAssignment();
        ta.setRole(TeacherRole.RESPONSABLE);
        assertEquals(TeacherRole.RESPONSABLE, ta.getRole());
    }

    @Test
    void setRoleToNull_returnsNull() {
        TeacherAssignment ta = new TeacherAssignment();
        ta.setRole(null);
        assertNull(ta.getRole());
    }

    @Test
    void setAndGetPeriod() {
        TeacherAssignment ta = new TeacherAssignment();
        ta.setPeriod("2024-1S");
        assertEquals("2024-1S", ta.getPeriod());
    }

    @Test
    void saveAndLoadTeacherAssignment() {
        Base.exec("INSERT INTO teachers(id, id_persona, nroLegajo) VALUES (1, 1, 'L001')");
        Base.exec("INSERT INTO subjects(id_subject, code, subject_name) VALUES (1, 'COD1', 'Subject 1')");

        TeacherAssignment ta = new TeacherAssignment();
        ta.setTeacherId(1L);
        ta.setSubjectId(1L);
        ta.setRole(TeacherRole.JTP);
        ta.setPeriod("2024-2S");
        ta.saveIt();

        assertNotNull(ta.getId());

        TeacherAssignment loaded = TeacherAssignment.findById(ta.getId());
        assertNotNull(loaded);
        assertEquals(Long.valueOf(1L), loaded.getTeacherId());
        assertEquals(Long.valueOf(1L), loaded.getSubjectId());
        assertEquals(TeacherRole.JTP, loaded.getRole());
        assertEquals("2024-2S", loaded.getPeriod());
    }

    @AfterAll
    void teardown() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("DROP TABLE IF EXISTS teacher_assignments");
        Base.exec("DROP TABLE IF EXISTS subjects");
        Base.exec("DROP TABLE IF EXISTS teachers");
        Base.close();
        new java.io.File("target/test-teacherassignment-model.db").delete();
    }
}
