package com.is1.proyecto.services;

import com.is1.proyecto.models.User;
import com.is1.proyecto.security.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuthService.
 * <p>
 * Cubre los criterios de aceptación:
 * AC-1: login() retorna token con credenciales correctas
 * AC-2: login() lanza AuthenticationException con credenciales incorrectas
 * AC-3: logout() invalida el token
 * AC-4: hashPassword() retorna un valor diferente al original
 * AC-5: Token contiene claims (userId, username, role)
 * AC-6: validateToken() retorna null para token inválido
 */
class AuthServiceTest {

    private static final String TEST_AES_KEY = "test-aes-key-for-unit-testing!!!!";
    private static final String TEST_JWT_SECRET = "test-jwt-secret-for-unit-testing!!";

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(TEST_AES_KEY, TEST_JWT_SECRET);
    }

    // ==================== AC-4: Password Hashing ====================

    @Test
    void hashPassword_returnsDifferentValueThanOriginal() {
        String password = "miContraseña123";
        String hash = authService.hashPassword(password);

        assertNotNull(hash);
        assertNotEquals(password, hash);
    }

    @Test
    void hashPassword_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> authService.hashPassword(null));
    }

    @Test
    void hashPassword_throwsOnEmpty() {
        assertThrows(IllegalArgumentException.class, () -> authService.hashPassword(""));
    }

    @Test
    void verifyPassword_returnsTrueForCorrectPassword() {
        String password = "miContraseña123";
        String hash = authService.hashPassword(password);

        assertTrue(authService.verifyPassword(password, hash));
    }

    @Test
    void verifyPassword_returnsFalseForWrongPassword() {
        String hash = authService.hashPassword("contraseñaReal");
        assertFalse(authService.verifyPassword("contraseñaIncorrecta", hash));
    }

    @Test
    void verifyPassword_returnsFalseForNullInput() {
        assertFalse(authService.verifyPassword(null, "hash"));
        assertFalse(authService.verifyPassword("pass", null));
    }

    @Test
    void hashPassword_generatesDifferentHashesForSamePassword() {
        String password = "testPassword";
        String hash1 = authService.hashPassword(password);
        String hash2 = authService.hashPassword(password);

        // Los hashes son diferentes por el IV aleatorio
        assertNotEquals(hash1, hash2);
        // Pero ambos verifican correctamente
        assertTrue(authService.verifyPassword(password, hash1));
        assertTrue(authService.verifyPassword(password, hash2));
    }

    // ==================== AC-6: Token Validation (null/invalid) ====================

    @Test
    void validateToken_returnsNullForNullToken() {
        assertNull(authService.validateToken(null));
    }

    @Test
    void validateToken_returnsNullForEmptyToken() {
        assertNull(authService.validateToken(""));
    }

    @Test
    void validateToken_returnsNullForMalformedToken() {
        assertNull(authService.validateToken("not.a.jwt"));
    }

    // ==================== AC-3: Logout (Blacklist) ====================

    @Test
    void logout_invalidatesToken() throws AuthenticationException {
        // Primero generamos un token válido mockeando User.findFirst
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getName()).thenReturn("testuser");
        when(mockUser.getPassword()).thenReturn(authService.hashPassword("password"));
        when(mockUser.getRole()).thenReturn("STUDENT");

        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(() -> User.findFirst("name = ?", "testuser"))
                    .thenReturn(mockUser);

            String token = authService.login("testuser", "password");
            assertNotNull(token);
            assertNotNull(authService.validateToken(token));

            // Invalidar y verificar
            authService.logout(token);
            assertNull(authService.validateToken(token));
        }
    }

    @Test
    void logout_nullTokenDoesNothing() {
        authService.logout(null);
        authService.logout("");
    }

    @Test
    void logout_onlyInvalidatesSpecifiedToken() throws AuthenticationException {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getName()).thenReturn("testuser");
        when(mockUser.getPassword()).thenReturn(authService.hashPassword("password"));
        when(mockUser.getRole()).thenReturn("STUDENT");

        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(() -> User.findFirst("name = ?", "testuser"))
                    .thenReturn(mockUser);

            String token1 = authService.login("testuser", "password");
            String token2 = authService.login("testuser", "password");

            assertNotNull(authService.validateToken(token1));
            assertNotNull(authService.validateToken(token2));

            authService.logout(token1);
            assertNull(authService.validateToken(token1));
            assertNotNull(authService.validateToken(token2));
        }
    }

    // ==================== AC-1: Login exitoso ====================

    @Test
    void login_returnsTokenWithValidCredentials() throws AuthenticationException {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getName()).thenReturn("testuser");
        when(mockUser.getPassword()).thenReturn(authService.hashPassword("miPassword123"));
        when(mockUser.getRole()).thenReturn("ADMIN");

        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(() -> User.findFirst("name = ?", "testuser"))
                    .thenReturn(mockUser);

            String token = authService.login("testuser", "miPassword123");

            assertNotNull(token);
            assertTrue(token.split("\\.").length == 3, "Debe ser un JWT con 3 partes");
        }
    }

    @Test
    void login_acceptsDifferentRoles() throws AuthenticationException {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(2L);
        when(mockUser.getName()).thenReturn("teacher1");
        when(mockUser.getPassword()).thenReturn(authService.hashPassword("pass"));
        when(mockUser.getRole()).thenReturn("TEACHER");

        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(() -> User.findFirst("name = ?", "teacher1"))
                    .thenReturn(mockUser);

            String token = authService.login("teacher1", "pass");
            assertNotNull(token);

            UserClaims claims = authService.validateToken(token);
            assertNotNull(claims);
            assertEquals("teacher1", claims.getUsername());
            assertEquals(Role.TEACHER, claims.getRole());
        }
    }

    // ==================== AC-2: Login con credenciales incorrectas ====================

    @Test
    void login_throwsAuthenticationExceptionForWrongPassword() {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getName()).thenReturn("testuser");
        when(mockUser.getPassword()).thenReturn(authService.hashPassword("correctPassword"));
        when(mockUser.getRole()).thenReturn("STUDENT");

        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(() -> User.findFirst("name = ?", "testuser"))
                    .thenReturn(mockUser);

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login("testuser", "wrongPassword"));
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void login_throwsAuthenticationExceptionForNonExistentUser() {
        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(() -> User.findFirst("name = ?", "noexiste"))
                    .thenReturn(null);

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login("noexiste", "password"));
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void login_throwsAuthenticationExceptionForNullUsername() {
        assertThrows(AuthenticationException.class,
                () -> authService.login(null, "password"));
    }

    @Test
    void login_throwsAuthenticationExceptionForEmptyUsername() {
        assertThrows(AuthenticationException.class,
                () -> authService.login("", "password"));
    }

    @Test
    void login_throwsAuthenticationExceptionForNullPassword() {
        assertThrows(AuthenticationException.class,
                () -> authService.login("testuser", null));
    }

    @Test
    void login_throwsAuthenticationExceptionForEmptyPassword() {
        assertThrows(AuthenticationException.class,
                () -> authService.login("testuser", ""));
    }

    // ==================== AC-5: Token Claims Structure ====================

    @Test
    void validateToken_returnsClaimsWithUserIdUsernameAndRole() throws AuthenticationException {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(42L);
        when(mockUser.getName()).thenReturn("juanperez");
        when(mockUser.getPassword()).thenReturn(authService.hashPassword("pass123"));
        when(mockUser.getRole()).thenReturn("ADMIN");

        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(() -> User.findFirst("name = ?", "juanperez"))
                    .thenReturn(mockUser);

            String token = authService.login("juanperez", "pass123");
            UserClaims claims = authService.validateToken(token);

            assertNotNull(claims);
            assertEquals(42, claims.getUserId());
            assertEquals("juanperez", claims.getUsername());
            assertEquals(Role.ADMIN, claims.getRole());
        }
    }

    @Test
    void validateToken_returnsNullForTokenWithWrongSignature() {
        // Token firmado con otra clave (no la del AuthService)
        String differentSecret = "completely-different-secret-key-12345678!!";
        byte[] keyBytes;
        try {
            keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(differentSecret.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        String fakeToken = Jwts.builder()
                .subject("1")
                .claim("username", "hacker")
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(keyBytes))
                .compact();

        assertNull(authService.validateToken(fakeToken));
    }

    @Test
    void validateToken_returnsNullForAlteredToken() throws AuthenticationException {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getName()).thenReturn("testuser");
        when(mockUser.getPassword()).thenReturn(authService.hashPassword("pass"));
        when(mockUser.getRole()).thenReturn("STUDENT");

        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(() -> User.findFirst("name = ?", "testuser"))
                    .thenReturn(mockUser);

            String token = authService.login("testuser", "pass");
            // Alterar el payload del token
            String[] parts = token.split("\\.");
            String tamperedToken = parts[0] + ".tamperedPayload." + parts[2];

            assertNull(authService.validateToken(tamperedToken));
        }
    }
}
