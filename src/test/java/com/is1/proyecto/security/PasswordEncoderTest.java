package com.is1.proyecto.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class PasswordEncoderTest {
    private final PasswordEncoder encoder = new PasswordEncoder();

    @Test
    void encode_generatesDifferentHashesForSamePassword() {
        // AC-2: mismo input → outputs diferentes
        String hash1 = encoder.encode("mypassword");
        String hash2 = encoder.encode("mypassword");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void verify_returnsTrueForCorrectPassword() {
        // AC-3: verify con password correcto → true
        String hash = encoder.encode("mypassword");
        assertTrue(encoder.verify("mypassword", hash));
    }

    @Test
    void verify_returnsFalseForIncorrectPassword() {
        // AC-4: verify con password incorrecto → false
        String hash = encoder.encode("mypassword");
        assertFalse(encoder.verify("wrongpassword", hash));
    }

    @Test
    void hashDoesNotContainOriginalPassword() {
        // Seguridad: el hash no debe contener la password original
        String hash = encoder.encode("mypassword");
        assertFalse(hash.contains("mypassword"));
    }

    @Test
    void encode_nullPassword_throwsException() {
        // Null no es una contraseña válida → debe lanzar excepción
        assertThrows(IllegalArgumentException.class, () -> {
            encoder.encode(null);
        });
        // O también podría ser NullPointerException de BCrypt —
        // corré primero a ver qué tira y ajustás el assert.
    }

    @Test
    void verify_nullHash_returnsFalse() {
        // Si el hash almacenado es null (ej: usuario sin password), verify no debe explotar
        assertFalse(encoder.verify("password", null));
    }

    @Test
    void verify_nullPassword_returnsFalse() {
        String hash = encoder.encode("password");
        assertFalse(encoder.verify(null, hash));
    }

    @Test
    void encodeAndVerify_emptyPassword() {
        // Contraseña vacía también debe ser hasheable y verificable
        String hash = encoder.encode("");
        assertTrue(encoder.verify("", hash));
    }

    @Test
    void encodeAndVerify_specialCharacters() {
        // Caracteres especiales, tildes, ñ, emojis
        String password = "Ñoño#15!*+{}[]|\\\"'¿?<>€😎";
        String hash = encoder.encode(password);
        assertTrue(encoder.verify(password, hash));
    }

    @Test
    void encodeAndVerify_whitespacePassword() {
        // Contraseña que son solo espacios
        String password = "   ";
        String hash = encoder.encode(password);
        assertTrue(encoder.verify(password, hash));
    }

    @Test
    void encodeAndVerify_veryLongPassword() {
        // BCrypt trunca a 72 bytes — pero sigue siendo consistente
        String longPassword = "a".repeat(200);
        String hash = encoder.encode(longPassword);
        assertTrue(encoder.verify(longPassword, hash));
    }

    @Test
    void verify_withMalformedHash_returnsFalse() {
        // Si alguien pasa un string que no es un hash BCrypt válido
        assertFalse(encoder.verify("password", "esto-no-es-un-hash"));
    }

    @Test
    void verify_caseSensitive() {
        // BCrypt es case-sensitive
        String hash = encoder.encode("Password");
        assertFalse(encoder.verify("password", hash)); // minúscula ≠ mayúscula
    }

    @Test
    void encodeAndVerify_multipleDifferentPasswords() {
        // Varias contraseñas diferentes, todas deben verificar correctamente
        String[] passwords = {"admin", "123456", "P@$$w0rd!", "una-frase-larga-con-separadores-y-numeros-2024"};
        for (String pwd : passwords) {
            String hash = encoder.encode(pwd);
            assertTrue(encoder.verify(pwd, hash));
        }
    }

    @Test
    void encode_returnsBcryptFormatHash() {
        // Un hash BCrypt válido empieza con $2a$, $2b$ o $2y$ y tiene 60 caracteres
        String hash = encoder.encode("anything");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"),
                "Hash debe tener prefijo BCrypt válido");
        assertEquals(60, hash.length(), "Hash BCrypt debe tener 60 caracteres");
    }

    @Test
    void encode_doesNotReturnPlainText() {
        // AC-1 (indirecto): el hash NO se parece a la contraseña original
        String password = "MiCoNtRaSeÑaSeCrEtA";
        String hash = encoder.encode(password);
        assertNotEquals(password, hash);
        assertFalse(hash.toLowerCase().contains("contraseña"));
        assertFalse(hash.contains(password));
    }

    @Test
    void verify_samePasswordWithDifferentSalts() {
        // Dados dos hashes del mismo password, verify debe funcionar con ambos
        String password = "test";
        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);
        assertNotEquals(hash1, hash2); // distintos salts
        assertTrue(encoder.verify(password, hash1)); // el primero funciona
        assertTrue(encoder.verify(password, hash2)); // el segundo también
    }
}
