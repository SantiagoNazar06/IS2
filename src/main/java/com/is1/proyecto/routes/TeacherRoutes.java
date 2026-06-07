package com.is1.proyecto.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Evaluation;
import com.is1.proyecto.services.EvaluationService;
import com.is1.proyecto.services.TeacherService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

/**
 * Rutas relacionadas con el registro de profesores.
 */
public class TeacherRoutes {

    private final TeacherService teacherService;
    private final EvaluationService evaluationService;
    private final ObjectMapper objectMapper;
    private final MustacheTemplateEngine templateEngine;

    public TeacherRoutes(TeacherService teacherService, EvaluationService evaluationService, ObjectMapper objectMapper) {
        this.teacherService = teacherService;
        this.evaluationService = evaluationService;
        this.objectMapper = objectMapper;
        this.templateEngine = new MustacheTemplateEngine();
    }

    /**
     * Registra todas las rutas de profesor en Spark.
     */
    public void register() {
        get("/register_teacher", this::showTeacherForm, templateEngine);
        post("/register_teacher", this::handleRegisterTeacher);
        post("/update_teacher/:id", this::handleUpdateTeacher);
        post("/delete_teacher/:id", this::handleDeleteTeacher);

        get("/teachers", this::listTeachers, templateEngine);
        get("/teachers/:id", this::showTeacherDetail, templateEngine);
        get("/teachers/:id/subjects", this::showTeacherSubjects, templateEngine);
        get("/teachers/:id/grades", this::showTeacherGrades, templateEngine);
        get("/teachers/:id/subjects/:subjectId/students", this::showSubjectStudents, templateEngine);
        post("/teachers/:id/grades", this::handleRegisterGrade);
    }

    private ModelAndView listTeachers(Request req, Response res) {
        String role = req.session().attribute("userRole");
        if ("TEACHER".equals(role)) {
            Long teacherId = req.session().attribute("teacherId");
            if (teacherId != null) {
                res.redirect("/teachers/" + teacherId);
                return null;
            }
        }
        Map<String, Object> model = new HashMap<>();
        model.put("teachers", teacherService.getAllTeachers());
        return new ModelAndView(model, "teacher_list.mustache");
    }

    private ModelAndView showTeacherDetail(Request req, Response res) {
        long id = Long.parseLong(req.params(":id"));
        String role = req.session().attribute("userRole");
        if ("TEACHER".equals(role)) {
            Long sessionTeacherId = req.session().attribute("teacherId");
            if (sessionTeacherId == null || sessionTeacherId != id) {
                halt(403, "Acceso denegado");
                return null;
            }
        }
        Map<String, Object> teacher = teacherService.getTeacherWithPerson(id);
        if (teacher == null) {
            halt(404, "Docente no encontrado");
            return null;
        }
        List<Map<String, Object>> subjects = teacherService.getAssignedSubjects(id);
        Map<String, Object> model = new HashMap<>();
        model.put("teacher", teacher);
        model.put("subjects", subjects);
        model.put("teacherId", id);
        return new ModelAndView(model, "teacher_detail.mustache");
    }

    private ModelAndView showSubjectStudents(Request req, Response res) {
        long teacherId = Long.parseLong(req.params(":id"));
        long subjectId = Long.parseLong(req.params(":subjectId"));
        String role = req.session().attribute("userRole");
        if ("TEACHER".equals(role)) {
            Long sessionTeacherId = req.session().attribute("teacherId");
            if (sessionTeacherId == null || sessionTeacherId != teacherId) {
                halt(403, "Acceso denegado");
                return null;
            }
        }
        List<Map<String, Object>> students = teacherService.getSubjectStudents(teacherId, subjectId);
        // Agregar flags para el selector de condicion en el template
        for (Map<String, Object> student : students) {
            String condition = (String) student.get("condition");
            student.put("isRegular", "REGULAR".equals(condition));
            student.put("isAprobada", "APROBADA".equals(condition));
            student.put("isPromocion", "PROMOCION".equals(condition));
        }
        Map<String, Object> model = new HashMap<>();
        model.put("students", students);
        model.put("teacherId", teacherId);
        model.put("subjectId", subjectId);
        return new ModelAndView(model, "teacher_subject_students.mustache");
    }

