package com.is1.proyecto.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.services.EvaluationService;
import com.is1.proyecto.services.TeacherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Session;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para TeacherRoutes.
 * Los handlers son privados — se accede via reflection.
 */
@ExtendWith(MockitoExtension.class)
class TeacherRoutesTest {

    @Mock
    private TeacherService teacherService;

    @Mock
    private EvaluationService evaluationService;

    @Mock
    private Request req;

    @Mock
    private Response res;

    @Mock
    private Session session;

    private TeacherRoutes teacherRoutes;

    @BeforeEach
    void setUp() {
        teacherRoutes = new TeacherRoutes(teacherService, evaluationService, new ObjectMapper());
    }

    // ───── Helper: invoke private method via reflection ─────

    private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = TeacherRoutes.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        T result = (T) method.invoke(teacherRoutes, args);
        return result;
    }

    // =====================================================================
    // listTeachers
    // =====================================================================

    @Test
    void listTeachers_teacherRole_redirectsToDetail() throws Exception {
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("TEACHER");
        when(session.attribute("teacherId")).thenReturn(5L);

        ModelAndView result = invokePrivate("listTeachers",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNull(result);
        verify(res).redirect("/teachers/5");
        verify(teacherService, never()).getAllTeachers();
    }

    @Test
    void listTeachers_notTeacher_returnsModelWithTeachers() throws Exception {
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("ADMIN");

        List<Map<String, Object>> teachers = Arrays.asList(Map.of("id", 1, "name", "Prof A"));
        when(teacherService.getAllTeachers()).thenReturn(teachers);

        ModelAndView result = invokePrivate("listTeachers",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("teacher_list.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertSame(teachers, model.get("teachers"));
    }

    // =====================================================================
    // showTeacherDetail
    // =====================================================================

    @Test
    void showTeacherDetail_success_returnsModel() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("ADMIN");

        Map<String, Object> teacher = new HashMap<>(Map.of("id", 1, "firstName", "Juan"));
        when(teacherService.getTeacherWithPerson(1L)).thenReturn(teacher);
        List<Map<String, Object>> subjects = Arrays.asList(Map.of("subjectId", 10));
        when(teacherService.getAssignedSubjects(1L)).thenReturn(subjects);

        ModelAndView result = invokePrivate("showTeacherDetail",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("teacher_detail.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertSame(teacher, model.get("teacher"));
        assertSame(subjects, model.get("subjects"));
        assertEquals(1L, model.get("teacherId"));
    }

    @Test
    void showTeacherDetail_teacherRoleMismatch_halts403() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("TEACHER");
        when(session.attribute("teacherId")).thenReturn(99L);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () ->
                invokePrivate("showTeacherDetail",
                        new Class<?>[]{Request.class, Response.class}, req, res));
        assertInstanceOf(spark.HaltException.class, ex.getCause());
    }

    @Test
    void showTeacherDetail_teacherNotFound_halts404() throws Exception {
        when(req.params(":id")).thenReturn("99");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("ADMIN");
        when(teacherService.getTeacherWithPerson(99L)).thenReturn(null);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () ->
                invokePrivate("showTeacherDetail",
                        new Class<?>[]{Request.class, Response.class}, req, res));
        assertInstanceOf(spark.HaltException.class, ex.getCause());
    }

    // =====================================================================
    // showTeacherSubjects
    // =====================================================================

    @Test
    void showTeacherSubjects_success_returnsModel() throws Exception {
        when(req.params(":id")).thenReturn("2");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("ADMIN");

        Map<String, Object> teacher = Map.of("id", 2, "firstName", "Ana");
        when(teacherService.getTeacherWithPerson(2L)).thenReturn(teacher);
        List<Map<String, Object>> subjects = Arrays.asList(Map.of("subjectId", 10, "name", "Matematica"));
        when(teacherService.getAssignedSubjects(2L)).thenReturn(subjects);

        ModelAndView result = invokePrivate("showTeacherSubjects",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("teacher_subjects.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertSame(teacher, model.get("teacher"));
        assertSame(subjects, model.get("subjects"));
        assertEquals(2L, model.get("teacherId"));
    }

    // =====================================================================
    // showTeacherGrades
    // =====================================================================

    @Test
    void showTeacherGrades_success_returnsModel() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("ADMIN");

        Map<String, Object> teacher = Map.of("id", 1, "firstName", "Carlos");
        when(teacherService.getTeacherWithPerson(1L)).thenReturn(teacher);

        Map<String, Object> subject1 = new HashMap<>(Map.of("subjectId", 10));
        Map<String, Object> subject2 = new HashMap<>(Map.of("subjectId", 20));
        when(teacherService.getAssignedSubjects(1L)).thenReturn(Arrays.asList(subject1, subject2));

        List<Map<String, Object>> students1 = Arrays.asList(Map.of("studentId", 100));
        List<Map<String, Object>> students2 = Collections.emptyList();
        when(teacherService.getSubjectStudents(1L, 10L)).thenReturn(students1);
        when(teacherService.getSubjectStudents(1L, 20L)).thenReturn(students2);

        ModelAndView result = invokePrivate("showTeacherGrades",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("teacher_grades.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertSame(teacher, model.get("teacher"));
        assertEquals(1L, model.get("teacherId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subjectsWithGrades = (List<Map<String, Object>>) model.get("subjectsWithGrades");
        assertEquals(2, subjectsWithGrades.size());
    }

    // =====================================================================
    // showSubjectStudents
    // =====================================================================

    @Test
    void showSubjectStudents_success_returnsModel() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.params(":subjectId")).thenReturn("10");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("ADMIN");

        List<Map<String, Object>> students = Arrays.asList(Map.of("studentId", 100, "name", "Pedro"));
        when(teacherService.getSubjectStudents(1L, 10L)).thenReturn(students);

        ModelAndView result = invokePrivate("showSubjectStudents",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("teacher_subject_students.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertSame(students, model.get("students"));
        assertEquals(1L, model.get("teacherId"));
        assertEquals(10L, model.get("subjectId"));
    }

    // =====================================================================
    // showTeacherForm
    // =====================================================================

    @Test
    void showTeacherForm_noParams_returnsEmptyForm() throws Exception {
        when(req.queryParams("error")).thenReturn(null);
        when(req.queryParams("message")).thenReturn(null);

        ModelAndView result = invokePrivate("showTeacherForm",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("teacher_form.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertFalse(model.containsKey("errorMessage"));
        assertFalse(model.containsKey("successMessage"));
    }

    @Test
    void showTeacherForm_withMessages_inModel() throws Exception {
        when(req.queryParams("error")).thenReturn("Campo invalido");
        when(req.queryParams("message")).thenReturn("Exito!");

        ModelAndView result = invokePrivate("showTeacherForm",
                new Class<?>[]{Request.class, Response.class}, req, res);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals("Campo invalido", model.get("errorMessage"));
        assertEquals("Exito!", model.get("successMessage"));
    }

    // =====================================================================
    // handleRegisterTeacher
    // =====================================================================

    @Test
    void handleRegisterTeacher_ok_redirects() throws Exception {
        when(req.queryParams("dni")).thenReturn("12345678");
        when(req.queryParams("nroLegajo")).thenReturn("LEG-001");
        when(req.queryParams("firstName")).thenReturn("Juan");
        when(req.queryParams("lastName")).thenReturn("Perez");
        when(req.queryParams("phone")).thenReturn("555-1234");
        when(req.queryParams("email")).thenReturn("juan@test.com");

        TeacherService.TeacherRegisterResult ok = TeacherService.TeacherRegisterResult.ok("Registrado!");
        when(teacherService.registerTeacher(any(TeacherService.TeacherData.class))).thenReturn(ok);

        Object result = invokePrivate("handleRegisterTeacher",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(res).status(201);
        verify(res).redirect("/register_teacher?message=Registrado!");
    }

    @Test
    void handleRegisterTeacher_duplicate_redirects() throws Exception {
        when(req.queryParams("dni")).thenReturn("99999999");
        when(req.queryParams("nroLegajo")).thenReturn("LEG-002");
        when(req.queryParams("firstName")).thenReturn("Maria");
        when(req.queryParams("lastName")).thenReturn("Gomez");
        when(req.queryParams("phone")).thenReturn("");
        when(req.queryParams("email")).thenReturn("maria@test.com");

        TeacherService.TeacherRegisterResult dup = TeacherService.TeacherRegisterResult.duplicate("99999999");
        when(teacherService.registerTeacher(any(TeacherService.TeacherData.class))).thenReturn(dup);

        Object result = invokePrivate("handleRegisterTeacher",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(res).status(400);
        verify(res).redirect(contains("error"));
    }
}
