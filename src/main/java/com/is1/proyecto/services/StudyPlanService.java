package com.is1.proyecto.services;

import com.is1.proyecto.dto.StudyPlanDTO;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.models.Career;
import com.is1.proyecto.models.StudyPlan;
import com.is1.proyecto.repositories.StudyPlanRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de StudyPlan: Contiene la logica de negocio para la gestion de planes de estudio.
 * Se encarga de validar los datos antes de permitir que el repositorio toque la base de datos.
 * <p>
 * Un plan de estudio agrupa materias bajo una carrera en un ano especifico.
 * Relacion: Career "1" -- "0..*" StudyPlan "1..*" -- "1..*" Subject
 * </p>
 */
public class StudyPlanService {

    private final StudyPlanRepository repository;

    public StudyPlanService(StudyPlanRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra un nuevo plan de estudio con validaciones previas.
     *
     * @param name     Nombre del plan (ej: "Plan 2024")
     * @param year     Ano del plan
     * @param careerId ID de la carrera a la que pertenece
     * @return StudyPlanDTO con los datos del plan creado
     * @throws ValidationException si alguna validacion falla
     */
    public StudyPlanDTO registerStudyPlan(String name, int year, int careerId) {
        // Validacion: nombre obligatorio
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("El nombre del plan de estudio es obligatorio", "name");
        }

        // Validacion: año valido
        if (year <= 0) {
            throw new ValidationException("El ano del plan de estudio debe ser mayor a cero", "year");
        }

        // Validacion: carrera existe
        Career career = Career.findById(careerId);
        if (career == null) {
            throw new ValidationException("La carrera con ID " + careerId + " no existe", "careerId");
        }

        StudyPlan studyPlan = new StudyPlan();
        studyPlan.setName(name.trim());
        studyPlan.setYear(year);
        studyPlan.setCareerId(careerId);

        repository.create(studyPlan);
        return toDTO(studyPlan);
    }

    /**
     * Actualiza un plan de estudio existente.
     *
     * @param id       ID del plan a actualizar
     * @param name     Nuevo nombre (opcional, null para no cambiar)
     * @param year     Nuevo ano (opcional, null para no cambiar)
     * @param careerId Nueva carrera (opcional, null para no cambiar)
     * @return StudyPlanDTO con los datos actualizados
     * @throws ValidationException si alguna validacion falla
     */
    public StudyPlanDTO updateStudyPlan(int id, String name, Integer year, Integer careerId) {
        StudyPlan studyPlan = repository.findById(id);
        if (studyPlan == null) {
            throw new ValidationException("El plan de estudio con ID " + id + " no existe", "id");
        }

        if (name != null && !name.trim().isEmpty()) {
            studyPlan.setName(name.trim());
        }

        if (year != null && year > 0) {
            studyPlan.setYear(year);
        }

        if (careerId != null) {
            Career career = Career.findById(careerId);
            if (career == null) {
                throw new ValidationException("La carrera con ID " + careerId + " no existe", "careerId");
            }
            studyPlan.setCareerId(careerId);
        }

        repository.update(studyPlan);
        return toDTO(studyPlan);
    }

    /**
     * Elimina un plan de estudio por su ID.
     *
     * @param id ID del plan a eliminar
     * @throws ValidationException si el plan no existe
     */
    public void deleteStudyPlan(int id) {
        boolean deleted = repository.deleteById(id);
        if (!deleted) {
            throw new ValidationException("El plan de estudio con ID " + id + " no existe", "id");
        }
    }

    /**
     * Obtiene todos los planes de estudio, opcionalmente filtrados por carrera.
     *
     * @param careerId ID de carrera para filtrar (null para obtener todos)
     * @return Lista de StudyPlanDTO
     */
    public List<StudyPlanDTO> getAllStudyPlans(Integer careerId) {
        List<StudyPlan> plans;
        if (careerId != null) {
            plans = repository.findByCareerId(careerId);
        } else {
            plans = repository.findAll();
        }
        return plans.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un plan de estudio por su ID.
     *
     * @param id ID del plan
     * @return StudyPlanDTO
     * @throws ValidationException si el plan no existe
     */
    public StudyPlanDTO getStudyPlanById(int id) {
        StudyPlan studyPlan = repository.findById(id);
        if (studyPlan == null) {
            throw new ValidationException("El plan de estudio con ID " + id + " no existe", "id");
        }
        return toDTO(studyPlan);
    }

    private StudyPlanDTO toDTO(StudyPlan studyPlan) {
        String careerName = null;
        Integer careerId = studyPlan.getCareerId();
        if (careerId != null) {
            Career career = studyPlan.getCareer();
            if (career != null) {
                careerName = career.getCareerName();
            }
        }
        return new StudyPlanDTO(
                studyPlan.getId(),
                studyPlan.getName(),
                studyPlan.getYear(),
                careerId,
                careerName
        );
    }
}
