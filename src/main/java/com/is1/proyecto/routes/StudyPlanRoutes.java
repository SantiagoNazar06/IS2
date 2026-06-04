package com.is1.proyecto.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.dto.StudyPlanDTO;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.services.StudyPlanService;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Map;

import static spark.Spark.*;

/**
 * Rutas REST para la gestion de planes de estudio (StudyPlan).
 * <p>
 * Un plan de estudio agrupa materias bajo una carrera en un ano especifico.
 * Relacion: Career "1" -- "0..*" StudyPlan "1..*" -- "1..*" Subject
 * </p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /study-plans} — Lista todos los planes (filtrable por {@code ?careerId=})</li>
 *   <li>{@code POST /study-plans} — Crea un nuevo plan de estudio</li>
 *   <li>{@code GET /study-plans/:id} — Obtiene un plan por ID</li>
 *   <li>{@code PUT /study-plans/:id} — Actualiza un plan existente</li>
 *   <li>{@code DELETE /study-plans/:id} — Elimina un plan</li>
 * </ul>
 */
public class StudyPlanRoutes {

    private final StudyPlanService studyPlanService;
    private final ObjectMapper objectMapper;

    public StudyPlanRoutes(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
        this.objectMapper = new ObjectMapper();
    }

    public void register() {
        get("/study-plans", this::handleGetAll);
        post("/study-plans", this::handleCreate);
        get("/study-plans/:id", this::handleGetById);
        put("/study-plans/:id", this::handleUpdate);
        delete("/study-plans/:id", this::handleDelete);
    }

    // ── GET /study-plans ──

    Object handleGetAll(Request req, Response res) {
        res.type("application/json");
        try {
            String careerIdParam = req.queryParams("careerId");
            Integer careerId = null;
            if (careerIdParam != null && !careerIdParam.isEmpty()) {
                careerId = Integer.parseInt(careerIdParam);
            }
            List<StudyPlanDTO> plans = studyPlanService.getAllStudyPlans(careerId);
            res.status(200);
            return toJson(plans);
        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "careerId debe ser un numero valido"));
        } catch (Exception e) {
            res.status(500);
            return toJson(Map.of("error", "Error interno al listar planes de estudio"));
        }
    }

    // ── POST /study-plans ──

    Object handleCreate(Request req, Response res) {
        res.type("application/json");
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.body(), Map.class);

            if (body.get("year") == null || body.get("careerId") == null) {
                res.status(400);
                return toJson(Map.of("error", "Los campos 'year' y 'careerId' son obligatorios"));
            }

            String name = (String) body.get("name");
            int year = ((Number) body.get("year")).intValue();
            int careerId = ((Number) body.get("careerId")).intValue();

            StudyPlanDTO created = studyPlanService.registerStudyPlan(name, year, careerId);
            res.status(201);
            return toJson(created);

        } catch (ValidationException e) {
            res.status(400);
            return toJson(Map.of("error", e.getMessage(), "field", e.getDetails()));
        } catch (Exception e) {
            res.status(400);
            return toJson(Map.of("error", "Solicitud invalida: " + e.getMessage()));
        }
    }

    // ── GET /study-plans/:id ──

    Object handleGetById(Request req, Response res) {
        res.type("application/json");
        try {
            int id = Integer.parseInt(req.params(":id"));
            StudyPlanDTO plan = studyPlanService.getStudyPlanById(id);
            res.status(200);
            return toJson(plan);
        } catch (ValidationException e) {
            res.status(404);
            return toJson(Map.of("error", e.getMessage()));
        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "ID debe ser un numero valido"));
        } catch (Exception e) {
            res.status(500);
            return toJson(Map.of("error", "Error interno al obtener el plan de estudio"));
        }
    }

    // ── PUT /study-plans/:id ──

    Object handleUpdate(Request req, Response res) {
        res.type("application/json");
        try {
            int id = Integer.parseInt(req.params(":id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.body(), Map.class);

            String name = (String) body.get("name");
            Integer year = body.get("year") != null ? ((Number) body.get("year")).intValue() : null;
            Integer careerId = body.get("careerId") != null ? ((Number) body.get("careerId")).intValue() : null;

            StudyPlanDTO updated = studyPlanService.updateStudyPlan(id, name, year, careerId);
            res.status(200);
            return toJson(updated);

        } catch (ValidationException e) {
            res.status(400);
            return toJson(Map.of("error", e.getMessage(), "field", e.getDetails()));
        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "ID debe ser un numero valido"));
        } catch (Exception e) {
            res.status(400);
            return toJson(Map.of("error", "Solicitud invalida: " + e.getMessage()));
        }
    }

    // ── DELETE /study-plans/:id ──

    Object handleDelete(Request req, Response res) {
        res.type("application/json");
        try {
            int id = Integer.parseInt(req.params(":id"));
            studyPlanService.deleteStudyPlan(id);
            res.status(204);
            return "";
        } catch (ValidationException e) {
            res.status(404);
            return toJson(Map.of("error", e.getMessage()));
        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "ID debe ser un numero valido"));
        } catch (Exception e) {
            res.status(500);
            return toJson(Map.of("error", "Error interno al eliminar el plan de estudio"));
        }
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
