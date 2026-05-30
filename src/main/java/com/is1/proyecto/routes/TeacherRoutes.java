package com.is1.proyecto.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.dto.StudentWithGradeDTO;
import com.is1.proyecto.services.TeacherService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

/**
 * Rutas relacionadas con el registro de profesores.
 */
public class TeacherRoutes {

    private final TeacherService teacherService;
    private final ObjectMapper objectMapper;
    private final MustacheTemplateEngine templateEngine;

    public TeacherRoutes(TeacherService teacherService) {
        this.teacherService = teacherService;
        this.objectMapper = new ObjectMapper();
        this.templateEngine = new MustacheTemplateEngine();
    }

    /**
     * Registra todas las rutas de profesor en Spark.
     */
    public void register() {
        // GET: Muestra el formulario de registro de profesor
        get("/register_teacher", this::showTeacherForm, templateEngine);

        // POST: Crea un nuevo teacher en la base de datos
        post("/register_teacher", this::handleRegisterTeacher);

        // GET: Retorna listado de alumnos inscriptos en una materia del docente
        get("/teachers/:id/subjects/:subjectId/students", this::handleGetStudents);
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
    Object handleGetStudents(Request req, Response res) {
        res.type("application/json");
        try {
            int teacherId = Integer.parseInt(req.params(":id"));
            int subjectId = Integer.parseInt(req.params(":subjectId"));

            List<StudentWithGradeDTO> students =
                teacherService.getStudentsBySubject(teacherId, subjectId);

            res.status(200);
            return objectMapper.writeValueAsString(students);

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            int status;
            if ("Teacher not found".equals(msg) || "Subject not found".equals(msg)) {
                status = 404;
            } else if ("Teacher not assigned to this subject".equals(msg)) {
                status = 403;
            } else {
                status = 400;
            }
            res.status(status);
            return toJson(Map.of("error", msg));

        } catch (Exception e) {
            res.status(500);
            return toJson(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Convierte un objeto a JSON usando Jackson ObjectMapper.
     */
    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
