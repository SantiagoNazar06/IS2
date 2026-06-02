package com.is1.proyecto.routes;

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
    private final MustacheTemplateEngine templateEngine;

    public TeacherRoutes(TeacherService teacherService) {
        this.teacherService = teacherService;
        this.templateEngine = new MustacheTemplateEngine();
    }

    /**
     * Registra todas las rutas de profesor en Spark.
     */
    public void register() {
        get("/register_teacher", this::showTeacherForm, templateEngine);
        post("/register_teacher", this::handleRegisterTeacher);

        get("/teachers", this::listTeachers, templateEngine);
        get("/teachers/:id", this::showTeacherDetail, templateEngine);
        get("/teachers/:id/subjects", this::showTeacherSubjects, templateEngine);
        get("/teachers/:id/grades", this::showTeacherGrades, templateEngine);
        get("/teachers/:id/subjects/:subjectId/students", this::showSubjectStudents, templateEngine);
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

    /**
     * GET /teachers/:id/subjects/:subjectId/students
     * <p>
     * Retorna el listado de alumnos inscriptos en una materia,
     * verificando que el docente esté asignado a ella.
     * </p>
     */
}
