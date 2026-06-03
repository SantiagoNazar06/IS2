package com.is1.proyecto.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.User;
import com.is1.proyecto.security.AuthService;
import spark.Response;

import java.util.Map;

/**
 * Servicio para operaciones relacionadas con usuarios.
 */
public class UserService {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public UserService(AuthService authService) {
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Resultado de registro de usuario.
     */
    public static class RegisterResult {
        public final boolean success;
        public final int statusCode;
        public final String redirectUrl;
        public final String message;
        public final String errorMessage;

        private RegisterResult(boolean success, int statusCode, String redirectUrl, String message, String errorMessage) {
            this.success = success;
            this.statusCode = statusCode;
            this.redirectUrl = redirectUrl;
            this.message = message;
            this.errorMessage = errorMessage;
        }

        public static RegisterResult ok(String redirectUrl, String message) {
            return new RegisterResult(true, 201, redirectUrl, message, null);
        }

        public static RegisterResult error(int statusCode, String redirectUrl, String errorMessage) {
            return new RegisterResult(false, statusCode, redirectUrl, null, errorMessage);
        }
    }

    /**
     * Crea un nuevo usuario en la base de datos.
     * 
     * @param name     Nombre del usuario
     * @param password Contraseña (será hasheada)
     * @return RegisterResult con el resultado de la operación
     */
    public RegisterResult createUser(String name, String password) {
        // Validaciones básicas: campos no pueden ser nulos o vacíos.
        if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
            return RegisterResult.error(400, "/user/create?error=Nombre y contraseña son requeridos.", null);
        }

        try {
            // Intenta crear y guardar la nueva cuenta en la base de datos.
            User newUser = new User(); // Crea una nueva instancia del modelo User.
            // Hashea la contraseña de forma segura antes de guardarla.
            String hashedPassword = authService.hashPassword(password);

            newUser.set("name", name); // Asigna el nombre de usuario.
            newUser.set("password", hashedPassword); // Asigna la contraseña hasheada.
            newUser.saveIt(); // Guarda el nuevo usuario en la tabla 'users'.

            return RegisterResult.ok("/user/create?message=Cuenta creada exitosamente para " + name + "!", null);

        } catch (Exception e) {
            // Si ocurre cualquier error durante la operación de DB (ej. nombre de usuario duplicado),
            // se captura aquí y se redirige con un mensaje de error.
            System.err.println("Error al registrar la cuenta: " + e.getMessage());
            e.printStackTrace(); // Imprime el stack trace para depuración.
            return RegisterResult.error(500, "/user/create?error=Error interno al crear la cuenta. Intente de nuevo.", null);
        }
    }

    /**
     * Añade un usuario vía API (devuelve JSON).
     * 
     * @param name     Nombre del usuario
     * @param password Contraseña
     * @param res      Respuesta HTTP
     * @return JSON con el resultado
     */
    public String addUserApi(String name, String password, Response res) {
        res.type("application/json"); // Establece el tipo de contenido de la respuesta a JSON.

        // --- Validaciones básicas ---
        if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
            res.status(400); // Bad Request.
            try {
                return objectMapper.writeValueAsString(Map.of("error", "Nombre y contraseña son requeridos."));
            } catch (Exception e) {
                return "{\"error\": \"Nombre y contraseña son requeridos.\"}";
            }
        }

        try {
            // --- Creación y guardado del usuario usando el modelo ActiveJDBC ---
            User newUser = new User(); // Crea una nueva instancia de tu modelo User.
            // ¡ADVERTENCIA DE SEGURIDAD CRÍTICA!
            // En una aplicación real, las contraseñas DEBEN ser hasheadas (ej. con BCrypt)
            // ANTES de guardarse en la base de datos, NUNCA en texto plano.
            // (Nota: El código original tenía la contraseña en texto plano aquí.
            // Se recomienda usar `BCrypt.hashpw(password, BCrypt.gensalt())` como en la
            // ruta '/user/new').
            newUser.set("name", name); // Asigna el nombre al campo 'name'.
            newUser.set("role", "STUDENT"); // Asigna rol STUDENT (BCrypt deferido)
            // TODO: Cifrar la contraseña antes de guardar
            newUser.set("password", password); // Asigna la contraseña al campo 'password'.
            newUser.saveIt(); // Guarda el nuevo usuario en la tabla 'users'.

            res.status(201); // Created.
            // Devuelve una respuesta JSON con el mensaje y el ID del nuevo usuario.
            try {
                return objectMapper.writeValueAsString(
                        Map.of("message", "Usuario '" + name + "' registrado con éxito.", "id", newUser.getId()));
            } catch (Exception e) {
                return "{\"message\": \"Usuario '" + name + "' registrado con éxito.\", \"id\": " + newUser.getId() + "}";
            }

        } catch (Exception e) {
            // Si ocurre cualquier error durante la operación de DB, se captura aquí.
            System.err.println("Error al registrar usuario: " + e.getMessage());
            e.printStackTrace(); // Imprime el stack trace para depuración.
            res.status(500); // Internal Server Error.
            try {
                return objectMapper.writeValueAsString(Map.of("error", "Error interno al registrar usuario: " + e.getMessage()));
            } catch (Exception ex) {
                return "{\"error\": \"Error interno al registrar usuario: " + e.getMessage() + "\"}";
            }
        }
    }
}
