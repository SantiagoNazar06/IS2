package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("enrollments")
@IdName("id")
public class Enrollment extends Model {

    public Long getId() {
        return getLong("id");
    }

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

    public EnrollmentStatus getStatusEnum() {
        return EnrollmentStatus.fromString(getString("status"));
    }

    public void setStatusEnum(EnrollmentStatus status) {
        if (status == null) {
            set("status", null);
        } else {
            set("status", status.name());
        }
    }

    public String getEnrollmentDate() {
        return getString("created_at");
    }

    public String getCreatedAt() {
        return getString("created_at");
    }
}
