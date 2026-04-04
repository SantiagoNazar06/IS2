package com.is1.proyecto.routes;

import com.is1.proyecto.services.CareerService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;
import java.util.HashMap;
import java.util.Map;
import static spark.Spark.*;

/**
 * Rutas de Carreras: Define los puntos de acceso (URLs) y conecta las peticiones
 * del navegador con el CareerService.
 */
public class CareerRoutes {

    private final CareerService careerService;
    private final MustacheTemplateEngine templateEngine;

    public CareerRoutes(CareerService careerService) {
        this.careerService = careerService;
        this.templateEngine = new MustacheTemplateEngine();
    }

    // Registra los endpoints disponibles para las carreras
    public void register() {
        // Muestra el formulario para cargar una nueva carrera
        get("/register_career", this::showCareerForm, templateEngine);

        // Procesa los datos enviados desde el formulario de registro
        post("/register_career", this::handleRegisterCareer);

        // Lista todas las carreras registradas en una tabla
        get("/careers", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("careers", careerService.getAllCareers());
            return new ModelAndView(model, "career_list.mustache");
        }, templateEngine);

        // Recibe el ID por URL y los nuevos datos para modificar una carrera
        post("/update_career/:id", (req, res) -> {
            try {
                int id = Integer.parseInt(req.params(":id"));
                String name = req.queryParams("career_name");
                int duration = Integer.parseInt(req.queryParams("career_duration"));
                
                CareerService.CareerRegisterResult result = careerService.updateCareer(id, name, duration);
                res.redirect("/register_career?message=" + result.message);
                return null;
            } catch (Exception e) {
                res.redirect("/register_career?error=La duracion debe ser un numero valido");
                return "";
            }
        });

        // Elimina una carrera específica usando el ID de la URL
        post("/delete_career/:id", (req, res) -> {
            try {
                String idParam = req.params(":id");
                if (idParam == null || idParam.isEmpty()) {
                    res.redirect("/careers?error=ID no válido");
                    return null;
                }
                int id = Integer.parseInt(idParam);

                CareerService.CareerRegisterResult result = careerService.deleteCareer(id);
                
                // Redirige al listado mostrando si se pudo borrar o hubo un error
                String param = result.success ? "message" : "error";
                res.redirect("/careers?" + param + "=" + result.message);
                return null;

            } catch (NumberFormatException e) {
                res.redirect("/careers?error=El ID debe ser un número");
                return null;
            } catch (Exception e) {
                System.err.println("ERROR CRÍTICO AL ELIMINAR:");
                e.printStackTrace(); 
                res.redirect("/careers?error=No se pudo eliminar: " + e.getMessage());
                return null;
            }
        });
    }

    // Prepara el modelo para la vista del formulario, capturando mensajes de éxito/error de la URL
    private ModelAndView showCareerForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }
        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }
        return new ModelAndView(model, "career_form.mustache");
    }

    // Extrae los datos del formulario POST y le pide al servicio que registre la carrera
    private Object handleRegisterCareer(Request req, Response res) {
        try {
            String careerName = req.queryParams("career_name");
            String durationStr = req.queryParams("career_duration");
            int careerDuration = (durationStr != null) ? Integer.parseInt(durationStr) : 0;

            CareerService.CareerRegisterResult result = careerService.registerCareer(careerName, careerDuration);

            res.status(result.statusCode);
            String param = result.success ? "message" : "error";
            res.redirect(result.redirectUrl + "?" + param + "=" + result.message);
            
        } catch (Exception e) {
            res.redirect("/register_career?error=La duracion debe ser un numero valido");
        }
        return "";
    }
}
