package com.is1.proyecto.routes;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.repositories.PersonRepository;
import com.is1.proyecto.security.AuthService;
import com.is1.proyecto.services.TeacherService;
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
 * Tests para los handlers de /profile en UserRoutes.
 */
@ExtendWith(MockitoExtension.class)
class UserRoutesProfileTest {

    @Mock
    private AuthService authService;
    @Mock
    private UserService userService;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private TeacherService teacherService;
    @Mock
    private Request req;
    @Mock
    private Response res;
    @Mock
    private Session session;

    private Person person;
    private UserRoutes userRoutes;

    @BeforeEach
    void setup() {
        person = mock(Person.class);
        lenient().when(person.getId()).thenReturn(1L);

        userRoutes = spy(new UserRoutes(authService, userService, personRepository, teacherService));
    }

    // ==================== GET /profile — showProfile ====================

    @Test
    void showProfile_returnsProfileTemplateForStudent() {
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("STUDENT");
        doReturn(person).when(userRoutes).findPersonForRole(req, "STUDENT");
        doReturn("REGULAR").when(userRoutes).resolveRoleSpecificField(req, "STUDENT");

        ModelAndView result = userRoutes.showProfile(req, res);

        assertNotNull(result);
        assertEquals("profile.mustache", result.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals(person, model.get("person"));
        assertEquals("STUDENT", model.get("role"));
        assertTrue((Boolean) model.get("isStudent"));
        assertFalse((Boolean) model.get("isTeacher"));
        assertEquals("REGULAR", model.get("roleSpecificField"));
    }

    @Test
    void showProfile_returnsProfileTemplateForTeacher() {
        Person teacherPerson = mock(Person.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("TEACHER");
        doReturn(teacherPerson).when(userRoutes).findPersonForRole(req, "TEACHER");
        doReturn("LEG-001").when(userRoutes).resolveRoleSpecificField(req, "TEACHER");

        ModelAndView result = userRoutes.showProfile(req, res);

        assertNotNull(result);
        assertEquals("profile.mustache", result.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals(teacherPerson, model.get("person"));
        assertEquals("TEACHER", model.get("role"));
        assertFalse((Boolean) model.get("isStudent"));
        assertTrue((Boolean) model.get("isTeacher"));
        assertEquals("LEG-001", model.get("roleSpecificField"));
    }

    @Test
    void showProfile_passesSuccessAndErrorMessages() {
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("STUDENT");
        doReturn(person).when(userRoutes).findPersonForRole(req, "STUDENT");
        doReturn("REGULAR").when(userRoutes).resolveRoleSpecificField(req, "STUDENT");
        when(req.queryParams("success")).thenReturn("Perfil actualizado!");
        when(req.queryParams("error")).thenReturn("Hubo un error");

        ModelAndView result = userRoutes.showProfile(req, res);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals("Perfil actualizado!", model.get("successMessage"));
        assertEquals("Hubo un error", model.get("errorMessage"));
    }

    // ==================== POST /profile — handleProfileUpdate ====================

    @Test
    void handleProfileUpdate_updatesPersonAndRedirectsOnSuccess() {
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("STUDENT");
        doReturn(person).when(userRoutes).findPersonForRole(req, "STUDENT");

        when(req.queryParams("firstName")).thenReturn("Juan");
        when(req.queryParams("lastName")).thenReturn("Pérez");
        when(req.queryParams("phone")).thenReturn("555-1234");
        when(req.queryParams("email")).thenReturn("juan@test.com");
        when(personRepository.update(eq(1L), anyMap())).thenReturn(true);

        Object result = userRoutes.handleProfileUpdate(req, res);

        assertNull(result);
        verify(res).redirect(contains("/profile?success="));
        verify(personRepository).update(eq(1L), anyMap());
    }

    @Test
    void handleProfileUpdate_rejectsBlankFirstName() {
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("STUDENT");
        doReturn(person).when(userRoutes).findPersonForRole(req, "STUDENT");
        doReturn("REGULAR").when(userRoutes).resolveRoleSpecificField(req, "STUDENT");

        when(req.queryParams("firstName")).thenReturn("");
        when(req.queryParams("lastName")).thenReturn("Pérez");

        ModelAndView result = (ModelAndView) userRoutes.handleProfileUpdate(req, res);

        assertNotNull(result);
        assertEquals("profile.mustache", result.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertNotNull(model.get("errorMessage"));
        assertEquals(person, model.get("person"));

        verify(personRepository, never()).update(anyLong(), anyMap());
    }

    @Test
    void handleProfileUpdate_rejectsBlankLastName() {
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("STUDENT");
        doReturn(person).when(userRoutes).findPersonForRole(req, "STUDENT");
        doReturn("REGULAR").when(userRoutes).resolveRoleSpecificField(req, "STUDENT");

        when(req.queryParams("firstName")).thenReturn("Juan");
        when(req.queryParams("lastName")).thenReturn("");

        ModelAndView result = (ModelAndView) userRoutes.handleProfileUpdate(req, res);

        assertNotNull(result);
        assertEquals("profile.mustache", result.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertNotNull(model.get("errorMessage"));
        assertEquals(person, model.get("person"));

        verify(personRepository, never()).update(anyLong(), anyMap());
    }

    @Test
    void handleProfileUpdate_acceptsEmptyPhoneAndEmail() {
        when(req.session()).thenReturn(session);
        when(session.attribute("userRole")).thenReturn("TEACHER");
        doReturn(person).when(userRoutes).findPersonForRole(req, "TEACHER");

        when(req.queryParams("firstName")).thenReturn("María");
        when(req.queryParams("lastName")).thenReturn("García");
        when(req.queryParams("phone")).thenReturn("");
        when(req.queryParams("email")).thenReturn("");

        when(personRepository.update(eq(1L), anyMap())).thenReturn(true);

        Object result = userRoutes.handleProfileUpdate(req, res);

        assertNull(result);
        verify(res).redirect(contains("/profile?success="));
        verify(personRepository).update(eq(1L), anyMap());
    }
}
