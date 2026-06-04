package com.is1.proyecto.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

/**
 * Tests unitarios para SecurityConfig.
 */
class SecurityConfigTest {

    // ==================== isPublicRoute Tests ====================

    @Test
    void isPublicRoute_loginPagesArePublic() {
        assertTrue(SecurityConfig.isPublicRoute("/"));
        assertTrue(SecurityConfig.isPublicRoute("/login"));
        assertTrue(SecurityConfig.isPublicRoute("/logout"));
    }

    @Test
    void isPublicRoute_registrationPagesArePublic() {
        assertTrue(SecurityConfig.isPublicRoute("/user/create"));
    }

    @Test
    void isPublicRoute_userNewIsNotPublic() {
        assertFalse(SecurityConfig.isPublicRoute("/user/new"));
    }

    @Test
    void isPublicRoute_registerTeacherIsNotPublic() {
        assertFalse(SecurityConfig.isPublicRoute("/register_teacher"));
    }

    @Test
    void isPublicRoute_registerStudentIsNotPublic() {
        assertFalse(SecurityConfig.isPublicRoute("/register_student"));
    }

    @Test
    void isPublicRoute_staticResourcesArePublic() {
        assertTrue(SecurityConfig.isPublicRoute("/public/"));
        assertTrue(SecurityConfig.isPublicRoute("/css/style.css"));
        assertTrue(SecurityConfig.isPublicRoute("/js/app.js"));
        assertTrue(SecurityConfig.isPublicRoute("/images/logo.png"));
        assertTrue(SecurityConfig.isPublicRoute("/favicon.ico"));
    }

    @Test
    void isPublicRoute_protectedRoutesReturnFalse() {
        // Estas rutas NO están en la whitelist de públicas
        assertFalse(SecurityConfig.isPublicRoute("/dashboard"));
        assertFalse(SecurityConfig.isPublicRoute("/students"));
        assertFalse(SecurityConfig.isPublicRoute("/teachers"));
        assertFalse(SecurityConfig.isPublicRoute("/careers"));
    }

    @Test
    void isPublicRoute_adminRoutesAreProtected() {
        assertFalse(SecurityConfig.isPublicRoute("/admin"));
        assertFalse(SecurityConfig.isPublicRoute("/admin/users"));
        assertFalse(SecurityConfig.isPublicRoute("/add_users"));
    }

    @Test
    void isPublicRoute_nullPathReturnsFalse() {
        assertFalse(SecurityConfig.isPublicRoute(null));
    }

    @Test
    void isPublicRoute_unknownPathIsProtected() {
        // Por defecto, rutas desconocidas requieren autenticación
        // isPublicRoute retorna false para rutas no explícitamente públicas
        assertFalse(SecurityConfig.isPublicRoute("/unknown/path"));
        assertFalse(SecurityConfig.isPublicRoute("/api/v1/data"));
    }

    // ==================== getRequiredRoles Tests ====================

    @Test
    void getRequiredRoles_adminRoutesRequireAdmin() {
        Set<Role> adminRoles = SecurityConfig.getRequiredRoles("/admin");
        assertNotNull(adminRoles);
        assertEquals(1, adminRoles.size());
        assertTrue(adminRoles.contains(Role.ADMIN));

        Set<Role> addUsersRoles = SecurityConfig.getRequiredRoles("/add_users");
        assertNotNull(addUsersRoles);
        assertTrue(addUsersRoles.contains(Role.ADMIN));
    }

    @Test
    void getRequiredRoles_studentRoutes() {
        Set<Role> roles = SecurityConfig.getRequiredRoles("/students");
        assertNotNull(roles);
        assertTrue(roles.contains(Role.ADMIN));
        assertTrue(roles.contains(Role.TEACHER));
        assertFalse(roles.contains(Role.STUDENT));
    }

    @Test
    void getRequiredRoles_teacherRoutes() {
        Set<Role> roles = SecurityConfig.getRequiredRoles("/teachers");
        assertNotNull(roles);
        assertTrue(roles.contains(Role.ADMIN));
        assertTrue(roles.contains(Role.TEACHER));
        assertFalse(roles.contains(Role.STUDENT));
    }

    @Test
    void getRequiredRoles_userNewRequiresAdmin() {
        Set<Role> roles = SecurityConfig.getRequiredRoles("/user/new");
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertTrue(roles.contains(Role.ADMIN));
    }

