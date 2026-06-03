package com.is1.proyecto.dto;

/**
 * DTO para la transferencia de datos de StudyPlan en las respuestas JSON.
 * Incluye informacion basica del plan y de la carrera asociada.
 */
public class StudyPlanDTO {

    private final Integer id;
    private final String name;
    private final Integer year;
    private final Integer careerId;
    private final String careerName;

    public StudyPlanDTO(Integer id, String name, Integer year, Integer careerId, String careerName) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.careerId = careerId;
        this.careerName = careerName;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public Integer getYear() { return year; }
    public Integer getCareerId() { return careerId; }
    public String getCareerName() { return careerName; }
}
