package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("careers")
@IdName("id_careers") //Como ActiveJDBC busca por defecto "id" lo modifico para que vea en "id_careers"
public class Career extends Model{

    public Integer getId() {
        return getInteger("id_careers");
    }

    public String getCareerName(){
        return getString("career_name");
    }

    public void setCareerName(String name){
        set("career_name", name);
    }

    public Integer getCareerDuration(){
        return getInteger("career_duration");
    }

    public void setCareerDuration(Integer duration){
        set("career_duration", duration);
    }
    
}
