package com.is1.proyecto.repositories;

import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.models.Teacher;
import org.javalite.activejdbc.Base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherRepository {

    private final DBConfigSingleton db;

    public TeacherRepository() {
        this.db = DBConfigSingleton.getInstance();
    }

    // AC-1: buscar docente por nroLegajo
    public Teacher findByLegajo(String legajo) {
        db.openConnection();
        try {
            return Teacher.findFirst("nroLegajo = ?", legajo);
        } finally {
            db.closeConnection();
        }
    }

    // AC-2: buscar docente por id de persona
    public Teacher findByPersonId(Long personId) {
        db.openConnection();
        try {
            return Teacher.findFirst("id_persona = ?", personId);
        } finally {
            db.closeConnection();
        }
    }

    // AC-3: docentes asignados a una materia (con su rol y período)
    public List<Map<String, Object>> findBySubject(Long subjectId) {
        db.openConnection();
        try {
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
        } finally {
            db.closeConnection();
        }
    }

    // AC-4: docentes activos en un período
    public List<Teacher> findByPeriod(String period) {
        db.openConnection();
        try {
            String sql =
                "SELECT DISTINCT t.* FROM teachers t " +
                "JOIN teacher_assignments ta ON ta.teacher_id = t.id " +
                "WHERE ta.period = ?";
            return Teacher.findBySQL(sql, period).load();
        } finally {
            db.closeConnection();
        }
    }

    // AC-5: materias asignadas a un docente con su rol y período
    public List<Map<String, Object>> getAssignedSubjects(Long teacherId) {
        db.openConnection();
        try {
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
        } finally {
            db.closeConnection();
        }
    }
}
