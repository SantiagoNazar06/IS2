package com.is1.proyecto.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.dto.StudyPlanDTO;
import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.models.Career;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.models.StudyPlan;
import com.is1.proyecto.services.CareerService;
import com.is1.proyecto.services.SubjectService;
import com.is1.proyecto.services.StudyPlanService;
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
 * Rutas REST + HTML para la gestion de planes de estudio (StudyPlan).
 * <p>
 * Un plan de estudio agrupa materias bajo una carrera en un ano especifico.
 * Relacion: Career "1" -- "0..*" StudyPlan "1..*" -- "1..*" Subject
 * </p>
 *
 * <h3>Endpoints JSON (API)</h3>
 * <ul>
 *   <li>{@code GET /study-plans} — Lista todos los planes (filtrable por {@code ?careerId=})</li>
 *   <li>{@code POST /study-plans} — Crea un nuevo plan de estudio</li>
 *   <li>{@code GET /study-plans/:id} — Obtiene un plan por ID</li>
 *   <li>{@code PUT /study-plans/:id} — Actualiza un plan existente</li>
 *   <li>{@code DELETE /study-plans/:id} — Elimina un plan</li>
 * </ul>
 *
 * <h3>Endpoints HTML (Vistas)</h3>
 * <ul>
 *   <li>{@code GET /study-plans/manage} — Listado HTML de planes de estudio</li>
 *   <li>{@code GET /study-plans/manage/new} — Formulario de creacion</li>
 *   <li>{@code POST /study-plans/manage/create} — Procesa creacion</li>
 *   <li>{@code POST /study-plans/manage/:id/delete} — Elimina plan</li>
 *   <li>{@code GET /study-plans/manage/:id} — Detalle del plan (materias)</li>
 *   <li>{@code POST /study-plans/manage/:id/subjects} — Agrega materia al plan</li>
 *   <li>{@code POST /study-plans/manage/:id/subjects/:subjectId/remove} — Quita materia del plan</li>
 * </ul>
 */
public class StudyPlanRoutes {

    private final StudyPlanService studyPlanService;
    private final CareerService careerService;
    private final SubjectService subjectService;
    private final ObjectMapper objectMapper;
    private final MustacheTemplateEngine templateEngine;

    // Constructor solo con StudyPlanService (para tests / solo JSON)
    public StudyPlanRoutes(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
        this.careerService = null;
        this.subjectService = null;
        this.objectMapper = new ObjectMapper();
        this.templateEngine = new MustacheTemplateEngine();
    }

    // Constructor completo con CareerService y SubjectService para vistas HTML
    public StudyPlanRoutes(StudyPlanService studyPlanService,
                           CareerService careerService,
                           SubjectService subjectService) {
        this.studyPlanService = studyPlanService;
        this.careerService = careerService;
        this.subjectService = subjectService;
        this.objectMapper = new ObjectMapper();
        this.templateEngine = new MustacheTemplateEngine();
    }

    public void register() {
        // ── Vistas HTML para administracion de planes de estudio ──
        get("/study-plans/manage", this::handleManageList, templateEngine);
        get("/study-plans/manage/new", this::handleNewForm, templateEngine);
        post("/study-plans/manage/create", this::handleCreateHtml);
        post("/study-plans/manage/:id/delete", this::handleDeleteHtml);
        get("/study-plans/manage/:id", this::handleDetail, templateEngine);
        post("/study-plans/manage/:id/subjects", this::handleAddSubject);
        post("/study-plans/manage/:id/subjects/:subjectId/remove", this::handleRemoveSubject);

        // ── API JSON ──
        get("/study-plans", this::handleGetAll);
        post("/study-plans", this::handleCreate);
        get("/study-plans/:id", this::handleGetById);
        put("/study-plans/:id", this::handleUpdate);
        delete("/study-plans/:id", this::handleDelete);
    }

    // ========================================================================
    // VISTAS HTML
    // ========================================================================

    /** GET /study-plans/manage — Listado HTML de planes */
    ModelAndView handleManageList(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        flashMessages(req, model);

        try {
            List<StudyPlanDTO> plans = studyPlanService.getAllStudyPlans(null);
            model.put("plans", toPlanMapList(plans));
        } catch (Exception e) {
            model.put("error", "Error al cargar los planes de estudio.");
        }

        return new ModelAndView(model, "study_plans.mustache");
    }

    /** GET /study-plans/manage/new — Formulario de creacion */
    ModelAndView handleNewForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        model.put("isEdit", false);

        if (careerService != null) {
            model.put("careers", toCareerMapList(careerService.getAllCareers(), null));
        }

