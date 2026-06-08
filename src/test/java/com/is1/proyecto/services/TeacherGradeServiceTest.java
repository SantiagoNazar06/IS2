package com.is1.proyecto.services;

import com.is1.proyecto.models.Evaluation;
import com.is1.proyecto.repositories.EvaluationRepository;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for {@link EvaluationService#registerTeacherGrade} (issue #24).
 * Cubre los tres estados académicos (REGULAR, APROBADA, PROMOCION), la transición
 * REGULAR→final y los códigos de error. Usa SQLite + TeacherService mockeado.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeacherGradeServiceTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-teacher-grade.db";

    private EvaluationService service;
    private TeacherService teacherService;

    private static final Long TEACHER_ID = 10L;
    private int subjectId;
    private int enrollmentId;       // inscripción activa (ENROLLED)

    @BeforeAll
    void setupDatabase() {
        new File("./target/test-teacher-grade.db").delete();
        System.setProperty("db.url", JDBC_URL);
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE IF NOT EXISTS enrollments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL, " +
                "period TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'ENROLLED')");
        Base.exec("CREATE TABLE IF NOT EXISTS evaluations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "enrollment_id INTEGER, " +
                "grade DOUBLE, " +
                "condition_type TEXT, " +
                "evaluation_date DATE)");
        Base.close();
    }

    @BeforeEach
    void openAndSeed() {
        try { Base.close(); } catch (Exception ignored) { }
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");

        service = new EvaluationService(new EvaluationRepository());
        teacherService = mock(TeacherService.class);

        subjectId = 100;
        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?,?,?,?)",
                1, subjectId, "2024-01", "ENROLLED");
        enrollmentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();
    }

    @AfterEach
    void clean() {
        Base.exec("DELETE FROM evaluations");
        Base.exec("DELETE FROM enrollments");
        Base.close();
    }

    @AfterAll
    void teardown() {
        try {
            Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
            Base.exec("DROP TABLE IF EXISTS evaluations");
            Base.exec("DROP TABLE IF EXISTS enrollments");
            Base.close();
        } catch (Exception ignored) { }
        new File("./target/test-teacher-grade.db").delete();
    }

    private void assigned() {
        when(teacherService.verifyAssignment(TEACHER_ID, (long) subjectId)).thenReturn(true);
    }

    private int insertEnrollment(String status) {
        Base.exec("INSERT INTO enrollments (student_id, subject_id, period, status) VALUES (?,?,?,?)",
                2, subjectId, "2024-02", status);
        return ((Number) Base.firstCell("SELECT last_insert_rowid()")).intValue();
    }

    // ───── APROBADA / PROMOCION: cierran la materia (COMPLETED) ─────

    @Test
    void aprobada_creates201AndCompletesEnrollment() {
        assigned();
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "APROBADA", 8.0, teacherService);

        assertTrue(r.success);
        assertEquals(201, r.statusCode);
        assertEquals("APROBADA", r.evaluation.getCondition());
        assertEquals(8.0, r.evaluation.getEvaluationGrade(), 0.001);
        assertEquals("COMPLETED", r.enrollmentStatus);
    }

    @Test
    void promocion_creates201AndCompletesEnrollment() {
        assigned();
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "PROMOCION", 9.0, teacherService);

        assertTrue(r.success);
        assertEquals("PROMOCION", r.evaluation.getCondition());
        assertEquals("COMPLETED", r.enrollmentStatus);
    }

    // ───── REGULAR: sin nota, la inscripción sigue activa ─────

    @Test
    void regular_creates201WithoutGradeAndKeepsEnrollmentActive() {
        assigned();
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "REGULAR", null, teacherService);

        assertTrue(r.success);
        assertEquals(201, r.statusCode);
        assertEquals("REGULAR", r.evaluation.getCondition());
        assertNull(r.evaluation.getEvaluationGrade());
        assertEquals("ENROLLED", r.enrollmentStatus);
    }

    // ───── Transición REGULAR -> APROBADA ─────

    @Test
    void regularThenAprobada_transitions200AndCompletes() {
        assigned();
        service.registerTeacherGrade(TEACHER_ID, enrollmentId, "REGULAR", null, teacherService);

        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "APROBADA", 7.0, teacherService);

        assertTrue(r.success);
        assertEquals(200, r.statusCode);
        assertEquals("APROBADA", r.evaluation.getCondition());
        assertEquals("COMPLETED", r.enrollmentStatus);
        // Sigue habiendo una sola evaluación para la inscripción
        assertEquals(1L, ((Number) Base.firstCell(
                "SELECT COUNT(*) FROM evaluations WHERE enrollment_id = ?", enrollmentId)).longValue());
    }

    // ───── 409: condición ya final / ya regular ─────

    @Test
    void aprobadaTwice_returns409() {
        assigned();
        service.registerTeacherGrade(TEACHER_ID, enrollmentId, "APROBADA", 8.0, teacherService);

        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "APROBADA", 9.0, teacherService);

        assertFalse(r.success);
        assertEquals(409, r.statusCode);
    }

    @Test
    void regularTwice_returns409() {
        assigned();
        service.registerTeacherGrade(TEACHER_ID, enrollmentId, "REGULAR", null, teacherService);

        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "REGULAR", null, teacherService);

        assertFalse(r.success);
        assertEquals(409, r.statusCode);
    }

    // ───── 404 / 400: inscripción inexistente o cancelada ─────

    @Test
    void unknownEnrollment_returns404() {
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, 999999, "APROBADA", 7.0, teacherService);
        assertFalse(r.success);
        assertEquals(404, r.statusCode);
    }

    @Test
    void droppedEnrollment_returns400() {
        int dropped = insertEnrollment("DROPPED");
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, dropped, "APROBADA", 7.0, teacherService);
        assertFalse(r.success);
        assertEquals(400, r.statusCode);
    }

    // ───── 403: docente no asignado ─────

    @Test
    void teacherNotAssigned_returns403() {
        when(teacherService.verifyAssignment(TEACHER_ID, (long) subjectId)).thenReturn(false);
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "APROBADA", 7.0, teacherService);
        assertFalse(r.success);
        assertEquals(403, r.statusCode);
    }

    // ───── 400: validaciones de condición y nota ─────

    @Test
    void invalidCondition_returns400() {
        assigned();
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "DESAPROBADA", 3.0, teacherService);
        assertFalse(r.success);
        assertEquals(400, r.statusCode);
    }

    @Test
    void aprobadaWithGradeBelowFive_returns400() {
        assigned();
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "APROBADA", 4.0, teacherService);
        assertFalse(r.success);
        assertEquals(400, r.statusCode);
    }

    @Test
    void aprobadaWithoutGrade_returns400() {
        assigned();
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "APROBADA", null, teacherService);
        assertFalse(r.success);
        assertEquals(400, r.statusCode);
    }

    @Test
    void gradeAboveTen_returns400() {
        assigned();
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, enrollmentId, "APROBADA", 15.0, teacherService);
        assertFalse(r.success);
        assertEquals(400, r.statusCode);
    }

    @Test
    void nullEnrollmentId_returns400() {
        EvaluationService.GradeResult r =
                service.registerTeacherGrade(TEACHER_ID, null, "APROBADA", 7.0, teacherService);
        assertFalse(r.success);
        assertEquals(400, r.statusCode);
    }
}
