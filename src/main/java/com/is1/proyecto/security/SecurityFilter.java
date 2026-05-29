package com.is1.proyecto.security;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.is1.proyecto.services.AuthService;
import com.is1.proyecto.services.UserClaims;

import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Spark;

/**
 * Filtro de seguridad que se ejecuta antes de cada request.
 * 
 * Responsabilidades:
 * 1. Validar autenticación en rutas protegidas mediante token JWT (cookie o header)
 * 2. Verificar roles de usuario
 * 3. Aplicar headers CORS
 * 4. Manejar preflight OPTIONS
 */
public class SecurityFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    // Referencia al AuthService (se inyecta al registrar)
    private static AuthService authService;

    /**
     * Inyecta el AuthService para poder validar tokens.
     */
    public static void setAuthService(AuthService service) {
        authService = service;
    }

    /**
     * Extrae el token JWT de la cookie 'token' o del header Authorization.
     */
    private static String extractToken(Request req) {
        // 1. Intentar desde cookie
        String token = req.cookie("token");
        if (token != null && !token.isEmpty()) {
            return token;
        }

        // 2. Fallback a header Authorization: Bearer <token>
        String authHeader = req.headers("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        return null;
    }

    /**
     * Crea el filtro before que valida autenticación y roles.
     */
    public static Filter createBeforeFilter() {
        return (Request req, Response res) -> {
            String path = req.pathInfo();
            String method = req.requestMethod();
            
            //Aplicar headers CORS a TODAS las respuestas
            applyCorsHeaders(res);
            
            //Manejar preflight OPTIONS
            if ("OPTIONS".equalsIgnoreCase(method)) {
                return;
            }
            
            //Verificar si la ruta es pública
            if (SecurityConfig.isPublicRoute(path)) {
                return;
            }

            //Extraer y validar token JWT
            String token = extractToken(req);
            UserClaims claims = (authService != null && token != null)
                    ? authService.validateToken(token)
                    : null;

            if (claims == null) {
                try {
                    HttpServletResponse raw = res.raw();
                    raw.setStatus(SecurityConfig.HTTP_UNAUTHORIZED);
                    raw.setContentType("application/json");
                    raw.getWriter().write(SecurityConfig.MSG_UNAUTHORIZED);
                    raw.getWriter().flush();
                    Spark.halt(401);
                } catch (IOException e) {
                    LOG.error("Error writing unauthorized response", e);
                }
                return;
            }

            //Guardar claims en el request para que los handlers puedan usarlos
            req.attribute("userClaims", claims);
            
            //Verificar rol si la ruta lo requiere
            Set<Role> requiredRoles = SecurityConfig.getRequiredRoles(path);
            if (requiredRoles != null && !requiredRoles.isEmpty()) {
                if (claims.getRole() == null || !requiredRoles.contains(claims.getRole())) {
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
