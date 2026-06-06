package com.is1.proyecto.models;

public enum TeacherRole {
    RESPONSABLE,
    JTP,
    AYUDANTE;

    public static TeacherRole fromString(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return TeacherRole.valueOf(name.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
