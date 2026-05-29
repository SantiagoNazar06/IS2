package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("enrollments")
public class Enrollment extends Model {

    public Long getStudentId() {
        return getLong("student_id");
    }

    public void setStudentId(Long id) {
        set("student_id", id);
    }

    public Long getSubjectId() {
        return getLong("subject_id");
    }

    public void setSubjectId(Long id) {
        set("subject_id", id);
    }

    public String getPeriod() {
        return getString("period");
    }

    public void setPeriod(String period) {
        set("period", period);
    }

    public String getStatus() {
        return getString("status");
    }

    public void setStatus(String status) {
        set("status", status);
    }

    public String getCreatedAt() {
        return getString("created_at");
    }
}
