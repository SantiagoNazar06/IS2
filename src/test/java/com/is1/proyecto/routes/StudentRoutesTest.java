package com.is1.proyecto.routes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.dto.StudentListDTO;
import com.is1.proyecto.models.Enrollment;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Session;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para StudentRoutes.
 * Handlers privados — acceso via reflection.
 */
@ExtendWith(MockitoExtension.class)
class StudentRoutesTest {

    @Mock
    private StudentService studentService;

    @Mock
    private CareerService careerService;

    @Mock
    private SubjectService subjectService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Request req;

    @Mock
    private Response res;

    @Mock
    private Session session;

    private StudentRoutes studentRoutes;

    @BeforeEach
    void setUp() {
        studentRoutes = new StudentRoutes(studentService, careerService, subjectService, objectMapper);
    }

    // ───── Helper: invoke private method ─────

    private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = StudentRoutes.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        T result = (T) method.invoke(studentRoutes, args);
        return result;
    }

    // =====================================================================
    // showStudentForm
    // =====================================================================

    @Test
    void showStudentForm_noParams_returnsEmptyForm() throws Exception {
        when(req.queryParams("error")).thenReturn(null);
        when(req.queryParams("message")).thenReturn(null);

        ModelAndView result = invokePrivate("showStudentForm",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("student_form.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertFalse(model.containsKey("errorMessage"));
        assertFalse(model.containsKey("successMessage"));
    }

    @Test
    void showStudentForm_withMessages_inModel() throws Exception {
        when(req.queryParams("error")).thenReturn("DNI invalido");
        when(req.queryParams("message")).thenReturn("Estudiante creado!");

        ModelAndView result = invokePrivate("showStudentForm",
                new Class<?>[]{Request.class, Response.class}, req, res);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals("DNI invalido", model.get("errorMessage"));
        assertEquals("Estudiante creado!", model.get("successMessage"));
    }

    // =====================================================================
    // handleRegisterStudent
    // =====================================================================

    @Test
    void handleRegisterStudent_success_redirects() throws Exception {
        when(req.queryParams("dni")).thenReturn("12345678");
        when(req.queryParams("student_type")).thenReturn("REGULAR");
        when(req.queryParams("firstName")).thenReturn("Juan");
        when(req.queryParams("lastName")).thenReturn("Perez");
        when(req.queryParams("phone")).thenReturn("555-1234");
        when(req.queryParams("email")).thenReturn("juan@test.com");

        StudentService.StudentRegisterResult ok = StudentService.StudentRegisterResult.ok("Creado!");
        when(studentService.registerStudent(any(StudentService.StudentData.class))).thenReturn(ok);

        Object result = invokePrivate("handleRegisterStudent",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(res).status(201);
        verify(res).redirect(contains("/register_student"));
    }

    // =====================================================================
    // showStudents
    // =====================================================================

    @Test
    void showStudents_admin_returnsFullList() throws Exception {
        when(req.queryParams("careerId")).thenReturn(null);
        when(req.queryParams("subjectId")).thenReturn(null);
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("ADMIN");

        StudentListDTO s1 = new StudentListDTO(Map.of("student_id", 1L, "full_name", "Juan Perez", "dni", "123", "email", "j@t.com", "careers", "Ing"));
        when(studentService.getStudents(null, null, null, "ADMIN")).thenReturn(Arrays.asList(s1));
        when(careerService.getAllCareers()).thenReturn(Collections.emptyList());
        when(subjectService.getAllSubjects(null)).thenReturn(Collections.emptyList());

        ModelAndView result = invokePrivate("showStudents",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("students.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertSame(s1, ((List<StudentListDTO>) model.get("students")).get(0));
        assertNull(model.get("selectedCareerId"));
        assertNull(model.get("selectedSubjectId"));
    }

    @Test
    void showStudents_withFilters_passesToService() throws Exception {
        when(req.queryParams("careerId")).thenReturn("1");
        when(req.queryParams("subjectId")).thenReturn("2");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("ADMIN");

        when(studentService.getStudents(1L, 2L, null, "ADMIN")).thenReturn(Collections.emptyList());
        when(careerService.getAllCareers()).thenReturn(Collections.emptyList());
        when(subjectService.getAllSubjects(null)).thenReturn(Collections.emptyList());

        ModelAndView result = invokePrivate("showStudents",
                new Class<?>[]{Request.class, Response.class}, req, res);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals(1L, model.get("selectedCareerId"));
        assertEquals(2L, model.get("selectedSubjectId"));
    }

    @Test
    void showStudents_teacherRole_usesTeacherIdScope() throws Exception {
        when(req.queryParams("careerId")).thenReturn(null);
        when(req.queryParams("subjectId")).thenReturn(null);
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("TEACHER");
        when(session.attribute("teacherId")).thenReturn(10L);

        when(studentService.getStudents(null, null, 10L, "TEACHER")).thenReturn(Collections.emptyList());
        when(careerService.getAllCareers()).thenReturn(Collections.emptyList());
        when(subjectService.getAllSubjects(null)).thenReturn(Collections.emptyList());

        ModelAndView result = invokePrivate("showStudents",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("students.mustache", result.getViewName());
    }

    // =====================================================================
    // handleEnroll — the BIG one with many code paths
    // =====================================================================

    @Test
    void handleEnroll_invalidStudentId_returns400() throws Exception {
        when(req.params(":id")).thenReturn("abc");
        // Need to use the real ObjectMapper for JSON serialization
        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleEnroll", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(400);
        verify(res).type("application/json");
        assertInstanceOf(String.class, result);
        String json = (String) result;
        assertTrue(json.contains("ID de estudiante inv\u00e1lido"));
    }

    @Test
    void handleEnroll_studentMismatch_returns403() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);
        when(session.attribute("studentId")).thenReturn(99L);

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleEnroll", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(403);
        verify(res).type("application/json");
        String json = (String) result;
        assertTrue(json.contains("No autorizado"));
    }

    @Test
    void handleEnroll_invalidJsonBody_returns400() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);
        when(session.attribute("studentId")).thenReturn(1L);
        when(req.body()).thenReturn("not json");

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleEnroll", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(400);
        String json = (String) result;
        assertTrue(json.contains("Body JSON inv\u00e1lido"));
    }

    @Test
    void handleEnroll_missingFields_returns400() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);
        when(session.attribute("studentId")).thenReturn(1L);
        when(req.body()).thenReturn("{}");

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleEnroll", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(400);
        String json = (String) result;
        assertTrue(json.contains("subjectId"));
    }

    @Test
    void handleEnroll_invalidPeriodFormat_returns400() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);
        when(session.attribute("studentId")).thenReturn(1L);
        when(req.body()).thenReturn("{\"subjectId\":10,\"period\":\"bad-format\"}");

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleEnroll", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(400);
        String json = (String) result;
        assertTrue(json.contains("Formato de per\u00edodo inv\u00e1lido"));
    }

    // =====================================================================
    // handleCancelEnrollment — DELETE /students/:id/enrollments/:enrollmentId
    // =====================================================================

    @Test
    void handleCancelEnrollment_invalidStudentId_returns400() throws Exception {
        when(req.params(":id")).thenReturn("abc");

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleCancelEnrollment", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(400);
        verify(res).type("application/json");
        String json = (String) result;
        assertTrue(json.contains("ID de estudiante o inscripción inválido"));
    }

    @Test
    void handleCancelEnrollment_invalidEnrollmentId_returns400() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.params(":enrollmentId")).thenReturn("abc");

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleCancelEnrollment", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(400);
        verify(res).type("application/json");
        String json = (String) result;
        assertTrue(json.contains("ID de estudiante o inscripción inválido"));
    }

    @Test
    void handleCancelEnrollment_studentNotOwner_returns403() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.params(":enrollmentId")).thenReturn("10");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("STUDENT");
        when(session.attribute("studentId")).thenReturn(99L);

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleCancelEnrollment", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(403);
        verify(res).type("application/json");
        String json = (String) result;
        assertTrue(json.contains("No autorizado"));
    }

    @Test
    void handleCancelEnrollment_notFound_returns404() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.params(":enrollmentId")).thenReturn("10");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("STUDENT");
        when(session.attribute("studentId")).thenReturn(1L);

        StudentService.CancelEnrollmentResult notFound = StudentService.CancelEnrollmentResult.notFound();
        when(studentService.cancelEnrollment(1L, 10)).thenReturn(notFound);

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleCancelEnrollment", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(404);
        verify(res).type("application/json");
        String json = (String) result;
        assertTrue(json.contains("Enrollment no encontrado"));
    }

    @Test
    void handleCancelEnrollment_conflict_returns409() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.params(":enrollmentId")).thenReturn("10");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("STUDENT");
        when(session.attribute("studentId")).thenReturn(1L);

        StudentService.CancelEnrollmentResult conflict = StudentService.CancelEnrollmentResult.conflict(
            "No se puede cancelar la inscripción porque ya tiene calificaciones cargadas.");
        when(studentService.cancelEnrollment(1L, 10)).thenReturn(conflict);

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleCancelEnrollment", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(409);
        verify(res).type("application/json");
        String json = (String) result;
        assertTrue(json.contains("calificaciones cargadas"));
    }

    @Test
    void handleCancelEnrollment_success_returns204() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.params(":enrollmentId")).thenReturn("10");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("STUDENT");
        when(session.attribute("studentId")).thenReturn(1L);

        StudentService.CancelEnrollmentResult ok = StudentService.CancelEnrollmentResult.ok();
        when(studentService.cancelEnrollment(1L, 10)).thenReturn(ok);

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleCancelEnrollment", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(204);
        verify(res).type("application/json");
        assertEquals("", result);
    }

    @Test
    void handleCancelEnrollment_adminCanCancelAny_returns204() throws Exception {
        when(req.params(":id")).thenReturn("5");
        when(req.params(":enrollmentId")).thenReturn("10");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("ADMIN");

        StudentService.CancelEnrollmentResult ok = StudentService.CancelEnrollmentResult.ok();
        when(studentService.cancelEnrollment(5L, 10)).thenReturn(ok);

        StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

        Method method = StudentRoutes.class.getDeclaredMethod("handleCancelEnrollment", Request.class, Response.class);
        method.setAccessible(true);
        Object result = method.invoke(realRoutes, req, res);

        verify(res).status(204);
        assertEquals("", result);
    }

    @Test
    void handleEnroll_success_returns201() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);
        when(session.attribute("studentId")).thenReturn(1L);
        when(req.body()).thenReturn("{\"subjectId\":10,\"period\":\"2026-1\"}");

        try (MockedStatic<Student> studentMock = mockStatic(Student.class);
             MockedStatic<Subject> subjectMock = mockStatic(Subject.class);
             MockedStatic<Enrollment> enrollmentMock = mockStatic(Enrollment.class);
             MockedConstruction<CorrelationEngine> engineMock = mockConstruction(CorrelationEngine.class, (mock, ctx) -> {
                 com.is1.proyecto.services.ValidationResult vr = mock(com.is1.proyecto.services.ValidationResult.class);
                 when(vr.isAllowed()).thenReturn(true);
                 when(mock.canEnroll(anyLong(), anyLong())).thenReturn(vr);
             })) {

            Student mockStudent = mock(Student.class);
            studentMock.when(() -> Student.findById(1L)).thenReturn(mockStudent);

            Subject mockSubject = mock(Subject.class);
            subjectMock.when(() -> Subject.findFirst(anyString(), anyLong())).thenReturn(mockSubject);

            enrollmentMock.when(() -> Enrollment.findFirst(anyString(), anyLong(), anyLong(), anyString())).thenReturn(null);

            StudentRoutes realRoutes = new StudentRoutes(studentService, careerService, subjectService, new ObjectMapper());

            Method method = StudentRoutes.class.getDeclaredMethod("handleEnroll", Request.class, Response.class);
            method.setAccessible(true);
            // Use real Enrollment constructor from MockedConstruction
            try (MockedConstruction<Enrollment> enrollCons = mockConstruction(Enrollment.class, (mock, ctx) -> {
                when(mock.getId()).thenReturn(999L);
                when(mock.getString("created_at")).thenReturn("2026-06-06");
            })) {
                Object result = method.invoke(realRoutes, req, res);

                verify(res).status(201);
                String json = (String) result;
                assertTrue(json.contains("Inscripci\u00f3n realizada exitosamente"));
            }
        }
    }
}
