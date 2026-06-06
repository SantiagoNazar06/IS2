package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para el modelo TeacherSubject.
 * Verifica getters/setters y ciclo save/load contra SQLite.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeacherSubjectTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-teachersubject-model.db";

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
        Base.exec("CREATE TABLE teacher_subject (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "teacher_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL)");
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
        TeacherSubject ts = new TeacherSubject();
        ts.setTeacherId(1L);
        assertEquals(Long.valueOf(1L), ts.getTeacherId());
    }

    @Test
    void setAndGetSubjectId() {
        TeacherSubject ts = new TeacherSubject();
        ts.setSubjectId(2L);
        assertEquals(Long.valueOf(2L), ts.getSubjectId());
    }

    @Test
    void saveAndLoadTeacherSubject() {
        Base.exec("INSERT INTO teachers(id, id_persona, nroLegajo) VALUES (1, 1, 'L001')");
        Base.exec("INSERT INTO subjects(id_subject, code, subject_name) VALUES (1, 'COD1', 'Subject 1')");

        TeacherSubject ts = new TeacherSubject();
        ts.setTeacherId(1L);
        ts.setSubjectId(1L);
        ts.saveIt();

        assertNotNull(ts.getId());

        TeacherSubject loaded = TeacherSubject.findById(ts.getId());
        assertNotNull(loaded);
        assertEquals(Long.valueOf(1L), loaded.getTeacherId());
        assertEquals(Long.valueOf(1L), loaded.getSubjectId());
    }

    @Test
    void getTeacher_returnsAssociatedTeacher() {
        Base.exec("INSERT INTO teachers(id, id_persona, nroLegajo) VALUES (2, 2, 'L002')");
        Base.exec("INSERT INTO subjects(id_subject, code, subject_name) VALUES (2, 'COD2', 'Math')");

        TeacherSubject ts = new TeacherSubject();
        ts.setTeacherId(2L);
        ts.setSubjectId(2L);
        ts.saveIt();

        assertNotNull(ts.getId());

        Teacher teacher = ts.getTeacher();
        assertNotNull(teacher);
        assertEquals(2, teacher.getId());
    }

    @Test
    void getSubject_returnsAssociatedSubject() {
        Base.exec("INSERT INTO teachers(id, id_persona, nroLegajo) VALUES (3, 3, 'L003')");
        Base.exec("INSERT INTO subjects(id_subject, code, subject_name) VALUES (3, 'COD3', 'Physics')");

        TeacherSubject ts = new TeacherSubject();
        ts.setTeacherId(3L);
        ts.setSubjectId(3L);
        ts.saveIt();

        assertNotNull(ts.getId());

        Subject subject = ts.getSubject();
        assertNotNull(subject);
        assertEquals("Physics", subject.getSubjectName());
    }

    @AfterAll
    void teardown() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("DROP TABLE IF EXISTS teacher_subject");
        Base.exec("DROP TABLE IF EXISTS subjects");
        Base.exec("DROP TABLE IF EXISTS teachers");
        Base.close();
        new java.io.File("target/test-teachersubject-model.db").delete();
    }
}
