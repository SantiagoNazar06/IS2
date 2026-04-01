package com.is1.proyecto.routes;

import com.is1.proyecto.services.TeacherService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;
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
        // GET: Muestra el formulario de registro de profesor
        get("/register_teacher", this::showTeacherForm, templateEngine);

        // POST: Crea un nuevo teacher en la base de datos
        post("/register_teacher", this::handleRegisterTeacher);
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
}
