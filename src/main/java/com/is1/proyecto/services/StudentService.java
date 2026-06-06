package com.is1.proyecto.services;

import com.is1.proyecto.dto.StudentListDTO;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.ports.out.StudentRepositoryInterface;

import java.util.List;

/**
 * Servicio para operaciones relacionadas con estudiantes.
 */
public class StudentService {

    private final StudentRepositoryInterface repository;

    public StudentService() {
        this.repository = null;
    }

    public StudentService(StudentRepositoryInterface repository) {
        this.repository = repository;
    }

    /**
     * Obtiene lista de estudiantes con filtros opcionales.
     * ADMIN ve todos los estudiantes; TEACHER ve solo los de sus materias.
     *
     * @param careerId  filtro por carrera (opcional)
     * @param subjectId filtro por materia (opcional)
     * @param teacherId ID del profesor (para scoping TEACHER)
     * @param role      rol del usuario actual
     * @return lista de StudentListDTO
     */
    public List<StudentListDTO> getStudents(Long careerId, Long subjectId, Long teacherId, String role) {
        boolean isAdmin = "ADMIN".equals(role);
        return repository.findStudents(careerId, subjectId, teacherId, isAdmin);
    }

    /**
     * Resultado de registro de estudiante.
     */
    public static class StudentRegisterResult {
        public final boolean success;
        public final int statusCode;
        public final String redirectUrl;
        public final String message;

        private StudentRegisterResult(boolean success, int statusCode, String redirectUrl, String message) {
            this.success = success;
            this.statusCode = statusCode;
            this.redirectUrl = redirectUrl;
            this.message = message;
        }

        public static StudentRegisterResult ok(String message) {
            return new StudentRegisterResult(true, 201, "/register_student?message=" + message, null);
        }

        public static StudentRegisterResult error(String message) {
            return new StudentRegisterResult(false, 500, "/register_student?error=" + message, null);
        }

        public static StudentRegisterResult duplicate(String dni) {
            return new StudentRegisterResult(false, 400, "/register_student?error=Ya existe un estudiante con el DNI " + dni, null);
        }

        public static StudentRegisterResult okEdit(Long id, String message) {
            return new StudentRegisterResult(true, 200, "/register_student?edit=" + id + "&message=" + message, null);
        }

        public static StudentRegisterResult errorEdit(Long id, String message) {
            return new StudentRegisterResult(false, 400, "/register_student?edit=" + id + "&error=" + message, null);
        }

        public static StudentRegisterResult okList(String message) {
            return new StudentRegisterResult(true, 200, "/students?message=" + message, null);
        }

        public static StudentRegisterResult errorList(String message) {
            return new StudentRegisterResult(false, 400, "/students?error=" + message, null);
        }

        public static StudentRegisterResult notFound() {
            return new StudentRegisterResult(false, 404, null, null);
        }
    }

    /**
     * Datos para registrar un estudiante.
     */
    public static class StudentData {
        public final String dni;
        public final String type;
        public final String firstName;
        public final String lastName;
        public final String phone;
        public final String email;

        public StudentData(String dni, String type, String firstName, String lastName, String phone, String email) {
            this.dni = dni;
            this.type = type;
            this.firstName = firstName;
            this.lastName = lastName;
            this.phone = phone;
            this.email = email;
        }
    }

    /**
     * Registra un nuevo estudiante en la base de datos.
     * 
     * @param data Datos del estudiante
     * @return StudentRegisterResult con el resultado de la operación
     */
    public StudentRegisterResult registerStudent(StudentData data) {
        try {
            Person ps = Person.findFirst("dni = ?", data.dni);
            // Creamos una nueva instancia de una persona
            if(ps == null){
                ps = new Person();
                ps.setDni(data.dni);
                ps.setFirstName(data.firstName);
                ps.setLastName(data.lastName);
                ps.setPhone(data.phone);
                ps.setEmail(data.email);
                ps.saveIt();// Guardamos la persona en la tabla personas
            }else{
                ps.setFirstName(data.firstName);
                ps.setLastName(data.lastName);
                ps.setPhone(data.phone);
                ps.setEmail(data.email);
                ps.saveIt();// Guardamos la persona en la tabla personas
            }

            Student existStudent = Student.findFirst("id_person = ?", ps.getId());

            if(existStudent != null){
                return StudentRegisterResult.duplicate(data.dni);
            }

            // Creamos una instancia de Estudiante
            Student st = new Student();
            // Le damos la informacion correspondiente de esa persona que es estudiante
            st.setPerson(ps);
            st.setType(data.type);
            st.saveIt(); // Guardamos a la persona como estudiante

            return StudentRegisterResult.ok("Cuenta creada exitosamente para " + data.firstName + "!");

        } catch (Exception e) {
            // Si ocurre cualquier error durante la operación de DB (ej. nombre de usuario duplicado),
            // se captura aquí y se redirige con un mensaje de error.
            System.err.println("Error al registrar el estudiante: " + e.getMessage());
            e.printStackTrace(); // Imprime el stack trace para depuración.
            return StudentRegisterResult.error("Error interno al crear la cuenta. Intente de nuevo.");
        }
    }

    /**
     * Actualiza un estudiante existente.
     *
     * @param id   ID del estudiante
     * @param data Nuevos datos del estudiante
     * @return StudentRegisterResult con el resultado de la operacion
     */
    public StudentRegisterResult updateStudent(Long id, StudentData data) {
        try {
            Student student = Student.findById(id);
            if (student == null) {
                return StudentRegisterResult.notFound();
            }

            Person person = student.getPerson();
            if (person == null) {
                return StudentRegisterResult.errorEdit(id, "El estudiante no tiene persona asociada.");
            }

            person.setDni(data.dni);
            person.setFirstName(data.firstName);
            person.setLastName(data.lastName);
            person.setPhone(data.phone);
            person.setEmail(data.email);
            person.saveIt();

            student.setType(data.type);
            student.saveIt();

            return StudentRegisterResult.okEdit(id, "Estudiante actualizado exitosamente.");

        } catch (Exception e) {
            System.err.println("Error al actualizar el estudiante: " + e.getMessage());
            e.printStackTrace();
            return StudentRegisterResult.errorEdit(id, "Error interno al actualizar el estudiante.");
        }
    }

    /**
     * Elimina un estudiante por su ID.
     *
     * @param id ID del estudiante
     * @return StudentRegisterResult con el resultado de la operacion
     */
    public StudentRegisterResult deleteStudent(Long id) {
        try {
            Student student = Student.findById(id);
            if (student == null) {
                return StudentRegisterResult.notFound();
            }

            Person person = student.getPerson();
            student.delete();
            if (person != null) {
                person.delete();
            }

            return StudentRegisterResult.okList("Estudiante eliminado exitosamente.");

        } catch (Exception e) {
            System.err.println("Error al eliminar el estudiante: " + e.getMessage());
            e.printStackTrace();
            return StudentRegisterResult.errorList("Error interno al eliminar el estudiante.");
        }
    }
}
