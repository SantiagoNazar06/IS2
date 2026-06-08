package com.is1.proyecto.repositories;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.ports.out.PersonRepositoryInterface;

import java.util.List;
import java.util.Map;
import org.javalite.activejdbc.Base;

// Repositorio CRUD de Person usando ActiveJDBC.
// La conexion a la BD es manejada por DBConnectionFilter a nivel de request HTTP,
// por lo que no se abre ni cierra conexion manualmente.
public class PersonRepository implements PersonRepositoryInterface{
    
    /**
     * Verifica si la tabla "persons" existe en la base de datos.
     * 
     * Consulta la tabla interna sqlite_master, que contiene el esquema de la BD.
     * Retorna true si la tabla existe, false en caso contrario.
     *
     * @return true si la tabla persons existe, false si no
     */
    public boolean personsTableExists(){
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name = ?";
        return Base.firstCell(sql, "persons") != null;
    }

    /**
     * Crea una nueva persona en la base de datos.
     * Abre una conexión antes de la operación y la cierra al finalizar.
     *
     * @param person instancia de Person con los datos a persistir
     * @return la persona creada
     */
    @Override
    public Person create(Person person){
        person.saveIt();
        return person;
    }

    /**
     * Busca una persona por su ID.
     * Requiere una conexión activa para ejecutar la consulta.
     *
     * @param id identificador de la persona
     * @return la persona encontrada o null si no existe
     */
    @Override
    public Person findById(Long id){
        return Person.findById(id);
    }

    @Override
    public List<Person> findAll(){
        return Person.findAll().load();
    }

    /**
     * Actualiza una persona existente.
     * Primero verifica su existencia en la base de datos.
     * 
     * Utiliza saveIt(), que realiza un UPDATE si el registro ya existe.
     *
     * @param updatedPerson persona con los datos actualizados
     * @return true si la actualización fue exitosa, false si la persona no existe
     */
    @Override
    public boolean update(Long id, Map<String, Object> data){
        if(id == null || id <= 0 || data == null || data.isEmpty()){
            return false;
        }
        if(data.containsKey("id")){
            data.remove("id");
        }

        Person currPerson = Person.findById(id);
        if(currPerson == null){
            return false;
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            currPerson.set(entry.getKey(), entry.getValue());
        }

        return currPerson.saveIt();
    }

    /**
     * Elimina una persona existente de la base de datos.
     * Primero verifica su existencia.
     *
     * @param person persona a eliminar
     * @return true si la eliminación fue exitosa, false si la persona no existe
     */
    @Override
    public boolean delete(Long id){
        Person currPerson = Person.findById(id);
        if(currPerson == null){
            return false;
        }
        currPerson.delete(true);
        return true;
    }

    /**
     * Busca a una persona por su DNI.
     * Utiliza findFirst para obtener una única persona que coincida con el DNI.
     * Si no existe ninguna coincidencia, retorna null.
     * 
     * @param dni documento de identidad de la persona.
     * @return la persona encontrada o null si no existe.
     */
    @Override
    public Person findByDni(String dni){
        return Person.findFirst("dni = ?", dni);
    }

    // Solo para testing: limpia la tabla persons
    public void deleteAll() {
        Base.exec("DELETE FROM persons");
    }
}
