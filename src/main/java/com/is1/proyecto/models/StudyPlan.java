package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo que representa un Plan de Estudio dentro de una Carrera.
 * <p>
 * Un plan de estudio agrupa materias bajo una carrera en un año específico.
 * Relacion: Career "1" -- "0..*" StudyPlan "1..*" -- "1..*" Subject
 * </p>
 */
@Table("study_plans")
@IdName("id_study_plan")
public class StudyPlan extends Model {

    public Integer getId() {
        return getInteger("id_study_plan");
    }

    // ── Nombre del plan (ej: "Plan 2024", "Plan Viejo") ──
    public String getName() {
        return getString("name");
    }

    public void setName(String name) {
        set("name", name);
    }

    // ── Año del plan ──
    public Integer getYear() {
        return getInteger("year");
    }

    public void setYear(Integer year) {
        set("year", year);
    }

    // ── Relación con Career (carrera) ──
    public Career getCareer() {
        return parent(Career.class);
    }

    public void setCareer(Career career) {
        setInteger("id_career", career.getId());
    }

    public Integer getCareerId() {
        return getInteger("id_career");
    }

    public void setCareerId(Integer careerId) {
        setInteger("id_career", careerId);
    }
}
