package com.is1.proyecto.routes;

import com.is1.proyecto.services.EvaluationService;
import com.is1.proyecto.services.EvaluationService.EvaluationRegisterResult;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;
import java.util.HashMap;
import java.util.Map;
import static spark.Spark.*;

public class EvaluationRoutes {
    
    private final EvaluationService evaluationService;
    private final MustacheTemplateEngine templateEngine;

    // Modificado: Recibe el motor desde App.java para mantener una sola instancia
    public EvaluationRoutes(EvaluationService eval){
        this.evaluationService = eval;
        this.templateEngine = new MustacheTemplateEngine();
    }

    public void register() {
        // 1. GET /grades - Lista las calificaciones (opcionalmente filtradas por materia)
        get("/grades", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            // Si viene subjectId, filtrar por materia
            String subjectIdParam = req.queryParams("subjectId");
            if (subjectIdParam != null && !subjectIdParam.isEmpty()) {
                try {
                    Integer subjectId = Integer.parseInt(subjectIdParam);
                    model.put("evaluations", evaluationService.getEvaluationsBySubject(subjectId));
                    model.put("subjectId", subjectId);
                } catch (NumberFormatException e) {
                    model.put("evaluations", evaluationService.getAllEvaluations());
                }
            } else {
                model.put("evaluations", evaluationService.getAllEvaluations());
            }
            
            // Si venís de un redireccionamiento con mensaje de error o éxito, se lo pasamos al Mustache
            if (req.session().attribute("flashMessage") != null) {
                model.put("message", req.session().attribute("flashMessage"));
                req.session().removeAttribute("flashMessage"); // Limpiamos la sesión
            }
            
            return new ModelAndView(model, "evaluation_list.mustache");
        }, templateEngine);

        // formulario para cargar una nota 
        get("/grades/new", this::showEvaluationForm, templateEngine);

        // Procesa el formulario para guardar la nota
        post("/grades", this::handleRegisterEvaluation);

        // (Bloqueado por inmutabilidad)
        put("/grades/:id", this::handleUpdateEvaluation);

        // (Bloqueado por inmutabilidad)
        delete("/grades/:id", this::handleDeleteEvaluation);
    }


    private ModelAndView showEvaluationForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        return new ModelAndView(model, "evaluation_form.mustache");
    }

    private Object handleRegisterEvaluation(Request req, Response res) {
        try {
            Integer enrollmentId = Integer.parseInt(req.queryParams("enrollment_id"));
            Double grade = Double.parseDouble(req.queryParams("grade"));

            // Llamamos al servicio de lógica de negocio
            EvaluationRegisterResult result = evaluationService.registerEvaluation(enrollmentId, grade);

            req.session().attribute("flashMessage", result.message);
            res.redirect("/grades");
            return "";

        } catch (NumberFormatException e) {
            req.session().attribute("flashMessage", "Error: Los datos ingresados deben ser numéricos válidos.");
            res.redirect("/grades");
            return "";
        }
    }

    private Object handleUpdateEvaluation(Request req, Response res) {
        Integer id = Integer.parseInt(req.params(":id"));
        EvaluationRegisterResult result = evaluationService.updateEvaluation(id, 0.0);
        
        res.status(result.statusCode);
        return result.message;
    }

    private Object handleDeleteEvaluation(Request req, Response res) {
        Integer id = Integer.parseInt(req.params(":id"));
        EvaluationRegisterResult result = evaluationService.deleteEvaluation(id);
        
        res.status(result.statusCode);
        return result.message;
    }
}
