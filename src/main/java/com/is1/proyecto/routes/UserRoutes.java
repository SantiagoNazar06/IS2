package com.is1.proyecto.routes;

import com.is1.proyecto.security.AuthService;
import com.is1.proyecto.services.UserService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

/**
 * Rutas relacionadas con usuarios: login, logout, registro, dashboard.
 */
public class UserRoutes {

    private final AuthService authService;
    private final UserService userService;
    private final MustacheTemplateEngine templateEngine;

    public UserRoutes(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
        this.templateEngine = new MustacheTemplateEngine();
    }

    /**
     * Registra todas las rutas de usuario en Spark.
     */
    public void register() {
        // GET: Muestra el formulario de inicio de sesión (login).
        get("/", this::showLogin, templateEngine);
        get("/login", this::showLogin, templateEngine);

        // GET: Muestra el formulario de creación de cuenta.
        // Soporta la visualización de mensajes de éxito o error pasados como query parameters.
        get("/user/create", this::showCreateForm, templateEngine);

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        get("/dashboard", this::showDashboard, templateEngine);

        // GET: Ruta para cerrar la sesión del usuario.
        get("/logout", this::logout);

        // GET: Ruta de alias para el formulario de creación de cuenta.
        get("/user/new", (req, res) -> new ModelAndView(new HashMap<>(), "user_form.mustache"), templateEngine);

        // POST: Maneja el envío del formulario de creación de nueva cuenta.
        post("/user/new", this::handleCreateUser);

        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/login", this::handleLogin, templateEngine);

        // POST: Endpoint para añadir usuarios (API que devuelve JSON, no HTML).
        post("/add_users", this::handleAddUsers);
    }

    /**
     * GET /user/create - Muestra el formulario de creación de cuenta.
     */
    private ModelAndView showCreateForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.

        // Obtener y añadir mensaje de éxito de los query parameters (ej. ?message=Cuenta creada!)
        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }

        // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos vacíos)
        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }

        // Renderiza la plantilla 'user_form.mustache' con los datos del modelo.
        return new ModelAndView(model, "user_form.mustache");
    }

    /**
     * GET /dashboard - Muestra el dashboard del usuario autenticado.
     */
    ModelAndView showDashboard(Request req, Response res) {
        Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla del dashboard.

        // 1. Verificar si el usuario ha iniciado sesión.
        if (!authService.isAuthenticated(req)) {
            System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
            // Redirige al login con un mensaje de error.
            res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
            return null; // Importante retornar null después de una redirección.
        }

        // 2. Si el usuario está logueado, añade el nombre de usuario al modelo para la plantilla.
        model.put("username", authService.getCurrentUsername(req));

        // 3. Renderiza el dashboard según el rol del usuario.
        String role = req.session().attribute("userRole");
        String template = dashboardTemplateForRole(role);
        return new ModelAndView(model, template);
    }

    /**
     * GET /logout - Cierra la sesión del usuario.
     */
    private Object logout(Request req, Response res) {
        authService.invalidateSession(req);
        // Redirige al usuario a la página de login con un mensaje de éxito.
        res.redirect("/");
        return null; // Importante retornar null después de una redirección.
    }

    /**
     * GET / - Muestra el formulario de login.
     */
    private ModelAndView showLogin(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }
        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }
        return new ModelAndView(model, "login.mustache");
    }

    /**
     * POST /user/new - Procesa el formulario de creación de cuenta.
     */
    private Object handleCreateUser(Request req, Response res) {
        String name = req.queryParams("name");
        String password = req.queryParams("password");

        UserService.RegisterResult result = userService.createUser(name, password);

        res.status(result.statusCode);
        res.redirect(result.redirectUrl);
        return ""; // Retorna una cadena vacía ya que la respuesta ya fue redirigida.
    }

    /**
     * POST /login - Procesa el formulario de inicio de sesión.
     */
    private ModelAndView handleLogin(Request req, Response res) {
        Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla de login o dashboard.

        String username = req.queryParams("username");
        String plainTextPassword = req.queryParams("password");

        AuthService.LoginResult loginResult = authService.authenticate(username, plainTextPassword);

        if (loginResult.success) {
            // Autenticación exitosa.
            res.status(200); // OK.
            // Incluye el rol del usuario en la sesión
            authService.createSession(req, username, loginResult.user.getId(), loginResult.user.getRole());
            if ("STUDENT".equals(loginResult.user.getRole())) {
                Long studentId = loginResult.user.getStudentId();
                if (studentId != null && studentId != 0) {
                    req.session().attribute("studentId", studentId);
                }
            }
            model.put("username", username); // Añade el nombre de usuario al modelo para el dashboard.
            // Renderiza el dashboard según el rol del usuario.
            String template = dashboardTemplateForRole(loginResult.user.getRole());
            return new ModelAndView(model, template);
        } else {
            // Fallo de autenticación.
            res.status(401); // Unauthorized.
            System.out.println("DEBUG: Intento de login fallido para: " + username);
            model.put("errorMessage", loginResult.message); // Mensaje genérico por seguridad.
            return new ModelAndView(model, "login.mustache"); // Renderiza la plantilla de login con error.
        }
    }

    /**
     * POST /add_users - API para añadir usuarios (devuelve JSON).
     */
    private Object handleAddUsers(Request req, Response res) {
        String name = req.queryParams("name");
        String password = req.queryParams("password");

        return userService.addUserApi(name, password, res);
    }

    /**
     * Devuelve el template de dashboard para el rol dado.
     */
    private String dashboardTemplateForRole(String role) {
        if ("ADMIN".equals(role)) return "dashboard_admin.mustache";
        if ("TEACHER".equals(role)) return "dashboard_teacher.mustache";
        return "dashboard_student.mustache";
    }
}
