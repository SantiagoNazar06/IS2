package com.is1.proyecto;

import org.junit.jupiter.api.*;
import java.io.*;
import java.sql.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para el schema de la base de datos (scheme.sql).
 */
public class SchemaTest {

    private static final String JDBC_URL = "jdbc:sqlite::memory:";
    private Connection conn;

    @BeforeEach
    void setup() throws Exception {
        conn = DriverManager.getConnection(JDBC_URL);
        String sql = loadSchemaSql();
        for (String statement : sql.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    @AfterEach
    void teardown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    private String loadSchemaSql() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("scheme.sql");
        assertNotNull(is, "scheme.sql not found in classpath");
        return new BufferedReader(new InputStreamReader(is))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    @Test
    void schema_allowsAdminRole() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO users (name, password, role) VALUES ('admin1', 'pass1', 'ADMIN')");
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT role FROM users WHERE name = 'admin1'")) {
            assertTrue(rs.next());
            assertEquals("ADMIN", rs.getString("role"));
        }
    }

    @Test
    void schema_allowsStudentRole() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO users (name, password, role) VALUES ('stud1', 'pass1', 'STUDENT')");
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT role FROM users WHERE name = 'stud1'")) {
            assertTrue(rs.next());
            assertEquals("STUDENT", rs.getString("role"));
        }
    }

    @Test
    void schema_allowsTeacherRole() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO users (name, password, role) VALUES ('teach1', 'pass1', 'TEACHER')");
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT role FROM users WHERE name = 'teach1'")) {
            assertTrue(rs.next());
            assertEquals("TEACHER", rs.getString("role"));
        }
    }

    @Test
    void schema_rejectsInvalidRole() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            assertThrows(SQLException.class, () -> {
                stmt.execute("INSERT INTO users (name, password, role) VALUES ('bad', 'pass', 'INVALID')");
            });
        }
    }

    @Test
    void schema_usersTableHasTeacherIdColumn() throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, "users", "teacher_id")) {
            assertTrue(rs.next(), "Column 'teacher_id' should exist on 'users' table");
        }
    }
}
