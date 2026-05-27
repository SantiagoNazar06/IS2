package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para Condition model.
 * Verifica getters/setters contra una DB SQLite real.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConditionTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-condition-model.db";

    @BeforeAll
    void setupDatabase() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("PRAGMA foreign_keys = ON");
        Base.exec("CREATE TABLE subjects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_name TEXT NOT NULL)");
        Base.exec("CREATE TABLE conditions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_id INTEGER NOT NULL REFERENCES subjects(id), " +
                "prerequisite_subject_id INTEGER NOT NULL REFERENCES subjects(id), " +
                "type VARCHAR(20) NOT NULL DEFAULT 'REGULAR', " +
                "CHECK(type IN ('REGULAR', 'APROBADA')), " +
                "CHECK(subject_id != prerequisite_subject_id))");
        Base.exec("INSERT INTO subjects(id, subject_name) VALUES (1, 'Algebra')");
        Base.exec("INSERT INTO subjects(id, subject_name) VALUES (2, 'Analisis')");
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("PRAGMA foreign_keys = ON");
    }

    @AfterEach
    void closeConnection() {
        Base.close();
    }

    @Test
    void setAndGetSubjectId() {
        Condition c = new Condition();
        c.setSubjectId(1);
        assertEquals(Integer.valueOf(1), c.getSubjectId());
    }

    @Test
    void setAndGetPrerequisiteSubjectId() {
        Condition c = new Condition();
        c.setPrerequisiteSubjectId(2);
        assertEquals(Integer.valueOf(2), c.getPrerequisiteSubjectId());
    }

    @Test
    void setAndGetType() {
        Condition c = new Condition();
        c.setType(ConditionType.REGULAR);
        assertEquals(ConditionType.REGULAR, c.getType());
    }

    @Test
    void setAndGetType_aprobada() {
        Condition c = new Condition();
        c.setType(ConditionType.APROBADA);
        assertEquals(ConditionType.APROBADA, c.getType());
    }

    @Test
    void saveAndLoadCondition() {
        Condition c = new Condition();
        c.setSubjectId(1);
        c.setPrerequisiteSubjectId(2);
        c.setType(ConditionType.REGULAR);
        c.saveIt();

        assertNotNull(c.getId());

        Condition loaded = Condition.findById(c.getId());
        assertNotNull(loaded);
        assertEquals(Integer.valueOf(1), loaded.getSubjectId());
        assertEquals(Integer.valueOf(2), loaded.getPrerequisiteSubjectId());
        assertEquals(ConditionType.REGULAR, loaded.getType());
    }

    @Test
    void defaultTypeIsRegular() {
        Condition c = new Condition();
        c.setSubjectId(2);
        c.setPrerequisiteSubjectId(1);
        c.saveIt();

        Condition loaded = Condition.findById(c.getId());
        assertEquals(ConditionType.REGULAR, loaded.getType());
    }

    @AfterAll
    void teardown() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("DROP TABLE IF EXISTS conditions");
        Base.exec("DROP TABLE IF EXISTS subjects");
        Base.close();
        new java.io.File("target/test-condition-model.db").delete();
    }
}
