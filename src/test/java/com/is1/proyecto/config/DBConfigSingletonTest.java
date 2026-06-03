package com.is1.proyecto.config;

import com.is1.proyecto.models.User;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para DBConfigSingleton.
 * Verifica que bootstrap() ejecute DataSeeder.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DBConfigSingletonTest {

    private static final String TEST_DB_PATH = "./target/test-dbconfig.db";
    private static final String TEST_DB_URL = "jdbc:sqlite:" + TEST_DB_PATH;

    @BeforeEach
    void setup() {
        // Ensure clean DB before each test
        new File(TEST_DB_PATH).delete();
        System.setProperty("db.url", TEST_DB_URL);
    }

    @AfterEach
    void cleanup() {
        Base.close();
        new File(TEST_DB_PATH).delete();
    }

    @Test
    void bootstrap_callsDataSeederAndCreatesSeedUsers() {
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();

        // Clear singleton state for clean test
        dbConfig.bootstrap();

        // Open connection to verify seed data
        Base.open("org.sqlite.JDBC", TEST_DB_URL, "", "");

        // Verify seed users exist
        User admin = User.findFirst("name = ?", "admin");
        assertNotNull(admin, "Seed admin should exist after bootstrap");
        assertEquals("ADMIN", admin.getString("role"));

        User teacher = User.findFirst("name = ?", "teacher");
        assertNotNull(teacher, "Seed teacher should exist after bootstrap");
        assertEquals("TEACHER", teacher.getString("role"));

        User student = User.findFirst("name = ?", "student");
        assertNotNull(student, "Seed student should exist after bootstrap");
        assertEquals("STUDENT", student.getString("role"));
    }
}
