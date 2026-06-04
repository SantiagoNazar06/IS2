package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Teacher;
import org.javalite.activejdbc.Base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherRepository {

    // AC-1: buscar docente por nroLegajo
    public Teacher findByLegajo(String legajo) {
        return Teacher.findFirst("nroLegajo = ?", legajo);
    }

    // AC-2: buscar docente por id de persona
    public Teacher findByPersonId(Long personId) {
        return Teacher.findFirst("id_persona = ?", personId);
    }

    // AC-3: docentes asignados a una materia (con su rol y período)
    public List<Map<String, Object>> findBySubject(Long subjectId) {
        String sql =
            "SELECT t.id AS teacherId, t.nroLegajo AS legajo, t.id_persona AS personId, " +
            "       ta.role AS role, ta.period AS period " +
            "FROM teachers t " +
            "JOIN teacher_assignments ta ON ta.teacher_id = t.id " +
            "WHERE ta.subject_id = ?";
        List<Map> rows = Base.findAll(sql, subjectId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("teacherId", row.get("teacherId"));
            item.put("legajo",    row.get("legajo"));
            item.put("personId",  row.get("personId"));
            item.put("role",      row.get("role"));
            item.put("period",    row.get("period"));
            result.add(item);
        }
        return result;
    }

    // AC-4: docentes activos en un período
    public List<Teacher> findByPeriod(String period) {
        String sql =
            "SELECT DISTINCT t.* FROM teachers t " +
            "JOIN teacher_assignments ta ON ta.teacher_id = t.id " +
            "WHERE ta.period = ?";
        return Teacher.findBySQL(sql, period).load();
    }

    public boolean existsAssignment(Long teacherId, Long subjectId) {
        String sql = "SELECT id FROM teacher_assignments WHERE teacher_id = ? AND subject_id = ? LIMIT 1";
        return Base.firstCell(sql, teacherId, subjectId) != null;
    }

    public List<Map<String, Object>> findAllWithPersons() {
        String sql =
            "SELECT t.id AS teacherId, t.nroLegajo AS legajo, " +
            "       p.firstName, p.lastName, p.dni, p.email, p.phone " +
            "FROM teachers t JOIN persons p ON p.id = t.id_persona";
        List<Map> rows = Base.findAll(sql);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            result.add(new HashMap<>(row));
        }
        return result;
    }

    public Map<String, Object> findWithPerson(Long teacherId) {
        String sql =
            "SELECT t.id AS teacherId, t.nroLegajo AS legajo, " +
            "       p.firstName, p.lastName, p.dni, p.email, p.phone " +
            "FROM teachers t JOIN persons p ON p.id = t.id_persona " +
            "WHERE t.id = ?";
        List<Map> rows = Base.findAll(sql, teacherId);
        return rows.isEmpty() ? null : new HashMap<>(rows.get(0));
    }

    public List<Map<String, Object>> findAssignedSubjectsWithCount(Long teacherId) {
        String sql =
            "SELECT s.id_subject AS subjectId, s.subject_name AS subjectName, " +
            "       ta.role, ta.period, " +
            "       COUNT(e.id_evaluations) AS studentCount " +
            "FROM teacher_assignments ta " +
            "JOIN subjects s ON s.id_subject = ta.subject_id " +
            "LEFT JOIN evaluations e ON e.subject_id = ta.subject_id " +
            "WHERE ta.teacher_id = ? " +
            "GROUP BY ta.subject_id, ta.role, ta.period";
        List<Map> rows = Base.findAll(sql, teacherId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            result.add(new HashMap<>(row));
        }
        return result;
    }

    public List<Map<String, Object>> findSubjectStudents(Long subjectId) {
        String sql =
            "SELECT st.id AS studentId, st.student_type AS studentType, " +
            "       p.firstName, p.lastName, p.dni, p.email, " +
            "       e.evaluation_note AS grade, e.evaluation_date AS gradeDate " +
            "FROM evaluations e " +
            "JOIN students st ON st.id = e.student_id " +
            "JOIN persons p ON p.id = st.id_person " +
            "WHERE e.subject_id = ?";
        List<Map> rows = Base.findAll(sql, subjectId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            result.add(new HashMap<>(row));
        }
        return result;
    }

    // AC-5: materias asignadas a un docente con su rol y período
    public List<Map<String, Object>> getAssignedSubjects(Long teacherId) {
        String sql =
            "SELECT s.id_subject AS subjectId, s.subject_name AS subjectName, " +
            "       ta.role AS role, ta.period AS period " +
            "FROM teacher_assignments ta " +
            "JOIN subjects s ON s.id_subject = ta.subject_id " +
            "WHERE ta.teacher_id = ?";
        List<Map> rows = Base.findAll(sql, teacherId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("subjectId",   row.get("subjectId"));
            item.put("subjectName", row.get("subjectName"));
            item.put("role",        row.get("role"));
            item.put("period",      row.get("period"));
            result.add(item);
        }
        return result;
    }
}
