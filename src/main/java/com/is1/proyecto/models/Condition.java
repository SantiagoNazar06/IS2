package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("conditions")
public class Condition extends Model {

    public Integer getSubjectId() {
        return getInteger("subject_id");
    }

    public void setSubjectId(Integer subjectId) {
        set("subject_id", subjectId);
    }

    public Integer getPrerequisiteSubjectId() {
        return getInteger("prerequisite_subject_id");
    }

    public void setPrerequisiteSubjectId(Integer prerequisiteSubjectId) {
        set("prerequisite_subject_id", prerequisiteSubjectId);
    }

    public ConditionType getType() {
        String raw = getString("type");
        if (raw == null) {
            return null;
        }
        return ConditionType.fromString(raw);
    }

    public void setType(ConditionType type) {
        if (type == null) {
            set("type", null);
        } else {
            set("type", type.name());
        }
    }
}
