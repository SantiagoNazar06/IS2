package com.is1.proyecto.repositories;

import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.ports.out.PersonRepositoryInterface;

import java.util.List;
import java.util.Map;
import org.javalite.activejdbc.Base; 

/**
 * Repositorio encargado de gestionar las operaciones CRUD de Person.
 * 
 * Cada método se encarga de abrir y cerrar la conexión a la base de datos
 * utilizando DBConfigSingleton, garantizando que las operaciones se ejecuten
 * con una conexión activa y evitando fugas de recursos.
 * 
 * Utiliza ActiveJDBC para la persistencia de datos.
 */
public class PersonRepository implements PersonRepositoryInterface{

    private DBConfigSingleton db;

    public PersonRepository(){
        this.db = DBConfigSingleton.getInstance();
    }

    /**
     * Verifica si la tabla "persons" existe en la base de datos.
     * 
     * Consulta la tabla interna sqlite_master, que contiene el esquema de la BD.
     * Retorna true si la tabla existe, false en caso contrario.
     *
     * @return true si la tabla persons existe, false si no
     */
    public boolean personsTableExists(){
        db.openConnection();
        try{
            String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name = ?";
            
            // Base.firstCell devuelve el primer valor de la query o null si no hay resultados
            return org.javalite.activejdbc.Base.firstCell(sql, "persons") != null;

        } finally {
            db.closeConnection();
        }
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
        
        db.openConnection();
        try{
            person.saveIt();
            return person;
        }finally{
            db.closeConnection(); // Asegura liberación de la conexión
        }
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
        db.openConnection();
        try{
            return Person.findById(id); 
        }finally{
            db.closeConnection();
        }
    }

    /**
     * Obtiene todas las personas almacenadas en la base de datos.
     * La conexión se abre antes de la consulta y se cierra al finalizar.
     *
     * @return lista de todas las personas
     */
    @Override
    public List<Person> findAll(){
        db.openConnection();
        try{
            List<Person> result = Person.findAll().load();
            return result; 
        }finally{
            db.closeConnection();
        }
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
        db.openConnection();
        try{
            //Chequeos de parametros correctos
            if(id <= 0 || id == null || data == null || data.isEmpty()){
                return false;
            }
            //No permitir la modificación del id de Person
            if(data.containsKey("id")){
                data.remove("id");
            }

            Person currPerson = Person.findById(id);

            // Si la persona no existe
            if(currPerson == null){
             return false;
            }

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                currPerson.set(entry.getKey(), entry.getValue());
            }
        
            return currPerson.saveIt();            
        }finally{
            db.closeConnection();
        }
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
        db.openConnection();
        try{
            Person currPerson = Person.findById(id);
            if(currPerson == null){
                return false;
            }
            // Elimina la entidad (true indica eliminación en cascada)
            currPerson.delete(true);
            return true;
        }finally{
            db.closeConnection();
        }
        
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
        db.openConnection();
        try{
            return Person.findFirst("dni = ?", dni);
        }finally{
            db.closeConnection();
        }
    }

    /**
     * Elimina la tabla persons.
     * Por el momento util solo para realizar testing
     */
    public void deleteAll() {
        db.openConnection();
        try {
            Base.exec("DELETE FROM persons");
        } finally {
            db.closeConnection();
        }
    }
}