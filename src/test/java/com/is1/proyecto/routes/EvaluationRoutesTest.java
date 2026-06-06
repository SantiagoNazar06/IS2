package com.is1.proyecto.routes;

import com.is1.proyecto.models.Evaluation;
import com.is1.proyecto.services.EvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Session;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para EvaluationRoutes.
 * Handlers privados — acceso via reflection.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationRoutesTest {

    @Mock
    private EvaluationService evaluationService;

    @Mock
    private Request req;

    @Mock
    private Response res;

    @Mock
    private Session session;

    private EvaluationRoutes evaluationRoutes;

    @BeforeEach
    void setUp() {
        evaluationRoutes = new EvaluationRoutes(evaluationService);
    }

    // ───── Helper: invoke private method ─────

    private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = EvaluationRoutes.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        T result = (T) method.invoke(evaluationRoutes, args);
        return result;
    }

    // =====================================================================
    // showEvaluationForm
    // =====================================================================

    @Test
    void showEvaluationForm_returnsFormTemplate() throws Exception {
        ModelAndView result = invokePrivate("showEvaluationForm",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("evaluation_form.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertTrue(model.isEmpty());
    }

    // =====================================================================
    // handleRegisterEvaluation
    // =====================================================================

    @Test
    void handleRegisterEvaluation_success_setsFlashAndRedirects() throws Exception {
        when(req.queryParams("enrollment_id")).thenReturn("1");
        when(req.queryParams("grade")).thenReturn("8.5");
        when(req.session()).thenReturn(session);

        EvaluationService.EvaluationRegisterResult ok = EvaluationService.EvaluationRegisterResult.ok("Evaluacion registrada exitosamente.");
        when(evaluationService.registerEvaluation(1, 8.5)).thenReturn(ok);

        Object result = invokePrivate("handleRegisterEvaluation",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(session).attribute("flashMessage", "Evaluacion registrada exitosamente.");
        verify(res).redirect("/grades");
    }

    @Test
    void handleRegisterEvaluation_numberFormatError_setsFlashAndRedirects() throws Exception {
        when(req.queryParams("enrollment_id")).thenReturn("abc");
        when(req.session()).thenReturn(session);

        Object result = invokePrivate("handleRegisterEvaluation",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(session).attribute(eq("flashMessage"), contains("Error"));
        verify(res).redirect("/grades");
        verify(evaluationService, never()).registerEvaluation(anyInt(), anyDouble());
    }

    @Test
    void handleRegisterEvaluation_invalidGrade_setsFlashAndRedirects() throws Exception {
        when(req.queryParams("enrollment_id")).thenReturn("1");
        when(req.queryParams("grade")).thenReturn("15.0");
        when(req.session()).thenReturn(session);

        EvaluationService.EvaluationRegisterResult error = EvaluationService.EvaluationRegisterResult.error("La calificacion debe estar entre 0.00 y 10.00.");
        when(evaluationService.registerEvaluation(1, 15.0)).thenReturn(error);

        Object result = invokePrivate("handleRegisterEvaluation",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(session).attribute("flashMessage", "La calificacion debe estar entre 0.00 y 10.00.");
        verify(res).redirect("/grades");
    }

    // =====================================================================
    // handleUpdateEvaluation
    // =====================================================================

    @Test
    void handleUpdateEvaluation_alwaysReturnsError() throws Exception {
        when(req.params(":id")).thenReturn("1");

        EvaluationService.EvaluationRegisterResult error = EvaluationService.EvaluationRegisterResult.error("No se permite la modificacion de calificaciones.");
        when(evaluationService.updateEvaluation(1, 0.0)).thenReturn(error);

        Object result = invokePrivate("handleUpdateEvaluation",
                new Class<?>[]{Request.class, Response.class}, req, res);

        verify(res).status(500);
        assertEquals("No se permite la modificacion de calificaciones.", result);
    }

    // =====================================================================
    // handleDeleteEvaluation
    // =====================================================================

    @Test
    void handleDeleteEvaluation_alwaysReturnsError() throws Exception {
        when(req.params(":id")).thenReturn("1");

        EvaluationService.EvaluationRegisterResult error = EvaluationService.EvaluationRegisterResult.error("No se permite eliminar calificaciones registradas.");
        when(evaluationService.deleteEvaluation(1)).thenReturn(error);

        Object result = invokePrivate("handleDeleteEvaluation",
                new Class<?>[]{Request.class, Response.class}, req, res);

        verify(res).status(500);
        assertEquals("No se permite eliminar calificaciones registradas.", result);
    }
}
