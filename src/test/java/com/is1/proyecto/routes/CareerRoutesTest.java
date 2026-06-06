package com.is1.proyecto.routes;

import com.is1.proyecto.services.CareerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para CareerRoutes.
 * Handlers privados — acceso via reflection.
 */
@ExtendWith(MockitoExtension.class)
class CareerRoutesTest {

    @Mock
    private CareerService careerService;

    @Mock
    private Request req;

    @Mock
    private Response res;

    private CareerRoutes careerRoutes;

    @BeforeEach
    void setUp() {
        careerRoutes = new CareerRoutes(careerService);
    }

    // ───── Helper: invoke private method ─────

    private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = CareerRoutes.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        T result = (T) method.invoke(careerRoutes, args);
        return result;
    }

    // =====================================================================
    // showCareerForm
    // =====================================================================

    @Test
    void showCareerForm_noParams_returnsEmptyForm() throws Exception {
        when(req.queryParams("error")).thenReturn(null);
        when(req.queryParams("message")).thenReturn(null);

        ModelAndView result = invokePrivate("showCareerForm",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("career_form.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertFalse(model.containsKey("errorMessage"));
        assertFalse(model.containsKey("successMessage"));
    }

    @Test
    void showCareerForm_withMessages_inModel() throws Exception {
        when(req.queryParams("error")).thenReturn("Duracion invalida");
        when(req.queryParams("message")).thenReturn("Carrera creada!");

        ModelAndView result = invokePrivate("showCareerForm",
                new Class<?>[]{Request.class, Response.class}, req, res);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals("Duracion invalida", model.get("errorMessage"));
        assertEquals("Carrera creada!", model.get("successMessage"));
    }

    @Test
    void showCareerForm_emptyMessagesNotAdded() throws Exception {
        when(req.queryParams("error")).thenReturn("");
        when(req.queryParams("message")).thenReturn("");

        ModelAndView result = invokePrivate("showCareerForm",
                new Class<?>[]{Request.class, Response.class}, req, res);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertFalse(model.containsKey("errorMessage"));
        assertFalse(model.containsKey("successMessage"));
    }

    // =====================================================================
    // handleRegisterCareer
    // =====================================================================

    @Test
    void handleRegisterCareer_success_redirects() throws Exception {
        when(req.queryParams("career_name")).thenReturn("Ingenieria");
        when(req.queryParams("career_duration")).thenReturn("5");

        CareerService.CareerRegisterResult ok = CareerService.CareerRegisterResult.ok("Creada!");
        when(careerService.registerCareer("Ingenieria", 5)).thenReturn(ok);

        Object result = invokePrivate("handleRegisterCareer",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(res).status(201);
        verify(res).redirect(contains("/register_career"));
    }

    @Test
    void handleRegisterCareer_duplicate_redirectsWithError() throws Exception {
        when(req.queryParams("career_name")).thenReturn("Existente");
        when(req.queryParams("career_duration")).thenReturn("4");

        CareerService.CareerRegisterResult dup = CareerService.CareerRegisterResult.duplicate("Existente");
        when(careerService.registerCareer("Existente", 4)).thenReturn(dup);

        Object result = invokePrivate("handleRegisterCareer",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(res).status(400);
        verify(res).redirect(contains("error"));
    }

    @Test
    void handleRegisterCareer_invalidDuration_redirectsWithError() throws Exception {
        when(req.queryParams("career_name")).thenReturn("Medicina");
        when(req.queryParams("career_duration")).thenReturn("abc");

        Object result = invokePrivate("handleRegisterCareer",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(res).redirect(contains("error"));
    }

    @Test
    void handleRegisterCareer_nullDuration_defaultsToZero() throws Exception {
        when(req.queryParams("career_name")).thenReturn("Derecho");
        when(req.queryParams("career_duration")).thenReturn(null);

        CareerService.CareerRegisterResult error = CareerService.CareerRegisterResult.error("Duracion invalida");
        when(careerService.registerCareer("Derecho", 0)).thenReturn(error);

        Object result = invokePrivate("handleRegisterCareer",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(careerService).registerCareer("Derecho", 0);
    }
}