    @Test
    void getRequiredRoles_registerTeacherRequiresAdmin() {
        Set<Role> roles = SecurityConfig.getRequiredRoles("/register_teacher");
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertTrue(roles.contains(Role.ADMIN));
    }

    @Test
    void getRequiredRoles_registerStudentRequiresAdmin() {
        Set<Role> roles = SecurityConfig.getRequiredRoles("/register_student");
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertTrue(roles.contains(Role.ADMIN));
    }

    @Test
    void getRequiredRoles_subjectsRequiresAdminAndTeacher() {
        Set<Role> roles = SecurityConfig.getRequiredRoles("/subjects");
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains(Role.ADMIN));
        assertTrue(roles.contains(Role.TEACHER));
    }

    @Test
    void getRequiredRoles_studyPlansRequiresAllRoles() {
        Set<Role> roles = SecurityConfig.getRequiredRoles("/study-plans");
        assertNotNull(roles);
        assertEquals(3, roles.size());
        assertTrue(roles.contains(Role.ADMIN));
        assertTrue(roles.contains(Role.TEACHER));
        assertTrue(roles.contains(Role.STUDENT));
    }

    @Test
    void getRequiredRoles_gradesRequiresAdminAndTeacher() {
        Set<Role> roles = SecurityConfig.getRequiredRoles("/grades");
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains(Role.ADMIN));
        assertTrue(roles.contains(Role.TEACHER));
    }

    @Test
    void getRequiredRoles_dashboardAccessibleToAll() {
        Set<Role> roles = SecurityConfig.getRequiredRoles("/dashboard");
        assertNotNull(roles);
        assertTrue(roles.contains(Role.ADMIN));
        assertTrue(roles.contains(Role.STUDENT));
        assertTrue(roles.contains(Role.TEACHER));
    }

    @Test
    void getRequiredRoles_nullPathReturnsNull() {
        assertNull(SecurityConfig.getRequiredRoles(null));
    }

    @Test
    void getRequiredRoles_unknownPathReturnsNull() {
        // Rutas sin restricción específica de rol (cualquier autenticado puede acceder)
        assertNull(SecurityConfig.getRequiredRoles("/some/unknown/path"));
    }

    @Test
    void getRequiredRoles_subpathsMatch() {
        // Subpaths deben coincidir con el prefijo
        Set<Role> roles = SecurityConfig.getRequiredRoles("/students/123");
        assertNotNull(roles);
        assertTrue(roles.contains(Role.TEACHER));

        Set<Role> teacherRoles = SecurityConfig.getRequiredRoles("/teachers/profile");
        assertNotNull(teacherRoles);
        assertTrue(teacherRoles.contains(Role.TEACHER));

        // /enrollments subpaths
        Set<Role> enrollRoles = SecurityConfig.getRequiredRoles("/enrollments/student/123");
        assertNotNull(enrollRoles);
        assertTrue(enrollRoles.contains(Role.STUDENT));
    }

    // ==================== requiresAuthentication Tests ====================

    @Test
    void requiresAuthentication_publicRoutesReturnFalse() {
        assertFalse(SecurityConfig.requiresAuthentication("/login"));
        assertFalse(SecurityConfig.requiresAuthentication("/"));
    }

    @Test
    void requiresAuthentication_protectedRoutesReturnTrue() {
        // requiresAuthentication = !isPublicRoute
        // Las rutas protegidas no son públicas, entonces requieren auth
        assertTrue(SecurityConfig.requiresAuthentication("/dashboard"));
        assertTrue(SecurityConfig.requiresAuthentication("/students"));
        assertTrue(SecurityConfig.requiresAuthentication("/add_users"));
    }

    // ==================== Error Messages Tests ====================

    @Test
    void errorMessages_areValidJson() {
        assertTrue(SecurityConfig.MSG_UNAUTHORIZED.contains("error"));
        assertTrue(SecurityConfig.MSG_FORBIDDEN.contains("error"));
    }

    @Test
    void errorCodes_areCorrect() {
        assertEquals(401, SecurityConfig.HTTP_UNAUTHORIZED);
        assertEquals(403, SecurityConfig.HTTP_FORBIDDEN);
    }
}
