package com.is1.proyecto.services;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.is1.proyecto.models.Enrollment;
import com.is1.proyecto.models.Evaluation;
import com.is1.proyecto.repositories.EvaluationRepository;

public class EvaluationService {

    /** Nota mínima (inclusive) para aprobar una materia y que la inscripción pase a COMPLETED. */
    public static final double APPROVAL_THRESHOLD = 5.0;

    // Condiciones académicas posibles. El alumno permanece REGULAR hasta que aprueba.
    public static final String COND_REGULAR = "REGULAR";     // aprobó parciales, sin final
    public static final String COND_APROBADA = "APROBADA";   // aprobó el final
    public static final String COND_PROMOCION = "PROMOCION"; // promocionó, sin rendir final

    private static final Set<String> VALID_CONDITIONS =
        Set.of(COND_REGULAR, COND_APROBADA, COND_PROMOCION);
    /** Condiciones que cierran la materia con nota y completan la inscripción. */
    private static final Set<String> COMPLETING_CONDITIONS =
        Set.of(COND_APROBADA, COND_PROMOCION);

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
            // Ruta legacy (formulario): deriva la condición de la nota como puente.
            eval.setCondition(grade >= APPROVAL_THRESHOLD ? COND_APROBADA : COND_REGULAR);
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

    /**
     * Obtiene las evaluaciones (calificaciones) filtradas por materia.
     *
     * @param subjectId ID de la materia
     * @return Lista de evaluaciones para esa materia
     */
    public List<Evaluation> getEvaluationsBySubject(Integer subjectId) {
        return repository.findBySubject(subjectId);
    }

    /**
     * Resultado de la carga de una calificación por parte de un docente (endpoint REST #24).
     * Transporta el código HTTP, la evaluación creada y el estado resultante de la inscripción.
     */
    public static class GradeResult {
        public final boolean success;
        public final int statusCode;
        public final String error;             // null si success
        public final Evaluation evaluation;    // null si error
        public final String enrollmentStatus;  // estado de la inscripción tras la carga

        private GradeResult(boolean success, int statusCode, String error, Evaluation evaluation, String enrollmentStatus) {
            this.success = success;
            this.statusCode = statusCode;
            this.error = error;
            this.evaluation = evaluation;
            this.enrollmentStatus = enrollmentStatus;
        }

        public static GradeResult ok(int statusCode, Evaluation evaluation, String enrollmentStatus) {
            return new GradeResult(true, statusCode, null, evaluation, enrollmentStatus);
        }

        public static GradeResult fail(int statusCode, String error) {
            return new GradeResult(false, statusCode, error, null, null);
        }
    }

    /**
     * Registra la condición/calificación de un estudiante en una inscripción (endpoint #24),
     * soportando los tres estados académicos:
     * <ul>
     *   <li><b>REGULAR</b>: aprobó parciales, sin nota final (grade = null).</li>
     *   <li><b>APROBADA</b>: aprobó el final; requiere nota &gt;= {@value #APPROVAL_THRESHOLD}.</li>
     *   <li><b>PROMOCION</b>: promocionó sin rendir final; requiere nota &gt;= {@value #APPROVAL_THRESHOLD}.</li>
     * </ul>
     * Reglas:
     * <ul>
     *   <li>404 si la inscripción no existe; 400 si está cancelada (DROPPED).</li>
     *   <li>403 si el docente no está asignado a la materia de la inscripción.</li>
     *   <li>400 si la condición es inválida o la nota no corresponde a la condición.</li>
     *   <li>Una inscripción REGULAR puede transicionar a APROBADA/PROMOCION (200);
     *       una condición ya final es inmutable (409).</li>
     *   <li>APROBADA/PROMOCION dejan la inscripción en COMPLETED.</li>
     * </ul>
     *
     * @param teacherService servicio usado para verificar la asignación docente-materia
     */
    public GradeResult registerTeacherGrade(Long teacherId, Integer enrollmentId, String condition, Double grade, TeacherService teacherService) {
        try {
            if (enrollmentId == null) {
                return GradeResult.fail(400, "El campo enrollmentId es obligatorio.");
            }

            // La inscripción debe existir
            Enrollment enrollment = Enrollment.findById(enrollmentId);
            if (enrollment == null) {
                return GradeResult.fail(404, "No existe la inscripción con id " + enrollmentId + ".");
            }

            // No se puede calificar una inscripción cancelada
            if ("DROPPED".equals(enrollment.getStatus())) {
                return GradeResult.fail(400, "La inscripción está cancelada (DROPPED).");
            }

            // El docente debe estar asignado a la materia de la inscripción
            Long subjectId = enrollment.getSubjectId();
            if (!teacherService.verifyAssignment(teacherId, subjectId)) {
                return GradeResult.fail(403, "El docente no está asignado a la materia de esta inscripción.");
            }

            // Validar la condición académica
            if (condition == null || !VALID_CONDITIONS.contains(condition)) {
                return GradeResult.fail(400, "La condición debe ser una de: REGULAR, APROBADA, PROMOCION.");
            }

            // Validar la nota según la condición
            if (COND_REGULAR.equals(condition)) {
                grade = null; // REGULAR no lleva nota final
            } else {
                if (grade == null || grade < 0.0 || grade > 10.0) {
                    return GradeResult.fail(400, "La calificación debe estar entre 0.00 y 10.00.");
                }
                if (grade < APPROVAL_THRESHOLD) {
                    return GradeResult.fail(400, "No se puede registrar " + condition
                        + " con una nota menor a " + APPROVAL_THRESHOLD + " (el alumno permanece REGULAR).");
                }
            }

            // Manejo de evaluación existente: permitir la transición REGULAR -> final
            Evaluation eval = repository.findByEnrollmentId(enrollmentId);
            int statusCode;
            if (eval != null) {
                if (!COND_REGULAR.equals(eval.getCondition())) {
                    return GradeResult.fail(409, "Esta inscripción ya tiene una condición final ("
                        + eval.getCondition() + ").");
                }
                if (COND_REGULAR.equals(condition)) {
                    return GradeResult.fail(409, "La inscripción ya figura como REGULAR.");
                }
                // Transición REGULAR -> APROBADA/PROMOCION: se completa la evaluación existente
                eval.setCondition(condition);
                eval.setEvaluationGrade(grade);
                eval.setEvaluationDate(new java.sql.Date(System.currentTimeMillis()));
                eval.saveIt();
                statusCode = 200;
            } else {
                eval = new Evaluation();
                eval.setEvaluationEnrollementId(enrollmentId);
                eval.setCondition(condition);
                eval.setEvaluationGrade(grade);
                eval.setEvaluationDate(new java.sql.Date(System.currentTimeMillis()));
                repository.createEvaluation(eval);
                statusCode = 201;
            }

            // APROBADA/PROMOCION cierran la materia: la inscripción pasa a COMPLETED
            if (COMPLETING_CONDITIONS.contains(condition)) {
                enrollment.setStatus("COMPLETED");
                enrollment.saveIt();
            }

            return GradeResult.ok(statusCode, eval, enrollment.getStatus());
        } catch (Exception e) {
            System.err.println("Error al registrar la calificación: " + e.getMessage());
            e.printStackTrace();
            return GradeResult.fail(500, "Error interno al registrar la calificación.");
        }
    }
}
