package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Career;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CareerRepository.
 * Uses a separate SQLite database file (target/test-career-repo.db).
 * Tests CRUD operations and name-based queries.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CareerRepositoryTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-career-repo.db";
    private CareerRepository repo;

    private int career1Id;
    private int career2Id;

    @BeforeAll
    void setupDatabase() {
        new File("./target/test-career-repo.db").delete();
        System.setProperty("db.url", JDBC_URL);
        repo = new CareerRepository();
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE IF NOT EXISTS careers (" +
                "id_careers INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "career_name TEXT NOT NULL, " +
                "career_duration INTEGER NOT NULL)");
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
                "Ingenieria en Sistemas", 5);
        career1Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();

        Base.exec("INSERT INTO careers (career_name, career_duration) VALUES (?,?)",
                "Licenciatura en Matematicas", 4);
        career2Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM careers");
        Base.close();
    }

    @AfterAll
    void teardown() {
        try { Base.open("org.sqlite.JDBC", JDBC_URL, "", ""); Base.exec("DROP TABLE IF EXISTS careers"); Base.close(); } catch (Exception ignored) { }
        new File("./target/test-career-repo.db").delete();
    }

    // =============================
    // findById
    // =============================
    @Test
    void findById_existingId_returnsCareer() {
        Career result = repo.findById(career1Id);
        assertNotNull(result);
        assertEquals("Ingenieria en Sistemas", result.getCareerName());
    }

    @Test
    void findById_unknownId_returnsNull() {
        assertNull(repo.findById(9999));
    }

    // =============================
    // findByName
    // =============================
    @Test
    void findByName_existingName_returnsCareer() {
        Career result = repo.findByName("Ingenieria en Sistemas");
        assertNotNull(result);
        assertEquals(career1Id, result.getId().intValue());
    }

    @Test
    void findByName_unknownName_returnsNull() {
        assertNull(repo.findByName("NONEXISTENT"));
    }

    // =============================
    // findAll
    // =============================
    @Test
    void findAll_returnsAllCareers() {
        List<Career> result = repo.findAll();
        assertEquals(2, result.size());
    }

    // =============================
    // create
    // =============================
    @Test
    void create_newCareer_savesAndReturns() {
        Career newCareer = new Career();
        newCareer.setCareerName("Medicina");
        newCareer.setCareerDuration(6);

        Career result = repo.create(newCareer);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Medicina", result.getCareerName());

        Career loaded = repo.findById(result.getId());
        assertNotNull(loaded);
        assertEquals(Integer.valueOf(6), loaded.getCareerDuration());
    }

    // =============================
    // update
    // =============================
    @Test
    void update_existingCareer_updatesFields() {
        Career career = repo.findById(career1Id);
        assertNotNull(career);
        career.setCareerName("Ingenieria en Sistemas v2");

        boolean updated = repo.update(career);
        assertTrue(updated);

        Career loaded = repo.findById(career1Id);
        assertNotNull(loaded);
        assertEquals("Ingenieria en Sistemas v2", loaded.getCareerName());
    }

    @Test
    void update_nonExistingCareer_returnsFalse() {
        Career phantom = new Career();
        phantom.setCareerName("Ghost");
        phantom.setCareerDuration(1);

        boolean updated = repo.update(phantom);
        assertFalse(updated);
    }

    // =============================
    // delete
    // =============================
    @Test
    void delete_existingCareer_returnsTrue() {
        Career career = repo.findById(career2Id);
        assertNotNull(career);

        boolean deleted = repo.delete(career);
        assertTrue(deleted);
        assertNull(repo.findById(career2Id));
    }

    @Test
    void delete_nonExistingCareer_returnsFalse() {
        Career phantom = new Career();
        phantom.setCareerName("Ghost");
        phantom.setCareerDuration(1);

        boolean deleted = repo.delete(phantom);
        assertFalse(deleted);
    }
}
