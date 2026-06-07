package com.is1.proyecto.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.dto.StudyPlanDTO;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.models.Career;
import com.is1.proyecto.models.ConditionType;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.services.CareerService;
import com.is1.proyecto.services.ConditionService;
import com.is1.proyecto.services.PrerequisiteDTO;
import com.is1.proyecto.services.StudyPlanService;
import com.is1.proyecto.services.SubjectService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static spark.Spark.*;

/**
 * Rutas para la gestion de materias (Subject).
 * <p>
 * Expone endpoints REST para CRUD de materias y gestion de correlatividades (JSON),
 * asi como endpoints HTML para la administracion visual desde el dashboard.
 * </p>
 *
 * <h3>Endpoints JSON (API)</h3>
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
 * <h3>Endpoints HTML (Vistas)</h3>
 * <ul>
 *   <li>{@code GET /subjects/manage} — Listado HTML con filtro por carrera</li>
 *   <li>{@code GET /subjects/manage/new} — Formulario de creacion</li>
 *   <li>{@code POST /subjects/manage/create} — Procesa creacion</li>
 *   <li>{@code GET /subjects/manage/:id/edit} — Formulario de edicion</li>
 *   <li>{@code POST /subjects/manage/:id/update} — Procesa actualizacion</li>
 *   <li>{@code POST /subjects/manage/:id/delete} — Elimina materia</li>
 * </ul>
 */
public class SubjectRoutes {

    private final SubjectService subjectService;
    private final ConditionService conditionService;
    private final CareerService careerService;
    private final StudyPlanService studyPlanService;
    private final ObjectMapper objectMapper;
    private final MustacheTemplateEngine templateEngine;

    // Constructor legacy: solo servicios de materia (para tests)
    public SubjectRoutes(SubjectService subjectService, ConditionService conditionService) {
        this.subjectService = subjectService;
        this.conditionService = conditionService;
        this.careerService = null;
        this.studyPlanService = null;
        this.objectMapper = new ObjectMapper();
        this.templateEngine = new MustacheTemplateEngine();
    }

    // Constructor completo: incluye CareerService y StudyPlanService para vistas HTML
    public SubjectRoutes(SubjectService subjectService, ConditionService conditionService,
                         CareerService careerService, StudyPlanService studyPlanService) {
        this.subjectService = subjectService;
        this.conditionService = conditionService;
        this.careerService = careerService;
        this.studyPlanService = studyPlanService;
        this.objectMapper = new ObjectMapper();
        this.templateEngine = new MustacheTemplateEngine();
    }

    /**
     * Registra todos los endpoints de Subject en Spark.
     */
    public void register() {
        // ── Vistas HTML para administracion de materias ──
        // Importante: registrar rutas ESTATICAS (mas especificas) ANTES que las parametrizadas,
        // porque Spark evalua las rutas en orden de registro. Si "/subjects/:id" se registra
        // antes que "/subjects/manage", la ruta "/subjects/manage" matchearia como ":id" = "manage".
        get("/subjects/manage", this::handleSubjectsManage, templateEngine);
        get("/subjects/manage/new", this::handleNewSubjectForm, templateEngine);
        post("/subjects/manage/create", this::handleCreateSubjectHtml);
        get("/subjects/manage/:id/edit", this::handleEditSubjectForm, templateEngine);
        post("/subjects/manage/:id/update", this::handleUpdateSubjectHtml);
        post("/subjects/manage/:id/delete", this::handleDeleteSubjectHtml);

        // ── CRUD de materias (JSON) ──
        get("/subjects", this::handleGetAllSubjects);
        post("/subjects", this::handleCreateSubject);
        get("/subjects/:id", this::handleGetSubject);
        put("/subjects/:id", this::handleUpdateSubject);
        delete("/subjects/:id", this::handleDeleteSubject);

        // ── Correlatividades (HTML) ──
        get("/subjects/:id/prerequisites", this::handleGetPrerequisites, templateEngine);
        post("/subjects/:id/prerequisites", this::handleAddPrerequisite);
        post("/subjects/:id/prerequisites/:conditionId/delete", this::handleRemovePrerequisite);
    }

