package com.is1.proyecto.dto;

/**
 * DTO unificado para la transferencia de datos de Subject.
 * Combina los campos de la entidad Subject (code, studyPlan, career) con los
 * campos del historial academico del estudiante (status, grade, period).
 *
 * Constructores disponibles
 * Constructor completo (11 parametros): todos los campos
 * Constructor parcial (5 parametros): solo historial academico (code y plan son null)
 * Constructor parcial (7 parametros): solo entidad Subject (status, grade, period son null)
 */
public class SubjectDTO {

    private final Long subjectId;
    private final String subjectName;
    private final String status;
    private final Double grade;
    private final String period;
    private final String code;
    private final Integer studyPlanId;
    private final String studyPlanName;
    private final Integer careerId;
    private final String careerName;

    /**
     * Constructor completo: todos los campos.
     * 
     * @param subjectId    ID de la materia (Long)
     * @param subjectName  Nombre de la materia
     * @param status       Estado de la inscripcion (ENROLLED, COMPLETED, DROPPED) — null si no aplica
     * @param grade        Calificacion — null si no aplica
     * @param period       Periodo academico — null si no aplica
     * @param code         Codigo unico de la materia — null si no aplica
     * @param studyPlanId  ID del plan de estudio — null si no aplica
     * @param studyPlanName Nombre del plan de estudio — null si no aplica
     * @param careerId     ID de la carrera — null si no aplica
     * @param careerName   Nombre de la carrera — null si no aplica
     */
    public SubjectDTO(Long subjectId, String subjectName, String status, Double grade, String period,
                      String code, Integer studyPlanId, String studyPlanName,
                      Integer careerId, String careerName) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.status = status;
        this.grade = grade;
        this.period = period;
        this.code = code;
        this.studyPlanId = studyPlanId;
        this.studyPlanName = studyPlanName;
        this.careerId = careerId;
        this.careerName = careerName;
    }

    // ──── Getters ────

    public Long getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public String getStatus() { return status; }
    public Double getGrade() { return grade; }
    public String getPeriod() { return period; }
    public String getCode() { return code; }
    public Integer getStudyPlanId() { return studyPlanId; }
    public String getStudyPlanName() { return studyPlanName; }
    public Integer getCareerId() { return careerId; }
    public String getCareerName() { return careerName; }
}
