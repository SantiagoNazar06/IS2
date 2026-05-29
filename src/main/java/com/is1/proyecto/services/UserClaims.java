package com.is1.proyecto.services;

import com.is1.proyecto.security.Role;

/**
 * Representa los claims extraídos de un token JWT.
 * Contiene la información del usuario autenticado.
 */
public class UserClaims {

    private final Object userId;
    private final String username;
    private final Role role;

    public UserClaims(Object userId, String username, Role role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Object getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }
}
