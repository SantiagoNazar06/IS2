package com.is1.proyecto.services;

import com.is1.proyecto.models.ConditionType;

/**
 * DTO que representa una relacion de correlatividad (prerrequisito) entre materias.
 * Se utiliza para transferir datos entre el service y las rutas,
 * incluyendo el nombre de la materia requisito obtenido del modelo Subject.
 */
public class PrerequisiteDTO {

    private Integer id;
    private Integer subjectId;
    private Integer prerequisiteSubjectId;
    private String prerequisiteSubjectName;
    private ConditionType type;

    public PrerequisiteDTO() {
    }

    public PrerequisiteDTO(Integer id, Integer subjectId, Integer prerequisiteSubjectId,
                           String prerequisiteSubjectName, ConditionType type) {
        this.id = id;
        this.subjectId = subjectId;
        this.prerequisiteSubjectId = prerequisiteSubjectId;
        this.prerequisiteSubjectName = prerequisiteSubjectName;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getPrerequisiteSubjectId() {
        return prerequisiteSubjectId;
    }

    public void setPrerequisiteSubjectId(Integer prerequisiteSubjectId) {
        this.prerequisiteSubjectId = prerequisiteSubjectId;
    }

    public String getPrerequisiteSubjectName() {
        return prerequisiteSubjectName;
    }

    public void setPrerequisiteSubjectName(String prerequisiteSubjectName) {
        this.prerequisiteSubjectName = prerequisiteSubjectName;
    }

    public ConditionType getType() {
        return type;
    }

    public void setType(ConditionType type) {
        this.type = type;
    }
}
