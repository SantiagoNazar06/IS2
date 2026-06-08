package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Condition;
import com.is1.proyecto.models.ConditionType;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ConditionRepository.
 * Uses a separate SQLite database file (target/test-condition-repo.db).
 * Follows the same pattern as SubjectRepositoryTest.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConditionRepositoryTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-condition-repo.db";
    private ConditionRepository repo;

    private int subject1Id;
    private int subject2Id;
    private int subject3Id;

    @BeforeAll
    void setupDatabase() {
        new File("./target/test-condition-repo.db").delete();
        System.setProperty("db.url", JDBC_URL);
        repo = new ConditionRepository();
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE IF NOT EXISTS subjects (" +
                "id_subject INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "code TEXT NOT NULL UNIQUE, " +
                "subject_name TEXT NOT NULL, " +
                "id_study_plan INTEGER)");
        Base.exec("CREATE TABLE IF NOT EXISTS conditions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_id INTEGER NOT NULL REFERENCES subjects(id_subject), " +
                "prerequisite_subject_id INTEGER NOT NULL REFERENCES subjects(id_subject), " +
                "type VARCHAR(20) NOT NULL DEFAULT 'REGULAR', " +
                "CHECK(type IN ('REGULAR', 'APROBADA')), " +
                "CHECK(subject_id != prerequisite_subject_id))");
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        try { Base.close(); } catch (Exception ignored) { }
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
    }

    @BeforeEach
    void insertTestData() {
        // Subject 1: Matematica I
        Base.exec("INSERT INTO subjects (code, subject_name) VALUES (?,?)",
                "MAT101", "Matematica I");
        subject1Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        // Subject 2: Algebra (prerequisite for Matematica I)
        Base.exec("INSERT INTO subjects (code, subject_name) VALUES (?,?)",
                "ALG101", "Algebra");
        subject2Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        // Subject 3: Fisica (prerequisite for Matematica I)
        Base.exec("INSERT INTO subjects (code, subject_name) VALUES (?,?)",
                "FIS101", "Fisica");
        subject3Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        // Subject 1 has Algebra (REGULAR) and Fisica (APROBADA) as prerequisites
        Base.exec("INSERT INTO conditions (subject_id, prerequisite_subject_id, type) VALUES (?,?,?)",
                subject1Id, subject2Id, "REGULAR");
        Base.exec("INSERT INTO conditions (subject_id, prerequisite_subject_id, type) VALUES (?,?,?)",
                subject1Id, subject3Id, "APROBADA");
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM conditions");
        Base.exec("DELETE FROM subjects");
        Base.close();
    }

    @AfterAll
    void teardown() {
        try {
            Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
            Base.exec("DROP TABLE IF EXISTS conditions");
            Base.exec("DROP TABLE IF EXISTS subjects");
            Base.close();
        } catch (Exception ignored) { }
        new File("./target/test-condition-repo.db").delete();
    }

    // =============================
    // findBySubject
    // =============================
    @Test
    void findBySubject_existingSubject_returnsConditions() {
        List<Condition> result = repo.findBySubject(subject1Id);

        assertNotNull(result);
        assertEquals(2, result.size());

        Condition first = result.get(0);
        assertNotNull(first.getInteger("prerequisite_subject_id"));
        assertNotNull(first.getString("type"));
    }

    @Test
    void findBySubject_noPrerequisites_returnsEmptyList() {
        // Subject 4 has no prerequisites
        Base.exec("INSERT INTO subjects (code, subject_name) VALUES (?,?)",
                "PROG101", "Programacion I");
        int subject4Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        List<Condition> result = repo.findBySubject(subject4Id);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // =============================
    // create
    // =============================
    @Test
    void create_regularType_savesAndReturns() {
        Condition result = repo.create(subject2Id, subject1Id, ConditionType.REGULAR);

        assertNotNull(result);
        assertNotNull(result.getId());

        // Verify it was persisted
        List<Condition> all = Condition.where("subject_id = ?", subject2Id);
        assertEquals(1, all.size());
        assertEquals(ConditionType.REGULAR.name(), all.get(0).getString("type"));
        assertEquals(subject1Id, all.get(0).getInteger("prerequisite_subject_id").intValue());
    }

    @Test
    void create_aprobadaType_savesAndReturns() {
        Condition result = repo.create(subject2Id, subject1Id, ConditionType.APROBADA);

        assertNotNull(result);
        assertNotNull(result.getId());

        // Verify it was persisted
        List<Condition> all = Condition.where("subject_id = ?", subject2Id);
        assertEquals(1, all.size());
        assertEquals(ConditionType.APROBADA.name(), all.get(0).getString("type"));
    }

    // =============================
    // delete
    // =============================
    @Test
    void delete_existingCondition_returnsTrue() {
        // Find the first condition for subject1
        List<Condition> conditions = Condition.where("subject_id = ?", subject1Id);
        assertFalse(conditions.isEmpty());
        int conditionId = conditions.get(0).getInteger("id");

        boolean result = repo.delete(conditionId);

        assertTrue(result);

        // Verify it was deleted
        assertNull(Condition.findById(conditionId));
        List<Condition> remaining = Condition.where("subject_id = ?", subject1Id);
        assertEquals(1, remaining.size());
    }

    @Test
    void delete_nonExistingCondition_returnsFalse() {
        boolean result = repo.delete(9999);

        assertFalse(result);
    }
}
