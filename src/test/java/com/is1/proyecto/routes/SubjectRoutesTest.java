package com.is1.proyecto.routes;

import com.is1.proyecto.models.ConditionType;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.services.ConditionService;
import com.is1.proyecto.services.PrerequisiteDTO;
import com.is1.proyecto.services.SubjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Session;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para SubjectRoutes — handlers HTML de correlatividades.
 * Verifica que los handlers devuelvan ModelAndView o redirects con flash messages,
 * y que utilicen req.queryParams() en lugar de req.body().
 */
@ExtendWith(MockitoExtension.class)
class SubjectRoutesTest {

    @Mock
    private ConditionService conditionService;

    @Mock
    private SubjectService subjectService;

    @Mock
    private Request req;

    @Mock
    private Response res;

    @Mock
    private Session session;

    private SubjectRoutes subjectRoutes;

    @BeforeEach
    void setUp() {
        subjectRoutes = new SubjectRoutes(subjectService, conditionService);
    }

    // =====================================================================
    // GET /subjects/:id/prerequisites
    // =====================================================================

    @Test
    void testGetPrerequisites_subjectExists_returnsModelAndView() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);

        PrerequisiteDTO dto = new PrerequisiteDTO(1, 1, 2, "Matematica", ConditionType.REGULAR);
        when(conditionService.getPrerequisites(1)).thenReturn(Arrays.asList(dto));

        when(subjectService.getAllSubjects(null)).thenReturn(Collections.emptyList());

        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            Subject subject = mock(Subject.class);
            when(subject.getId()).thenReturn(1);
            when(subject.getCode()).thenReturn("MATE101");
            when(subject.getSubjectName()).thenReturn("Matematica I");
            subjectMock.when(() -> Subject.findById(1)).thenReturn(subject);

            // Mock the prereq subject lookup for prerequisiteSubjectCode
            Subject prereqSubject = mock(Subject.class);
            when(prereqSubject.getCode()).thenReturn("ALG101");
            subjectMock.when(() -> Subject.findById(2)).thenReturn(prereqSubject);

            Object result = subjectRoutes.handleGetPrerequisites(req, res);

            assertInstanceOf(ModelAndView.class, result);
            ModelAndView mnv = (ModelAndView) result;
            assertEquals("subjects-prerequisites.mustache", mnv.getViewName());

            @SuppressWarnings("unchecked")
            Map<String, Object> model = (Map<String, Object>) mnv.getModel();

            assertNotNull(model.get("subject"));
            assertEquals(1, model.get("subjectId"));

            assertNotNull(model.get("prerequisites"));
            List<Map<String, Object>> prereqs = (List<Map<String, Object>>) model.get("prerequisites");
            assertEquals(1, prereqs.size());
            assertEquals("Matematica", prereqs.get(0).get("prerequisiteSubjectName"));
            assertEquals("ALG101", prereqs.get(0).get("prerequisiteSubjectCode"));
            assertEquals("REGULAR", prereqs.get(0).get("type"));
            assertEquals(true, prereqs.get(0).get("isRegular"));

            assertNotNull(model.get("availableSubjects"));
            assertNotNull(model.get("conditionTypes"));
            assertEquals(2, ((List<?>) model.get("conditionTypes")).size());

            verify(conditionService).getPrerequisites(1);
        }
    }

    @Test
    void testGetPrerequisites_emptyList_returnsModelAndViewWithEmptyPrereqs() throws Exception {
        when(req.params(":id")).thenReturn("2");
        when(req.session()).thenReturn(session);
        when(conditionService.getPrerequisites(2)).thenReturn(Collections.emptyList());
        when(subjectService.getAllSubjects(null)).thenReturn(Collections.emptyList());

        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            Subject subject = mock(Subject.class);
            when(subject.getId()).thenReturn(2);
            when(subject.getCode()).thenReturn("PROG101");
            when(subject.getSubjectName()).thenReturn("Programacion I");
            subjectMock.when(() -> Subject.findById(2)).thenReturn(subject);

            Object result = subjectRoutes.handleGetPrerequisites(req, res);

            assertInstanceOf(ModelAndView.class, result);
            ModelAndView mnv = (ModelAndView) result;
            assertEquals("subjects-prerequisites.mustache", mnv.getViewName());

            @SuppressWarnings("unchecked")
            Map<String, Object> model = (Map<String, Object>) mnv.getModel();
            assertNotNull(model.get("prerequisites"));
            assertTrue(((List<?>) model.get("prerequisites")).isEmpty());

            verify(conditionService).getPrerequisites(2);
        }
    }

    @Test
    void testGetPrerequisites_subjectNotFound_redirectsWithError() throws Exception {
        when(req.params(":id")).thenReturn("99");
        when(req.session()).thenReturn(session);

        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            subjectMock.when(() -> Subject.findById(99)).thenReturn(null);

            Object result = subjectRoutes.handleGetPrerequisites(req, res);

            assertNull(result);
            verify(res).redirect("/subjects/manage");
            verify(session).attribute("flashError", "Materia no encontrada.");
            verify(conditionService, never()).getPrerequisites(anyInt());
        }
    }

    // =====================================================================
    // POST /subjects/:id/prerequisites
    // =====================================================================

    @Test
    void testAddPrerequisite_happyPath_redirectsWithFlash() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.queryParams("prerequisiteSubjectId")).thenReturn("2");
        when(req.queryParams("type")).thenReturn("REGULAR");
        when(req.session()).thenReturn(session);
        when(conditionService.addPrerequisite(1, 2, ConditionType.REGULAR)).thenReturn(new PrerequisiteDTO());

        Object result = subjectRoutes.handleAddPrerequisite(req, res);

        assertEquals("", result);
        verify(session).attribute("flashMessage", "Correlativa agregada exitosamente.");
        verify(res).redirect("/subjects/1/prerequisites");
        verify(conditionService).addPrerequisite(1, 2, ConditionType.REGULAR);
    }

    @Test
    void testAddPrerequisite_illegalArgument_redirectsWithError() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.queryParams("prerequisiteSubjectId")).thenReturn("1");
        when(req.queryParams("type")).thenReturn("REGULAR");
        when(req.session()).thenReturn(session);
        when(conditionService.addPrerequisite(1, 1, ConditionType.REGULAR))
                .thenThrow(new IllegalArgumentException("Una materia no puede ser requisito de si misma"));

        Object result = subjectRoutes.handleAddPrerequisite(req, res);

        assertEquals("", result);
        verify(session).attribute("flashError", "Una materia no puede ser requisito de si misma");
        verify(res).redirect("/subjects/1/prerequisites");
        verify(conditionService).addPrerequisite(1, 1, ConditionType.REGULAR);
    }

    @Test
    void testAddPrerequisite_genericException_redirectsWithError() throws Exception {
        when(req.params(":id")).thenReturn("1");
        when(req.queryParams("prerequisiteSubjectId")).thenReturn("2");
        when(req.queryParams("type")).thenReturn("REGULAR");
        when(req.session()).thenReturn(session);
        when(conditionService.addPrerequisite(1, 2, ConditionType.REGULAR))
                .thenThrow(new RuntimeException("DB error"));

        Object result = subjectRoutes.handleAddPrerequisite(req, res);

        assertEquals("", result);
        verify(session).attribute("flashError", "Error al agregar la correlativa.");
        verify(res).redirect("/subjects/1/prerequisites");
        verify(conditionService).addPrerequisite(1, 2, ConditionType.REGULAR);
    }

    // =====================================================================
    // POST /subjects/:id/prerequisites/:conditionId/delete
    // =====================================================================

    @Test
    void testRemovePrerequisite_success_redirectsWithFlash() throws Exception {
        when(req.params(":conditionId")).thenReturn("1");
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);
        when(conditionService.removePrerequisite(1)).thenReturn(true);

        Object result = subjectRoutes.handleRemovePrerequisite(req, res);

        assertEquals("", result);
        verify(session).attribute("flashMessage", "Correlativa eliminada exitosamente.");
        verify(res).redirect("/subjects/1/prerequisites");
        verify(conditionService).removePrerequisite(1);
    }

    @Test
    void testRemovePrerequisite_notFound_redirectsWithError() throws Exception {
        when(req.params(":conditionId")).thenReturn("999");
        when(req.params(":id")).thenReturn("1");
        when(req.session()).thenReturn(session);
        when(conditionService.removePrerequisite(999)).thenReturn(false);

        Object result = subjectRoutes.handleRemovePrerequisite(req, res);

        assertEquals("", result);
        verify(session).attribute("flashError", "La correlativa no existe.");
        verify(res).redirect("/subjects/1/prerequisites");
        verify(conditionService).removePrerequisite(999);
    }
}
