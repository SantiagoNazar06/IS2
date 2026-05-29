package com.is1.proyecto.models;

import java.sql.Date;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("evaluations")
public class Evaluation extends Model {

    public Integer getEvaluationId(){
        return getInteger("id");
    }

    public Date getEvaluationDate(){
        return getDate("evaluation_date");
    }

    public void setEvaluationDate(Date eval_date){
        set("evaluation_date", eval_date);
    }

    public Double getEvaluationGrade(){
        return getDouble("grade");
    }

    public void setEvaluationGrade(Double grade){
        set("grade", grade);
    }

    public Integer getEvaluationEnrollementId(){
        return getInteger("enrollment_id");
    }

    public void setEvaluationEnrollementId(Integer enrollment_id){
        set("enrollment_id", enrollment_id);
    }

}
