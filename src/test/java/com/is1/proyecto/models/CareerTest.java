package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para el modelo Career.
 * Verifica getters/setters y ciclo save/load contra SQLite.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CareerTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-career-model.db";

    @BeforeAll
    void setupDatabase() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE careers (" +
                "id_careers INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "career_name TEXT NOT NULL, " +
                "career_duration INTEGER NOT NULL)");
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
    void setAndGetCareerName() {
        Career c = new Career();
        c.setCareerName("Ingeniería");
        assertEquals("Ingeniería", c.getCareerName());
    }

    @Test
    void setAndGetCareerDuration() {
        Career c = new Career();
        c.setCareerDuration(5);
        assertEquals(Integer.valueOf(5), c.getCareerDuration());
    }

    @Test
    void idIsNullBeforeSave() {
        Career c = new Career();
        assertNull(c.getId());
    }

    @Test
    void saveAndLoadCareer() {
        Career c = new Career();
        c.setCareerName("Licenciatura");
        c.setCareerDuration(4);
        c.saveIt();

        assertNotNull(c.getId());

        Career loaded = Career.findById(c.getId());
        assertNotNull(loaded);
        assertEquals("Licenciatura", loaded.getCareerName());
        assertEquals(Integer.valueOf(4), loaded.getCareerDuration());
    }

    @AfterAll
    void teardown() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("DROP TABLE IF EXISTS careers");
        Base.close();
        new java.io.File("target/test-career-model.db").delete();
    }
}