    // ========================================================================
    // CRUD: LIST (GET /subjects) — JSON
    // ========================================================================

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
    // VISTA HTML: GET /subjects/manage
    // ========================================================================

    ModelAndView handleSubjectsManage(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        // Flash messages
        if (req.session().attribute("flashMessage") != null) {
            model.put("message", req.session().attribute("flashMessage"));
            req.session().removeAttribute("flashMessage");
        }
        if (req.session().attribute("flashError") != null) {
            model.put("error", req.session().attribute("flashError"));
            req.session().removeAttribute("flashError");
        }

        try {
            // Obtener todas las materias
            List<SubjectDTO> allSubjects = subjectService.getAllSubjects(null);

            // Filtro por carrera
            String careerIdParam = req.queryParams("careerId");
            Integer careerId = null;
            if (careerIdParam != null && !careerIdParam.isEmpty()) {
                try {
                    careerId = Integer.parseInt(careerIdParam);
                    model.put("selectedCareerId", careerId);
                } catch (NumberFormatException e) {
                    // ignorar
                }
            }

            if (careerId != null) {
                Integer finalCareerId = careerId;
                allSubjects = allSubjects.stream()
                        .filter(s -> s.getCareerId() != null && s.getCareerId().equals(finalCareerId))
                        .collect(Collectors.toList());
            }

            model.put("subjects", allSubjects);

            // Lista de carreras para el filtro (con flag isSelected)
            if (careerService != null) {
                List<Career> careers = careerService.getAllCareers();
                model.put("careers", toCareerMapList(careers, careerId));
            }

        } catch (Exception e) {
            model.put("error", "Error al cargar las materias.");
        }

        return new ModelAndView(model, "subjects.mustache");
    }

    // ========================================================================
    // VISTA HTML: GET /subjects/manage/new
    // ========================================================================

    ModelAndView handleNewSubjectForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        model.put("isEdit", false);

        if (studyPlanService != null) {
            model.put("studyPlans", toStudyPlanMapList(
                    studyPlanService.getAllStudyPlans(null), null));
        }

