package com.is1.proyecto.security;

import com.is1.proyecto.models.User;

import spark.Request;
import spark.Session;

/**
 * Servicio de autenticación y gestión de sesiones.
 * Maneja login, logout y verificación de estado de autenticación.
 */
public class AuthService {

    /** Encoder centralizado para hasheo y verificación de contraseñas. */
    private final PasswordEncoder passwordEncoder;

    public AuthService(){
        this.passwordEncoder = new PasswordEncoder();
    }

    /**
     * Resultado de una operación de login.
     */
    public static class LoginResult {
        public final boolean success;
        public final String message;
        public final User user;

        public LoginResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public static LoginResult success(User user) {
            return new LoginResult(true, "Login exitoso", user);
        }

        public static LoginResult failure(String message) {
            return new LoginResult(false, message, null);
        }
    }
    /**
     * Autentica un usuario con nombre de usuario y contraseña.
     * 
     * @param username          Nombre de usuario
     * @param plainTextPassword Contraseña en texto plano
     * @return LoginResult con el resultado de la autenticación
     */
    public LoginResult authenticate(String username, String plainTextPassword) {
        // Validaciones básicas: campos de usuario y contraseña no pueden ser nulos o vacíos.
        if (username == null || username.isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
            return LoginResult.failure("El nombre de usuario y la contraseña son requeridos.");
        }

        // Busca la cuenta en la base de datos por el nombre de usuario.
        User user = User.findFirst("name = ?", username);

        // Si no se encuentra ninguna cuenta con ese nombre de usuario.
        if (user == null) {
            return LoginResult.failure("Usuario o contraseña incorrectos."); // Mensaje genérico por seguridad.
        }

        // Obtiene la contraseña hasheada almacenada en la base de datos.
        String storedHashedPassword = user.getString("password");

        // Compara la contraseña en texto plano ingresada con la contraseña hasheada almacenada.
        // PasswordEncoder.verify delega en BCrypt.checkpw internamente.
        if (passwordEncoder.verify(plainTextPassword, storedHashedPassword)) {
            // Autenticación exitosa.
            return LoginResult.success(user);
        } else {
            // Contraseña incorrecta.
            return LoginResult.failure("Usuario o contraseña incorrectos."); // Mensaje genérico por seguridad.
        }
    }

    /**
     * Crea una sesión para el usuario autenticado.
     * 
     * @param req       Solicitud HTTP
     * @param res       Respuesta HTTP
     * @param username  Nombre de usuario
     * @param userId    ID del usuario
     * @param role      Rol del usuario (ADMIN, STUDENT, TEACHER)
     */
    public void createSession(Request req, String username, Object userId, String role) {
        // --- Gestión de Sesión ---
        Session session = req.session(true);
        session.attribute("currentUserUsername", username); // Guarda el nombre de usuario en la sesión.
        session.attribute("userId", userId); // Guarda el ID de la cuenta en la sesión (útil).
        session.attribute("userRole", role); // Guarda el rol del usuario.
        session.attribute("loggedIn", true); // Establece una bandera para indicar que el usuario está logueado.

        System.out.println("DEBUG: Login exitoso para la cuenta: " + username + " con rol: " + role);
        System.out.println("DEBUG: ID de Sesión: " + session.id());
    }

    /**
     * Crea una sesión para el usuario autenticado (sin rol, para backward compatibility).
     * 
     * @param req       Solicitud HTTP
     * @param username  Nombre de usuario
     * @param userId    ID del usuario
     */
    public void createSession(Request req, String username, Object userId) {
        createSession(req, username, userId, "STUDENT"); // Default role
    }

    /**
     * Invalida la sesión del usuario.
     * 
     * @param req Solicitud HTTP
     */
    public void invalidateSession(Request req) {
        // Invalida completamente la sesión del usuario.
        // Esto elimina todos los atributos guardados en la sesión y la marca como inválida.
        // La cookie JSESSIONID en el navegador también será gestionada para invalidarse.
        req.session().invalidate();

        System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /login.");
    }

    /**
     * Verifica si el usuario está autenticado.
     * 
     * @param req Solicitud HTTP
     * @return true si el usuario está logueado, false en caso contrario
     */
    public boolean isAuthenticated(Request req) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
        // significa que el usuario no está logueado o su sesión expiró.
        return currentUsername != null && loggedIn != null && loggedIn;
    }

    /**
     * Obtiene el nombre de usuario actual de la sesión.
     * 
     * @param req Solicitud HTTP
     * @return Nombre de usuario o null si no está autenticado
     */
    public String getCurrentUsername(Request req) {
        return req.session().attribute("currentUserUsername");
    }

    /**
     * Obtiene el rol del usuario actual de la sesión.
     * 
     * @param req Solicitud HTTP
     * @return Role del usuario o null si no está autenticado
     */
    public Role getCurrentUserRole(Request req) {
        String roleStr = req.session().attribute("userRole");
        return Role.fromString(roleStr);
    }

    /**
     * Obtiene el ID del usuario actual de la sesión.
     * 
     * @param req Solicitud HTTP
     * @return ID del usuario o null si no está autenticado
     */
    public Object getCurrentUserId(Request req) {
        return req.session().attribute("userId");
    }

    /**
     * Hashea una contraseña delegando en {@link PasswordEncoder#encode(String)}.
     *
     * @param plainTextPassword Contraseña en texto plano
     * @return Contraseña hasheada
     */
    public String hashPassword(String plainTextPassword) {
        return passwordEncoder.encode(plainTextPassword);
    }
}
