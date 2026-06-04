package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Teacher;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeacherRepositoryTest {

    private static final String DB_URL = "jdbc:sqlite:./target/teacher-repo-test.db";
    private TeacherRepository repo;

    private long personId;
    private long teacherId;
    private long otherTeacherId;
    private long subjectId;
    private long otherSubjectId;

    @BeforeAll
    void setUp() {
        new File("./target/teacher-repo-test.db").delete();
        System.setProperty("db.url", DB_URL);
        repo = new TeacherRepository();
        Base.open("org.sqlite.JDBC", DB_URL, "", "");
        createSchema();
        insertTestData();
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        Base.open("org.sqlite.JDBC", DB_URL, "", "");
    }

    @AfterEach
    void closeConnection() {
        Base.close();
    }

    @AfterAll
    void tearDown() {
        new File("./target/teacher-repo-test.db").delete();
    }

    // AC-1
    @Test
    void findByLegajo_existingLegajo_returnsTeacher() {
        Teacher t = repo.findByLegajo("LEG-001");
        assertNotNull(t);
        assertEquals(teacherId, t.getLongId().longValue());
    }

    @Test
    void findByLegajo_unknownLegajo_returnsNull() {
        assertNull(repo.findByLegajo("NO-EXISTE"));
    }

    // AC-2
    @Test
    void findByPersonId_existingPerson_returnsTeacher() {
        Teacher t = repo.findByPersonId(personId);
        assertNotNull(t);
        assertEquals("LEG-001", t.getNroLegajo());
    }

    @Test
    void findByPersonId_unknownPerson_returnsNull() {
        assertNull(repo.findByPersonId(9999L));
    }

    // AC-3
    @Test
    void findBySubject_subjectWithTeachers_returnsList() {
        List<Map<String, Object>> result = repo.findBySubject(subjectId);
        assertFalse(result.isEmpty());
        Map<String, Object> entry = result.get(0);
        assertTrue(entry.containsKey("teacherId"));
        assertTrue(entry.containsKey("legajo"));
        assertTrue(entry.containsKey("role"));
        assertTrue(entry.containsKey("period"));
        assertEquals("RESPONSABLE", entry.get("role").toString());
    }

    @Test
    void findBySubject_subjectWithNoTeachers_returnsEmptyList() {
        assertTrue(repo.findBySubject(otherSubjectId).isEmpty());
    }

    // AC-4
    @Test
    void findByPeriod_periodWithAssignments_returnsList() {
        List<Teacher> result = repo.findByPeriod("2025-1");
        assertFalse(result.isEmpty());
        assertEquals(teacherId, result.get(0).getLongId().longValue());
    }

    @Test
    void findByPeriod_unknownPeriod_returnsEmptyList() {
        assertTrue(repo.findByPeriod("1900-1").isEmpty());
    }

    // AC-5
    @Test
    void getAssignedSubjects_teacherWithAssignments_returnsList() {
        List<Map<String, Object>> result = repo.getAssignedSubjects(teacherId);
        assertEquals(1, result.size());
        Map<String, Object> entry = result.get(0);
        assertTrue(entry.containsKey("subjectId"));
        assertTrue(entry.containsKey("subjectName"));
        assertTrue(entry.containsKey("role"));
        assertTrue(entry.containsKey("period"));
        assertEquals("Análisis Matemático", entry.get("subjectName").toString());
    }

    @Test
    void getAssignedSubjects_teacherWithNoAssignments_returnsEmptyList() {
        assertTrue(repo.getAssignedSubjects(otherTeacherId).isEmpty());
    }

    // --- Helpers ---

    private void createSchema() {
        Base.exec("CREATE TABLE IF NOT EXISTS persons (id INTEGER PRIMARY KEY AUTOINCREMENT, dni TEXT NOT NULL UNIQUE, firstName TEXT NOT NULL, lastName TEXT NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS teachers (id INTEGER PRIMARY KEY AUTOINCREMENT, id_persona INTEGER NOT NULL, nroLegajo VARCHAR(30) NOT NULL UNIQUE)");
        Base.exec("CREATE TABLE IF NOT EXISTS subjects (id_subject INTEGER PRIMARY KEY AUTOINCREMENT, subject_name TEXT NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS teacher_assignments (id INTEGER PRIMARY KEY AUTOINCREMENT, teacher_id INTEGER NOT NULL, subject_id INTEGER NOT NULL, role VARCHAR(30) NOT NULL DEFAULT 'RESPONSABLE', period TEXT NOT NULL, UNIQUE(teacher_id, subject_id, period))");
    }

    private void insertTestData() {
        Base.exec("INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?,?,?,?,?)",
                "11111111", "Juan", "Perez", "1111111111", "juan@test.com");
        personId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?,?,?,?,?)",
                "22222222", "Ana", "Lopez", "2222222222", "ana@test.com");
        long otherPersonId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO teachers (id_persona, nroLegajo) VALUES (?,?)", personId, "LEG-001");
        teacherId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO teachers (id_persona, nroLegajo) VALUES (?,?)", otherPersonId, "LEG-002");
        otherTeacherId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Análisis Matemático");
        subjectId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Física I");
        otherSubjectId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        // Solo teacherId está asignado a subjectId en período 2025-1
        Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, role, period) VALUES (?,?,?,?)",
                teacherId, subjectId, "RESPONSABLE", "2025-1");
    }
}
