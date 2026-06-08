package com.is1.proyecto.repositories;

import com.is1.proyecto.models.User;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserRepository.
 * Uses a separate SQLite database file (target/test-user-repo.db).
 * Tests CRUD operations, password hashing, and role-based queries.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-user-repo.db";
    private UserRepository repo;

    private Long adminId;
    private Long studentId;
    private Long teacherId;

    @BeforeAll
    void setupDatabase() {
        new File("./target/test-user-repo.db").delete();
        System.setProperty("db.url", JDBC_URL);
        repo = new UserRepository();
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, " +
                "username TEXT, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL DEFAULT 'ADMIN' CHECK(role IN('ADMIN','STUDENT','TEACHER')), " +
                "student_id INTEGER, " +
                "teacher_id INTEGER)");
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        // Close any leftover connection from a previous failed test
        try { Base.close(); } catch (Exception ignored) { }

        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
    }

    @BeforeEach
    void insertTestData() {
        Base.exec("INSERT INTO users (name, username, password, role) VALUES (?,?,?,?)",
                "admin", "admin", BCrypt.hashpw("admin123", BCrypt.gensalt()), "ADMIN");
        adminId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO users (name, username, password, role) VALUES (?,?,?,?)",
                "student1", "student1", BCrypt.hashpw("pass123", BCrypt.gensalt()), "STUDENT");
        studentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO users (name, username, password, role) VALUES (?,?,?,?)",
                "teacher1", "teacher1", BCrypt.hashpw("pass456", BCrypt.gensalt()), "TEACHER");
        teacherId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM users");
        Base.close();
    }

    @AfterAll
    void teardown() {
        try { Base.open("org.sqlite.JDBC", JDBC_URL, "", ""); Base.exec("DROP TABLE IF EXISTS users"); Base.close(); } catch (Exception ignored) { }
        new File("./target/test-user-repo.db").delete();
    }

    // =============================
    // findByUsername
    // =============================
    @Test
    void findByUsername_existingUser_returnsUser() {
        User result = repo.findByUsername("admin");
        assertNotNull(result);
        assertEquals("admin", result.getName());
    }

    @Test
    void findByUsername_unknownUser_returnsNull() {
        assertNull(repo.findByUsername("nonexistent"));
    }

    // =============================
    // findById
    // =============================
    @Test
    void findById_existingId_returnsUser() {
        User result = repo.findById(adminId);
        assertNotNull(result);
        assertEquals("admin", result.getName());
    }

    @Test
    void findById_unknownId_returnsNull() {
        assertNull(repo.findById(9999L));
    }

    // =============================
    // findAll
    // =============================
    @Test
    void findAll_returnsAllUsers() {
        List<User> result = repo.findAll();
        assertEquals(3, result.size());
    }

    // =============================
    // create
    // =============================
    @Test
    void create_newUser_savesAndHashesPassword() {
        User newUser = new User();
        newUser.setName("newuser");
        newUser.setPassword("plainPassword");
        newUser.setRole("STUDENT");

        User result = repo.create(newUser);

        assertNotNull(result);
        assertNotNull(result.getId());
        // Password should be hashed, not plaintext
        String savedHash = result.getPassword();
        assertNotEquals("plainPassword", savedHash);
        assertTrue(BCrypt.checkpw("plainPassword", savedHash));
    }

    // =============================
    // update
    // =============================
    @Test
    void update_existingUser_updatesFields() {
        boolean updated = repo.update(adminId, Map.of("name", "adminUpdated"));

        assertTrue(updated);
        User loaded = repo.findById(adminId);
        assertNotNull(loaded);
        assertEquals("adminUpdated", loaded.getName());
    }

    @Test
    void update_withPassword_hashesNewPassword() {
        boolean updated = repo.update(adminId, new java.util.HashMap<>(Map.of("password", "newPassword123")));

        assertTrue(updated);
        User loaded = repo.findById(adminId);
        assertNotNull(loaded);
        String savedHash = loaded.getPassword();
        assertTrue(BCrypt.checkpw("newPassword123", savedHash));
    }

    @Test
    void update_nonExistingId_returnsFalse() {
        boolean updated = repo.update(9999L, Map.of("name", "ghost"));
        assertFalse(updated);
    }

    // =============================
    // delete
    // =============================
    @Test
    void delete_existingUser_returnsTrue() {
        boolean deleted = repo.delete(studentId);
        assertTrue(deleted);
        assertNull(repo.findById(studentId));
    }

    @Test
    void delete_nonExistingUser_returnsFalse() {
        boolean deleted = repo.delete(9999L);
        assertFalse(deleted);
    }

    // =============================
    // findByRole
    // =============================
    @Test
    void findByRole_adminRole_returnsAdminUsers() {
        List<User> result = repo.findByRole("ADMIN");
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getName());
    }

    @Test
    void findByRole_unknownRole_returnsEmptyList() {
        List<User> result = repo.findByRole("NONEXISTENT");
        assertTrue(result.isEmpty());
    }
}
