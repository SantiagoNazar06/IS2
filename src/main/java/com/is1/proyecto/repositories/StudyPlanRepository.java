package com.is1.proyecto.repositories;

import com.is1.proyecto.models.StudyPlan;
import java.util.List;

/**
 * Repositorio de StudyPlan: Encargado de la comunicacion directa con la base de datos.
 * Centraliza todas las consultas SQL usando el modelo ActiveJDBC.
 * Sigue el mismo patron que CareerRepository.
 */
public class StudyPlanRepository {

    // Busca un plan de estudio por su ID primario
    public StudyPlan findById(int id) {
        return StudyPlan.findById(id);
    }

    // Busca planes por nombre
    public StudyPlan findByName(String name) {
        return StudyPlan.findFirst("name = ?", name);
    }

    // Retorna la lista completa de todos los planes de estudio
    public List<StudyPlan> findAll() {
        return StudyPlan.findAll();
    }

    // Retorna los planes de estudio filtrados por carrera
    public List<StudyPlan> findByCareerId(int careerId) {
        return StudyPlan.where("id_career = ?", careerId);
    }

    // Persiste una nueva instancia de StudyPlan en la base de datos
    public StudyPlan create(StudyPlan studyPlan) {
        studyPlan.saveIt();
        return studyPlan;
    }

    // Actualiza los datos de un plan de estudio existente si el ID es valido
    public boolean update(StudyPlan studyPlan) {
        StudyPlan existing = StudyPlan.findById(studyPlan.getId());
        if (existing == null) {
            return false;
        }
        studyPlan.saveIt();
        return true;
    }

    // Elimina un plan de estudio validando su existencia previa
    public boolean delete(StudyPlan studyPlan) {
        StudyPlan existing = StudyPlan.findById(studyPlan.getId());
        if (existing == null) {
            return false;
        }
        studyPlan.delete();
        return true;
    }

    // Elimina un plan de estudio por su ID
    public boolean deleteById(int id) {
        StudyPlan studyPlan = StudyPlan.findById(id);
        if (studyPlan == null) {
            return false;
        }
        studyPlan.delete();
        return true;
    }
}
