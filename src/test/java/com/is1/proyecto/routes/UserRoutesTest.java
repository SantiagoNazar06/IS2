package com.is1.proyecto.routes;

import com.is1.proyecto.models.User;
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

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para UserRoutes (handlers aun no cubiertos).
 * Cubre: showLogin, handleLogin, showCreateForm, handleCreateUser,
 * handleAddUsers, logout.
 * Handlers privados — acceso via reflection.
 */
@ExtendWith(MockitoExtension.class)
class UserRoutesTest {

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

    private UserRoutes userRoutes;

    @BeforeEach
    void setUp() {
        userRoutes = new UserRoutes(authService, userService, personRepository, teacherService);
    }

    // ───── Helper: invoke private method ─────

    private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = UserRoutes.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        T result = (T) method.invoke(userRoutes, args);
        return result;
    }

    // =====================================================================
    // GET /login — showLogin
    // =====================================================================

    @Test
    void showLogin_noParams_returnsLoginTemplate() throws Exception {
        when(req.queryParams("error")).thenReturn(null);
        when(req.queryParams("message")).thenReturn(null);

        ModelAndView result = invokePrivate("showLogin",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("login.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertFalse(model.containsKey("errorMessage"));
        assertFalse(model.containsKey("successMessage"));
    }

    @Test
    void showLogin_withMessages_inModel() throws Exception {
        when(req.queryParams("error")).thenReturn("Credenciales invalidas");
        when(req.queryParams("message")).thenReturn("Cuenta creada!");

        ModelAndView result = invokePrivate("showLogin",
                new Class<?>[]{Request.class, Response.class}, req, res);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals("Credenciales invalidas", model.get("errorMessage"));
        assertEquals("Cuenta creada!", model.get("successMessage"));
    }

    // =====================================================================
    // POST /login — handleLogin
    // =====================================================================

    @Test
    void handleLogin_success_admin_returnsDashboard() throws Exception {
        when(req.queryParams("username")).thenReturn("admin");
        when(req.queryParams("password")).thenReturn("pass");

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1);
        when(mockUser.getRole()).thenReturn("ADMIN");
        AuthService.LoginResult loginOk = AuthService.LoginResult.success(mockUser);
        when(authService.authenticate("admin", "pass")).thenReturn(loginOk);

        // Must use lenient because some mocked methods might not be called
        lenient().when(req.session()).thenReturn(session);

        ModelAndView result = invokePrivate("handleLogin",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("dashboard_admin.mustache", result.getViewName());
        verify(res).status(200);
        verify(authService).createSession(req, "admin", 1, "ADMIN");
    }

    @Test
    void handleLogin_success_student_setsStudentId() throws Exception {
        when(req.queryParams("username")).thenReturn("student1");
        when(req.queryParams("password")).thenReturn("pass");

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(2);
        when(mockUser.getRole()).thenReturn("STUDENT");
        when(mockUser.getStudentId()).thenReturn(100L);
        AuthService.LoginResult loginOk = AuthService.LoginResult.success(mockUser);
        when(authService.authenticate("student1", "pass")).thenReturn(loginOk);
        when(req.session()).thenReturn(session);

        lenient().when(req.session(true)).thenReturn(session);

        ModelAndView result = invokePrivate("handleLogin",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("dashboard_student.mustache", result.getViewName());
        verify(session).attribute("studentId", 100L);
    }

    @Test
    void handleLogin_success_teacher_setsTeacherId() throws Exception {
        when(req.queryParams("username")).thenReturn("teacher1");
        when(req.queryParams("password")).thenReturn("pass");

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(3);
        when(mockUser.getRole()).thenReturn("TEACHER");
        when(mockUser.getTeacherId()).thenReturn(200L);
        AuthService.LoginResult loginOk = AuthService.LoginResult.success(mockUser);
        when(authService.authenticate("teacher1", "pass")).thenReturn(loginOk);
        when(req.session()).thenReturn(session);

        ModelAndView result = invokePrivate("handleLogin",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("dashboard_teacher.mustache", result.getViewName());
        verify(session).attribute("teacherId", 200L);
    }

    @Test
    void handleLogin_failure_returnsLoginWithError() throws Exception {
        when(req.queryParams("username")).thenReturn("bad");
        when(req.queryParams("password")).thenReturn("wrong");

        AuthService.LoginResult loginFail = AuthService.LoginResult.failure("Usuario o contrase\u00f1a incorrectos.");
        when(authService.authenticate("bad", "wrong")).thenReturn(loginFail);

        ModelAndView result = invokePrivate("handleLogin",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("login.mustache", result.getViewName());
        verify(res).status(401);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals("Usuario o contrase\u00f1a incorrectos.", model.get("errorMessage"));
    }

    // =====================================================================
    // GET /user/create — showCreateForm
    // =====================================================================

    @Test
    void showCreateForm_noParams_returnsForm() throws Exception {
        when(req.queryParams("message")).thenReturn(null);
        when(req.queryParams("error")).thenReturn(null);

        ModelAndView result = invokePrivate("showCreateForm",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNotNull(result);
        assertEquals("user_form.mustache", result.getViewName());
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertFalse(model.containsKey("successMessage"));
        assertFalse(model.containsKey("errorMessage"));
    }

    @Test
    void showCreateForm_withMessages_inModel() throws Exception {
        when(req.queryParams("message")).thenReturn("Cuenta creada!");
        when(req.queryParams("error")).thenReturn("Error de validacion");

        ModelAndView result = invokePrivate("showCreateForm",
                new Class<?>[]{Request.class, Response.class}, req, res);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel();
        assertEquals("Cuenta creada!", model.get("successMessage"));
        assertEquals("Error de validacion", model.get("errorMessage"));
    }

    // =====================================================================
    // POST /user/new — handleCreateUser
    // =====================================================================

    @Test
    void handleCreateUser_success_redirects() throws Exception {
        when(req.queryParams("name")).thenReturn("nuevoUsuario");
        when(req.queryParams("password")).thenReturn("pass123");

        UserService.RegisterResult ok = UserService.RegisterResult.ok("/user/create?message=Creado!", null);
        when(userService.createUser("nuevoUsuario", "pass123")).thenReturn(ok);

        Object result = invokePrivate("handleCreateUser",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(res).status(201);
        verify(res).redirect(anyString());
    }

    @Test
    void handleCreateUser_validationError_redirects() throws Exception {
        when(req.queryParams("name")).thenReturn("");
        when(req.queryParams("password")).thenReturn("");

        UserService.RegisterResult error = UserService.RegisterResult.error(400, "/user/create?error=Error", null);
        when(userService.createUser("", "")).thenReturn(error);

        Object result = invokePrivate("handleCreateUser",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("", result);
        verify(res).status(400);
    }

    // =====================================================================
    // POST /add_users — handleAddUsers
    // =====================================================================

    @Test
    void handleAddUsers_success_returnsJson() throws Exception {
        when(req.queryParams("name")).thenReturn("apiUser");
        when(req.queryParams("password")).thenReturn("pass");
        when(userService.addUserApi("apiUser", "pass", res)).thenReturn("{\"message\":\"ok\"}");

        Object result = invokePrivate("handleAddUsers",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertEquals("{\"message\":\"ok\"}", result);
        verify(userService).addUserApi("apiUser", "pass", res);
    }

    // =====================================================================
    // GET /logout — logout
    // =====================================================================

    @Test
    void logout_invalidatesSessionAndRedirects() throws Exception {
        doNothing().when(authService).invalidateSession(req);

        Object result = invokePrivate("logout",
                new Class<?>[]{Request.class, Response.class}, req, res);

        assertNull(result);
        verify(authService).invalidateSession(req);
        verify(res).redirect("/");
    }
}
