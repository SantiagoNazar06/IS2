package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para el modelo User.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-user-model.db";

    @BeforeAll
    void setupDatabase() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL DEFAULT 'ADMIN' CHECK(role IN('ADMIN','STUDENT','TEACHER')), " +
                "student_id INTEGER, " +
                "teacher_id INTEGER)");
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
    void getTeacherId_returnsSetValue() {
        User user = new User();
        user.setTeacherId(42L);
        Long result = user.getTeacherId();
        assertEquals(42L, result);
    }

    @Test
    void getTeacherId_returnsNullWhenNotSet() {
        User user = new User();
        assertNull(user.getTeacherId());
    }

    @AfterAll
    void teardown() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("DROP TABLE IF EXISTS users");
        Base.close();
        new java.io.File("target/test-user-model.db").delete();
    }
}
