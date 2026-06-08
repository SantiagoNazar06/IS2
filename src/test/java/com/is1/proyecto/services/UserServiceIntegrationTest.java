package com.is1.proyecto.services;

import com.is1.proyecto.models.User;
import com.is1.proyecto.security.AuthService;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;
import spark.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests de integración para UserService.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceIntegrationTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-user-service.db";

    private AuthService authService;
    private UserService userService;

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
    void setup() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        authService = new AuthService();
        userService = new UserService(authService);
    }

    @AfterEach
    void cleanup() {
        Base.exec("DELETE FROM users");
        Base.close();
    }

    @Test
    void addUserApi_setsRoleStudent() throws Exception {
        Response mockResponse = mock(Response.class);

        String result = userService.addUserApi("teststudent", "password123", mockResponse);

        assertTrue(result.contains("registrado con éxito"));

        User savedUser = User.findFirst("name = ?", "teststudent");
        assertNotNull(savedUser);
        assertEquals("STUDENT", savedUser.getString("role"));
    }

    @Test
    void addUserApi_createdUserHasCorrectRole() throws Exception {
        Response mockResponse = mock(Response.class);

        userService.addUserApi("anotherstudent", "mypassword", mockResponse);

        User savedUser = User.findFirst("name = ?", "anotherstudent");
        assertNotNull(savedUser);
        assertEquals("STUDENT", savedUser.getString("role"));
        assertEquals("anotherstudent", savedUser.getString("name"));
    }

    @AfterAll
    void teardown() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("DROP TABLE IF EXISTS users");
        Base.close();
        new java.io.File("target/test-user-service.db").delete();
    }
}
