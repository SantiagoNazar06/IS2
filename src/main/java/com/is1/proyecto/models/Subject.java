package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("subjects")
@IdName("id_subject")
public class Subject extends Model {

    public Integer getId() {
        return getInteger("id_subject");
    }

    // ── Código único de materia (ej: "ING101", "MAT202") ──
    public String getCode() {
        return getString("code");
    }

    public void setCode(String code) {
        set("code", code);
    }

    // ── Nombre de la materia ──
    public String getSubjectName() {
        return getString("subject_name");
    }

    public void setSubjectName(String name) {
        set("subject_name", name);
    }

    // ── Relación con StudyPlan (plan de estudio) ──
    // Subject → StudyPlan → Career
    public StudyPlan getStudyPlan() {
        return parent(StudyPlan.class);
    }

    public void setStudyPlan(StudyPlan studyPlan) {
        setInteger("id_study_plan", studyPlan.getId());
    }

    public Integer getStudyPlanId() {
        return getInteger("id_study_plan");
    }

    public void setStudyPlanId(Integer studyPlanId) {
        setInteger("id_study_plan", studyPlanId);
    }
}
