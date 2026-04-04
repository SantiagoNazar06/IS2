package com.is1.proyecto.services;

import java.util.List;
import com.is1.proyecto.models.Career;
import com.is1.proyecto.repositories.CareerRepository;

/**
 * Servicio de Carrera: Contiene la lógica de negocio.
 * Se encarga de validar los datos antes de permitir que el repositorio toque la base de datos.
 */
public class CareerService {

    private final CareerRepository repository;

    public CareerService(CareerRepository repo){
        repository = repo;
    }

    /**
     * Clase interna para estructurar las respuestas del servicio.
     * Ayuda a comunicar éxito, error, mensajes y códigos HTTP a las rutas.
     */
    public static class CareerRegisterResult {
        public final boolean success;
        public final int statusCode;
        public final String redirectUrl;
        public final String message;

        private CareerRegisterResult(boolean success, int statusCode, String redirectUrl, String message) {
            this.success = success;
            this.statusCode = statusCode;
            this.redirectUrl = redirectUrl;
            this.message = message;
        }

        // Genera una respuesta de éxito (201 Created)
        public static CareerRegisterResult ok(String message) {
            return new CareerRegisterResult(true, 201, "/register_career", message);
        }

        // Genera una respuesta de error genérico (500 Internal Server Error)
        public static CareerRegisterResult error(String message) {
            return new CareerRegisterResult(false, 500, "/register_career", message);
        }

        // Respuesta específica para cuando se intenta registrar un nombre que ya existe (400 Bad Request)
        public static CareerRegisterResult duplicate(String nameCareer) {
            return new CareerRegisterResult(false, 400, "/register_career", "Ya existe una carrera con el nombre " + nameCareer);
        }
    }

    /**
     * Lógica para registrar una nueva carrera con validaciones previas.
     */
    public CareerRegisterResult registerCareer(String nameCareer, int duration){
        try {
            // Valida que el nombre no esté repetido
            Career career = repository.findByName(nameCareer);
            if(career != null){
                return CareerRegisterResult.duplicate(nameCareer);
            }
            
            // Valida que la duración sea un tiempo lógico
            if(duration <= 0){
                return CareerRegisterResult.error("La duración de la carrera debe ser mayor a cero");
            }

            // Crea y guarda la nueva entidad
            career = new Career();
            career.setCareerDuration(duration);
            career.setCareerName(nameCareer);
            repository.create(career);

            return CareerRegisterResult.ok("Carrera registrada exitosamente: " + nameCareer);

        } catch (Exception e) {
            System.err.println("Error al registrar la carrera: " + e.getMessage());
            e.printStackTrace();
            return CareerRegisterResult.error("Error interno al crear la carrera.");
        }
    }

    /**
     * Lógica para actualizar una carrera existente.
     */
    public CareerRegisterResult updateCareer(int id, String newName, int newDuration){
        try {
            Career career = repository.findById(id);

            // Verifica que la carrera exista antes de editar
            if (career == null) {
                return CareerRegisterResult.error("Carrera no encontrada");
            }

            if (newDuration <= 0) {
                return CareerRegisterResult.error("La duración debe ser mayor a cero");
            }

            // Verifica que el nuevo nombre no lo tenga otra carrera distinta
            Career existCareerName = repository.findByName(newName);
            if (existCareerName != null && !(existCareerName.getId().equals(id))) {
                return CareerRegisterResult.error("Ya existe otra carrera con ese nombre");
            }

            career.setCareerName(newName);
            career.setCareerDuration(newDuration);
            repository.update(career);

            return CareerRegisterResult.ok("Carrera actualizada exitosamente");

        } catch (Exception e) {
            System.err.println("Error al actualizar la carrera: " + e.getMessage());
            e.printStackTrace();
            return CareerRegisterResult.error("Error interno al actualizar.");
        }
    }

    /**
     * Lógica para eliminar una carrera por su ID.
     */
    public CareerRegisterResult deleteCareer(int id) {
        try {
            Career career = repository.findById(id);

            // Valida existencia antes de intentar borrar
            if (career == null) {
                return CareerRegisterResult.error("La carrera que intenta eliminar no existe.");
            }

            repository.delete(career);
            return CareerRegisterResult.ok("Carrera eliminada exitosamente");

        } catch (Exception e) {
            System.err.println("Error al eliminar la carrera: " + e.getMessage());
            e.printStackTrace();
            return CareerRegisterResult.error("Error interno al eliminar la carrera.");
        }
    }

    // Obtiene todas las carreras delegando la consulta al repositorio
    public List<Career> getAllCareers() {
        return repository.findAll();
    }
}