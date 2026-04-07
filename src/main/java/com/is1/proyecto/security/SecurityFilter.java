package com.is1.proyecto.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * Filtro de seguridad que se ejecuta antes de cada request.
 * 
 * Responsabilidades:
 * 1. Validar autenticación en rutas protegidas
 * 2. Verificar roles de usuario
 * 3. Aplicar headers CORS
 * 4. Manejar preflight OPTIONS
 */
public class SecurityFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityFilter.class);

    // Referencia al AuthService (se inyecta al registrar)
    private static AuthService authService;

    /**
     * Inyecta el AuthService para poder verificar sesiones.
     */
    public static void setAuthService(AuthService service) {
        authService = service;
    }

    /**
     * Crea el filtro before que valida autenticación y roles.
     */
    public static Filter createBeforeFilter() {
        return (Request req, Response res) -> {
            String path = req.pathInfo();
            String method = req.requestMethod();
            
            // 1. Aplicar headers CORS a TODAS las respuestas
            applyCorsHeaders(res);
            
            // 2. Manejar preflight OPTIONS
            if ("OPTIONS".equalsIgnoreCase(method)) {
                return;
            }
            
            // 3. Verificar si la ruta es pública
            if (SecurityConfig.isPublicRoute(path)) {
                return;
            }
            
            // 4. Verificar si hay sesión activa
            boolean isAuth = authService != null && authService.isAuthenticated(req);
            
            if (!isAuth) {
                try {
                    // Escribimos la respuesta directamente usando el response raw
                    HttpServletResponse raw = res.raw();
                    raw.setStatus(SecurityConfig.HTTP_UNAUTHORIZED);
                    raw.setContentType("application/json");
                    raw.getWriter().write(SecurityConfig.MSG_UNAUTHORIZED);
                    raw.getWriter().flush();
                    // Importante: Spark necesita saber que la respuesta ya fue enviada
                    Spark.halt(401);
                } catch (IOException e) {
                    LOG.error("Error writing unauthorized response", e);
                }
                return;
            }
            
            // 5. Verificar rol si la ruta lo requiere
            Set<Role> requiredRoles = SecurityConfig.getRequiredRoles(path);
            if (requiredRoles != null && !requiredRoles.isEmpty()) {
                Role userRole = authService.getCurrentUserRole(req);
                
                if (userRole == null || !requiredRoles.contains(userRole)) {
                    try {
                        HttpServletResponse raw = res.raw();
                        raw.setStatus(SecurityConfig.HTTP_FORBIDDEN);
                        raw.setContentType("application/json");
                        raw.getWriter().write(SecurityConfig.MSG_FORBIDDEN);
                        raw.getWriter().flush();
                        Spark.halt(403);
                    } catch (IOException e) {
                        LOG.error("Error writing forbidden response", e);
                    }
                    return;
                }
            }            
        };
    }

    /**
     * Aplica headers CORS a la respuesta.
     */
    private static void applyCorsHeaders(Response res) {
        res.header("Access-Control-Allow-Origin", SecurityConfig.CORS_ALLOWED_ORIGINS);
        res.header("Access-Control-Allow-Methods", SecurityConfig.CORS_ALLOWED_METHODS);
        res.header("Access-Control-Allow-Headers", SecurityConfig.CORS_ALLOWED_HEADERS);
        res.header("Access-Control-Expose-Headers", SecurityConfig.CORS_EXPOSED_HEADERS);
        res.header("Access-Control-Max-Age", SecurityConfig.CORS_MAX_AGE);
        res.header("Access-Control-Allow-Credentials", "true");
    }

    /**
     * Registra el filtro en Spark.
     */
    public static void register() {
        if (authService == null) {
            throw new IllegalStateException("AuthService no configurado. Llama a setAuthService() antes de register()");
        }
        Spark.before(createBeforeFilter());
        LOG.info("SecurityFilter registrado correctamente");
    }
}
