package com.is1.proyecto.repositories;

import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.dto.EnrollmentDTO;
import com.is1.proyecto.dto.GradeDTO;
import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.models.Enrollment;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.ports.out.StudentRepositoryInterface;
import org.javalite.activejdbc.Base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repositorio para operaciones de consulta sobre la entidad Student.
 * <p>
 * Sigue el mismo patrón que PersonRepository (Pattern A):
 * cada método abre conexión via DBConfigSingleton, ejecuta la operación
 * dentro de try/finally, y cierra la conexión en el bloque finally.
 * <p>
 * Consultas simples usan métodos estáticos de ActiveJDBC.
 * Consultas multi-tabla usan Base.findAll() con SQL directo porque
 * ActiveJDBC no puede cruzar modelos con @IdName divergentes.
 */
public class StudentRepository implements StudentRepositoryInterface {

    private DBConfigSingleton db;

    public StudentRepository() {
        this.db = DBConfigSingleton.getInstance();
    }

    // ========================================================================
    // findByDni — two-step: Person → Student
    // ========================================================================

    @Override
    public Student findByDni(String dni) {
        if (dni == null) {
            return null;
        }
        db.openConnection();
        try {
            Person person = Person.findFirst("dni = ?", dni);
            if (person == null) {
                return null;
            }
            return Student.findFirst("id_person = ?", person.getId());
        } finally {
            db.closeConnection();
        }
    }

    // ========================================================================
    // findByPersonId
    // ========================================================================

    @Override
    public Student findByPersonId(Long personId) {
        if (personId == null) {
            return null;
        }
        db.openConnection();
        try {
            return Student.findFirst("id_person = ?", personId);
        } finally {
            db.closeConnection();
        }
    }

    // ========================================================================
    // getAcademicHistory — 3-table JOIN: enrollments → subjects → evaluations
    // ========================================================================

    @Override
    public List<SubjectDTO> getAcademicHistory(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }
        db.openConnection();
        try {
            String sql = "SELECT s.id_subject, s.subject_name, e.status, ev.grade, e.period "
                    + "FROM enrollments e "
                    + "JOIN subjects s ON e.subject_id = s.id_subject "
                    + "LEFT JOIN evaluations ev ON ev.enrollment_id = e.id "
                    + "WHERE e.student_id = ? "
                    + "ORDER BY e.period DESC";

            List<Map> rows = Base.findAll(sql, studentId);
            return mapToSubjectDTOList(rows);
        } finally {
            db.closeConnection();
        }
    }

    // ========================================================================
    // getApprovedSubjects — 3-table JOIN + grade >= 4.0 filter
    // ========================================================================

    @Override
    public List<SubjectDTO> getApprovedSubjects(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }
        db.openConnection();
        try {
            String sql = "SELECT s.id_subject, s.subject_name, e.status, ev.grade, e.period "
                    + "FROM enrollments e "
                    + "JOIN subjects s ON e.subject_id = s.id_subject "
                    + "JOIN evaluations ev ON ev.enrollment_id = e.id "
                    + "WHERE e.student_id = ? AND ev.grade >= 4.0 "
                    + "ORDER BY e.period DESC";

            List<Map> rows = Base.findAll(sql, studentId);
            return mapToSubjectDTOList(rows);
        } finally {
            db.closeConnection();
        }
    }

    // ========================================================================
    // getCurrentEnrollments — ActiveJDBC where() + status filter
    // ========================================================================

    @Override
    public List<EnrollmentDTO> getCurrentEnrollments(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }
        db.openConnection();
        try {
            List<Enrollment> enrollments = Enrollment.where(
                    "student_id = ? AND status = 'ENROLLED'", studentId);

            List<EnrollmentDTO> result = new ArrayList<>();
            for (Enrollment enrollment : enrollments) {
                result.add(new EnrollmentDTO(
                        enrollment.getSubjectId(),
                        enrollment.getStatus(),
                        null, // grade not available without evaluation JOIN
                        enrollment.getPeriod()
                ));
            }
            return result;
        } finally {
            db.closeConnection();
        }
    }

    // ========================================================================
    // getGrades — 3-table JOIN: evaluations → enrollments → subjects
    // ========================================================================

    @Override
    public List<GradeDTO> getGrades(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }
        db.openConnection();
        try {
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
        } finally {
            db.closeConnection();
        }
    }

    // ========================================================================
    // Mapping helpers
    // ========================================================================

    /**
     * Maps a list of Maps (from Base.findAll) to List of SubjectDTO.
     * Handles null grade values (LEFT JOIN produces null when no evaluation exists).
     */
    private List<SubjectDTO> mapToSubjectDTOList(List<Map> rows) {
        List<SubjectDTO> result = new ArrayList<>();
        for (Map row : rows) {
            Long subjectId = row.get("id_subject") != null
                    ? ((Number) row.get("id_subject")).longValue() : null;
            String subjectName = (String) row.get("subject_name");
            String status = (String) row.get("status");
            Double grade = toDouble(row.get("grade"));
            String period = (String) row.get("period");

            result.add(new SubjectDTO(subjectId, subjectName, status, grade, period));
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
