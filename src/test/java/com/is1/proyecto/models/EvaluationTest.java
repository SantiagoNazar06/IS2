package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para el modelo Evaluation.
 * Verifica getters/setters y ciclo save/load contra SQLite.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EvaluationTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-evaluation-model.db";

    @BeforeAll
    void setupDatabase() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE evaluations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "evaluation_date DATE, " +
                "grade DOUBLE, " +
                "enrollment_id INTEGER)");
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
    void setAndGetEvaluationGrade() {
        Evaluation e = new Evaluation();
        e.setEvaluationGrade(8.5);
        assertEquals(8.5, e.getEvaluationGrade());
    }

    @Test
    void setAndGetEvaluationDate() {
        Evaluation e = new Evaluation();
        Date date = Date.valueOf("2024-06-01");
        e.setEvaluationDate(date);
        assertEquals(date, e.getEvaluationDate());
    }

    @Test
    void setAndGetEnrollmentId() {
        Evaluation e = new Evaluation();
        e.setEvaluationEnrollementId(42);
        assertEquals(Integer.valueOf(42), e.getEvaluationEnrollementId());
    }

    @Test
    void evaluationIdIsNullBeforeSave() {
        Evaluation e = new Evaluation();
        assertNull(e.getEvaluationId());
    }

    @Test
    void saveAndLoadEvaluation() {
        Evaluation e = new Evaluation();
        e.setEvaluationGrade(7.0);
        e.setEvaluationEnrollementId(1);
        e.saveIt();

        assertNotNull(e.getEvaluationId());

        Evaluation loaded = Evaluation.findById(e.getEvaluationId());
        assertNotNull(loaded);
        assertEquals(7.0, loaded.getEvaluationGrade(), 0.001);
        assertEquals(Integer.valueOf(1), loaded.getEvaluationEnrollementId());
    }

    @AfterAll
    void teardown() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("DROP TABLE IF EXISTS evaluations");
        Base.close();
        new java.io.File("target/test-evaluation-model.db").delete();
    }
}
