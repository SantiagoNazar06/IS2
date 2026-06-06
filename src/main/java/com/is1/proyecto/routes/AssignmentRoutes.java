package com.is1.proyecto.routes;

import com.is1.proyecto.services.TeacherService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class AssignmentRoutes {

    private final TeacherService teacherService;
    private final MustacheTemplateEngine templateEngine;

    public AssignmentRoutes(TeacherService teacherService) {
        this.teacherService = teacherService;
        this.templateEngine = new MustacheTemplateEngine();
    }

    public void register() {
        get("/teacher/assignments", this::showTeacherAssignments, templateEngine);
        get("/admin/assignments", this::showAssignments, templateEngine);
        post("/admin/assignments", this::handleCreateAssignment);
        post("/admin/assignments/:id/delete", this::handleDeleteAssignment);
    }

    private ModelAndView showAssignments(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        List<Map<String, Object>> assignments = teacherService.getAllAssignments();
        model.put("assignments", assignments);
        model.put("hasAssignments", assignments != null && !assignments.isEmpty());
        model.put("teachers", teacherService.getAllTeachers());
        model.put("subjects", teacherService.getAllSubjectsSimple());

        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }
        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }

        return new ModelAndView(model, "assignments.mustache");
    }

    private Object handleCreateAssignment(Request req, Response res) {
        String teacherIdStr = req.queryParams("teacherId");
        String subjectIdStr = req.queryParams("subjectId");
        String period = req.queryParams("period");
        String role = req.queryParams("role");

        if (teacherIdStr == null || subjectIdStr == null || period == null || period.isBlank()
                || role == null || role.isBlank()) {
            res.redirect("/admin/assignments?error=Todos los campos son obligatorios");
            return "";
        }

        try {
            Long teacherId = Long.parseLong(teacherIdStr);
            Long subjectId = Long.parseLong(subjectIdStr);

            teacherService.createAssignment(teacherId, subjectId, period.trim(), role.trim());
            res.redirect("/admin/assignments?message=Asignación creada exitosamente");
        } catch (Exception e) {
            System.err.println("Error al crear asignación: " + e.getMessage());
            res.redirect("/admin/assignments?error=Error al crear la asignación: " + e.getMessage());
        }
        return "";
    }

    private Object handleDeleteAssignment(Request req, Response res) {
        String idStr = req.params(":id");
        try {
            Long id = Long.parseLong(idStr);
            if (teacherService.deleteAssignment(id)) {
                res.redirect("/admin/assignments?message=Asignación eliminada correctamente");
            } else {
                res.redirect("/admin/assignments?error=La asignación no existe");
            }
        } catch (Exception e) {
            System.err.println("Error al eliminar asignación: " + e.getMessage());
            res.redirect("/admin/assignments?error=Error al eliminar la asignación");
        }
        return "";
    }

    private ModelAndView showTeacherAssignments(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        Long teacherId = req.session().attribute("teacherId");
        if (teacherId != null) {
            List<Map<String, Object>> assignments = teacherService.getAssignedSubjectsSimple(teacherId);
            model.put("assignments", assignments);
            model.put("hasAssignments", assignments != null && !assignments.isEmpty());
        }
        return new ModelAndView(model, "teacher_assignments.mustache");
    }
}
