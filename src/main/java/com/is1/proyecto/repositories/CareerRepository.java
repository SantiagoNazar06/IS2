package com.is1.proyecto.repositories;

import java.util.List;
import com.is1.proyecto.models.Career;

/**
 * Repositorio de Carrera: Encargado de la comunicación directa con la base de datos.
 * Centraliza todas las consultas SQL usando el modelo ActiveJDBC.
 */
public class CareerRepository {

    // Busca una carrera específica por su ID primario
    public Career findById(int id){
        return Career.findById(id); 
    }

    // Busca una carrera por su nombre (útil para evitar duplicados)
    public Career findByName(String careerName){
        return Career.findFirst("career_name = ?", careerName);
    }

    // Retorna la lista completa de todas las carreras registradas
    public List<Career> findAll(){
        return Career.findAll();
    }

    // Persiste una nueva instancia de Carrera en la base de datos
    public Career create(Career car){
        car.saveIt();
        return car;
    }

    // Actualiza los datos de una carrera existente si el ID es válido
    public boolean update(Career car) {
        Career career = Career.findById(car.getId());
        if (career == null) {
            return false; // Retorna falso si la carrera no existe
        }
        car.saveIt();
        return true;
    }

    // Elimina una carrera de la base de datos validando su existencia previa
    public boolean delete(Career car){
        Career career = Career.findById(car.getId());
        if (career == null) {
            return false; 
        }
        car.delete();
        return true;
    }
}