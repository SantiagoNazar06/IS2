package com.is1.proyecto.services;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.repositories.TeacherRepository;

import java.util.Collections;
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

    // AC-5: verifica si un docente está asignado a una materia
    public boolean verifyAssignment(Long teacherId, Long subjectId) {
        return teacherRepository.existsAssignment(teacherId, subjectId);
    }

    // AC-1 (lista): todos los docentes con datos personales
    public List<Map<String, Object>> getAllTeachers() {
        return teacherRepository.findAllWithPersons();
    }

    // AC-6: datos combinados de docente y persona
    public Map<String, Object> getTeacherWithPerson(Long teacherId) {
        return teacherRepository.findWithPerson(teacherId);
    }

    // AC-1 (detalle): materias asignadas con rol, período y cantidad de alumnos evaluados
    public List<Map<String, Object>> getAssignedSubjects(Long teacherId) {
        return teacherRepository.findAssignedSubjectsWithCount(teacherId);
    }

    // AC-2 / AC-3: alumnos de una materia con notas (solo si el docente está asignado)
    public List<Map<String, Object>> getSubjectStudents(Long teacherId, Long subjectId) {
        if (!verifyAssignment(teacherId, subjectId)) return Collections.emptyList();
        return teacherRepository.findSubjectStudents(subjectId);
    }

    // AC-4: actas de calificaciones de una materia
    public List<Map<String, Object>> getGrades(Long teacherId, Long subjectId) {
        return getSubjectStudents(teacherId, subjectId);
    }
}
