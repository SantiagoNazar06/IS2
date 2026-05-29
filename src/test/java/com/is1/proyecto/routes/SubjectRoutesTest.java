package com.is1.proyecto.routes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.ConditionType;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.services.ConditionService;
import com.is1.proyecto.services.PrerequisiteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para SubjectRoutes.
 * Utiliza mocks de Spark Request/Response y de ConditionService.
 * Sigue el patron de ConditionServiceTest con MockedStatic para Subject.findById.
 */
@ExtendWith(MockitoExtension.class)
class SubjectRoutesTest {

    @Mock
    private ConditionService conditionService;

    @Mock
    private Request req;

    @Mock
    private Response res;

    private SubjectRoutes subjectRoutes;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        subjectRoutes = new SubjectRoutes(conditionService);
        objectMapper = new ObjectMapper();
    }

    // =====================================================================
    // GET /subjects/:id/prerequisites
    // =====================================================================

    @Test
    void testGetPrerequisites_subjectExists_returns200WithJsonArray() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("1");

        PrerequisiteDTO dto = new PrerequisiteDTO(1, 1, 2, "Matematica", ConditionType.REGULAR);
        when(conditionService.getPrerequisites(1)).thenReturn(Arrays.asList(dto));

        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            Subject subject = mock(Subject.class);
            subjectMock.when(() -> Subject.findById(1)).thenReturn(subject);

            // Act
            Object result = subjectRoutes.handleGetPrerequisites(req, res);

            // Assert
            verify(res).type("application/json");
            verify(res).status(200);
            assertNotNull(result);
            assertInstanceOf(String.class, result);

            List<PrerequisiteDTO> parsed = objectMapper.readValue(
                    (String) result,
                    new TypeReference<List<PrerequisiteDTO>>() {});
            assertEquals(1, parsed.size());
            PrerequisiteDTO actual = parsed.get(0);
            assertEquals(1, actual.getId());
            assertEquals(1, actual.getSubjectId());
            assertEquals(2, actual.getPrerequisiteSubjectId());
            assertEquals("Matematica", actual.getPrerequisiteSubjectName());
            assertEquals(ConditionType.REGULAR, actual.getType());

            verify(conditionService).getPrerequisites(1);
            subjectMock.verify(() -> Subject.findById(1));
        }
    }

    @Test
    void testGetPrerequisites_subjectNotFound_returns404WithError() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("99");

        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            subjectMock.when(() -> Subject.findById(99)).thenReturn(null);

            // Act
            Object result = subjectRoutes.handleGetPrerequisites(req, res);

            // Assert
            verify(res).type("application/json");
            verify(res).status(404);
            assertNotNull(result);
            assertInstanceOf(String.class, result);

            Map<String, String> parsed = objectMapper.readValue(
                    (String) result,
                    new TypeReference<Map<String, String>>() {});
            assertEquals("Subject not found", parsed.get("error"));

            // Service should NOT be called when subject doesn't exist
            verify(conditionService, never()).getPrerequisites(anyInt());
            subjectMock.verify(() -> Subject.findById(99));
        }
    }

    @Test
    void testGetPrerequisites_emptyList_returns200WithEmptyArray() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("2");

        when(conditionService.getPrerequisites(2)).thenReturn(Collections.emptyList());

        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            Subject subject = mock(Subject.class);
            subjectMock.when(() -> Subject.findById(2)).thenReturn(subject);

            // Act
            Object result = subjectRoutes.handleGetPrerequisites(req, res);

            // Assert
            verify(res).type("application/json");
            verify(res).status(200);
            assertNotNull(result);

            List<PrerequisiteDTO> parsed = objectMapper.readValue(
                    (String) result,
                    new TypeReference<List<PrerequisiteDTO>>() {});
            assertTrue(parsed.isEmpty());

            verify(conditionService).getPrerequisites(2);
        }
    }

    // =====================================================================
    // POST /subjects/:id/prerequisites
    // =====================================================================

    @Test
    void testAddPrerequisite_happyPath_returns201WithJson() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("1");
        String body = "{\"prerequisiteSubjectId\": 2, \"type\": \"REGULAR\"}";
        when(req.body()).thenReturn(body);

        PrerequisiteDTO created = new PrerequisiteDTO(10, 1, 2, "Matematica", ConditionType.REGULAR);
        when(conditionService.addPrerequisite(1, 2, ConditionType.REGULAR)).thenReturn(created);

        // Act
        Object result = subjectRoutes.handleAddPrerequisite(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(201);
        assertNotNull(result);
        assertInstanceOf(String.class, result);

        PrerequisiteDTO actual = objectMapper.readValue((String) result, PrerequisiteDTO.class);
        assertEquals(10, actual.getId());
        assertEquals(1, actual.getSubjectId());
        assertEquals(2, actual.getPrerequisiteSubjectId());
        assertEquals("Matematica", actual.getPrerequisiteSubjectName());
        assertEquals(ConditionType.REGULAR, actual.getType());

        verify(conditionService).addPrerequisite(1, 2, ConditionType.REGULAR);
    }

    @Test
    void testAddPrerequisite_illegalArgument_returns409WithError() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("1");
        String body = "{\"prerequisiteSubjectId\": 1, \"type\": \"REGULAR\"}";
        when(req.body()).thenReturn(body);

        when(conditionService.addPrerequisite(1, 1, ConditionType.REGULAR))
                .thenThrow(new IllegalArgumentException("Una materia no puede ser requisito de si misma"));

        // Act
        Object result = subjectRoutes.handleAddPrerequisite(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(409);
        assertNotNull(result);

        Map<String, String> parsed = objectMapper.readValue(
                (String) result,
                new TypeReference<Map<String, String>>() {});
        assertEquals("Una materia no puede ser requisito de si misma", parsed.get("error"));

        verify(conditionService).addPrerequisite(1, 1, ConditionType.REGULAR);
    }

    @Test
    void testAddPrerequisite_genericException_returns400WithError() throws Exception {
        // Arrange
        when(req.params(":id")).thenReturn("1");
        String body = "{\"prerequisiteSubjectId\": 2, \"type\": \"REGULAR\"}";
        when(req.body()).thenReturn(body);

        when(conditionService.addPrerequisite(1, 2, ConditionType.REGULAR))
                .thenThrow(new RuntimeException("DB connection error"));

        // Act
        Object result = subjectRoutes.handleAddPrerequisite(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(400);
        assertNotNull(result);

        Map<String, String> parsed = objectMapper.readValue(
                (String) result,
                new TypeReference<Map<String, String>>() {});
        assertEquals("Invalid request", parsed.get("error"));

        verify(conditionService).addPrerequisite(1, 2, ConditionType.REGULAR);
    }

    // =====================================================================
    // DELETE /subjects/:id/prerequisites/:conditionId
    // =====================================================================

    @Test
    void testRemovePrerequisite_success_returns204() throws Exception {
        // Arrange
        when(req.params(":conditionId")).thenReturn("1");
        when(conditionService.removePrerequisite(1)).thenReturn(true);

        // Act
        Object result = subjectRoutes.handleRemovePrerequisite(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(204);
        verify(conditionService).removePrerequisite(1);
        // For 204, the body should be empty
        assertEquals("", result);
    }

    @Test
    void testRemovePrerequisite_notFound_returns404WithError() throws Exception {
        // Arrange
        when(req.params(":conditionId")).thenReturn("999");
        when(conditionService.removePrerequisite(999)).thenReturn(false);

        // Act
        Object result = subjectRoutes.handleRemovePrerequisite(req, res);

        // Assert
        verify(res).type("application/json");
        verify(res).status(404);
        assertNotNull(result);

        Map<String, String> parsed = objectMapper.readValue(
                (String) result,
                new TypeReference<Map<String, String>>() {});
        assertEquals("Prerequisite not found", parsed.get("error"));

        verify(conditionService).removePrerequisite(999);
    }
}
