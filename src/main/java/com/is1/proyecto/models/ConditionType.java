package com.is1.proyecto.models;

/**
 * Enum que representa los tipos de condición/correlatividad entre materias.
 * <ul>
 *   <li>{@link #REGULAR}: la materia requisito debe estar cursada (regular)</li>
 *   <li>{@link #APROBADA}: la materia requisito debe estar aprobada</li>
 * </ul>
 */
public enum ConditionType {
    REGULAR,
    APROBADA;

    /**
     * Obtiene un ConditionType a partir de su nombre en string.
     * La comparación es case-insensitive y tolera whitespace.
     *
     * @param name Nombre del tipo
     * @return El ConditionType correspondiente o null si no existe
     */
    public static ConditionType fromString(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return ConditionType.valueOf(name.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
