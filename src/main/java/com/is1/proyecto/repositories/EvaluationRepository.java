package com.is1.proyecto.repositories;

import java.util.List;
import com.is1.proyecto.models.Evaluation;

/**
 * Repositorio de Evaluacion: Encargado de la comunicación directa con la base de datos.
 * Centraliza todas las consultas SQL usando el modelo ActiveJDBC.
 */

public class EvaluationRepository {

    public Evaluation findById(Integer id){
        return Evaluation.findFirst("id = ?",id);
    }
    
    public Evaluation findByEnrollmentId(Integer enrollmentId){
        return Evaluation.findFirst("enrollment_id = ?",enrollmentId);
    }

    public Evaluation createEvaluation(Evaluation eval){
        eval.saveIt();
        return eval;
    }

    public List<Evaluation> findByStudent(Integer student_id){
       return Evaluation.where("enrollment_id IN (SELECT id FROM enrollments WHERE student_id = ?)", student_id);
    } 

    public List<Evaluation> findBySubject(Integer subject_id){
       return Evaluation.where("enrollment_id IN (SELECT id FROM enrollments WHERE subject_id = ?)", subject_id);
    }

    public List<Evaluation> findAll(){
        return Evaluation.findAll();
    }
}
