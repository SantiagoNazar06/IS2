package com.is1.proyecto.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import spark.Request;
import spark.Session;

import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuthService relacionados con roles y sesiones.
 * Nota: Solo testa la lógica de extracción de roles, no la conexión real a DB.
 */
class AuthServiceRoleTest {

    private final AuthService authService = new AuthService();

    @Test
    void getCurrentUserRole_returnsStoredRole() {
        // Arrange
        Request mockReq = mock(Request.class);
        Session mockSession = mock(Session.class);
        when(mockReq.session()).thenReturn(mockSession);
        when(mockSession.attribute("currentUserUsername")).thenReturn("testuser");
        when(mockSession.attribute("loggedIn")).thenReturn(true);
        when(mockSession.attribute("userRole")).thenReturn("ADMIN");

        // Act
        Role role = authService.getCurrentUserRole(mockReq);

        // Assert
        assertEquals(Role.ADMIN, role);
    }

    @Test
    void getCurrentUserRole_studentRole() {
        // Arrange
        Request mockReq = mock(Request.class);
        Session mockSession = mock(Session.class);
        when(mockReq.session()).thenReturn(mockSession);
        when(mockSession.attribute("currentUserUsername")).thenReturn("student1");
        when(mockSession.attribute("loggedIn")).thenReturn(true);
        when(mockSession.attribute("userRole")).thenReturn("STUDENT");

        // Act
        Role role = authService.getCurrentUserRole(mockReq);

        // Assert
        assertEquals(Role.STUDENT, role);
    }

    @Test
    void getCurrentUserRole_teacherRole() {
        // Arrange
        Request mockReq = mock(Request.class);
        Session mockSession = mock(Session.class);
        when(mockReq.session()).thenReturn(mockSession);
        when(mockSession.attribute("currentUserUsername")).thenReturn("teacher1");
        when(mockSession.attribute("loggedIn")).thenReturn(true);
        when(mockSession.attribute("userRole")).thenReturn("TEACHER");

        // Act
        Role role = authService.getCurrentUserRole(mockReq);

        // Assert
        assertEquals(Role.TEACHER, role);
    }

    @Test
    void getCurrentUserRole_nullRoleReturnsNull() {
        // Arrange
        Request mockReq = mock(Request.class);
        Session mockSession = mock(Session.class);
        when(mockReq.session()).thenReturn(mockSession);
        when(mockSession.attribute("currentUserUsername")).thenReturn("testuser");
        when(mockSession.attribute("loggedIn")).thenReturn(true);
        when(mockSession.attribute("userRole")).thenReturn(null);

        // Act
        Role role = authService.getCurrentUserRole(mockReq);

        // Assert
        assertNull(role);
    }

    @Test
    void getCurrentUserRole_invalidRoleReturnsNull() {
        // Arrange
        Request mockReq = mock(Request.class);
        Session mockSession = mock(Session.class);
        when(mockReq.session()).thenReturn(mockSession);
        when(mockSession.attribute("currentUserUsername")).thenReturn("testuser");
        when(mockSession.attribute("loggedIn")).thenReturn(true);
        when(mockSession.attribute("userRole")).thenReturn("INVALID_ROLE");

        // Act
        Role role = authService.getCurrentUserRole(mockReq);

        // Assert
        assertNull(role);
    }

    @Test
    void getCurrentUserRole_caseInsensitive() {
        // Arrange
        Request mockReq = mock(Request.class);
        Session mockSession = mock(Session.class);
        when(mockReq.session()).thenReturn(mockSession);
        when(mockSession.attribute("currentUserUsername")).thenReturn("testuser");
        when(mockSession.attribute("loggedIn")).thenReturn(true);
        when(mockSession.attribute("userRole")).thenReturn("admin"); // lowercase

        // Act
        Role role = authService.getCurrentUserRole(mockReq);

        // Assert
        assertEquals(Role.ADMIN, role);
    }

    @Test
    void getCurrentUserRole_whitespaceTrimmed() {
        // Arrange
        Request mockReq = mock(Request.class);
        Session mockSession = mock(Session.class);
        when(mockReq.session()).thenReturn(mockSession);
        when(mockSession.attribute("currentUserUsername")).thenReturn("testuser");
        when(mockSession.attribute("loggedIn")).thenReturn(true);
        when(mockSession.attribute("userRole")).thenReturn("  STUDENT  "); // with whitespace

        // Act
        Role role = authService.getCurrentUserRole(mockReq);

        // Assert
        assertEquals(Role.STUDENT, role);
    }

    @Test
    void isAuthenticated_returnsTrueWithValidSession() {
        // Arrange
        Request mockReq = mock(Request.class);
        Session mockSession = mock(Session.class);
        when(mockReq.session()).thenReturn(mockSession);
        when(mockSession.attribute("currentUserUsername")).thenReturn("testuser");
        when(mockSession.attribute("loggedIn")).thenReturn(true);

        // Act
        boolean isAuth = authService.isAuthenticated(mockReq);

        // Assert
        assertTrue(isAuth);
    }

    @Test
    void isAuthenticated_returnsFalseWithoutSession() {
        // Arrange
        Request mockReq = mock(Request.class);
        Session mockSession = mock(Session.class);
        when(mockReq.session()).thenReturn(mockSession);
        when(mockSession.attribute("currentUserUsername")).thenReturn(null);
        when(mockSession.attribute("loggedIn")).thenReturn(null);

        // Act
        boolean isAuth = authService.isAuthenticated(mockReq);

        // Assert
        assertFalse(isAuth);
    }

    @Test
    void isAuthenticated_returnsFalseWhenLoggedInIsFalse() {
        // Arrange
        Request mockReq = mock(Request.class);
        Session mockSession = mock(Session.class);
        when(mockReq.session()).thenReturn(mockSession);
        when(mockSession.attribute("currentUserUsername")).thenReturn("testuser");
        when(mockSession.attribute("loggedIn")).thenReturn(false);

        // Act
        boolean isAuth = authService.isAuthenticated(mockReq);

        // Assert
        assertFalse(isAuth);
    }
}