    private ModelAndView showTeacherSubjects(Request req, Response res) {
        long id = Long.parseLong(req.params(":id"));
        String role = req.session().attribute("userRole");
        if ("TEACHER".equals(role)) {
            Long sessionTeacherId = req.session().attribute("teacherId");
            if (sessionTeacherId == null || sessionTeacherId != id) {
                halt(403, "Acceso denegado");
                return null;
            }
        }
        Map<String, Object> teacher = teacherService.getTeacherWithPerson(id);
        if (teacher == null) {
            halt(404, "Docente no encontrado");
            return null;
        }
        Map<String, Object> model = new HashMap<>();
        model.put("teacher", teacher);
        model.put("subjects", teacherService.getAssignedSubjects(id));
        model.put("teacherId", id);
        return new ModelAndView(model, "teacher_subjects.mustache");
    }

    private ModelAndView showTeacherGrades(Request req, Response res) {
        long id = Long.parseLong(req.params(":id"));
        String role = req.session().attribute("userRole");
        if ("TEACHER".equals(role)) {
            Long sessionTeacherId = req.session().attribute("teacherId");
            if (sessionTeacherId == null || sessionTeacherId != id) {
                halt(403, "Acceso denegado");
                return null;
            }
        }
        Map<String, Object> teacher = teacherService.getTeacherWithPerson(id);
        if (teacher == null) {
            halt(404, "Docente no encontrado");
            return null;
        }
        List<Map<String, Object>> subjectsWithStudents = new ArrayList<>();
        for (Map<String, Object> subject : teacherService.getAssignedSubjects(id)) {
            Long subjectId = ((Number) subject.get("subjectId")).longValue();
            List<Map<String, Object>> students = teacherService.getSubjectStudents(id, subjectId);
            Map<String, Object> entry = new HashMap<>(subject);
            entry.put("students", students);
            subjectsWithStudents.add(entry);
        }
        Map<String, Object> model = new HashMap<>();
        model.put("teacher", teacher);
        model.put("subjectsWithGrades", subjectsWithStudents);
        model.put("teacherId", id);
        return new ModelAndView(model, "teacher_grades.mustache");
    }

