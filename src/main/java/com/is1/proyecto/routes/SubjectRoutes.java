package com.is1.proyecto.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.models.ConditionType;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.services.ConditionService;
import com.is1.proyecto.services.PrerequisiteDTO;
import com.is1.proyecto.services.SubjectService;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Map;

import static spark.Spark.*;

/**
 * Rutas para la gestion de materias (Subject).
 * <p>
 * Expone endpoints REST para CRUD de materias y gestion de correlatividades.
 * Todos los endpoints devuelven JSON usando Jackson ObjectMapper.
 * </p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /subjects} — Lista todas las materias (filtrable por {@code ?studyPlanId=})</li>
 *   <li>{@code POST /subjects} — Crea una nueva materia</li>
 *   <li>{@code GET /subjects/:id} — Obtiene una materia por ID</li>
 *   <li>{@code PUT /subjects/:id} — Actualiza una materia existente</li>
 *   <li>{@code DELETE /subjects/:id} — Elimina una materia</li>
 *   <li>{@code GET /subjects/:id/prerequisites} — Lista correlatividades de una materia</li>
 *   <li>{@code POST /subjects/:id/prerequisites} — Agrega una correlatividad</li>
 *   <li>{@code DELETE /subjects/:id/prerequisites/:conditionId} — Elimina una correlatividad</li>
 * </ul>
 *
 * <h3>Relacion con SRS</h3>
 * <ul>
 *   <li><b>SRS-FUN-002</b>: Gestion de la Oferta Academica (ABMC)</li>
 *   <li><b>SRS-FUN-003</b>: Gestion de Correlatividades</li>
 *   <li><b>SRS-FUN-004</b>: Inscripcion a Materias</li>
 * </ul>
 */
public class SubjectRoutes {

    private final SubjectService subjectService;
    private final ConditionService conditionService;
    private final ObjectMapper objectMapper;

