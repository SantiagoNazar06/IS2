package com.is1.proyecto.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Centraliza el hasheo y verificación de contraseñas usando BCrypt.
 * Único punto de contacto con el algoritmo de hasheo en toda la aplicación.
 */
public class PasswordEncoder {
    
    public PasswordEncoder() {}

    /**
     * Hashea una contraseña en texto plano usando BCrypt con salt automático.
     *
     * @param plainPassword Contraseña en texto plano (no debe ser {@code null})
     * @return Hash de la contraseña (incluye el salt embebido)
     * @throws IllegalArgumentException si plainPassword es {@code null}
     */
    public String encode(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("La contraseña no puede ser null");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * Verifica si una contraseña en texto plano coincide con un hash generado por BCrypt.
     *
     * @param plainPassword  Contraseña en texto plano a verificar
     * @param hashedPassword Hash previamente generado con {@link #encode(String)}
     * @return {@code true} si la contraseña coincide, {@code false} en caso contrario.
     *         Retorna {@code false} si cualquiera de los argumentos es {@code null}
     *         o si el hash no tiene un formato válido.
     */
    public boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Si el hash no es un hash BCrypt válido (malformed), no coincide.
            return false;
        }
    }
}
