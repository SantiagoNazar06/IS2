package com.is1.proyecto.routes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.dto.StudentWithGradeDTO;
import com.is1.proyecto.services.TeacherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para TeacherRoutes.handleGetStudents.
 * Mockea TeacherService, Request y Response.
 * No necesita MockedStatic porque el route no llama a modelos ActiveJDBC directamente.
 */
@ExtendWith(MockitoExtension.class)
class TeacherRoutesTest {

    @Mock
    private TeacherService teacherService;

    @Mock
    private Request req;

    @Mock
    private Response res;

    private TeacherRoutes teacherRoutes;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        teacherRoutes = new TeacherRoutes(teacherService);
        objectMapper = new ObjectMapper();
    }

    // =====================================================================
    // GET /teachers/:id/subjects/:subjectId/students — casos exitosos
    // =====================================================================

    @Test
    void testHandleGetStudents_happyPath_returns200WithJsonArray() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("1");
        when(req.params(":subjectId")).thenReturn("5");

        List<StudentWithGradeDTO> mockStudents = Arrays.asList(
                new StudentWithGradeDTO(10L, "Juan Perez", "2025-03-01", 8.5, "2025-06-15"),
                new StudentWithGradeDTO(20L, "Maria Lopez", "2025-03-15", null, null)
        );
        when(teacherService.getStudentsBySubject(1, 5)).thenReturn(mockStudents);

        // Act
        Object result = teacherRoutes.handleGetStudents(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(200);
        assertNotNull(result);
        assertInstanceOf(String.class, result);

        List<Map<String, Object>> parsed = objectMapper.readValue(
                (String) result,
                new TypeReference<List<Map<String, Object>>>() {});
        assertEquals(2, parsed.size());

        Map<String, Object> first = parsed.get(0);
        assertEquals(10, ((Number) first.get("studentId")).intValue());
        assertEquals("Juan Perez", first.get("studentName"));
        assertEquals("2025-03-01", first.get("enrollmentDate"));
        assertEquals(8.5, ((Number) first.get("grade")).doubleValue());
        assertEquals("2025-06-15", first.get("gradeDate"));

        Map<String, Object> second = parsed.get(1);
        assertEquals(20, ((Number) second.get("studentId")).intValue());
        assertEquals("Maria Lopez", second.get("studentName"));
        assertEquals("2025-03-15", second.get("enrollmentDate"));
        assertNull(second.get("grade"));
        assertNull(second.get("gradeDate"));

        verify(teacherService).getStudentsBySubject(1, 5);
    }

    @Test
    void testHandleGetStudents_noStudents_returns200WithEmptyArray() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("1");
        when(req.params(":subjectId")).thenReturn("5");

        when(teacherService.getStudentsBySubject(1, 5))
                .thenReturn(Collections.emptyList());

        // Act
        Object result = teacherRoutes.handleGetStudents(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(200);
        assertNotNull(result);

        List<Map<String, Object>> parsed = objectMapper.readValue(
                (String) result,
                new TypeReference<List<Map<String, Object>>>() {});
        assertTrue(parsed.isEmpty());

        verify(teacherService).getStudentsBySubject(1, 5);
    }

    // =====================================================================
    // GET /teachers/:id/subjects/:subjectId/students — errores
    // =====================================================================

    @Test
    void testHandleGetStudents_teacherNotAssigned_returns403() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("1");
        when(req.params(":subjectId")).thenReturn("5");

        when(teacherService.getStudentsBySubject(1, 5))
                .thenThrow(new IllegalArgumentException("Teacher not assigned to this subject"));

        // Act
        Object result = teacherRoutes.handleGetStudents(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(403);
        assertNotNull(result);

        Map<String, String> parsed = objectMapper.readValue(
                (String) result,
                new TypeReference<Map<String, String>>() {});
        assertEquals("Teacher not assigned to this subject", parsed.get("error"));

        verify(teacherService).getStudentsBySubject(1, 5);
    }

    @Test
    void testHandleGetStudents_teacherNotFound_returns404() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("99");
        when(req.params(":subjectId")).thenReturn("5");

        when(teacherService.getStudentsBySubject(99, 5))
                .thenThrow(new IllegalArgumentException("Teacher not found"));

        // Act
        Object result = teacherRoutes.handleGetStudents(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(404);
        assertNotNull(result);

        Map<String, String> parsed = objectMapper.readValue(
                (String) result,
                new TypeReference<Map<String, String>>() {});
        assertEquals("Teacher not found", parsed.get("error"));

        verify(teacherService).getStudentsBySubject(99, 5);
    }

    @Test
    void testHandleGetStudents_subjectNotFound_returns404() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("1");
        when(req.params(":subjectId")).thenReturn("99");

        when(teacherService.getStudentsBySubject(1, 99))
                .thenThrow(new IllegalArgumentException("Subject not found"));

        // Act
        Object result = teacherRoutes.handleGetStudents(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(404);
        assertNotNull(result);

        Map<String, String> parsed = objectMapper.readValue(
                (String) result,
                new TypeReference<Map<String, String>>() {});
        assertEquals("Subject not found", parsed.get("error"));

        verify(teacherService).getStudentsBySubject(1, 99);
    }

    @Test
    void testHandleGetStudents_invalidTeacherId_returns400() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("invalid");

        // Act
        Object result = teacherRoutes.handleGetStudents(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(400);
        assertNotNull(result);

        Map<String, String> parsed = objectMapper.readValue(
                (String) result,
                new TypeReference<Map<String, String>>() {});
        assertNotNull(parsed.get("error"));

        // Service no debe ser llamado si falla el parseo
        verify(teacherService, never()).getStudentsBySubject(anyInt(), anyInt());
    }
}