    public SubjectRoutes(SubjectService subjectService, ConditionService conditionService) {
        this.subjectService = subjectService;
        this.conditionService = conditionService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Registra todos los endpoints de Subject en Spark.
     */
    public void register() {
        // ── CRUD de materias ──
        get("/subjects", this::handleGetAllSubjects);
        post("/subjects", this::handleCreateSubject);
        get("/subjects/:id", this::handleGetSubject);
        put("/subjects/:id", this::handleUpdateSubject);
        delete("/subjects/:id", this::handleDeleteSubject);

        // ── Correlatividades (prerrequisitos) ──
        get("/subjects/:id/prerequisites", this::handleGetPrerequisites);
        post("/subjects/:id/prerequisites", this::handleAddPrerequisite);
        delete("/subjects/:id/prerequisites/:conditionId", this::handleRemovePrerequisite);
    }

    // ========================================================================
    // CRUD: LIST (GET /subjects)
    // ========================================================================

    /**
     * GET /subjects
     * <p>
     * Retorna todas las materias en formato JSON.
     * Soporta filtro por plan de estudio mediante query param {@code ?studyPlanId=}.
     * </p>
     */
    Object handleGetAllSubjects(Request req, Response res) {
        res.type("application/json");
        try {
            String studyPlanIdParam = req.queryParams("studyPlanId");
            Integer studyPlanId = null;
            if (studyPlanIdParam != null && !studyPlanIdParam.isEmpty()) {
                studyPlanId = Integer.parseInt(studyPlanIdParam);
            }
            List<SubjectDTO> subjects = subjectService.getAllSubjects(studyPlanId);
            res.status(200);
            return toJson(subjects);
        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "studyPlanId debe ser un numero valido"));
        } catch (Exception e) {
            res.status(500);
            return toJson(Map.of("error", "Error interno al listar materias"));
        }
    }

    // ========================================================================
    // CRUD: CREATE (POST /subjects)
    // ========================================================================

    /**
     * POST /subjects
     * <p>
     * Crea una nueva materia. El body JSON debe contener:
     * <pre>
     * {
     *   "code": "ING101",
     *   "name": "Ingles I",
     *   "studyPlanId": 1
     * }
     * </pre>
     * {@code studyPlanId} es opcional.
     * </p>
     */
    Object handleCreateSubject(Request req, Response res) {
        res.type("application/json");
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.body(), Map.class);

            String code = (String) body.get("code");
            String name = (String) body.get("name");
            Integer studyPlanId = body.get("studyPlanId") != null
                    ? ((Number) body.get("studyPlanId")).intValue()
                    : null;

            SubjectDTO created = subjectService.registerSubject(code, name, studyPlanId);
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

    // ========================================================================
    // CRUD: GET BY ID (GET /subjects/:id)
    // ========================================================================

    /**
     * GET /subjects/:id
     * <p>
     * Retorna una materia por su ID.
     * Retorna 404 si no existe.
     * </p>
     */
    Object handleGetSubject(Request req, Response res) {
        res.type("application/json");
        try {
            int id = Integer.parseInt(req.params(":id"));
            SubjectDTO subject = subjectService.getSubjectById(id);
            res.status(200);
            return toJson(subject);
        } catch (ValidationException e) {
            res.status(404);
            return toJson(Map.of("error", e.getMessage()));
        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "ID debe ser un numero valido"));
        } catch (Exception e) {
            res.status(500);
            return toJson(Map.of("error", "Error interno al obtener la materia"));
        }
    }

    // ========================================================================
    // CRUD: UPDATE (PUT /subjects/:id)
    // ========================================================================

    /**
     * PUT /subjects/:id
     * <p>
     * Actualiza una materia existente. El body JSON puede contener:
     * <pre>
     * {
     *   "code": "ING102",
     *   "name": "Ingles II",
     *   "studyPlanId": 1
     * }
     * </pre>
     * Todos los campos son opcionales en update (solo se actualizan los presentes).
     * Para desasignar el plan de estudio, enviar {@code "studyPlanId": -1}.
     * </p>
     */
    Object handleUpdateSubject(Request req, Response res) {
        res.type("application/json");
        try {
            int id = Integer.parseInt(req.params(":id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(req.body(), Map.class);

            String code = (String) body.get("code");
            String name = (String) body.get("name");
            Integer studyPlanId = body.containsKey("studyPlanId")
                    ? (body.get("studyPlanId") != null ? ((Number) body.get("studyPlanId")).intValue() : -1)
                    : null;

            SubjectDTO updated = subjectService.updateSubject(id, code, name, studyPlanId);
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

    // ========================================================================
    // CRUD: DELETE (DELETE /subjects/:id)
    // ========================================================================

    /**
     * DELETE /subjects/:id
     * <p>
     * Elimina una materia por su ID.
     * Retorna 204 si se elimino correctamente, 404 si no existe.
     * </p>
     */
    Object handleDeleteSubject(Request req, Response res) {
        res.type("application/json");
        try {
            int id = Integer.parseInt(req.params(":id"));
            subjectService.deleteSubject(id);
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
            return toJson(Map.of("error", "Error interno al eliminar la materia"));
        }
    }

    // ========================================================================
    // CORRELATIVIDADES: GET /subjects/:id/prerequisites
    // ========================================================================

    /**
     * GET /subjects/:id/prerequisites
     * <p>
     * Devuelve la lista de prerrequisitos para una materia en formato JSON.
     * Retorna 404 si la materia no existe.
     * </p>
     */
    Object handleGetPrerequisites(Request req, Response res) {
        res.type("application/json");
        try {
            int subjectId = Integer.parseInt(req.params(":id"));

            Subject subject = Subject.findById(subjectId);
            if (subject == null) {
                res.status(404);
                return toJson(Map.of("error", "Subject not found"));
            }

            List<PrerequisiteDTO> prerequisites = conditionService.getPrerequisites(subjectId);
            res.status(200);
            return toJson(prerequisites);
        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "ID debe ser un numero valido"));
        }
    }

    // ========================================================================
    // CORRELATIVIDADES: POST /subjects/:id/prerequisites
    // ========================================================================

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
        try {
            int subjectId = Integer.parseInt(req.params(":id"));

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

    // ========================================================================
    // CORRELATIVIDADES: DELETE /subjects/:id/prerequisites/:conditionId
    // ========================================================================

    /**
     * DELETE /subjects/:id/prerequisites/:conditionId
     * <p>
     * Elimina un prerrequisito por su ID.
     * Retorna 204 si se elimino correctamente, 404 si no existe.
     * </p>
     */
    Object handleRemovePrerequisite(Request req, Response res) {
        res.type("application/json");
        try {
            int conditionId = Integer.parseInt(req.params(":conditionId"));

            boolean removed = conditionService.removePrerequisite(conditionId);
            if (removed) {
                res.status(204);
                return "";
            } else {
                res.status(404);
                return toJson(Map.of("error", "Prerequisite not found"));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return toJson(Map.of("error", "ID debe ser un numero valido"));
        }
    }

    // ========================================================================
    // UTILIDADES
    // ========================================================================

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
