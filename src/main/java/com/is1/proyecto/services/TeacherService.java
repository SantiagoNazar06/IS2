package com.is1.proyecto.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.is1.proyecto.dto.StudentWithGradeDTO;
import com.is1.proyecto.models.Enrollment;
import com.is1.proyecto.models.Evaluation;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.TeacherSubject;

/**
 * Servicio para operaciones relacionadas con profesores.
 */
public class TeacherService {

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
     * 
     * @param data Datos del profesor
     * @return TeacherRegisterResult con el resultado de la operación
     */
    public TeacherRegisterResult registerTeacher(TeacherData data) {
        try {
            // Buscar si ya existe una persona con el mismo DNI
            Person ps = Person.findFirst("dni = ?", data.dni);

            if (ps == null) {
                // Si no existe, la creamos
                ps = new Person();
                ps.set("dni", data.dni);
                ps.set("firstName", data.firstName);
                ps.set("lastName", data.lastName);
                ps.set("phone", data.phone);
                ps.set("email", data.email);
                ps.saveIt();
            } else {
                // Si ya existe, actualizamos por si cambió algún dato
                ps.set("firstName", data.firstName);
                ps.set("lastName", data.lastName);
                ps.set("phone", data.phone);
                ps.set("email", data.email);
                ps.saveIt();
            }
            
            // Verificar si ya existe un profesor asociado a esa persona
            Teacher existingTeacher = Teacher.findFirst("id_persona = ?", ps.getId());

            if (existingTeacher != null) {
                return TeacherRegisterResult.duplicate(data.dni);
            }

            // Crear nuevo registro de profesor usando la persona existente
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

    /**
     * Obtiene el listado de alumnos inscriptos en una materia,
     * verificando que el docente esté asignado a ella.
     *
     * @param teacherId ID del docente
     * @param subjectId ID de la materia
     * @return Lista de StudentWithGradeDTO con datos y notas
     * @throws IllegalArgumentException si el teacher no existe,
     *         no está asignado a la materia, o la materia no existe
     */
    public List<StudentWithGradeDTO> getStudentsBySubject(Integer teacherId, Integer subjectId){
        Teacher teacher = Teacher.findById(teacherId);
        if(teacher == null){
            throw new IllegalArgumentException("Teacher not found");
        }

        TeacherSubject assignment = TeacherSubject.findFirst("teacher_id = ? AND subject_id = ?", teacherId, subjectId);
        if(assignment == null){
            throw new IllegalArgumentException("Teacher not assigned to this subject");
        }

        Subject subject = Subject.findById(subjectId);
        if (subject == null) {
            throw new IllegalArgumentException("Subject not found");
        }

        List<Enrollment> enrollments = Enrollment.where(
            "subject_id = ? AND status = ?", subjectId, "ENROLLED"
        );

        List<StudentWithGradeDTO> students = new ArrayList<>();
        
        for (Enrollment enrollment : enrollments) {
            Student student = Student.findById(enrollment.getStudentId());
            Person person = student.getPerson();
            String fullName = person.getFirstName() + " " + person.getLastName();

            Evaluation eval = Evaluation.findFirst("enrollment_id = ?", enrollment.getId());
            
            Double grade = null;
            String gradeDate = null;
            
            if (eval != null) {
                grade = eval.getEvaluationGrade();
                Date date = eval.getEvaluationDate();
                gradeDate = date != null ? date.toString() : null;
            }

            students.add(new StudentWithGradeDTO(
                ((Number) student.getId()).longValue(),
                fullName,
                enrollment.getCreatedAt(),
                grade,
                gradeDate));
        }

        return students;
    }
}
