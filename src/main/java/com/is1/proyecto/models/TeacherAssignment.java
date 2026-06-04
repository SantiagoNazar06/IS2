package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("teacher_assignments")
public class TeacherAssignment extends Model {

    public Long getTeacherId() {
        return getLong("teacher_id");
    }

    public void setTeacherId(Long teacherId) {
        set("teacher_id", teacherId);
    }

    public Long getSubjectId() {
        return getLong("subject_id");
    }

    public void setSubjectId(Long subjectId) {
        set("subject_id", subjectId);
    }

    public String getRole() {
        return getString("role");
    }

    public void setRole(String role) {
        set("role", role);
    }

    public String getPeriod() {
        return getString("period");
    }

    public void setPeriod(String period) {
        set("period", period);
    }
}
