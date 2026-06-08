package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Subject;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SubjectRepository.
 * Uses a separate SQLite database file (target/test-subject-repo.db).
 * Tests CRUD operations and query methods.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubjectRepositoryTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-subject-repo.db";
    private SubjectRepository repo;

    private int subject1Id;
    private int subject2Id;
    private int studyPlanId;

    @BeforeAll
    void setupDatabase() {
        new File("./target/test-subject-repo.db").delete();
        System.setProperty("db.url", JDBC_URL);
        repo = new SubjectRepository();
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE IF NOT EXISTS study_plans (" +
                "id_study_plan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "year INTEGER NOT NULL, " +
                "id_career INTEGER)");
        Base.exec("CREATE TABLE IF NOT EXISTS subjects (" +
                "id_subject INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "code TEXT NOT NULL UNIQUE, " +
                "subject_name TEXT NOT NULL, " +
                "id_study_plan INTEGER)");
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        try { Base.close(); } catch (Exception ignored) { }
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
    }

    @BeforeEach
    void insertTestData() {
        Base.exec("INSERT INTO study_plans (name, year, id_career) VALUES (?,?,?)",
                "Plan 2024", 2024, 1);
        studyPlanId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        Base.exec("INSERT INTO subjects (code, subject_name, id_study_plan) VALUES (?,?,?)",
                "MAT101", "Matematica", studyPlanId);
        subject1Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        Base.exec("INSERT INTO subjects (code, subject_name, id_study_plan) VALUES (?,?,?)",
                "FIS101", "Fisica", studyPlanId);
        subject2Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        // Subject belonging to a different study plan (no subjects should match)
        Base.exec("INSERT INTO study_plans (name, year, id_career) VALUES (?,?,?)",
                "Plan 2023", 2023, 2);
        int otherPlanId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        Base.exec("INSERT INTO subjects (code, subject_name, id_study_plan) VALUES (?,?,?)",
                "HIS101", "Historia", otherPlanId);
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM subjects");
        Base.exec("DELETE FROM study_plans");
        Base.close();
    }

    @AfterAll
    void teardown() {
        try { Base.open("org.sqlite.JDBC", JDBC_URL, "", ""); Base.exec("DROP TABLE IF EXISTS subjects"); Base.exec("DROP TABLE IF EXISTS study_plans"); Base.close(); } catch (Exception ignored) { }
        new File("./target/test-subject-repo.db").delete();
    }

    // =============================
    // findById
    // =============================
    @Test
    void findById_existingId_returnsSubject() {
        Subject result = repo.findById(subject1Id);
        assertNotNull(result);
        assertEquals("MAT101", result.getCode());
    }

    @Test
    void findById_unknownId_returnsNull() {
        assertNull(repo.findById(9999));
    }

    // =============================
    // findByCode
    // =============================
    @Test
    void findByCode_existingCode_returnsSubject() {
        Subject result = repo.findByCode("MAT101");
        assertNotNull(result);
        assertEquals("Matematica", result.getSubjectName());
    }

    @Test
    void findByCode_unknownCode_returnsNull() {
        assertNull(repo.findByCode("NONEXISTENT"));
    }

    // =============================
    // findAll
    // =============================
    @Test
    void findAll_returnsAllSubjects() {
        List<Subject> result = repo.findAll();
        // 3 subjects across both plans
        assertEquals(3, result.size());
    }

    // =============================
    // findByStudyPlanId
    // =============================
    @Test
    void findByStudyPlanId_existingPlan_returnsSubjects() {
        List<Subject> result = repo.findByStudyPlanId(studyPlanId);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(s -> s.getStudyPlanId() == studyPlanId));
    }

    @Test
    void findByStudyPlanId_unknownPlan_returnsEmptyList() {
        List<Subject> result = repo.findByStudyPlanId(9999);
        assertTrue(result.isEmpty());
    }

    // =============================
    // create
    // =============================
    @Test
    void create_newSubject_savesAndReturns() {
        Subject newSubject = new Subject();
        newSubject.setCode("ING202");
        newSubject.setSubjectName("Ingles II");
        newSubject.setStudyPlanId(studyPlanId);

        Subject result = repo.create(newSubject);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("ING202", result.getCode());

        Subject loaded = repo.findById(result.getId());
        assertNotNull(loaded);
        assertEquals("Ingles II", loaded.getSubjectName());
    }

    // =============================
    // update
    // =============================
    @Test
    void update_existingSubject_updatesFields() {
        Subject subject = repo.findById(subject1Id);
        assertNotNull(subject);
        subject.setSubjectName("Matematica Avanzada");

        boolean updated = repo.update(subject);
        assertTrue(updated);

        Subject loaded = repo.findById(subject1Id);
        assertNotNull(loaded);
        assertEquals("Matematica Avanzada", loaded.getSubjectName());
    }

    @Test
    void update_nonExistingSubject_returnsFalse() {
        Subject phantom = new Subject();
        phantom.setCode("GHOST");
        phantom.setSubjectName("Phantom");

        boolean updated = repo.update(phantom);
        assertFalse(updated);
    }

    // =============================
    // delete
    // =============================
    @Test
    void delete_existingSubject_returnsTrue() {
        Subject subject = repo.findById(subject2Id);
        assertNotNull(subject);

        boolean deleted = repo.delete(subject);
        assertTrue(deleted);
        assertNull(repo.findById(subject2Id));
    }

    @Test
    void delete_nonExistingSubject_returnsFalse() {
        Subject phantom = new Subject();
        phantom.setCode("GHOST");
        phantom.setSubjectName("Phantom");

        boolean deleted = repo.delete(phantom);
        assertFalse(deleted);
    }

    // =============================
    // deleteById
    // =============================
    @Test
    void deleteById_existingId_returnsTrue() {
        boolean deleted = repo.deleteById(subject1Id);
        assertTrue(deleted);
        assertNull(repo.findById(subject1Id));
    }

    @Test
    void deleteById_unknownId_returnsFalse() {
        boolean deleted = repo.deleteById(9999);
        assertFalse(deleted);
    }
}