        return new ModelAndView(model, "subject_form.mustache");
    }

    // ========================================================================
    // VISTA HTML: POST /subjects/manage/create
    // ========================================================================

    Object handleCreateSubjectHtml(Request req, Response res) {
        try {
            String code = req.queryParams("code");
            String name = req.queryParams("name");
            String studyPlanIdParam = req.queryParams("studyPlanId");

            Integer studyPlanId = null;
            if (studyPlanIdParam != null && !studyPlanIdParam.isEmpty()) {
                studyPlanId = Integer.parseInt(studyPlanIdParam);
            }

            subjectService.registerSubject(code, name, studyPlanId);
            req.session().attribute("flashMessage", "Materia creada exitosamente.");
        } catch (Exception e) {
            req.session().attribute("flashError", "Error al crear la materia: " + e.getMessage());
        }

        res.redirect("/subjects/manage");
        return "";
    }

    // ========================================================================
    // VISTA HTML: GET /subjects/manage/:id/edit
    // ========================================================================

    ModelAndView handleEditSubjectForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        model.put("isEdit", true);

        try {
            int id = Integer.parseInt(req.params(":id"));
            SubjectDTO subject = subjectService.getSubjectById(id);
            model.put("subject", subject);

            // Planes de estudio para el selector (con el actual seleccionado)
            if (studyPlanService != null) {
                model.put("studyPlans", toStudyPlanMapList(
                        studyPlanService.getAllStudyPlans(null),
                        subject.getStudyPlanId()));
            }

        } catch (NumberFormatException e) {
            res.redirect("/subjects/manage");
            return null;
        } catch (ValidationException e) {
            req.session().attribute("flashError", "Materia no encontrada.");
            res.redirect("/subjects/manage");
            return null;
        }

        return new ModelAndView(model, "subject_form.mustache");
    }

    // ========================================================================
    // VISTA HTML: POST /subjects/manage/:id/update
    // ========================================================================

    Object handleUpdateSubjectHtml(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            String code = req.queryParams("code");
            String name = req.queryParams("name");
            String studyPlanIdParam = req.queryParams("studyPlanId");

            Integer studyPlanId = null;
            if (studyPlanIdParam != null && !studyPlanIdParam.isEmpty()) {
                studyPlanId = Integer.parseInt(studyPlanIdParam);
            }

            subjectService.updateSubject(id, code, name, studyPlanId);
            req.session().attribute("flashMessage", "Materia actualizada exitosamente.");
        } catch (Exception e) {
            req.session().attribute("flashError", "Error al actualizar la materia: " + e.getMessage());
        }

        res.redirect("/subjects/manage");
        return "";
    }

    // ========================================================================
    // VISTA HTML: POST /subjects/manage/:id/delete
    // ========================================================================

    Object handleDeleteSubjectHtml(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            subjectService.deleteSubject(id);
            req.session().attribute("flashMessage", "Materia eliminada exitosamente.");
        } catch (Exception e) {
            req.session().attribute("flashError", "Error al eliminar la materia: " + e.getMessage());
        }

        res.redirect("/subjects/manage");
        return "";
    }

    // ========================================================================
    // CRUD: CREATE (POST /subjects) — JSON
    // ========================================================================

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
    // CRUD: GET BY ID (GET /subjects/:id) — JSON
    // ========================================================================

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
    // CRUD: UPDATE (PUT /subjects/:id) — JSON
    // ========================================================================

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
    // CRUD: DELETE (DELETE /subjects/:id) — JSON
    // ========================================================================

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

    ModelAndView handleGetPrerequisites(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        // Flash messages
        if (req.session().attribute("flashMessage") != null) {
            model.put("message", req.session().attribute("flashMessage"));
            req.session().removeAttribute("flashMessage");
        }
        if (req.session().attribute("flashError") != null) {
            model.put("error", req.session().attribute("flashError"));
            req.session().removeAttribute("flashError");
        }

        try {
            int subjectId = Integer.parseInt(req.params(":id"));

            Subject subject = Subject.findById(subjectId);
            if (subject == null) {
                req.session().attribute("flashError", "Materia no encontrada.");
                res.redirect("/subjects/manage");
                return null;
            }

            model.put("subject", Map.of(
                    "id", subject.getId(),
                    "code", subject.getCode(),
                    "subjectName", subject.getSubjectName()
            ));
            model.put("subjectId", subjectId);

            // Build prerequisites list with code
            List<PrerequisiteDTO> prerequisites = conditionService.getPrerequisites(subjectId);
            List<Map<String, Object>> prereqModels = new ArrayList<>();
            for (PrerequisiteDTO p : prerequisites) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", p.getId());
                m.put("subjectId", p.getSubjectId());
                m.put("prerequisiteSubjectId", p.getPrerequisiteSubjectId());
                m.put("prerequisiteSubjectName", p.getPrerequisiteSubjectName());
                Subject prereqSubject = Subject.findById(p.getPrerequisiteSubjectId());
                m.put("prerequisiteSubjectCode", prereqSubject != null ? prereqSubject.getCode() : "");
                m.put("type", p.getType().name());
                m.put("isRegular", p.getType() == ConditionType.REGULAR);
                prereqModels.add(m);
            }
            model.put("prerequisites", prereqModels);

            // Available subjects (all subjects except current)
            List<Map<String, Object>> availableSubjects = new ArrayList<>();
            for (SubjectDTO s : subjectService.getAllSubjects(null)) {
                if (!s.getSubjectId().equals((long) subjectId)) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("subjectId", s.getSubjectId());
                    m.put("subjectName", s.getSubjectName());
                    m.put("code", s.getCode());
                    availableSubjects.add(m);
                }
            }
            model.put("availableSubjects", availableSubjects);

            // Condition types for selector
            List<Map<String, Object>> conditionTypes = new ArrayList<>();
            for (ConditionType ct : ConditionType.values()) {
                Map<String, Object> m = new HashMap<>();
                m.put("name", ct.name());
                // Capitalize first letter: REGULAR → Regular, APROBADA → Aprobada
                String label = ct.name().charAt(0) + ct.name().substring(1).toLowerCase();
                m.put("label", label);
                conditionTypes.add(m);
            }
            model.put("conditionTypes", conditionTypes);

        } catch (NumberFormatException e) {
            req.session().attribute("flashError", "ID de materia invalido.");
            res.redirect("/subjects/manage");
            return null;
        }

        return new ModelAndView(model, "subjects-prerequisites.mustache");
    }

    // ========================================================================
    // CORRELATIVIDADES: POST /subjects/:id/prerequisites
    // ========================================================================

    Object handleAddPrerequisite(Request req, Response res) {
        try {
            int subjectId = Integer.parseInt(req.params(":id"));

            String prereqIdParam = req.queryParams("prerequisiteSubjectId");
            String typeStr = req.queryParams("type");

            if (prereqIdParam == null || typeStr == null) {
                req.session().attribute("flashError", "Parametros incompletos.");
                res.redirect("/subjects/" + subjectId + "/prerequisites");
                return "";
            }

            int prereqId = Integer.parseInt(prereqIdParam);
            ConditionType type = ConditionType.fromString(typeStr);
            if (type == null) {
                throw new IllegalArgumentException("Tipo de condicion invalido: " + typeStr);
            }

            conditionService.addPrerequisite(subjectId, prereqId, type);
            req.session().attribute("flashMessage", "Correlativa agregada exitosamente.");

        } catch (IllegalArgumentException e) {
            req.session().attribute("flashError", e.getMessage());
        } catch (Exception e) {
            req.session().attribute("flashError", "Error al agregar la correlativa.");
        }

        int subjectId;
        try {
            subjectId = Integer.parseInt(req.params(":id"));
        } catch (NumberFormatException e) {
            res.redirect("/subjects/manage");
            return "";
        }
        res.redirect("/subjects/" + subjectId + "/prerequisites");
        return "";
    }

    // ========================================================================
    // CORRELATIVIDADES: POST /subjects/:id/prerequisites/:conditionId/delete
    // ========================================================================

    Object handleRemovePrerequisite(Request req, Response res) {
        try {
            int subjectId = Integer.parseInt(req.params(":id"));
            int conditionId = Integer.parseInt(req.params(":conditionId"));

            boolean removed = conditionService.removePrerequisite(conditionId);
            if (removed) {
                req.session().attribute("flashMessage", "Correlativa eliminada exitosamente.");
            } else {
                req.session().attribute("flashError", "La correlativa no existe.");
            }

            res.redirect("/subjects/" + subjectId + "/prerequisites");
        } catch (NumberFormatException e) {
            req.session().attribute("flashError", "ID invalido.");
            res.redirect("/subjects/manage");
        }
        return "";
    }

    // ========================================================================
    // UTILIDADES
    // ========================================================================

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

    /**
     * Convierte una lista de Career a lista de Map para JMustache,
     * agregando la flag {@code isSelected}.
     */
    private List<Map<String, Object>> toCareerMapList(List<Career> careers, Integer selectedId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Career c : careers) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("careerName", c.getCareerName());
            item.put("isSelected", selectedId != null && selectedId.equals(c.getId()));
            result.add(item);
        }
        return result;
    }

    /**
     * Convierte una lista de StudyPlanDTO a lista de Map para JMustache,
     * agregando la flag {@code isSelected}.
     */
    private List<Map<String, Object>> toStudyPlanMapList(List<StudyPlanDTO> plans, Integer selectedId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudyPlanDTO plan : plans) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", plan.getId());
            item.put("name", plan.getName());
            item.put("year", plan.getYear());
            item.put("careerName", plan.getCareerName());
            item.put("isSelected", selectedId != null && selectedId.equals(plan.getId()));
            result.add(item);
        }
        return result;
    }
}
