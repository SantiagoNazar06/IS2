package com.is1.proyecto.dto;

import java.util.Map;

/**
 * DTO for student list view.
 * Maps from SQL query row with columns: student_id, full_name, dni, email, careers.
 */
public class StudentListDTO {

    private final Long studentId;
    private final String fullName;
    private final String dni;
    private final String email;
    private final String careers;

    public StudentListDTO(Map<String, Object> row) {
        Object rawId = row.get("student_id");
        this.studentId = rawId instanceof Number ? ((Number) rawId).longValue() : null;
        this.fullName = (String) row.get("full_name");
        this.dni = (String) row.get("dni");
        this.email = (String) row.get("email");
        this.careers = (String) row.get("careers");
    }

    public Long getStudentId() { return studentId; }
    public String getFullName() { return fullName; }
    public String getDni() { return dni; }
    public String getEmail() { return email; }
    public String getCareers() { return careers; }
}
