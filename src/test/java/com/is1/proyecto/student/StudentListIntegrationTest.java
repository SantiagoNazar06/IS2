package com.is1.proyecto.student;

import com.is1.proyecto.dto.StudentListDTO;
import com.is1.proyecto.repositories.StudentRepository;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for StudentRepository.findStudents().
 * Uses a separate SQLite database with 5+ tables: students → persons → enrollments → subjects → study_plans → careers.
 * Follows pattern from StudentRepositoryIntegrationTest.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudentListIntegrationTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-student-list.db";

    private StudentRepository repository;

    // Seeded IDs
    private Long student1Id;  // enrolled in subject1 (career1) and subject2 (career2)
    private Long student2Id;  // enrolled in subject1 only (career1)
    private Long subject1Id;
    private Long subject2Id;
    private Long career1Id;
    private Long career2Id;
    private Long teacherId;   // assigned to subject1 only

    @BeforeAll
    void setupDatabase() {
        System.setProperty("db.url", JDBC_URL);
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");

        // Create tables matching the project schema
        Base.exec("CREATE TABLE IF NOT EXISTS persons (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "dni TEXT NOT NULL UNIQUE, " +
                "firstName TEXT NOT NULL, " +
                "lastName TEXT NOT NULL, " +
                "phone TEXT, " +
                "email TEXT" +
                ")");

        Base.exec("CREATE TABLE IF NOT EXISTS students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_person INTEGER NOT NULL, " +
                "student_type TEXT NOT NULL, " +
                "FOREIGN KEY (id_person) REFERENCES persons(id)" +
                ")");

        Base.exec("CREATE TABLE IF NOT EXISTS careers (" +
                "id_careers INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "career_name TEXT NOT NULL, " +
                "career_duration INTEGER" +
                ")");

        Base.exec("CREATE TABLE IF NOT EXISTS study_plans (" +
                "id_study_plan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "\"year\" INTEGER, " +
                "id_career INTEGER, " +
                "FOREIGN KEY (id_career) REFERENCES careers(id_careers)" +
                ")");

        Base.exec("CREATE TABLE IF NOT EXISTS subjects (" +
                "id_subject INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_name TEXT NOT NULL, " +
                "id_study_plan INTEGER, " +
                "code TEXT, " +
                "FOREIGN KEY (id_study_plan) REFERENCES study_plans(id_study_plan)" +
                ")");

        Base.exec("CREATE TABLE IF NOT EXISTS enrollments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL, " +
                "period TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'ENROLLED', " +
                "created_at TEXT, " +
                "FOREIGN KEY (student_id) REFERENCES students(id), " +
                "FOREIGN KEY (subject_id) REFERENCES subjects(id_subject)" +
                ")");

        Base.exec("CREATE TABLE IF NOT EXISTS teacher_subject (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "teacher_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL" +
                ")");

        Base.close();
    }

    private long insertAndGetId(String sql, Object... params) {
        Base.exec(sql, params);
        List<Map> rows = Base.findAll("SELECT last_insert_rowid() AS id");
        return ((Number) rows.get(0).get("id")).longValue();
    }

    @BeforeEach
    void setUp() {
        try { Base.close(); } catch (Exception ignored) {}
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        repository = new StudentRepository();

        // Persons
        long person1Id = insertAndGetId(
                "INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?, ?, ?, ?, ?)",
                "11111111", "Juan", "Perez", "3510000001", "juan@test.com");
        long person2Id = insertAndGetId(
                "INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?, ?, ?, ?, ?)",
                "22222222", "Maria", "Gomez", "3510000002", "maria@test.com");

        // Students
        student1Id = insertAndGetId(
                "INSERT INTO students (id_person, student_type) VALUES (?, ?)", person1Id, "REGULAR");
        student2Id = insertAndGetId(
                "INSERT INTO students (id_person, student_type) VALUES (?, ?)", person2Id, "REGULAR");

        // Careers
        career1Id = insertAndGetId(
                "INSERT INTO careers (career_name, career_duration) VALUES (?, ?)", "Ingenieria", 5);
        career2Id = insertAndGetId(
                "INSERT INTO careers (career_name, career_duration) VALUES (?, ?)", "Medicina", 6);

        // Study plans
        long sp1Id = insertAndGetId(
                "INSERT INTO study_plans (name, year, id_career) VALUES (?, ?, ?)", "Plan 2024", 2024, career1Id);
        long sp2Id = insertAndGetId(
                "INSERT INTO study_plans (name, year, id_career) VALUES (?, ?, ?)", "Plan 2023", 2023, career2Id);

        // Subjects
        subject1Id = insertAndGetId(
                "INSERT INTO subjects (subject_name, id_study_plan, code) VALUES (?, ?, ?)",
                "Matematica", sp1Id, "MAT101");
        subject2Id = insertAndGetId(
                "INSERT INTO subjects (subject_name, id_study_plan, code) VALUES (?, ?, ?)",
                "Anatomia", sp2Id, "ANA101");

        // Enrollments: student1 in both subjects, student2 only in subject1
        insertAndGetId(
                "INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?, ?, ?, ?)",
                student1Id, subject1Id, "2024-01", "ENROLLED");
        insertAndGetId(
                "INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?, ?, ?, ?)",
                student1Id, subject2Id, "2024-01", "ENROLLED");
        insertAndGetId(
                "INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?, ?, ?, ?)",
                student2Id, subject1Id, "2024-01", "ENROLLED");

        // Teacher assigned to subject1 only
        teacherId = 100L;
        insertAndGetId(
                "INSERT INTO teacher_subject (teacher_id, subject_id) VALUES (?, ?)",
                teacherId, subject1Id);
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM teacher_subject");
        Base.exec("DELETE FROM enrollments");
        Base.exec("DELETE FROM subjects");
        Base.exec("DELETE FROM study_plans");
        Base.exec("DELETE FROM careers");
        Base.exec("DELETE FROM students");
        Base.exec("DELETE FROM persons");
        Base.close();
    }

    @AfterAll
    void teardown() {
        try { Base.close(); } catch (Exception ignored) { }
        new File("target/test-student-list.db").delete();
    }

    // ========================================================================
    // Tests
    // ========================================================================

    @Test
    void testFindStudents_adminSeesAll() {
        List<StudentListDTO> results = repository.findStudents(null, null, null, true);

        assertNotNull(results);
        assertEquals(2, results.size());

        // Both students should appear
        StudentListDTO student1 = results.stream()
                .filter(s -> s.getStudentId().equals(student1Id))
                .findFirst().orElse(null);
        assertNotNull(student1, "Student 1 should be in results");
        assertEquals("Juan Perez", student1.getFullName());
        assertEquals("11111111", student1.getDni());
        assertEquals("juan@test.com", student1.getEmail());
        assertTrue(student1.getCareers().contains("Ingenieria"));
        assertTrue(student1.getCareers().contains("Medicina"));

        StudentListDTO student2 = results.stream()
                .filter(s -> s.getStudentId().equals(student2Id))
                .findFirst().orElse(null);
        assertNotNull(student2, "Student 2 should be in results");
        assertEquals("Maria Gomez", student2.getFullName());
        assertEquals("Ingenieria", student2.getCareers());
    }

    @Test
    void testFindStudents_teacherSeesOnlyTheirSubjects() {
        List<StudentListDTO> results = repository.findStudents(null, null, teacherId, false);

        assertNotNull(results);
        assertEquals(2, results.size(), "Teacher assigned to subject1 should see both students enrolled in it");

        // Only careers related to the teacher's subject should be shown
        for (StudentListDTO dto : results) {
            assertTrue(dto.getCareers().contains("Ingenieria"),
                    "Teacher's subject is in Ingenieria career, got: " + dto.getCareers());
        }
    }

    @Test
    void testFindStudents_filterByCareer() {
        List<StudentListDTO> results = repository.findStudents(career2Id, null, null, true);

        assertNotNull(results);
        assertEquals(1, results.size(), "Only student1 is enrolled in a Medicina subject");
        assertEquals(student1Id, results.get(0).getStudentId());
        assertTrue(results.get(0).getCareers().contains("Medicina"));
    }

    @Test
    void testFindStudents_filterBySubject() {
        List<StudentListDTO> results = repository.findStudents(null, subject2Id, null, true);

        assertNotNull(results);
        assertEquals(1, results.size(), "Only student1 is enrolled in subject2");
        assertEquals(student1Id, results.get(0).getStudentId());
    }

    @Test
    void testFindStudents_filterByCareerAndSubject() {
        List<StudentListDTO> results = repository.findStudents(career1Id, subject1Id, null, true);

        assertNotNull(results);
        assertEquals(2, results.size(), "Both students enrolled in subject1 which is in career1");
    }

    @Test
    void testFindStudents_noResults() {
        List<StudentListDTO> results = repository.findStudents(99999L, null, null, true);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindStudents_teacherWithFilter() {
        // Teacher scoped to subject1 + filter by career1
        List<StudentListDTO> results = repository.findStudents(career1Id, null, teacherId, false);

        assertNotNull(results);
        assertEquals(2, results.size(), "Both students in subject1 (career1)");
    }

    @Test
    void testFindStudents_teacherWithNoMatches() {
        // Teacher scoped to subject1 but filtering subject2 (which teacher is not assigned to)
        List<StudentListDTO> results = repository.findStudents(null, subject2Id, teacherId, false);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Teacher not assigned to subject2, should see no results");
    }
}
