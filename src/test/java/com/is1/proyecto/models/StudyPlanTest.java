package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para el modelo StudyPlan.
 * Verifica getters/setters y ciclo save/load contra SQLite.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudyPlanTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-studyplan-model.db";

    @BeforeAll
    void setupDatabase() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE careers (" +
                "id_careers INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "career_name TEXT NOT NULL, " +
                "career_duration INTEGER NOT NULL)");
        Base.exec("CREATE TABLE study_plans (" +
                "id_study_plan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "year INTEGER NOT NULL, " +
                "id_career INTEGER)");
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
    void setAndGetName() {
        StudyPlan sp = new StudyPlan();
        sp.setName("Plan 2024");
        assertEquals("Plan 2024", sp.getName());
    }

    @Test
    void setAndGetYear() {
        StudyPlan sp = new StudyPlan();
        sp.setYear(2024);
        assertEquals(Integer.valueOf(2024), sp.getYear());
    }

    @Test
    void setAndGetCareerId() {
        StudyPlan sp = new StudyPlan();
        sp.setCareerId(5);
        assertEquals(Integer.valueOf(5), sp.getCareerId());
    }

    @Test
    void idIsNullBeforeSave() {
        StudyPlan sp = new StudyPlan();
        assertNull(sp.getId());
    }

    @Test
    void saveAndLoadStudyPlan() {
        Career career = new Career();
        career.setCareerName("Ingeniería");
        career.setCareerDuration(5);
        career.saveIt();

        StudyPlan sp = new StudyPlan();
        sp.setName("Plan 2024");
        sp.setYear(2024);
        sp.setCareerId(career.getId());
        sp.saveIt();

        assertNotNull(sp.getId());

        StudyPlan loaded = StudyPlan.findById(sp.getId());
        assertNotNull(loaded);
        assertEquals("Plan 2024", loaded.getName());
        assertEquals(Integer.valueOf(2024), loaded.getYear());
        assertEquals(career.getId(), loaded.getCareerId());
    }

    @AfterAll
    void teardown() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("DROP TABLE IF EXISTS study_plans");
        Base.exec("DROP TABLE IF EXISTS careers");
        Base.close();
        new java.io.File("target/test-studyplan-model.db").delete();
    }
}
