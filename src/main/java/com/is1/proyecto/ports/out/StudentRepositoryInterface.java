package com.is1.proyecto.ports.out;

import com.is1.proyecto.dto.EnrollmentDTO;
import com.is1.proyecto.dto.GradeDTO;
import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.models.Student;

import java.util.List;

public interface StudentRepositoryInterface {

    Student findByDni(String dni);

    Student findByPersonId(Long personId);

    List<SubjectDTO> getAcademicHistory(Long studentId);

    List<SubjectDTO> getApprovedSubjects(Long studentId);

    List<EnrollmentDTO> getCurrentEnrollments(Long studentId);

    List<GradeDTO> getGrades(Long studentId);
}
