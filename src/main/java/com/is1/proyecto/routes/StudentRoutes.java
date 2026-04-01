package com.is1.proyecto.routes;

import com.is1.proyecto.services.StudentService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

/**
 * Rutas relacionadas con el registro de estudiantes.
 */
public class StudentRoutes {

    private final StudentService studentService;
    private final MustacheTemplateEngine templateEngine;

    public StudentRoutes(StudentService studentService) {
        this.studentService = studentService;
        this.templateEngine = new MustacheTemplateEngine();
    }

    /**
     * Registra todas las rutas de estudiante en Spark.
     */
    public void register() {
        // GET: Muestra el formulario de registro de estudiante
        get("/register_student", this::showStudentForm, templateEngine);

        // POST: Crea un nuevo student en la base de datos
        post("/register_student", this::handleRegisterStudent);
    }

    /**
     * GET /register_student - Muestra el formulario de registro de estudiante.
     */
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
        return new ModelAndView(model, "student_form.mustache");
    }

    /**
     * POST /register_student - Procesa el formulario de registro de estudiante.
     */
    private Object handleRegisterStudent(Request req, Response res) {
        // Obtenemos los datos del formulario de registro de estudiante
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
        return ""; // Retorna una cadena vacía ya que la respuesta ya fue redirigida.
    }
}
