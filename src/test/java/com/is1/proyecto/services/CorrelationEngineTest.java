package com.is1.proyecto.services;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CorrelationEngineTest {

    private static final String DB_URL = "jdbc:sqlite:./target/correlation-test.db";
    private final CorrelationEngine engine = new CorrelationEngine();

    // IDs de datos de prueba
    private long studentId;
    private long subjectA;   // sin correlativas
    private long subjectB;   // requiere A (aprobado)
    private long subjectC;   // requiere A y B (aprobado)

    @BeforeAll
    void setUpDatabase() {
        new File("./target/correlation-test.db").delete();
        Base.open("org.sqlite.JDBC", DB_URL, "", "");
        createSchema();
        insertTestData();
    }

    @AfterAll
    void tearDown() {
        Base.close();
        new File("./target/correlation-test.db").delete();
    }

    // --- Tests ---

    @Test
    void canEnroll_subjectWithNoPrerequisites_returnsAllowed() {
        ValidationResult result = engine.canEnroll(studentId, subjectA);
        assertTrue(result.isAllowed());
        assertNull(result.getMissingPrerequisites());
    }

    @Test
    void canEnroll_allPrerequisitesMet_returnsAllowed() {
        // El estudiante tiene subjectA aprobado, subjectB lo requiere
        ValidationResult result = engine.canEnroll(studentId, subjectB);
        assertTrue(result.isAllowed());
    }

    @Test
    void canEnroll_prerequisiteMissing_returnsFailWithMissingList() {
        // subjectC requiere A y B; el estudiante solo tiene A aprobado
        ValidationResult result = engine.canEnroll(studentId, subjectC);

        assertFalse(result.isAllowed());
        assertEquals("MISSING_PREREQUISITES", result.getReason());
        assertNotNull(result.getMissingPrerequisites());
        assertEquals(1, result.getMissingPrerequisites().size());
        assertEquals(subjectB, result.getMissingPrerequisites().get(0).id);
        assertEquals("Análisis Matemático II", result.getMissingPrerequisites().get(0).name);
    }

    @Test
    void canEnroll_noPrerequisitesMet_returnsAllMissingInList() {
        // Nuevo estudiante sin evaluaciones
        long newStudentId = insertStudent("87654321");

        ValidationResult result = engine.canEnroll(newStudentId, subjectC);

        assertFalse(result.isAllowed());
        assertEquals(2, result.getMissingPrerequisites().size());
    }

    // --- Helpers de setup ---

    private void createSchema() {
        Base.exec("CREATE TABLE IF NOT EXISTS persons (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "dni TEXT NOT NULL UNIQUE, " +
                "firstName TEXT NOT NULL, " +
                "lastName TEXT NOT NULL, " +
                "phone TEXT NOT NULL, " +
                "email TEXT NOT NULL)");

        Base.exec("CREATE TABLE IF NOT EXISTS students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_person INTEGER NOT NULL, " +
                "student_type TEXT NOT NULL)");

        Base.exec("CREATE TABLE IF NOT EXISTS subjects (" +
                "id_subject INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_name TEXT NOT NULL)");

        Base.exec("CREATE TABLE IF NOT EXISTS conditions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_id INTEGER NOT NULL, " +
                "prerequisite_subject_id INTEGER NOT NULL, " +
                "type VARCHAR(20) NOT NULL DEFAULT 'REGULAR')");

        Base.exec("CREATE TABLE IF NOT EXISTS evaluations (" +
                "id_evaluations INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL, " +
                "evaluation_date DATE NOT NULL, " +
                "evaluation_note INTEGER, " +
                "condition_type TEXT)");
    }

    private void insertTestData() {
        // Materias
        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Análisis Matemático I");
        subjectA = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Análisis Matemático II");
        subjectB = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Física II");
        subjectC = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        // Correlativas: B requiere A; C requiere A y B
        Base.exec("INSERT INTO conditions (subject_id, prerequisite_subject_id, type) VALUES (?,?,?)",
                subjectB, subjectA, "APROBADA");
        Base.exec("INSERT INTO conditions (subject_id, prerequisite_subject_id, type) VALUES (?,?,?)",
                subjectC, subjectA, "APROBADA");
        Base.exec("INSERT INTO conditions (subject_id, prerequisite_subject_id, type) VALUES (?,?,?)",
                subjectC, subjectB, "APROBADA");

        // Estudiante
        studentId = insertStudent("12345678");

        // El estudiante tiene A aprobado, pero no B
        Base.exec("INSERT INTO evaluations (student_id, subject_id, evaluation_date, evaluation_note, condition_type) VALUES (?,?,?,?,?)",
                studentId, subjectA, "2025-12-01", 8, "aprobado");
    }

    private long insertStudent(String dni) {
        Base.exec("INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?,?,?,?,?)",
                dni, "Test", "User", "1111111111", "test@test.com");
        long personId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();
        Base.exec("INSERT INTO students (id_person, student_type) VALUES (?,?)", personId, "Avanzado");
        return ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();
    }
}