        return new ModelAndView(model, "study_plan_form.mustache");
    }

    /** POST /study-plans/manage/create — Procesa creacion */
    Object handleCreateHtml(Request req, Response res) {
        try {
            String name = req.queryParams("name");
            String yearStr = req.queryParams("year");
            String careerIdStr = req.queryParams("careerId");

            if (name == null || name.trim().isEmpty()) {
                throw new ValidationException("El nombre es obligatorio", "name");
            }
            int year = Integer.parseInt(yearStr);
            int careerId = Integer.parseInt(careerIdStr);

            studyPlanService.registerStudyPlan(name.trim(), year, careerId);
            req.session().attribute("flashMessage", "Plan de estudio creado exitosamente.");
        } catch (Exception e) {
            req.session().attribute("flashError", "Error al crear el plan: " + e.getMessage());
        }

        res.redirect("/study-plans/manage");
        return "";
    }

    /** POST /study-plans/manage/:id/delete — Elimina plan */
    Object handleDeleteHtml(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            studyPlanService.deleteStudyPlan(id);
            req.session().attribute("flashMessage", "Plan de estudio eliminado.");
        } catch (Exception e) {
            req.session().attribute("flashError", "Error al eliminar: " + e.getMessage());
        }

        res.redirect("/study-plans/manage");
        return "";
    }

    /** GET /study-plans/manage/:id — Detalle del plan con sus materias */
    ModelAndView handleDetail(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        flashMessages(req, model);

        try {
            int id = Integer.parseInt(req.params(":id"));
            StudyPlanDTO plan = studyPlanService.getStudyPlanById(id);

            model.put("plan", plan);

            // Materias actuales del plan
            List<SubjectDTO> planSubjects = subjectService != null
                    ? subjectService.getAllSubjects(id)
                    : List.of();

            // Materias disponibles para agregar (de la misma carrera o sin plan)
            List<SubjectDTO> available = new ArrayList<>();
            if (subjectService != null && plan.getCareerId() != null) {
                for (SubjectDTO s : subjectService.getSubjectsByCareerOrUnassigned(plan.getCareerId())) {
                    // Excluir las que ya estan en este plan
                    boolean alreadyInPlan = planSubjects.stream()
                            .anyMatch(p -> p.getSubjectId().equals(s.getSubjectId()));
                    if (!alreadyInPlan) {
                        available.add(s);
                    }
                }
            }

            model.put("subjects", planSubjects);
            model.put("availableSubjects", available);

        } catch (NumberFormatException e) {
            res.redirect("/study-plans/manage");
            return null;
        } catch (ValidationException e) {
            req.session().attribute("flashError", "Plan no encontrado.");
            res.redirect("/study-plans/manage");
            return null;
        }

        return new ModelAndView(model, "study_plan_detail.mustache");
    }

    /** POST /study-plans/manage/:id/subjects — Agrega materia al plan */
    Object handleAddSubject(Request req, Response res) {
        try {
            int planId = Integer.parseInt(req.params(":id"));
            int subjectId = Integer.parseInt(req.queryParams("subjectId"));

            StudyPlan studyPlan = StudyPlan.findById(planId);
            if (studyPlan == null) {
                throw new ValidationException("Plan de estudio no encontrado", "id");
            }

            Subject subject = Subject.findById(subjectId);
            if (subject == null) {
                throw new ValidationException("Materia no encontrada", "subjectId");
            }

            // Verificar que la materia pertenezca a la misma carrera (si ya tiene plan)
            if (subject.getStudyPlanId() != null) {
                StudyPlan currentPlan = subject.getStudyPlan();
                if (currentPlan != null && !currentPlan.getCareerId().equals(studyPlan.getCareerId())) {
                    throw new ValidationException(
                            "La materia pertenece a otra carrera y no puede asignarse a este plan", "subjectId");
                }
            }

            subject.setStudyPlanId(planId);
            subject.saveIt();

            req.session().attribute("flashMessage", "Materia agregada al plan exitosamente.");
        } catch (Exception e) {
            req.session().attribute("flashError", "Error al agregar materia: " + e.getMessage());
        }

        res.redirect("/study-plans/manage/" + req.params(":id"));
        return "";
    }

    /** POST /study-plans/manage/:id/subjects/:subjectId/remove — Quita materia del plan */
    Object handleRemoveSubject(Request req, Response res) {
        try {
            int planId = Integer.parseInt(req.params(":id"));
            int subjectId = Integer.parseInt(req.params(":subjectId"));

            Subject subject = Subject.findById(subjectId);
            if (subject == null) {
                throw new ValidationException("Materia no encontrada", "subjectId");
            }

            subject.setStudyPlanId(null);
            subject.saveIt();

            req.session().attribute("flashMessage", "Materia desasignada del plan.");
        } catch (Exception e) {
            req.session().attribute("flashError", "Error al quitar materia: " + e.getMessage());
        }

        res.redirect("/study-plans/manage/" + req.params(":id"));
        return "";
    }

    // ── Helpers de modelo para templates ──

    private void flashMessages(Request req, Map<String, Object> model) {
        if (req.session().attribute("flashMessage") != null) {
            model.put("message", req.session().attribute("flashMessage"));
            req.session().removeAttribute("flashMessage");
        }
        if (req.session().attribute("flashError") != null) {
            model.put("error", req.session().attribute("flashError"));
            req.session().removeAttribute("flashError");
        }
    }

    private List<Map<String, Object>> toPlanMapList(List<StudyPlanDTO> plans) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudyPlanDTO p : plans) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("year", p.getYear());
            item.put("careerName", p.getCareerName());
            result.add(item);
        }
        return result;
    }

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
