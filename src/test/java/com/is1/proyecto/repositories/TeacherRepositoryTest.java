package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.TeacherAssignment;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TeacherRepository.
 * Extends existing coverage — fills gaps for methods not yet tested.
 * Covers all 14 methods.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeacherRepositoryTest {

    private static final String DB_URL = "jdbc:sqlite:./target/teacher-repo-test.db";
    private TeacherRepository repo;

    private long personId;
    private long otherPersonId;
    private long teacherId;
    private long otherTeacherId;
    private long subjectId;
    private long otherSubjectId;
    private long assignmentId;

    @BeforeAll
    void setUp() {
        new File("./target/teacher-repo-test.db").delete();
        System.setProperty("db.url", DB_URL);
        repo = new TeacherRepository();
        Base.open("org.sqlite.JDBC", DB_URL, "", "");
        createSchema();
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        Base.open("org.sqlite.JDBC", DB_URL, "", "");
    }

    @BeforeEach
    void insertTestData() {
        Base.exec("INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?,?,?,?,?)",
                "11111111", "Juan", "Perez", "1111111111", "juan@test.com");
        personId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?,?,?,?,?)",
                "22222222", "Ana", "Lopez", "2222222222", "ana@test.com");
        otherPersonId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

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
        assignmentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        // Second assignment for otherTeacherId at a different period
        Base.exec("INSERT INTO teacher_assignments (teacher_id, subject_id, role, period) VALUES (?,?,?,?)",
                otherTeacherId, otherSubjectId, "JTP", "2025-1");

        // Student with evaluations for findSubjectStudents and findAssignedSubjectsWithCount
        Base.exec("INSERT INTO students (id_person, student_type) VALUES (?,?)",
                otherPersonId, "REGULAR");
        long studentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO evaluations (student_id, subject_id, evaluation_date, evaluation_note, condition_type) VALUES (?,?,?,?,?)",
                studentId, subjectId, "2025-06-01", 8, "aprobado");
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM evaluations");
        Base.exec("DELETE FROM teacher_assignments");
        Base.exec("DELETE FROM subjects");
        Base.exec("DELETE FROM students");
        Base.exec("DELETE FROM teachers");
        Base.exec("DELETE FROM persons");
        Base.close();
    }

    @AfterAll
    void tearDown() {
        try { Base.open("org.sqlite.JDBC", DB_URL, "", "");
            Base.exec("DROP TABLE IF EXISTS evaluations");
            Base.exec("DROP TABLE IF EXISTS teacher_assignments");
            Base.exec("DROP TABLE IF EXISTS subjects");
            Base.exec("DROP TABLE IF EXISTS students");
            Base.exec("DROP TABLE IF EXISTS teachers");
            Base.exec("DROP TABLE IF EXISTS persons");
            Base.close();
        } catch (Exception ignored) { }
        new File("./target/teacher-repo-test.db").delete();
    }

    // =============================
    // AC-1: findByLegajo (existing coverage preserved)
    // =============================
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

    // =============================
    // AC-2: findByPersonId (existing coverage preserved)
    // =============================
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

    // =============================
    // AC-3: findBySubject (existing coverage preserved)
    // =============================
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
        assertTrue(repo.findBySubject(otherSubjectId + 999).isEmpty());
    }

    // =============================
    // AC-4: findByPeriod (existing coverage preserved)
    // =============================
    @Test
    void findByPeriod_periodWithAssignments_returnsList() {
        List<Teacher> result = repo.findByPeriod("2025-1");
        assertFalse(result.isEmpty());
    }

    @Test
    void findByPeriod_unknownPeriod_returnsEmptyList() {
        assertTrue(repo.findByPeriod("1900-1").isEmpty());
    }

    // =============================
    // AC-5: getAssignedSubjects (existing coverage preserved)
    // =============================
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
        // Teacher with no assignments — use a new teacher ID that has no assignments
        Base.exec("INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?,?,?,?,?)",
                "33333333", "Carlos", "Garcia", "3333333333", "carlos@test.com");
        long newPersonId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();
        Base.exec("INSERT INTO teachers (id_persona, nroLegajo) VALUES (?,?)", newPersonId, "LEG-003");
        long newTeacherId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        assertTrue(repo.getAssignedSubjects(newTeacherId).isEmpty());
    }

    // =============================
    // existsAssignment — NEW coverage
    // =============================
    @Test
    void existsAssignment_existing_returnsTrue() {
        assertTrue(repo.existsAssignment(teacherId, subjectId));
    }

    @Test
    void existsAssignment_nonExisting_returnsFalse() {
        assertFalse(repo.existsAssignment(999L, 999L));
    }

    // =============================
    // findAllWithPersons — NEW coverage
    // =============================
    @Test
    void findAllWithPersons_returnsList() {
        List<Map<String, Object>> result = repo.findAllWithPersons();
        assertEquals(2, result.size());
        for (Map<String, Object> row : result) {
            assertTrue(row.containsKey("teacherId"));
            assertTrue(row.containsKey("legajo"));
            assertTrue(row.containsKey("firstName"));
            assertTrue(row.containsKey("lastName"));
        }
    }

    // =============================
    // findWithPerson — NEW coverage
    // =============================
    @Test
    void findWithPerson_existingId_returnsMap() {
        Map<String, Object> result = repo.findWithPerson(teacherId);
        assertNotNull(result);
        assertEquals("LEG-001", result.get("legajo"));
        assertEquals("Juan", result.get("firstName"));
        assertEquals("Perez", result.get("lastName"));
    }

    @Test
    void findWithPerson_unknownId_returnsNull() {
        assertNull(repo.findWithPerson(9999L));
    }

    // =============================
    // findAssignedSubjectsWithCount — NEW coverage
    // =============================
    @Test
    void findAssignedSubjectsWithCount_teacherWithAssignments_returnsList() {
        List<Map<String, Object>> result = repo.findAssignedSubjectsWithCount(teacherId);
        // teacherId has 1 assignment with 1 evaluation
        assertEquals(1, result.size());
        Map<String, Object> entry = result.get(0);
        assertTrue(entry.containsKey("subjectId"));
        assertTrue(entry.containsKey("subjectName"));
        assertTrue(entry.containsKey("role"));
        assertTrue(entry.containsKey("period"));
        assertTrue(entry.containsKey("studentCount"));
    }

    @Test
    void findAssignedSubjectsWithCount_teacherWithNoAssignments_returnsEmptyList() {
        Base.exec("INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?,?,?,?,?)",
                "44444444", "Diana", "Martinez", "4444444444", "diana@test.com");
        long newPersonId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();
        Base.exec("INSERT INTO teachers (id_persona, nroLegajo) VALUES (?,?)", newPersonId, "LEG-004");
        long newTeacherId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        assertTrue(repo.findAssignedSubjectsWithCount(newTeacherId).isEmpty());
    }

    // =============================
    // findSubjectStudents — NEW coverage
    // =============================
    @Test
    void findSubjectStudents_existingSubject_returnsList() {
        List<Map<String, Object>> result = repo.findSubjectStudents(subjectId);
        assertFalse(result.isEmpty());
        Map<String, Object> entry = result.get(0);
        assertTrue(entry.containsKey("studentId"));
        assertTrue(entry.containsKey("studentType"));
        assertTrue(entry.containsKey("firstName"));
        assertTrue(entry.containsKey("grade"));
        assertEquals(8, ((Number) entry.get("grade")).intValue());
    }

    // =============================
    // findByTeacherAndPeriod — NEW coverage
    // =============================
    @Test
    void findByTeacherAndPeriod_existing_returnsList() {
        List<TeacherAssignment> result = repo.findByTeacherAndPeriod(teacherId, "2025-1");
        assertEquals(1, result.size());
        assertEquals("RESPONSABLE", result.get(0).getRole().name());
    }

    @Test
    void findByTeacherAndPeriod_unknownPeriod_returnsEmptyList() {
        List<TeacherAssignment> result = repo.findByTeacherAndPeriod(teacherId, "1900-1");
        assertTrue(result.isEmpty());
    }

    // =============================
    // findAllAssignmentsWithDetails — NEW coverage
    // =============================
    @Test
    void findAllAssignmentsWithDetails_returnsList() {
        List<Map<String, Object>> result = repo.findAllAssignmentsWithDetails();
        assertEquals(2, result.size());
        for (Map<String, Object> row : result) {
            assertTrue(row.containsKey("teacher_id"));
            assertTrue(row.containsKey("subject_id"));
            assertTrue(row.containsKey("role"));
            assertTrue(row.containsKey("period"));
            assertTrue(row.containsKey("nroLegajo"));
            assertTrue(row.containsKey("subject_name"));
        }
    }

    // =============================
    // Helpers
    // =============================
    private void createSchema() {
        Base.exec("CREATE TABLE IF NOT EXISTS persons (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "dni TEXT NOT NULL UNIQUE, " +
                "firstName TEXT NOT NULL, " +
                "lastName TEXT NOT NULL, " +
                "phone TEXT NOT NULL, " +
                "email TEXT NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS teachers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_persona INTEGER NOT NULL, " +
                "nroLegajo VARCHAR(30) NOT NULL UNIQUE)");
        Base.exec("CREATE TABLE IF NOT EXISTS subjects (" +
                "id_subject INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "code TEXT, " +
                "subject_name TEXT NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_person INTEGER NOT NULL, " +
                "student_type TEXT NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS evaluations (" +
                "id_evaluations INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL, " +
                "evaluation_date DATE NOT NULL, " +
                "evaluation_note INTEGER, " +
                "condition_type TEXT)");
        Base.exec("CREATE TABLE IF NOT EXISTS teacher_assignments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "teacher_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL, " +
                "role VARCHAR(30) NOT NULL DEFAULT 'RESPONSABLE', " +
                "period TEXT NOT NULL, " +
                "UNIQUE(teacher_id, subject_id, period))");
    }
}