    /**
     * GET /register_teacher - Muestra el formulario de registro de profesor.
     */
    private ModelAndView showTeacherForm(Request req, Response res) {
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
                Long teacherId = Long.parseLong(editParam);
                Map<String, Object> teacher = teacherService.getTeacherWithPerson(teacherId);
                if (teacher != null) {
                    model.put("editMode", true);
                    model.put("teacherId", teacherId);
                    model.put("dni", teacher.getOrDefault("dni", ""));
                    model.put("nroLegajo", teacher.getOrDefault("legajo", ""));
                    model.put("firstName", teacher.getOrDefault("firstName", ""));
                    model.put("lastName", teacher.getOrDefault("lastName", ""));
                    model.put("phone", teacher.getOrDefault("phone", ""));
                    model.put("email", teacher.getOrDefault("email", ""));
                }
            } catch (NumberFormatException e) {
                // ignorar
            }
        }

        return new ModelAndView(model, "teacher_form.mustache");
    }

    /**
     * POST /register_teacher - Procesa el formulario de registro de profesor.
     */
    private Object handleRegisterTeacher(Request req, Response res) {
        String dni = req.queryParams("dni");
        String nroLegajo = req.queryParams("nroLegajo");
        String firstName = req.queryParams("firstName");
        String lastName = req.queryParams("lastName");
        String phone = req.queryParams("phone");
        String email = req.queryParams("email");

        TeacherService.TeacherData data = new TeacherService.TeacherData(
            dni, nroLegajo, firstName, lastName, phone, email
        );

        TeacherService.TeacherRegisterResult result = teacherService.registerTeacher(data);

        res.status(result.statusCode);
        res.redirect(result.redirectUrl);
        return ""; // Retorna una cadena vacía ya que la respuesta ya fue redirigida.
    }

    private Object handleUpdateTeacher(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));
            String dni = req.queryParams("dni");
            String nroLegajo = req.queryParams("nroLegajo");
            String firstName = req.queryParams("firstName");
            String lastName = req.queryParams("lastName");
            String phone = req.queryParams("phone");
            String email = req.queryParams("email");

            TeacherService.TeacherData data = new TeacherService.TeacherData(
                dni, nroLegajo, firstName, lastName, phone, email
            );

            TeacherService.TeacherRegisterResult result = teacherService.updateTeacher(id, data);

            if (result.redirectUrl != null) {
                res.redirect(result.redirectUrl);
            } else {
                res.status(result.statusCode);
                res.redirect("/register_teacher?edit=" + id + "&error=" + result.message);
            }
            return "";

        } catch (NumberFormatException e) {
            res.redirect("/teachers?error=ID de profesor invalido");
            return "";
        }
    }

    private Object handleDeleteTeacher(Request req, Response res) {
        try {
            Long id = Long.parseLong(req.params(":id"));

            TeacherService.TeacherRegisterResult result = teacherService.deleteTeacher(id);

            if (result.redirectUrl != null) {
                res.redirect(result.redirectUrl);
            } else {
                res.status(result.statusCode);
                res.redirect("/teachers?error=" + result.message);
            }
            return "";

        } catch (NumberFormatException e) {
            res.redirect("/teachers?error=ID de profesor invalido");
            return "";
        }
    }

    /**
     * POST /teachers/:id/grades
     * <p>
     * Endpoint REST JSON (issue #24) para que un docente cargue la condición/nota de
     * una inscripción. Cuerpo esperado:
     * {@code {"enrollmentId": ..., "condition": "REGULAR|APROBADA|PROMOCION", "grade": ...}}.
     * REGULAR no lleva nota; APROBADA y PROMOCION requieren nota &gt;= 5.
     * </p>
     * <p>Códigos: 201 creada · 200 transición REGULAR→final · 400 datos inválidos ·
     * 403 docente no asignado · 404 inscripción inexistente · 409 condición ya final.</p>
     */
    private Object handleRegisterGrade(Request req, Response res) {
        res.type("application/json");

        long teacherId;
        try {
            teacherId = Long.parseLong(req.params(":id"));
        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "ID de docente inválido."));
        }

        // Un TEACHER solo puede cargar notas para sí mismo; ADMIN puede para cualquiera.
        String role = req.session().attribute("userRole");
        if ("TEACHER".equals(role)) {
            Long sessionTeacherId = req.session().attribute("teacherId");
            if (sessionTeacherId == null || sessionTeacherId != teacherId) {
                res.status(403);
                return toJson(Map.of("error", "No puede cargar calificaciones para otro docente."));
            }
        }

        Integer enrollmentId;
        String condition;
        Double grade;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.body(), Map.class);
            enrollmentId = body.get("enrollmentId") != null ? ((Number) body.get("enrollmentId")).intValue() : null;
            condition = body.get("condition") != null ? body.get("condition").toString() : null;
            grade = body.get("grade") != null ? ((Number) body.get("grade")).doubleValue() : null;
        } catch (Exception e) {
            res.status(400);
            return toJson(Map.of("error", "Cuerpo inválido. Se espera JSON {\"enrollmentId\": ..., \"condition\": \"REGULAR|APROBADA|PROMOCION\", \"grade\": ...}."));
        }

        EvaluationService.GradeResult result =
            evaluationService.registerTeacherGrade(teacherId, enrollmentId, condition, grade, teacherService);

        res.status(result.statusCode);
        if (!result.success) {
            return toJson(Map.of("error", result.error));
        }

        Evaluation eval = result.evaluation;
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", eval.getEvaluationId());
        payload.put("enrollmentId", eval.getEvaluationEnrollementId());
        payload.put("condition", eval.getCondition());
        payload.put("grade", eval.getEvaluationGrade());
        payload.put("evaluationDate", eval.getEvaluationDate() != null ? eval.getEvaluationDate().toString() : null);
        payload.put("enrollmentStatus", result.enrollmentStatus);
        return toJson(payload);
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"error\":\"Error de serialización\"}";
        }
    }
}
