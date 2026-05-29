package com.is1.proyecto.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.ConditionType;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.services.ConditionService;
import com.is1.proyecto.services.PrerequisiteDTO;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Map;

import static spark.Spark.*;

/**
 * Rutas JSON para la gestion de correlatividades (prerrequisitos) entre materias.
 * <p>
 * Expone endpoints REST para consultar, agregar y eliminar prerrequisitos.
 * Todos los endpoints devuelven JSON usando Jackson ObjectMapper.
 * No utiliza MustacheTemplateEngine - son endpoints de API, no de vistas.
 * </p>
 */
public class SubjectRoutes {

    private final ConditionService conditionService;
    private final ObjectMapper objectMapper;

    public SubjectRoutes(ConditionService conditionService) {
        this.conditionService = conditionService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Registra los endpoints JSON de correlatividades en Spark.
     */
    public void register() {
        get("/subjects/:id/prerequisites", this::handleGetPrerequisites);
        post("/subjects/:id/prerequisites", this::handleAddPrerequisite);
        delete("/subjects/:id/prerequisites/:conditionId", this::handleRemovePrerequisite);
    }

    /**
     * GET /subjects/:id/prerequisites
     * <p>
     * Devuelve la lista de prerrequisitos para una materia en formato JSON.
     * Retorna 404 si la materia no existe.
     * </p>
     */
    Object handleGetPrerequisites(Request req, Response res) {
        res.type("application/json");
        int subjectId = Integer.parseInt(req.params(":id"));

        Subject subject = Subject.findById(subjectId);
        if (subject == null) {
            res.status(404);
            return toJson(Map.of("error", "Subject not found"));
        }

        List<PrerequisiteDTO> prerequisites = conditionService.getPrerequisites(subjectId);
        res.status(200);
        return toJson(prerequisites);
    }

    /**
     * POST /subjects/:id/prerequisites
     * <p>
     * Agrega un prerrequisito a una materia.
     * Lee el cuerpo JSON con los campos prerequisiteSubjectId y type.
     * Retorna 201 con el DTO creado, 409 si hay conflicto (ciclo/auto-referencia),
     * o 400 si la solicitud es invalida.
     * </p>
     */
    Object handleAddPrerequisite(Request req, Response res) {
        res.type("application/json");
        int subjectId = Integer.parseInt(req.params(":id"));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.body(), Map.class);

            int prereqId = ((Number) body.get("prerequisiteSubjectId")).intValue();
            String typeStr = (String) body.get("type");

            ConditionType type = ConditionType.fromString(typeStr);
            if (type == null) {
                throw new IllegalArgumentException("Invalid condition type: " + typeStr);
            }

            PrerequisiteDTO result = conditionService.addPrerequisite(subjectId, prereqId, type);
            res.status(201);
            return toJson(result);

        } catch (IllegalArgumentException e) {
            res.status(409);
            return toJson(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            res.status(400);
            return toJson(Map.of("error", "Invalid request"));
        }
    }

    /**
     * DELETE /subjects/:id/prerequisites/:conditionId
     * <p>
     * Elimina un prerrequisito por su ID.
     * Retorna 204 si se elimino correctamente, 404 si no existe.
     * </p>
     */
    Object handleRemovePrerequisite(Request req, Response res) {
        res.type("application/json");
        int conditionId = Integer.parseInt(req.params(":conditionId"));

        boolean removed = conditionService.removePrerequisite(conditionId);
        if (removed) {
            res.status(204);
            return "";
        } else {
            res.status(404);
            return toJson(Map.of("error", "Prerequisite not found"));
        }
    }

    /**
     * Convierte un objeto a JSON usando Jackson ObjectMapper.
     * Si falla la serializacion, devuelve un JSON de error como fallback.
     *
     * @param data Objeto a serializar
     * @return String JSON
     */
    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
