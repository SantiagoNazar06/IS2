package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Subject;
import java.util.List;

/**
 * Repositorio de Subject: Encargado de la comunicacion directa con la base de datos.
 * Centraliza todas las consultas SQL usando el modelo ActiveJDBC.
 * Sigue el mismo patron que CareerRepository.
 */
public class SubjectRepository {

    // Busca una materia por su ID primario
    public Subject findById(int id) {
        return Subject.findById(id);
    }

    // Busca una materia por su codigo unico (util para evitar duplicados)
    public Subject findByCode(String code) {
        return Subject.findFirst("code = ?", code);
    }

    // Retorna la lista completa de todas las materias registradas
    public List<Subject> findAll() {
        return Subject.findAll();
    }

    // Retorna las materias filtradas por plan de estudio
    public List<Subject> findByStudyPlanId(int studyPlanId) {
        return Subject.where("id_study_plan = ?", studyPlanId);
    }

    // Persiste una nueva instancia de Subject en la base de datos
    public Subject create(Subject subject) {
        subject.saveIt();
        return subject;
    }

    // Actualiza los datos de una materia existente si el ID es valido
    public boolean update(Subject subject) {
        Subject existing = Subject.findById(subject.getId());
        if (existing == null) {
            return false;
        }
        subject.saveIt();
        return true;
    }

    // Elimina una materia de la base de datos validando su existencia previa
    public boolean delete(Subject subject) {
        Subject existing = Subject.findById(subject.getId());
        if (existing == null) {
            return false;
        }
        subject.delete();
        return true;
    }

    // Elimina una materia por su ID
    public boolean deleteById(int id) {
        Subject subject = Subject.findById(id);
        if (subject == null) {
            return false;
        }
        subject.delete();
        return true;
    }
}
