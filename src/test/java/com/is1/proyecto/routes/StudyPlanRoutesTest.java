package com.is1.proyecto.routes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.dto.StudyPlanDTO;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.services.StudyPlanService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para StudyPlanRoutes.
 * Los handlers son package-private — acceso directo.
 */
@ExtendWith(MockitoExtension.class)
class StudyPlanRoutesTest {

    @Mock
    private StudyPlanService studyPlanService;

    @Mock
    private Request req;

    @Mock
    private Response res;

    private StudyPlanRoutes studyPlanRoutes;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        studyPlanRoutes = new StudyPlanRoutes(studyPlanService);
        objectMapper = new ObjectMapper();
    }

    // ───── Helper: assert JSON string contains expected content ─────

    private void assertJsonContains(String json, String expected) {
        assertTrue(json.contains(expected), "Expected JSON to contain: " + expected + " but was: " + json);
    }

    private String responseBody(Object result) {
        assertInstanceOf(String.class, result);
        return (String) result;
    }

    private Map<String, Object> parseMap(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    // =====================================================================
    // GET /study-plans — handleGetAll
    // =====================================================================

    @Test
    void handleGetAll_noFilter_returns200WithAll() throws Exception {
        when(req.queryParams("careerId")).thenReturn(null);

        StudyPlanDTO plan1 = new StudyPlanDTO(1, "Plan 2024", 2024, 1, "Ingenieria");
        when(studyPlanService.getAllStudyPlans(null)).thenReturn(Arrays.asList(plan1));

        Object result = studyPlanRoutes.handleGetAll(req, res);

        verify(res).type("application/json");
        verify(res).status(200);
        String json = responseBody(result);
        assertJsonContains(json, "\"id\":1");
        assertJsonContains(json, "\"name\":\"Plan 2024\"");
        assertJsonContains(json, "\"year\":2024");
        assertJsonContains(json, "\"careerId\":1");
    }

    @Test
    void handleGetAll_withCareerFilter_returnsFiltered() throws Exception {
        when(req.queryParams("careerId")).thenReturn("2");
        StudyPlanDTO plan = new StudyPlanDTO(2, "Plan 2025", 2025, 2, "Sistemas");
        when(studyPlanService.getAllStudyPlans(2)).thenReturn(Arrays.asList(plan));

        Object result = studyPlanRoutes.handleGetAll(req, res);

        verify(res).status(200);
        String json = responseBody(result);
        assertJsonContains(json, "\"id\":2");
        assertJsonContains(json, "\"name\":\"Plan 2025\"");
    }

    @Test
    void handleGetAll_emptyList_returns200WithEmptyArray() throws Exception {
        when(req.queryParams("careerId")).thenReturn(null);
        when(studyPlanService.getAllStudyPlans(null)).thenReturn(Collections.emptyList());

        Object result = studyPlanRoutes.handleGetAll(req, res);

        verify(res).status(200);
        assertEquals("[]", responseBody(result));
    }

    @Test
    void handleGetAll_invalidCareerId_returns400() throws Exception {
        when(req.queryParams("careerId")).thenReturn("abc");

        Object result = studyPlanRoutes.handleGetAll(req, res);

        verify(res).status(400);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertEquals("careerId debe ser un numero valido", parsed.get("error"));
        verify(studyPlanService, never()).getAllStudyPlans(any());
    }

    @Test
    void handleGetAll_serviceException_returns500() throws Exception {
        when(req.queryParams("careerId")).thenReturn(null);
        when(studyPlanService.getAllStudyPlans(null)).thenThrow(new RuntimeException("DB error"));

        Object result = studyPlanRoutes.handleGetAll(req, res);

        verify(res).status(500);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertEquals("Error interno al listar planes de estudio", parsed.get("error"));
    }

    // =====================================================================
    // POST /study-plans — handleCreate
    // =====================================================================

    @Test
    void handleCreate_validData_returns201() throws Exception {
        String body = "{\"name\":\"Plan 2024\",\"year\":2024,\"careerId\":1}";
        when(req.body()).thenReturn(body);
        StudyPlanDTO created = new StudyPlanDTO(1, "Plan 2024", 2024, 1, "Ingenieria");
        when(studyPlanService.registerStudyPlan("Plan 2024", 2024, 1)).thenReturn(created);

        Object result = studyPlanRoutes.handleCreate(req, res);

        verify(res).type("application/json");
        verify(res).status(201);
        String json = responseBody(result);
        assertJsonContains(json, "\"id\":1");
        assertJsonContains(json, "\"name\":\"Plan 2024\"");
    }

    @Test
    void handleCreate_missingFields_returns400() throws Exception {
        String body = "{\"name\":\"Plan 2024\"}"; // missing year and careerId
        when(req.body()).thenReturn(body);

        Object result = studyPlanRoutes.handleCreate(req, res);

        verify(res).status(400);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertEquals("Los campos 'year' y 'careerId' son obligatorios", parsed.get("error"));
        verify(studyPlanService, never()).registerStudyPlan(anyString(), anyInt(), anyInt());
    }

    @Test
    void handleCreate_validationException_returns400WithField() throws Exception {
        String body = "{\"name\":\"\",\"year\":2024,\"careerId\":1}";
        when(req.body()).thenReturn(body);
        when(studyPlanService.registerStudyPlan("", 2024, 1))
                .thenThrow(new ValidationException("El nombre del plan de estudio es obligatorio", "name"));

        Object result = studyPlanRoutes.handleCreate(req, res);

        verify(res).status(400);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertEquals("El nombre del plan de estudio es obligatorio", parsed.get("error"));
        assertEquals("name", parsed.get("field"));
    }

    @Test
    void handleCreate_genericException_returns400() throws Exception {
        String body = "{\"name\":\"Plan\",\"year\":2024,\"careerId\":1}";
        when(req.body()).thenReturn(body);
        when(studyPlanService.registerStudyPlan("Plan", 2024, 1))
                .thenThrow(new RuntimeException("parse error"));

        Object result = studyPlanRoutes.handleCreate(req, res);

        verify(res).status(400);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertTrue(((String) parsed.get("error")).contains("Solicitud invalida"));
    }

    // =====================================================================
    // GET /study-plans/:id — handleGetById
    // =====================================================================

    @Test
    void handleGetById_found_returns200() throws Exception {
        when(req.params(":id")).thenReturn("1");
        StudyPlanDTO plan = new StudyPlanDTO(1, "Plan 2024", 2024, 1, "Ingenieria");
        when(studyPlanService.getStudyPlanById(1)).thenReturn(plan);

        Object result = studyPlanRoutes.handleGetById(req, res);

        verify(res).type("application/json");
        verify(res).status(200);
        String json = responseBody(result);
        assertJsonContains(json, "\"id\":1");
    }

    @Test
    void handleGetById_notFound_returns404() throws Exception {
        when(req.params(":id")).thenReturn("99");
        when(studyPlanService.getStudyPlanById(99)).thenThrow(new ValidationException("no existe", "id"));

        Object result = studyPlanRoutes.handleGetById(req, res);

        verify(res).status(404);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertEquals("no existe", parsed.get("error"));
    }

    @Test
    void handleGetById_invalidId_returns400() throws Exception {
        when(req.params(":id")).thenReturn("abc");

        Object result = studyPlanRoutes.handleGetById(req, res);

        verify(res).status(400);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertEquals("ID debe ser un numero valido", parsed.get("error"));
    }

    @Test
    void handleGetById_serviceError_returns500() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(studyPlanService.getStudyPlanById(1)).thenThrow(new RuntimeException("DB error"));

        Object result = studyPlanRoutes.handleGetById(req, res);

        verify(res).status(500);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertEquals("Error interno al obtener el plan de estudio", parsed.get("error"));
    }

    // =====================================================================
    // PUT /study-plans/:id — handleUpdate
    // =====================================================================

    @Test
    void handleUpdate_success_returns200() throws Exception {
        when(req.params(":id")).thenReturn("1");
        String body = "{\"name\":\"Plan Updated\",\"year\":2025,\"careerId\":2}";
        when(req.body()).thenReturn(body);
        StudyPlanDTO updated = new StudyPlanDTO(1, "Plan Updated", 2025, 2, "Sistemas");
        when(studyPlanService.updateStudyPlan(eq(1), anyString(), any(), any())).thenReturn(updated);

        Object result = studyPlanRoutes.handleUpdate(req, res);

        verify(res).type("application/json");
        verify(res).status(200);
        String json = responseBody(result);
        assertJsonContains(json, "\"name\":\"Plan Updated\"");
    }

    @Test
    void handleUpdate_validationException_returns400() throws Exception {
        when(req.params(":id")).thenReturn("1");
        String body = "{\"careerId\":999}";
        when(req.body()).thenReturn(body);
        when(studyPlanService.updateStudyPlan(eq(1), isNull(), isNull(), eq(999)))
                .thenThrow(new ValidationException("La carrera con ID 999 no existe", "careerId"));

        Object result = studyPlanRoutes.handleUpdate(req, res);

        verify(res).status(400);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertNotNull(parsed.get("error"));
        assertEquals("careerId", parsed.get("field"));
    }

    // =====================================================================
    // DELETE /study-plans/:id — handleDelete
    // =====================================================================

    @Test
    void handleDelete_success_returns204() throws Exception {
        when(req.params(":id")).thenReturn("1");
        doNothing().when(studyPlanService).deleteStudyPlan(1);

        Object result = studyPlanRoutes.handleDelete(req, res);

        verify(res).type("application/json");
        verify(res).status(204);
        assertEquals("", result);
    }

    @Test
    void handleDelete_notFound_returns404() throws Exception {
        when(req.params(":id")).thenReturn("99");
        doThrow(new ValidationException("no existe", "id")).when(studyPlanService).deleteStudyPlan(99);

        Object result = studyPlanRoutes.handleDelete(req, res);

        verify(res).status(404);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertEquals("no existe", parsed.get("error"));
    }

    @Test
    void handleDelete_invalidId_returns400() throws Exception {
        when(req.params(":id")).thenReturn("abc");

        Object result = studyPlanRoutes.handleDelete(req, res);

        verify(res).status(400);
        Map<String, Object> parsed = parseMap(responseBody(result));
        assertEquals("ID debe ser un numero valido", parsed.get("error"));
    }
}
