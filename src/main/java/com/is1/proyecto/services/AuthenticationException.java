package com.is1.proyecto.services;

/**
 * Excepción lanzada cuando falla la autenticación.
 * Cubre credenciales inválidas, usuario inexistente, etc.
 */
public class AuthenticationException extends Exception {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
