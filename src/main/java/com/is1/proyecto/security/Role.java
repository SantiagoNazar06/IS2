package com.is1.proyecto.security;

/**
 * Enumeración de roles disponibles en el sistema.
 */
public enum Role {
    ADMIN,
    STUDENT,
    TEACHER;

    /**
     * Obtiene un Role a partir de su nombre en string.
     * 
     * @param name Nombre del rol
     * @return El Role correspondiente o null si no existe
     */
    public static Role fromString(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return Role.valueOf(name.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
