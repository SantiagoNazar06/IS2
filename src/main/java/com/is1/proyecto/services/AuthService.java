package com.is1.proyecto.services;

import com.is1.proyecto.models.User;
import com.is1.proyecto.security.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de autenticación y gestión de sesiones con JWT.
 * <p>
 * Responsabilidades:
 * - Verificar username/contraseña contra la tabla users
 * - Retornar token JWT tras login exitoso (con claims: userId, username, role)
 * - Invalidar token en logout (blacklist en memoria)
 * - Hash de contraseñas usando AES-256/GCM (cifrado simétrico)
 * - Validar tokens JWT
 */
public class AuthService {

    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static final String ENV_AES_KEY = "AES_KEY";
    private static final String ENV_JWT_SECRET = "JWT_SECRET";
    private static final String FALLBACK_AES_KEY = "dev-aes-key-fallback-32bytes!!";
    private static final String FALLBACK_JWT_SECRET = "dev-jwt-secret-fallback-32bytes!!";

    private final SecretKey aesKey;
    private final SecretKey jwtSecretKey;
    private final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Inicializa el AuthService derivando las claves desde variables de entorno.
     * Si no están definidas, usa fallbacks para desarrollo.
     */
    public AuthService() {
        this.aesKey = deriveAESKey(getEnvOrFallback(ENV_AES_KEY, FALLBACK_AES_KEY));
        this.jwtSecretKey = deriveJWTSecret(getEnvOrFallback(ENV_JWT_SECRET, FALLBACK_JWT_SECRET));
    }

    /**
     * Constructor con claves explícitas (útil para testing).
     */
    public AuthService(String aesKeyValue, String jwtSecretValue) {
        this.aesKey = deriveAESKey(aesKeyValue);
        this.jwtSecretKey = deriveJWTSecret(jwtSecretValue);
    }

    // ==================== Login / Logout / Validate ====================

    /**
     * Autentica un usuario con username y contraseña.
     *
     * @param username Nombre de usuario
     * @param password Contraseña en texto plano
     * @return Token JWT
     * @throws AuthenticationException si las credenciales son inválidas
     */
    public String login(String username, String password) throws AuthenticationException {
        // Validaciones básicas
        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("El nombre de usuario es requerido.");
        }
        if (password == null || password.isEmpty()) {
            throw new AuthenticationException("La contraseña es requerida.");
        }

        // Buscar usuario por nombre
        User user = User.findFirst("name = ?", username.trim());
        if (user == null) {
            throw new AuthenticationException("Usuario o contraseña incorrectos.");
        }

        // Verificar contraseña contra el hash AES-256 almacenado
        String storedHash = user.getPassword();
        if (storedHash == null || !verifyPassword(password, storedHash)) {
            throw new AuthenticationException("Usuario o contraseña incorrectos.");
        }

        // Obtener rol del usuario (con default STUDENT)
        String roleStr = user.getRole();
        Role role = Role.fromString(roleStr);
        if (role == null) {
            role = Role.STUDENT;
        }

        // Generar y retornar token JWT
        return generateToken(user.getId(), user.getName(), role);
    }

    /**
     * Invalida un token JWT (lo agrega a la blacklist).
     *
     * @param token Token a invalidar
     */
    public void logout(String token) {
        if (token != null && !token.trim().isEmpty()) {
            invalidatedTokens.add(token.trim());
        }
    }

    /**
     * Valida un token JWT y retorna los claims del usuario.
     *
     * @param token Token JWT
     * @return UserClaims si el token es válido, null en caso contrario
     */
    public UserClaims validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        // Verificar si el token fue invalidado (logout)
        if (invalidatedTokens.contains(token.trim())) {
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();

            Object userId = claims.get("userId");
            String username = claims.get("username", String.class);
            String roleStr = claims.get("role", String.class);

            if (userId == null || username == null || roleStr == null) {
                return null;
            }

            Role role = Role.fromString(roleStr);
            if (role == null) {
                return null;
            }

            return new UserClaims(userId, username, role);

        } catch (ExpiredJwtException e) {
            return null; // Token expirado
        } catch (JwtException e) {
            return null; // Token inválido (firma, malformado, etc.)
        }
    }

    // ==================== Password Hashing (AES-256/GCM) ====================

    /**
     * Hashea (cifra) una contraseña usando AES-256/GCM.
     * Retorna Base64(IV + ciphertext).
     *
     * @param plain Contraseña en texto plano
     * @return Hash AES-256 en formato Base64
     */
    public String hashPassword(String plain) {
        if (plain == null || plain.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);

            byte[] plainBytes = plain.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plainBytes);

            // IV + ciphertext
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar la contraseña.", e);
        }
    }

    /**
     * Verifica una contraseña contra un hash AES-256/GCM almacenado.
     *
     * @param plain  Contraseña en texto plano
     * @param stored Hash almacenado en Base64(IV + ciphertext)
     * @return true si coincide, false en caso contrario
     */
    public boolean verifyPassword(String plain, String stored) {
        if (plain == null || stored == null) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(stored);

            // Extraer IV (primeros 12 bytes)
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            // Resto es ciphertext
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            String decryptedPassword = new String(decrypted, StandardCharsets.UTF_8);

            return decryptedPassword.equals(plain);

        } catch (Exception e) {
            return false; // Cualquier error en descifrado = no coincide
        }
    }

    // ==================== JWT Token Generation ====================

    /**
     * Genera un token JWT con los claims del usuario.
     *
     * @param userId   ID del usuario
     * @param username Nombre de usuario
     * @param role     Rol del usuario
     * @return Token JWT
     */
    private String generateToken(Object userId, String username, Role role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + (long) TOKEN_EXPIRATION_HOURS * 3600 * 1000);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(jwtSecretKey)
                .compact();
    }

    // ==================== Key Derivation ====================

    private SecretKey deriveAESKey(String secret) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible.", e);
        }
    }

    private SecretKey deriveJWTSecret(String secret) {
        // HMAC-SHA requiere al menos 256 bits; derivamos con SHA-256
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible.", e);
        }
    }

    private String getEnvOrFallback(String envKey, String fallback) {
        String value = System.getenv(envKey);
        return (value != null && !value.isEmpty()) ? value : fallback;
    }
}
