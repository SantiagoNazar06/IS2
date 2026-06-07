package com.is1.proyecto.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuración centralizada de seguridad.
 * Define rutas públicas, rutas protegidas con roles requeridos y códigos de error.
 */
public class SecurityConfig {

    // ==================== RUTAS PÚBLICAS (WHITELIST) ====================
    // Estas rutas NO requieren autenticación

    public static final Set<String> PUBLIC_ROUTES = new HashSet<>(Arrays.asList(
        // Rutas de autenticación
        "/",
        "/login",
        "/logout",
        
        // Rutas de registro (solo creación vía form público)
        "/user/create",
        
        // Rutas estáticas (templates)
        "/public/",
        "/css/",
        "/js/",
        "/images/",
        "/favicon.ico"
    ));

    // ==================== RUTAS PROTEGIDAS POR ROL ====================
    // clave = prefijo de ruta, valor = roles que pueden acceder

    public static final Map<String, Set<Role>> PROTECTED_ROUTES = new LinkedHashMap<>();

    static {
        // Rutas administrativas - solo ADMIN
        PROTECTED_ROUTES.put("/admin/assignments", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/admin", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/add_users", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/api/users", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/user/delete", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/user/update", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/user/new", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/register_teacher", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/update_teacher", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/delete_teacher", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/register_student", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/update_student", Set.of(Role.ADMIN));
        PROTECTED_ROUTES.put("/delete_student", Set.of(Role.ADMIN));
        
        // Rutas de dashboard - cualquier usuario autenticado
        PROTECTED_ROUTES.put("/dashboard", Set.of(Role.ADMIN, Role.STUDENT, Role.TEACHER));
        
        // Rutas de estudiantes - ADMIN o TEACHER (listado)
        PROTECTED_ROUTES.put("/students", Set.of(Role.ADMIN, Role.TEACHER));
        // Sub-rutas de estudiantes (enrollments, etc.) — ADMIN o STUDENT
        PROTECTED_ROUTES.put("/students/", Set.of(Role.ADMIN, Role.STUDENT));
        PROTECTED_ROUTES.put("/student", Set.of(Role.ADMIN, Role.STUDENT));

        // Rutas de inscripciones - ADMIN o STUDENT
        PROTECTED_ROUTES.put("/enrollments", Set.of(Role.ADMIN, Role.STUDENT));
        
        // Rutas de profesores - TEACHER o ADMIN
        PROTECTED_ROUTES.put("/teachers", Set.of(Role.ADMIN, Role.TEACHER));
        PROTECTED_ROUTES.put("/teacher", Set.of(Role.ADMIN, Role.TEACHER));
        
        // Rutas de carreras - cualquier usuario autenticado
        PROTECTED_ROUTES.put("/careers", Set.of(Role.ADMIN, Role.STUDENT, Role.TEACHER));
        PROTECTED_ROUTES.put("/career", Set.of(Role.ADMIN, Role.STUDENT, Role.TEACHER));
        
        // Rutas de materias - ADMIN y TEACHER
        PROTECTED_ROUTES.put("/teacher/assignments", Set.of(Role.TEACHER));
        PROTECTED_ROUTES.put("/subjects", Set.of(Role.ADMIN, Role.TEACHER));
        PROTECTED_ROUTES.put("/subject", Set.of(Role.ADMIN, Role.TEACHER));
        
        // Rutas de planes de estudio - ADMIN gestiona, TEACHER/STUDENT solo ven JSON
        PROTECTED_ROUTES.put("/study-plans", Set.of(Role.ADMIN, Role.TEACHER, Role.STUDENT));
        PROTECTED_ROUTES.put("/study-plan", Set.of(Role.ADMIN, Role.TEACHER, Role.STUDENT));
        
        // Rutas de calificaciones - ADMIN y TEACHER
        PROTECTED_ROUTES.put("/grades", Set.of(Role.ADMIN, Role.TEACHER));
        
        // Rutas de evaluaciones - ADMIN y TEACHER
        PROTECTED_ROUTES.put("/evaluations", Set.of(Role.ADMIN, Role.TEACHER));
        
        // Ruta de perfil - STUDENT y TEACHER (admin excluido)
        PROTECTED_ROUTES.put("/profile", Set.of(Role.STUDENT, Role.TEACHER));
    }

    // ==================== CÓDIGOS DE ERROR ====================

    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;

    public static final String MSG_UNAUTHORIZED = "{\"error\": \"No autorizado. Inicie sesión para acceder a este recurso.\"}";
    public static final String MSG_FORBIDDEN = "{\"error\": \"Acceso denegado. No tiene los permisos necesarios para este recurso.\"}";

    // ==================== HELPERS ====================

    /**
     * Verifica si una ruta es pública (no requiere autenticación).
     * 
     * @param path Ruta a verificar
     * @return true si es pública, false si requiere autenticación
     */
    public static boolean isPublicRoute(String path) {
        if (path == null) {
            return false;
        }
        
        // Normalizar la ruta (asegurar que empieza con /)
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        // Verificar exact match
        if (PUBLIC_ROUTES.contains(path)) {
            return true;
        }
        
        // Verificar prefijos públicos (ej: /public/, /css/)
        // Excluir "/" del matching de prefijos porque coincidiría con todas las rutas
        for (String publicRoute : PUBLIC_ROUTES) {
            if (publicRoute.endsWith("/") && !publicRoute.equals("/") && path.startsWith(publicRoute)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Obtiene los roles requeridos para una ruta.
     * Usa el prefijo más largo que matchee (longest prefix match)
     * para permitir rutas más específicas con diferentes roles.
     * 
     * @param path Ruta a verificar
     * @return Set de roles permitidos, o null si la ruta no está protegida por rol específico
     */
    public static Set<Role> getRequiredRoles(String path) {
        if (path == null) {
            return null;
        }
        
        // Normalizar la ruta
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        String bestMatch = null;
        Set<Role> bestRoles = null;
        
        for (Map.Entry<String, Set<Role>> entry : PROTECTED_ROUTES.entrySet()) {
            String protectedPath = entry.getKey();
            
            // Construir prefijo para matching:
            // - Si protectedPath termina en "/", usarlo directamente
            // - Si no, agregar "/" al final (para evitar falsos positivos)
            String matchPrefix = protectedPath.endsWith("/") ? protectedPath : protectedPath + "/";
            
            if (path.equals(protectedPath)) {
                // Coincidencia exacta → siempre gana
                return entry.getValue();
            } else if (path.startsWith(matchPrefix)) {
                // Coincidencia por prefijo → elegir el más largo
                if (bestMatch == null || protectedPath.length() > bestMatch.length()) {
                    bestMatch = protectedPath;
                    bestRoles = entry.getValue();
                }
            }
        }
        
        // Si encontramos match por prefijo, devolver el más específico
        if (bestRoles != null) {
            return bestRoles;
        }
        
        // Ruta protegida pero sin restricción de rol específica (cualquier autenticado)
        // Retornar null significa que cualquier usuario autenticado puede acceder
        return null;
    }

    /**
     * Verifica si una ruta requiere autenticación.
     * 
     * @param path Ruta a verificar
     * @return true si requiere autenticación
     */
    public static boolean requiresAuthentication(String path) {
        return !isPublicRoute(path);
    }
}
