package com.is1.proyecto.routes;

import com.is1.proyecto.repositories.PersonRepository;
import com.is1.proyecto.security.AuthService;
import com.is1.proyecto.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Session;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests para el dashboard de UserRoutes por rol.
 */
@ExtendWith(MockitoExtension.class)
class UserRoutesDashboardTest {

    @Mock
    private AuthService authService;
    @Mock
    private UserService userService;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private Request req;
    @Mock
    private Response res;
    @Mock
    private Session session;

    private UserRoutes userRoutes;

    @BeforeEach
    void setup() {
        userRoutes = new UserRoutes(authService, userService, personRepository);
    }

    @Test
    void showDashboard_usesAdminTemplateForAdminRole() {
        when(authService.isAuthenticated(req)).thenReturn(true);
        when(authService.getCurrentUsername(req)).thenReturn("adminUser");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("ADMIN");

        ModelAndView result = userRoutes.showDashboard(req, res);

        assertNotNull(result);
        assertEquals("dashboard_admin.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals("adminUser", model.get("username"));
    }

    @Test
    void showDashboard_usesTeacherTemplateForTeacherRole() {
        when(authService.isAuthenticated(req)).thenReturn(true);
        when(authService.getCurrentUsername(req)).thenReturn("teacherUser");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("TEACHER");

        ModelAndView result = userRoutes.showDashboard(req, res);

        assertNotNull(result);
        assertEquals("dashboard_teacher.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals("teacherUser", model.get("username"));
    }

    @Test
    void showDashboard_usesStudentTemplateForStudentRole() {
        when(authService.isAuthenticated(req)).thenReturn(true);
        when(authService.getCurrentUsername(req)).thenReturn("studentUser");
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("STUDENT");

        ModelAndView result = userRoutes.showDashboard(req, res);

        assertNotNull(result);
        assertEquals("dashboard_student.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals("studentUser", model.get("username"));
    }

    @Test
    void showDashboard_redirectsWhenNotAuthenticated() {
        when(authService.isAuthenticated(req)).thenReturn(false);

        ModelAndView result = userRoutes.showDashboard(req, res);

        assertNull(result);
        verify(res).redirect(contains("/login"));
    }
}
