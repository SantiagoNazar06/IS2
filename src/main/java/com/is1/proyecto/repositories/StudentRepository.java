package com.is1.proyecto.repositories;

import com.is1.proyecto.dto.EnrollmentDTO;
import com.is1.proyecto.dto.GradeDTO;
import com.is1.proyecto.dto.StudentListDTO;
import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.models.Enrollment;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.ports.out.StudentRepositoryInterface;
import org.javalite.activejdbc.Base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Repositorio de consultas sobre Student usando ActiveJDBC.
// Consultas multi-tabla usan Base.findAll() con SQL directo porque
// ActiveJDBC no puede cruzar modelos con @IdName divergentes.
// La conexion a la BD es manejada por DBConnectionFilter a nivel de request HTTP.
public class StudentRepository implements StudentRepositoryInterface {

    // ========================================================================
    // findByDni — two-step: Person → Student
    // ========================================================================
    @Override
    public Student findByDni(String dni) {
        if (dni == null) {
            return null;
        }
        Person person = Person.findFirst("dni = ?", dni);
        if (person == null) {
            return null;
        }
        return Student.findFirst("id_person = ?", person.getId());
    }

    // ========================================================================
    // findByPersonId
    // ========================================================================
    @Override
    public Student findByPersonId(Long personId) {
        if (personId == null) {
            return null;
        }
        return Student.findFirst("id_person = ?", personId);
    }

    // ========================================================================
    // getAcademicHistory — 3-table JOIN: enrollments → subjects → evaluations
    // ========================================================================
    @Override
    public List<SubjectDTO> getAcademicHistory(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT s.id_subject, s.subject_name, e.status, ev.grade, e.period "
                + "FROM enrollments e "
                + "JOIN subjects s ON e.subject_id = s.id_subject "
                + "LEFT JOIN evaluations ev ON ev.enrollment_id = e.id "
                + "WHERE e.student_id = ? "
                + "ORDER BY e.period DESC";

        List<Map> rows = Base.findAll(sql, studentId);
        return mapToSubjectDTOList(rows);
    }

    // ========================================================================
    // getApprovedSubjects — 3-table JOIN + grade >= 4.0 filter
    // ========================================================================
    @Override
    public List<SubjectDTO> getApprovedSubjects(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT s.id_subject, s.subject_name, e.status, ev.grade, e.period "
                + "FROM enrollments e "
                + "JOIN subjects s ON e.subject_id = s.id_subject "
                + "JOIN evaluations ev ON ev.enrollment_id = e.id "
                + "WHERE e.student_id = ? AND ev.grade >= 4.0 "
                + "ORDER BY e.period DESC";

        List<Map> rows = Base.findAll(sql, studentId);
        return mapToSubjectDTOList(rows);
    }

    // ========================================================================
    // getCurrentEnrollments — ActiveJDBC where() + status filter
    // ========================================================================
    @Override
    public List<EnrollmentDTO> getCurrentEnrollments(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }
        List<Enrollment> enrollments = Enrollment.where(
                "student_id = ? AND status = 'ENROLLED'", studentId);

        List<EnrollmentDTO> result = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            result.add(new EnrollmentDTO(
                    enrollment.getSubjectId(),
                    enrollment.getStatus(),
                    null,
                    enrollment.getPeriod()
            ));
        }
        return result;
    }

    // ========================================================================
    // getGrades — 3-table JOIN: evaluations → enrollments → subjects
    // ========================================================================
    @Override
    public List<GradeDTO> getGrades(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT s.subject_name, ev.grade, ev.evaluation_date "
                + "FROM evaluations ev "
                + "JOIN enrollments e ON ev.enrollment_id = e.id "
                + "JOIN subjects s ON e.subject_id = s.id_subject "
                + "WHERE e.student_id = ? "
                + "ORDER BY ev.evaluation_date DESC";

        List<Map> rows = Base.findAll(sql, studentId);
        List<GradeDTO> result = new ArrayList<>();
        for (Map row : rows) {
            String subjectName = (String) row.get("subject_name");
            Double grade = toDouble(row.get("grade"));
            String evaluationDate = row.get("evaluation_date") != null
                    ? row.get("evaluation_date").toString() : null;
            result.add(new GradeDTO(subjectName, grade, evaluationDate));
        }
        return result;
    }

    // ========================================================================
    // findStudents — 5-table JOIN with GROUP_CONCAT and dynamic filters
    // ========================================================================
    @Override
    public List<StudentListDTO> findStudents(Long careerId, Long subjectId, Long teacherId, boolean isAdmin) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT s.id AS student_id, ")
                .append("p.firstName || ' ' || p.lastName AS full_name, ")
                .append("p.dni, p.email, ")
                .append("GROUP_CONCAT(DISTINCT c.career_name) AS careers ")
                .append("FROM students s ")
                .append("JOIN persons p ON s.id_person = p.id ")
                .append("LEFT JOIN enrollments e ON e.student_id = s.id ")
                .append("LEFT JOIN subjects sub ON e.subject_id = sub.id_subject ")
                .append("LEFT JOIN study_plans sp ON sub.id_study_plan = sp.id_study_plan ")
                .append("LEFT JOIN careers c ON sp.id_career = c.id_careers ");

        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (!isAdmin && teacherId != null) {
            sql.append("JOIN teacher_subject ts ON ts.subject_id = sub.id_subject AND ts.teacher_id = ? ");
            params.add(teacherId);
        }

        if (careerId != null) {
            conditions.add("c.id_careers = ?");
            params.add(careerId);
        }

        if (subjectId != null) {
            conditions.add("sub.id_subject = ?");
            params.add(subjectId);
        }

        if (!isAdmin && teacherId != null) {
            conditions.add("ts.teacher_id = ?");
            params.add(teacherId);
        }

        if (!conditions.isEmpty()) {
            sql.append("WHERE ");
            sql.append(String.join(" AND ", conditions));
            sql.append(" ");
        }

        sql.append("GROUP BY s.id, p.firstName, p.lastName, p.dni, p.email ");
        sql.append("ORDER BY p.lastName, p.firstName");

        List<Map> rows = Base.findAll(sql.toString(), params.toArray());
        List<StudentListDTO> result = new ArrayList<>();
        for (Map row : rows) {
            result.add(new StudentListDTO(row));
        }
        return result;
    }

    private List<SubjectDTO> mapToSubjectDTOList(List<Map> rows) {
        List<SubjectDTO> result = new ArrayList<>();
        for (Map row : rows) {
            Long subjectId = row.get("id_subject") != null
                    ? ((Number) row.get("id_subject")).longValue() : null;
            String subjectName = (String) row.get("subject_name");
            String status = (String) row.get("status");
            Double grade = toDouble(row.get("grade"));
            String period = (String) row.get("period");

            result.add(new SubjectDTO(
                    subjectId,
                    subjectName,
                    status,
                    grade,
                    period,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }
        return result;
    }

    /**
     * Safely converts a column value to Double.
     * Handles nulls and numeric types from SQLite (BigDecimal, Double, etc.).
     */
    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}
