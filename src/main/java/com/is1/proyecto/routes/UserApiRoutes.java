package com.is1.proyecto.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.User;
import com.is1.proyecto.security.AuthService;
import com.is1.proyecto.services.UserService;
import spark.Request;
import spark.Response;

import java.util.Map;

import static spark.Spark.*;

public class UserApiRoutes {

    private final AuthService authService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public UserApiRoutes(AuthService authService, UserService userService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public void register() {
        get("/api/users", this::handleListAll);
        get("/api/users/:id", this::handleGetById);
        post("/api/users", this::handleCreate);
        put("/api/users/:id", this::handleUpdate);
        delete("/api/users/:id", this::handleDelete);
    }

    // ── GET /api/users ──

    Object handleListAll(Request req, Response res) {
        res.type("application/json");
        try {
            return toJson(userService.listAllUsers());
        } catch (Exception e) {
            res.status(500);
            return toJson(Map.of("error", "Error interno al listar usuarios"));
        }
    }

    // ── GET /api/users/:id ──

    Object handleGetById(Request req, Response res) {
        res.type("application/json");
        try {
            Long id = Long.parseLong(req.params(":id"));
            Map<String, Object> user = userService.getUserById(id);
            if (user == null) {
                res.status(404);
                return toJson(Map.of("error", "Usuario no encontrado"));
            }
            return toJson(user);
        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "ID debe ser un numero valido"));
        } catch (Exception e) {
            res.status(500);
            return toJson(Map.of("error", "Error interno al obtener el usuario"));
        }
    }

    // ── POST /api/users ──

    Object handleCreate(Request req, Response res) {
        res.type("application/json");
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.body(), Map.class);

            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String role = (String) body.get("role");
            Long personId = body.get("personId") != null
                    ? ((Number) body.get("personId")).longValue()
                    : null;

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                res.status(400);
                return toJson(Map.of("error", "username y password son requeridos"));
            }

            User created = userService.createUserRest(username, password, role, personId);
            res.status(201);
            Long newId = ((Number) created.getId()).longValue();
            return toJson(userService.getUserById(newId));

        } catch (Exception e) {
            res.status(400);
            return toJson(Map.of("error", "Solicitud invalida: " + e.getMessage()));
        }
    }

    // ── PUT /api/users/:id ──

    Object handleUpdate(Request req, Response res) {
        res.type("application/json");
        try {
            Long id = Long.parseLong(req.params(":id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.body(), Map.class);

            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String role = (String) body.get("role");
            Long personId = body.get("personId") != null
                    ? ((Number) body.get("personId")).longValue()
                    : null;

            User updated = userService.updateUserRest(id, username, password, role, personId);
            if (updated == null) {
                res.status(404);
                return toJson(Map.of("error", "Usuario no encontrado"));
            }

            return toJson(userService.getUserById(id));

        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "ID debe ser un numero valido"));
        } catch (Exception e) {
            res.status(400);
            return toJson(Map.of("error", "Solicitud invalida: " + e.getMessage()));
        }
    }

    // ── DELETE /api/users/:id ──

    Object handleDelete(Request req, Response res) {
        res.type("application/json");
        try {
            Long id = Long.parseLong(req.params(":id"));

            boolean deleted = userService.deleteUserRest(id);
            if (!deleted) {
                res.status(404);
                return toJson(Map.of("error", "Usuario no encontrado"));
            }

            res.status(204);
            return "";

        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "ID debe ser un numero valido"));
        } catch (Exception e) {
            res.status(500);
            return toJson(Map.of("error", "Error interno al eliminar el usuario"));
        }
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
