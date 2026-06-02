package com.is1.proyecto.services;

import java.util.Date;
import java.util.List;

import com.is1.proyecto.models.Evaluation;
import com.is1.proyecto.repositories.EvaluationRepository;

public class EvaluationService {
    private final EvaluationRepository repository;

    public EvaluationService(EvaluationRepository repo){
        repository = repo;
    }

    /**
     * Clase interna para estructurar las respuestas del servicio.
     * Ayuda a comunicar éxito, error, mensajes y códigos HTTP a las rutas.
     */
    public static class EvaluationRegisterResult {
        public final boolean success;
        public final int statusCode;
        public final String redirectUrl;
        public final String message;

        private EvaluationRegisterResult(boolean success, int statusCode, String redirectUrl, String message) {
            this.success = success;
            this.statusCode = statusCode;
            this.redirectUrl = redirectUrl;
            this.message = message;
        }

        // Genera una respuesta de éxito (201 Created)
        public static EvaluationRegisterResult ok(String message) {
            return new EvaluationRegisterResult(true, 201, "/register_evaluation", message);
        }

        // Genera una respuesta de error genérico (500 Internal Server Error)
        public static EvaluationRegisterResult error(String message) {
            return new EvaluationRegisterResult(false, 500, "/register_evaluation", message);
        }

        // Respuesta específica para cuando se intenta registrar un nombre que ya existe (400 Bad Request)
        public static EvaluationRegisterResult duplicate(String nameEvaluation) {
            return new EvaluationRegisterResult(false, 400, "/register_evaluation", "Ya existe una evaluacion con el nombre " + nameEvaluation);
        }
    }

    public EvaluationRegisterResult registerEvaluation(Integer enrollmentId,Double grade){
        try {

            //Chequeo que el "grade" este en un rango valido
            if (grade == null || grade < 0.0 || grade > 10.0) {
                return EvaluationRegisterResult.error("La calificación debe estar entre 0.00 y 10.00.");
            }

            //Cheaqueo que si existe alguna nota para la id de inscripcion que me pasaron.
            Evaluation existingEval = repository.findByEnrollmentId(enrollmentId);
            if (existingEval != null) {
                return EvaluationRegisterResult.error("Esta inscripción ya cuenta con una calificación registrada.");
            }

            Evaluation eval = new Evaluation();
            eval.setEvaluationEnrollementId(enrollmentId);
            eval.setEvaluationGrade(grade);
            repository.createEvaluation(eval);

            return EvaluationRegisterResult.ok("Evaluacion registrada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al registrar la evaluacion: " + e.getMessage());
            e.printStackTrace();
            return EvaluationRegisterResult.error("Error interno al crear la evalucion.");
        }
    }

    public EvaluationRegisterResult updateEvaluation(Integer id, Double newGrade) {
        return EvaluationRegisterResult.error("No se permite la modificacion de calificaciones.");
    }

    public EvaluationRegisterResult deleteEvaluation(Integer id) {

        return EvaluationRegisterResult.error("No se permite eliminar calificaciones registradas.");
    }

    public List<Evaluation> getAllEvaluations(){
        return repository.findAll();
    }
}
