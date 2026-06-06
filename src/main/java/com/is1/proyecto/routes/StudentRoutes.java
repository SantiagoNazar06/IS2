package com.is1.proyecto.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.dto.StudentListDTO;
import com.is1.proyecto.models.Enrollment;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.services.CareerService;
import com.is1.proyecto.services.CorrelationEngine;
import com.is1.proyecto.services.PeriodValidator;
import com.is1.proyecto.services.StudentService;
import com.is1.proyecto.services.SubjectService;
import com.is1.proyecto.services.ValidationResult;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class StudentRoutes {

    private final StudentService studentService;
    private final CareerService careerService;
    private final SubjectService subjectService;
    private final ObjectMapper objectMapper;
    private final MustacheTemplateEngine templateEngine;

    public StudentRoutes(StudentService studentService, CareerService careerService,
                         SubjectService subjectService, ObjectMapper objectMapper) {
        this.studentService = studentService;
        this.careerService = careerService;
        this.subjectService = subjectService;
        this.objectMapper = objectMapper;
        this.templateEngine = new MustacheTemplateEngine();
    }

    public void register() {
        get("/register_student", this::showStudentForm, templateEngine);
        post("/register_student", this::handleRegisterStudent);
        get("/students", this::showStudents, templateEngine);
        post("/update_student/:id", this::handleUpdateStudent);
        post("/delete_student/:id", this::handleDeleteStudent);
        post("/enrollments/student/:id", this::handleEnroll);
    }

    private ModelAndView showStudentForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }
        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }

        String editParam = req.queryParams("edit");
        if (editParam != null && !editParam.isEmpty()) {
            try {
                Long studentId = Long.parseLong(editParam);
                Student student = Student.findById(studentId);
                if (student != null) {
                    Person person = student.getPerson();
                    model.put("editMode", true);
                    model.put("studentId", studentId);
                    model.put("dni", person != null ? person.getDni() : "");
                    model.put("student_type", student.getType());
                    model.put("firstName", person != null ? person.getFirstName() : "");
                    model.put("lastName", person != null ? person.getLastName() : "");
                    model.put("phone", person != null ? person.getPhone() : "");
                    model.put("email", person != null ? person.getEmail() : "");
                }
            } catch (NumberFormatException e) {
                // si el ID no es valido, ignoramos y mostramos el formulario vacio
            }
        }

        return new ModelAndView(model, "student_form.mustache");
    }

    private Object handleRegisterStudent(Request req, Response res) {
        String dni = req.queryParams("dni");
        String type = req.queryParams("student_type");
        String firstName = req.queryParams("firstName");
        String lastName = req.queryParams("lastName");
        String phone = req.queryParams("phone");
        String email = req.queryParams("email");

        StudentService.StudentData data = new StudentService.StudentData(
            dni, type, firstName, lastName, phone, email
        );

        StudentService.StudentRegisterResult result = studentService.registerStudent(data);

        res.status(result.statusCode);
        res.redirect(result.redirectUrl);
        return "";
    }

    private Object handleUpdateStudent(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));
            String dni = req.queryParams("dni");
            String type = req.queryParams("student_type");
            String firstName = req.queryParams("firstName");
            String lastName = req.queryParams("lastName");
            String phone = req.queryParams("phone");
            String email = req.queryParams("email");

            StudentService.StudentData data = new StudentService.StudentData(
                dni, type, firstName, lastName, phone, email
            );

            StudentService.StudentRegisterResult result = studentService.updateStudent(id, data);

            if (result.redirectUrl != null) {
                res.redirect(result.redirectUrl);
            } else {
                res.status(result.statusCode);
                res.redirect("/register_student?edit=" + id + "&error=" + result.message);
            }
            return "";

        } catch (NumberFormatException e) {
            res.redirect("/students?error=ID de estudiante invalido");
            return "";
        }
    }

    private Object handleDeleteStudent(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));

            StudentService.StudentRegisterResult result = studentService.deleteStudent(id);

            if (result.redirectUrl != null) {
                res.redirect(result.redirectUrl);
            } else {
                res.status(result.statusCode);
                res.redirect("/students?error=" + result.message);
            }
            return "";

        } catch (NumberFormatException e) {
            res.redirect("/students?error=ID de estudiante invalido");
            return "";
        }
    }

    // GET /students — listado de estudiantes con filtros
    private ModelAndView showStudents(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        // Leer filtros de query params
        String careerIdParam = req.queryParams("careerId");
        String subjectIdParam = req.queryParams("subjectId");
        Long careerId = careerIdParam != null && !careerIdParam.isEmpty() ? Long.parseLong(careerIdParam) : null;
        Long subjectId = subjectIdParam != null && !subjectIdParam.isEmpty() ? Long.parseLong(subjectIdParam) : null;

        // Leer rol y teacherId de la sesión
        String role = req.session().attribute("userRole");
        Long teacherId = "TEACHER".equals(role)
                ? req.session().attribute("teacherId")
                : null;

        // Obtener estudiantes
        List<StudentListDTO> students = studentService.getStudents(careerId, subjectId, teacherId, role);

        // Dropdowns para filtros
        model.put("students", students);
        model.put("careers", careerService.getAllCareers());
        model.put("subjects", subjectService.getAllSubjects(null));

        // Mantener filtros seleccionados en el formulario
        model.put("selectedCareerId", careerId);
        model.put("selectedSubjectId", subjectId);

        return new ModelAndView(model, "students.mustache");
    }

    // AC-1: POST /students/:id/enrollments
    private Object handleEnroll(Request req, Response res) throws Exception {
        res.type("application/json");
        try {
            long studentId;
            try {
                studentId = Long.parseLong(req.params(":id"));
            } catch (NumberFormatException e) {
                res.status(400);
                return objectMapper.writeValueAsString(Map.of("error", "ID de estudiante inválido."));
            }

            // AC-5: STUDENT solo puede inscribirse a sí mismo
            Long sessionStudentId = req.session().attribute("studentId");
            if (sessionStudentId != null && sessionStudentId.longValue() != studentId) {
                res.status(403);
                return objectMapper.writeValueAsString(
                    Map.of("error", "No autorizado: solo puedes inscribirte a ti mismo."));
            }

            // Parsear body JSON
            Map<?, ?> body;
            try {
                body = objectMapper.readValue(req.body(), Map.class);
            } catch (Exception e) {
                res.status(400);
                return objectMapper.writeValueAsString(Map.of("error", "Body JSON inválido o ausente."));
            }

            Object subjectIdRaw = body.get("subjectId");
            Object periodRaw = body.get("period");

            if (subjectIdRaw == null || periodRaw == null) {
                res.status(400);
                return objectMapper.writeValueAsString(
                    Map.of("error", "Se requieren los campos 'subjectId' y 'period'."));
            }

            long subjectId;
            try {
                subjectId = ((Number) subjectIdRaw).longValue();
            } catch (ClassCastException e) {
                res.status(400);
                return objectMapper.writeValueAsString(
                    Map.of("error", "El campo 'subjectId' debe ser un número."));
            }

            String period = periodRaw.toString();

            // AC-6: validar formato YYYY-S
            if (!PeriodValidator.isValidFormat(period)) {
                res.status(400);
                return objectMapper.writeValueAsString(
                    Map.of("error", "Formato de período inválido. Use YYYY-S (ej. 2024-1 o 2024-2)."));
            }

            // AC-7: no permitir período pasado
            if (PeriodValidator.isPastPeriod(period)) {
                res.status(400);
                return objectMapper.writeValueAsString(
                    Map.of("error", "No se puede inscribir a un período pasado."));
            }

            // Verificar que el estudiante exista
            Student student = Student.findById(studentId);
            if (student == null) {
                res.status(404);
                return objectMapper.writeValueAsString(Map.of("error", "Estudiante no encontrado."));
            }

            // Verificar que la materia exista
            Subject subject = Subject.findFirst("id_subject = ?", subjectId);
            if (subject == null) {
                res.status(404);
                return objectMapper.writeValueAsString(Map.of("error", "Materia no encontrada."));
            }

            // AC-2: verificar inscripción duplicada
            Enrollment existing = Enrollment.findFirst(
                "student_id = ? AND subject_id = ? AND period = ?", studentId, subjectId, period);
            if (existing != null) {
                res.status(400);
                return objectMapper.writeValueAsString(Map.of(
                    "error", "El estudiante ya está inscripto en esta materia para el período " + period + "."));
            }

            // AC-2/AC-3/AC-4: validar correlatividades
            ValidationResult result = new CorrelationEngine().canEnroll(studentId, subjectId);
            if (!result.isAllowed()) {
                res.status(400);
                return objectMapper.writeValueAsString(Map.of(
                    "error", result.getMessage(),
                    "reason", result.getReason(),
                    "missingPrerequisites", result.getMissingPrerequisites()));
            }

            // AC-2: crear la inscripción
            Enrollment enrollment = new Enrollment();
            enrollment.setStudentId(studentId);
            enrollment.setSubjectId(subjectId);
            enrollment.setPeriod(period);
            enrollment.setStatus("ENROLLED");
            enrollment.saveIt();

            res.status(201);
            return objectMapper.writeValueAsString(Map.of(
                "message", "Inscripción realizada exitosamente.",
                "enrollment", Map.of(
                    "id", enrollment.getId(),
                    "studentId", studentId,
                    "subjectId", subjectId,
                    "period", period,
                    "status", "ENROLLED",
                    "createdAt", enrollment.getString("created_at"))));

        } catch (Exception e) {
            res.status(500);
            return objectMapper.writeValueAsString(
                Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }
}
