package com.is1.proyecto.dto;

/**
 * DTO para la transferencia de datos de Subject en las respuestas JSON.
 * Desacopla el modelo ActiveJDBC de la capa de presentacion.
 * <p>
 * Incluye informacion del plan de estudio y, a traves de el, de la carrera.
 * Subject → StudyPlan → Career
 * </p>
 */
public class SubjectDTO {

    private final Integer id;
    private final String code;
    private final String name;
    private final Integer studyPlanId;
    private final String studyPlanName;
    private final Integer careerId;
    private final String careerName;

    public SubjectDTO(Integer id, String code, String name,
                      Integer studyPlanId, String studyPlanName,
                      Integer careerId, String careerName) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.studyPlanId = studyPlanId;
        this.studyPlanName = studyPlanName;
        this.careerId = careerId;
        this.careerName = careerName;
    }

    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getStudyPlanId() { return studyPlanId; }
    public String getStudyPlanName() { return studyPlanName; }
    public Integer getCareerId() { return careerId; }
    public String getCareerName() { return careerName; }
}
