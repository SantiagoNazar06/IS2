package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Enrollment;

import java.util.List;

public class EnrollmentRepository {

    public Enrollment findById(Integer id) {
        return Enrollment.findFirst("id = ?", id);
    }

    public List<Enrollment> findByStudentAndPeriod(Long studentId, String period) {
        return Enrollment.where("student_id = ? AND period = ?", studentId, period);
    }

    public List<Enrollment> findByStudent(Long studentId) {
        return Enrollment.where("student_id = ?", studentId);
    }

    public List<Enrollment> findBySubject(Long subjectId) {
        return Enrollment.where("subject_id = ?", subjectId);
    }

    public Enrollment create(Enrollment enrollment) {
        enrollment.saveIt();
        return enrollment;
    }
}
