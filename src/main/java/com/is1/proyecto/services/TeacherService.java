package com.is1.proyecto.services;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.TeacherAssignment;
import com.is1.proyecto.models.TeacherRole;
import com.is1.proyecto.repositories.TeacherRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para operaciones relacionadas con profesores.
 */
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    /**
     * Resultado de registro de profesor.
     */
    public static class TeacherRegisterResult {
        public final boolean success;
        public final int statusCode;
        public final String redirectUrl;
        public final String message;

        private TeacherRegisterResult(boolean success, int statusCode, String redirectUrl, String message) {
            this.success = success;
            this.statusCode = statusCode;
            this.redirectUrl = redirectUrl;
            this.message = message;
        }

        public static TeacherRegisterResult ok(String message) {
            return new TeacherRegisterResult(true, 201, "/register_teacher?message=" + message, null);
        }

        public static TeacherRegisterResult error(String message) {
            return new TeacherRegisterResult(false, 500, "/register_teacher?error=" + message, null);
        }

        public static TeacherRegisterResult duplicate(String dni) {
            return new TeacherRegisterResult(false, 400, "/register_teacher?error=Ya existe un profesor con el DNI " + dni, null);
        }

        public static TeacherRegisterResult okEdit(Long id, String message) {
            return new TeacherRegisterResult(true, 200, "/register_teacher?edit=" + id + "&message=" + message, null);
        }

        public static TeacherRegisterResult errorEdit(Long id, String message) {
            return new TeacherRegisterResult(false, 400, "/register_teacher?edit=" + id + "&error=" + message, null);
        }

        public static TeacherRegisterResult okList(String message) {
            return new TeacherRegisterResult(true, 200, "/teachers?message=" + message, null);
        }

        public static TeacherRegisterResult errorList(String message) {
            return new TeacherRegisterResult(false, 400, "/teachers?error=" + message, null);
        }

        public static TeacherRegisterResult notFound() {
            return new TeacherRegisterResult(false, 404, null, null);
        }
    }

    /**
     * Datos para registrar un profesor.
     */
    public static class TeacherData {
        public final String dni;
        public final String nroLegajo;
        public final String firstName;
        public final String lastName;
        public final String phone;
        public final String email;

        public TeacherData(String dni, String nroLegajo, String firstName, String lastName, String phone, String email) {
            this.dni = dni;
            this.nroLegajo = nroLegajo;
            this.firstName = firstName;
            this.lastName = lastName;
            this.phone = phone;
            this.email = email;
        }
    }

    /**
     * Registra un nuevo profesor en la base de datos.
     */
    public TeacherRegisterResult registerTeacher(TeacherData data) {
        try {
            Person ps = Person.findFirst("dni = ?", data.dni);

            if (ps == null) {
                ps = new Person();
                ps.set("dni", data.dni);
                ps.set("firstName", data.firstName);
                ps.set("lastName", data.lastName);
                ps.set("phone", data.phone);
                ps.set("email", data.email);
                ps.saveIt();
            } else {
                ps.set("firstName", data.firstName);
                ps.set("lastName", data.lastName);
                ps.set("phone", data.phone);
                ps.set("email", data.email);
                ps.saveIt();
            }

            Teacher existingTeacher = Teacher.findFirst("id_persona = ?", ps.getId());
            if (existingTeacher != null) {
                return TeacherRegisterResult.duplicate(data.dni);
            }

            Teacher th = new Teacher();
            th.set("id_persona", ps.getId());
            th.set("nroLegajo", data.nroLegajo);
            th.saveIt();

            return TeacherRegisterResult.ok("Profesor registrado exitosamente para " + data.firstName + "!");

        } catch (Exception e) {
            System.err.println("Error al registrar el profesor: " + e.getMessage());
            e.printStackTrace();
            return TeacherRegisterResult.error("Error interno al crear la cuenta. Intente de nuevo.");
        }
    }

    public boolean verifyAssignment(Long teacherId, Long subjectId) {
        return teacherRepository.existsAssignment(teacherId, subjectId);
    }

    public List<Map<String, Object>> getAllTeachers() {
        return teacherRepository.findAllWithPersons();
    }

    public Map<String, Object> getTeacherWithPerson(Long teacherId) {
        return teacherRepository.findWithPerson(teacherId);
    }

    public List<Map<String, Object>> getAssignedSubjects(Long teacherId) {
        return teacherRepository.findAssignedSubjectsWithCount(teacherId);
    }

    public List<Map<String, Object>> getSubjectStudents(Long teacherId, Long subjectId) {
        if (!verifyAssignment(teacherId, subjectId)) return Collections.emptyList();
        return teacherRepository.findSubjectStudents(subjectId);
    }

    public List<Map<String, Object>> getGrades(Long teacherId, Long subjectId) {
        return getSubjectStudents(teacherId, subjectId);
    }

    public List<Map<String, Object>> getAllAssignments() {
        List<Map<String, Object>> assignments = teacherRepository.findAllAssignmentsWithDetails();
        for (Map<String, Object> a : assignments) {
            String role = (String) a.get("role");
            a.put("isResponsable", "RESPONSABLE".equals(role));
            a.put("isJtp", "JTP".equals(role));
            a.put("isAyudante", "AYUDANTE".equals(role));
        }
        return assignments;
    }

    public TeacherAssignment createAssignment(Long teacherId, Long subjectId, String period, String role) {
        TeacherAssignment ta = new TeacherAssignment();
        ta.setTeacherId(teacherId);
        ta.setSubjectId(subjectId);
        ta.setPeriod(period);
        ta.setRole(TeacherRole.fromString(role));
        ta.saveIt();
        return ta;
    }

    public boolean deleteAssignment(Long id) {
        TeacherAssignment ta = TeacherAssignment.findById(id);
        if (ta == null) return false;
        ta.delete();
        return true;
    }

    public List<Map<String, Object>> getAllSubjectsSimple() {
        List<Subject> subjects = Subject.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Subject s : subjects) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("name", s.getSubjectName());
            item.put("code", s.getCode());
            result.add(item);
        }
        return result;
    }

    public TeacherRegisterResult updateTeacher(Long id, TeacherData data) {
        try {
            Teacher teacher = Teacher.findById(id);
            if (teacher == null) {
                return TeacherRegisterResult.notFound();
            }

            Person person = teacher.getPerson();
            if (person == null) {
                return TeacherRegisterResult.errorEdit(id, "El profesor no tiene persona asociada.");
            }

            person.setDni(data.dni);
            person.setFirstName(data.firstName);
            person.setLastName(data.lastName);
            person.setPhone(data.phone);
            person.setEmail(data.email);
            person.saveIt();

            teacher.setNroLegajo(data.nroLegajo);
            teacher.saveIt();

            return TeacherRegisterResult.okEdit(id, "Profesor actualizado exitosamente.");

        } catch (Exception e) {
            System.err.println("Error al actualizar el profesor: " + e.getMessage());
            e.printStackTrace();
            return TeacherRegisterResult.errorEdit(id, "Error interno al actualizar el profesor.");
        }
    }

    public TeacherRegisterResult deleteTeacher(Long id) {
        try {
            Teacher teacher = Teacher.findById(id);
            if (teacher == null) {
                return TeacherRegisterResult.notFound();
            }

            Person person = teacher.getPerson();
            teacher.delete();
            if (person != null) {
                person.delete();
            }

            return TeacherRegisterResult.okList("Profesor eliminado exitosamente.");

        } catch (Exception e) {
            System.err.println("Error al eliminar el profesor: " + e.getMessage());
            e.printStackTrace();
            return TeacherRegisterResult.errorList("Error interno al eliminar el profesor.");
        }
    }

    public List<Map<String, Object>> getAssignedSubjectsSimple(Long teacherId) {
        List<Map<String, Object>> subjects = teacherRepository.getAssignedSubjects(teacherId);
        for (Map<String, Object> s : subjects) {
            String role = (String) s.get("role");
            s.put("isResponsable", "RESPONSABLE".equals(role));
            s.put("isJtp", "JTP".equals(role));
            s.put("isAyudante", "AYUDANTE".equals(role));
        }
        return subjects;
    }
}
