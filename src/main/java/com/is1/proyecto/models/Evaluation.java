package com.is1.proyecto.models;

import java.sql.Date;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("evaluations")
public class Evaluation extends Model {

    public Long getStudentId() {
        return getLong("student_id");
    }

    public void setStudentId(Long studentId) {
        set("student_id", studentId);
    }

    public Long getSubjectId() {
        return getLong("subject_id");
    }

    public void setSubjectId(Long subjectId) {
        set("subject_id", subjectId);
    }

    public Date getEvaluationDate() {
        return getDate("evaluation_date");
    }

    public void setEvaluationDate(Date date) {
        set("evaluation_date", date);
    }

    public Integer getEvaluationNote() {
        return getInteger("evaluation_note");
    }

    public void setEvaluationNote(Integer note) {
        set("evaluation_note", note);
    }

    public String getConditionType() {
        return getString("condition_type");
    }

    public void setConditionType(String conditionType) {
        set("condition_type", conditionType);
    }
}
