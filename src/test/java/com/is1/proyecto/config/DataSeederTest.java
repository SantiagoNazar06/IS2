package com.is1.proyecto.config;

import com.is1.proyecto.models.*;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para DataSeeder.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataSeederTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-seeder.db";

    @BeforeAll
    void setupDatabase() throws Exception {
        new java.io.File("target/test-seeder.db").delete();
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        String sql = loadSchemaSql();
        for (String statement : sql.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                Base.exec(trimmed);
            }
        }
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
    }

    @AfterEach
    void closeAndClean() {
        Base.exec("PRAGMA foreign_keys = OFF");
        Base.exec("DELETE FROM users");
        Base.exec("DELETE FROM teachers");
        Base.exec("DELETE FROM students");
        Base.exec("DELETE FROM persons");
        Base.exec("PRAGMA foreign_keys = ON");
        Base.close();
    }

    @AfterAll
    void teardown() {
        new java.io.File("target/test-seeder.db").delete();
    }

    @Test
    void seed_createsAdminUser() {
        DataSeeder.seed();

        User admin = User.findFirst("name = ?", "admin");
        assertNotNull(admin, "Admin user should exist");
        assertEquals("ADMIN", admin.getString("role"), "Admin role should be ADMIN");
        assertNotNull(admin.getString("password"), "Admin password should be hashed");
    }

    @Test
    void seed_createsTeacherUserWithLinkedRecords() {
        DataSeeder.seed();

        User teacher = User.findFirst("name = ?", "teacher");
        assertNotNull(teacher, "Teacher user should exist");
        assertEquals("TEACHER", teacher.getString("role"), "Teacher role should be TEACHER");
        assertNotNull(teacher.getLong("teacher_id"), "Teacher user should have teacher_id");

        Teacher teacherRecord = Teacher.findById(teacher.getLong("teacher_id"));
        assertNotNull(teacherRecord, "Teacher record should exist");
        assertNotNull(teacherRecord.getNroLegajo(), "Teacher should have nroLegajo");

        // Verify linked person record exists
        Long personId = teacherRecord.getLong("id_persona");
        assertNotNull(personId, "Teacher should have id_persona");
        Person person = Person.findById(personId);
        assertNotNull(person, "Teacher should have linked person");
    }

    @Test
    void seed_createsStudentUserWithLinkedRecords() {
        DataSeeder.seed();

        User student = User.findFirst("name = ?", "student");
        assertNotNull(student, "Student user should exist");
        assertEquals("STUDENT", student.getString("role"), "Student role should be STUDENT");
        assertNotNull(student.getLong("student_id"), "Student user should have student_id");

        Student studentRecord = Student.findById(student.getLong("student_id"));
        assertNotNull(studentRecord, "Student record should exist");

        // Verify linked person record exists
        Long personId = studentRecord.getLong("id_person");
        assertNotNull(personId, "Student should have id_person");
        Person person = Person.findById(personId);
        assertNotNull(person, "Student should have linked person");
    }

    @Test
    void seed_isIdempotent() {
        DataSeeder.seed();
        long countAfterFirst = User.count();

        DataSeeder.seed();
        long countAfterSecond = User.count();

        assertEquals(countAfterFirst, countAfterSecond,
                "Second seed should not create additional users");
    }

    private String loadSchemaSql() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("scheme.sql");
        assertNotNull(is, "scheme.sql not found in classpath");
        return new BufferedReader(new InputStreamReader(is))
                .lines()
                .collect(Collectors.joining("\n"));
    }
}
