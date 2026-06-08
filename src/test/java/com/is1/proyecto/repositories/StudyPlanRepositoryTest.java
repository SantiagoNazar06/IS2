package com.is1.proyecto.repositories;

import com.is1.proyecto.models.StudyPlan;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for StudyPlanRepository.
 * Uses a separate SQLite database file (target/test-studyplan-repo.db).
 * Tests CRUD operations and cascade queries.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudyPlanRepositoryTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-studyplan-repo.db";
    private StudyPlanRepository repo;

    private int plan1Id;
    private int plan2Id;
    private int careerId;

    @BeforeAll
    void setupDatabase() {
        new File("./target/test-studyplan-repo.db").delete();
        System.setProperty("db.url", JDBC_URL);
        repo = new StudyPlanRepository();
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE IF NOT EXISTS careers (" +
                "id_careers INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "career_name TEXT NOT NULL, " +
                "career_duration INTEGER NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS study_plans (" +
                "id_study_plan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "year INTEGER NOT NULL, " +
                "id_career INTEGER)");
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        try { Base.close(); } catch (Exception ignored) { }
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
    }

    @BeforeEach
    void insertTestData() {
        Base.exec("INSERT INTO careers (career_name, career_duration) VALUES (?,?)",
                "Ingenieria", 5);
        careerId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        Base.exec("INSERT INTO careers (career_name, career_duration) VALUES (?,?)",
                "Licenciatura", 4);

        Base.exec("INSERT INTO study_plans (name, year, id_career) VALUES (?,?,?)",
                "Plan 2024", 2024, careerId);
        plan1Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        Base.exec("INSERT INTO study_plans (name, year, id_career) VALUES (?,?,?)",
                "Plan 2025", 2025, careerId);
        plan2Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        // Plan for a different career (should not match findByCareerId for careerId)
        Base.exec("INSERT INTO study_plans (name, year, id_career) VALUES (?,?,?)",
                "Plan 2024 - Lic", 2024, 2);
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM study_plans");
        Base.exec("DELETE FROM careers");
        Base.close();
    }

    @AfterAll
    void teardown() {
        try { Base.open("org.sqlite.JDBC", JDBC_URL, "", ""); Base.exec("DROP TABLE IF EXISTS study_plans"); Base.exec("DROP TABLE IF EXISTS careers"); Base.close(); } catch (Exception ignored) { }
        new File("./target/test-studyplan-repo.db").delete();
    }

    // =============================
    // findById
    // =============================
    @Test
    void findById_existingId_returnsPlan() {
        StudyPlan result = repo.findById(plan1Id);
        assertNotNull(result);
        assertEquals("Plan 2024", result.getName());
    }

    @Test
    void findById_unknownId_returnsNull() {
        assertNull(repo.findById(9999));
    }

    // =============================
    // findByName
    // =============================
    @Test
    void findByName_existingName_returnsPlan() {
        StudyPlan result = repo.findByName("Plan 2024");
        assertNotNull(result);
        assertEquals(plan1Id, result.getId().intValue());
    }

    @Test
    void findByName_unknownName_returnsNull() {
        assertNull(repo.findByName("NONEXISTENT"));
    }

    // =============================
    // findAll
    // =============================
    @Test
    void findAll_returnsAllPlans() {
        List<StudyPlan> result = repo.findAll();
        assertEquals(3, result.size());
    }

    // =============================
    // findByCareerId
    // =============================
    @Test
    void findByCareerId_existingCareer_returnsPlans() {
        List<StudyPlan> result = repo.findByCareerId(careerId);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getCareerId() == careerId));
    }

    @Test
    void findByCareerId_unknownCareer_returnsEmptyList() {
        List<StudyPlan> result = repo.findByCareerId(9999);
        assertTrue(result.isEmpty());
    }

    // =============================
    // create
    // =============================
    @Test
    void create_newPlan_savesAndReturns() {
        StudyPlan newPlan = new StudyPlan();
        newPlan.setName("Plan 2026");
        newPlan.setYear(2026);
        newPlan.setCareerId(careerId);

        StudyPlan result = repo.create(newPlan);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Plan 2026", result.getName());

        StudyPlan loaded = repo.findById(result.getId());
        assertNotNull(loaded);
        assertEquals(Integer.valueOf(2026), loaded.getYear());
    }

    // =============================
    // update
    // =============================
    @Test
    void update_existingPlan_updatesFields() {
        StudyPlan plan = repo.findById(plan1Id);
        assertNotNull(plan);
        plan.setName("Plan 2024 v2");

        boolean updated = repo.update(plan);
        assertTrue(updated);

        StudyPlan loaded = repo.findById(plan1Id);
        assertNotNull(loaded);
        assertEquals("Plan 2024 v2", loaded.getName());
    }

    @Test
    void update_nonExistingPlan_returnsFalse() {
        StudyPlan phantom = new StudyPlan();
        phantom.setName("Ghost");
        phantom.setYear(9999);

        boolean updated = repo.update(phantom);
        assertFalse(updated);
    }

    // =============================
    // delete
    // =============================
    @Test
    void delete_existingPlan_returnsTrue() {
        StudyPlan plan = repo.findById(plan2Id);
        assertNotNull(plan);

        boolean deleted = repo.delete(plan);
        assertTrue(deleted);
        assertNull(repo.findById(plan2Id));
    }

    @Test
    void delete_nonExistingPlan_returnsFalse() {
        StudyPlan phantom = new StudyPlan();
        phantom.setName("Ghost");
        phantom.setYear(9999);

        boolean deleted = repo.delete(phantom);
        assertFalse(deleted);
    }

    // =============================
    // deleteById
    // =============================
    @Test
    void deleteById_existingId_returnsTrue() {
        boolean deleted = repo.deleteById(plan1Id);
        assertTrue(deleted);
        assertNull(repo.findById(plan1Id));
    }

    @Test
    void deleteById_unknownId_returnsFalse() {
        boolean deleted = repo.deleteById(9999);
        assertFalse(deleted);
    }
}
