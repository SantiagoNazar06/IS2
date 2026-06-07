package com.is1.proyecto.models;

public enum EnrollmentStatus {
    ENROLLED,
    DROPPED,
    COMPLETED,
    CANCELLED;

    public static EnrollmentStatus fromString(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return EnrollmentStatus.valueOf(name.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
