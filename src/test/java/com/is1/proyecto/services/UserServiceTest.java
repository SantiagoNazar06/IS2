package com.is1.proyecto.services;

import com.is1.proyecto.models.User;
import com.is1.proyecto.security.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.Response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * @Mock AuthService (constructor-injected), MockedConstruction<User> for new User() calls.
 * PasswordEncoder and ObjectMapper are created internally by the constructor — tested as real.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserService service;

    // =====================================================================
    // createUser — validation
    // =====================================================================

    @Test
    void createUser_nullName_returnsError() {
        UserService.RegisterResult result = service.createUser(null, "password123");

        assertFalse(result.success);
        assertEquals(400, result.statusCode);
    }

    @Test
    void createUser_emptyName_returnsError() {
        UserService.RegisterResult result = service.createUser("", "password123");

        assertFalse(result.success);
        assertEquals(400, result.statusCode);
    }

    @Test
    void createUser_nullPassword_returnsError() {
        UserService.RegisterResult result = service.createUser("john", null);

        assertFalse(result.success);
        assertEquals(400, result.statusCode);
    }

    @Test
    void createUser_emptyPassword_returnsError() {
        UserService.RegisterResult result = service.createUser("john", "");

        assertFalse(result.success);
        assertEquals(400, result.statusCode);
    }

    // =====================================================================
    // createUser — success
    // =====================================================================

    @Test
    void createUser_success_returnsOk() {
        when(authService.hashPassword("password123")).thenReturn("$2a$10$hashed");

        try (MockedConstruction<User> userConst = mockConstruction(User.class)) {
            UserService.RegisterResult result = service.createUser("john", "password123");

            assertTrue(result.success);
            assertEquals(201, result.statusCode);

            List<User> constructed = userConst.constructed();
            assertEquals(1, constructed.size());
            User mockUser = constructed.get(0);
            verify(mockUser).set("name", "john");
            verify(mockUser).set("password", "$2a$10$hashed");
            verify(mockUser).saveIt();
        }
    }

    // =====================================================================
    // createUser — error propagation
    // =====================================================================

    @Test
    void createUser_authServiceThrows_returnsError() {
        try (MockedConstruction<User> userConst = mockConstruction(User.class)) {
            when(authService.hashPassword(anyString())).thenThrow(new RuntimeException("Hashing failed"));

            UserService.RegisterResult result = service.createUser("john", "password123");

            assertFalse(result.success);
            assertEquals(500, result.statusCode);
        }
    }

    // =====================================================================
    // addUserApi — validation
    // =====================================================================

    @Test
    void addUserApi_nullName_returns400() {
        Response res = mock(Response.class);

        String result = service.addUserApi(null, "password123", res);

        verify(res).type("application/json");
        verify(res).status(400);
        assertTrue(result.contains("requeridos"));
    }

    @Test
    void addUserApi_emptyName_returns400() {
        Response res = mock(Response.class);

        String result = service.addUserApi("", "password123", res);

        verify(res).type("application/json");
        verify(res).status(400);
        assertTrue(result.contains("requeridos"));
    }

    @Test
    void addUserApi_nullPassword_returns400() {
        Response res = mock(Response.class);

        String result = service.addUserApi("john", null, res);

        verify(res).type("application/json");
        verify(res).status(400);
        assertTrue(result.contains("requeridos"));
    }

    // =====================================================================
    // addUserApi — success
    // =====================================================================

    @Test
    void addUserApi_success_returns201() {
        Response res = mock(Response.class);

        try (MockedConstruction<User> userConst = mockConstruction(User.class)) {
            String result = service.addUserApi("john", "password123", res);

            assertTrue(result.contains("registrado con"));
            verify(res).type("application/json");
            verify(res).status(201);

            List<User> constructed = userConst.constructed();
            assertEquals(1, constructed.size());
            User mockUser = constructed.get(0);
            verify(mockUser).set("name", "john");
            verify(mockUser).set("role", "STUDENT");
            verify(mockUser).set(eq("password"), anyString());
            verify(mockUser).saveIt();
        }
    }
}
