package com.is1.proyecto.routes;

import com.is1.proyecto.services.AuthService;
import com.is1.proyecto.services.AuthenticationException;
import com.is1.proyecto.services.UserClaims;
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
    private ModelAndView showDashboard(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        // 1. Obtener token desde la cookie
        String token = req.cookie("token");

        // 2. Validar token
        if (token == null || authService.validateToken(token) == null) {
            System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
            res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
            return null;
        }

        // 3. Obtener claims del usuario
        UserClaims claims = authService.validateToken(token);
        model.put("username", claims.getUsername());

        return new ModelAndView(model, "dashboard.mustache");
    }

    /**
     * GET /logout - Cierra la sesión del usuario invalidando el token JWT.
     */
    private Object logout(Request req, Response res) {
        String token = req.cookie("token");
        if (token != null) {
            authService.logout(token);
        }
        // Eliminar la cookie del token
        res.removeCookie("/", "token");
        // Redirige al usuario a la página de login con un mensaje de éxito.
        res.redirect("/");
        return null;
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
        Map<String, Object> model = new HashMap<>();

        String username = req.queryParams("username");
        String password = req.queryParams("password");

        try {
            // Autenticar y obtener token JWT
            String token = authService.login(username, password);

            // Configurar cookie httpOnly con el token (expira en 24h)
            res.cookie("/", "token", token, 86400, false, true);

            // Redirigir al dashboard
            res.redirect("/dashboard");
            return null;

        } catch (AuthenticationException e) {
            // Fallo de autenticación
            res.status(401);
            System.out.println("DEBUG: Intento de login fallido para: " + username);
            model.put("errorMessage", e.getMessage());
            return new ModelAndView(model, "login.mustache");
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
}
