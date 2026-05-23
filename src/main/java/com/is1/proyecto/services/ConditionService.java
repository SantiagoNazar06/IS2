package com.is1.proyecto.services;

import com.is1.proyecto.models.Condition;
import com.is1.proyecto.models.ConditionType;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.repositories.ConditionRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de Condition: contiene la logica de negocio para la gestion
 * de correlatividades (prerrequisitos) entre materias.
 *
 * Responsabilidades:
 * - Listar prerrequisitos de una materia con el nombre cargado desde Subject
 * - Agregar un prerrequisito con validaciones (auto-referencia, existencia, ciclos DFS)
 * - Eliminar un prerrequisito existente
 */
public class ConditionService {

    private final ConditionRepository conditionRepository;

    public ConditionService(ConditionRepository conditionRepository) {
        this.conditionRepository = conditionRepository;
    }

    /**
     * Obtiene la lista de prerrequisitos para una materia dada.
     * Cada DTO incluye el nombre de la materia requisito cargado desde Subject.
     *
     * @param subjectId ID de la materia
     * @return Lista de PrerequisiteDTO
     */
    public List<PrerequisiteDTO> getPrerequisites(int subjectId) {
        List<Condition> conditions = conditionRepository.findBySubject(subjectId);
        return conditions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Agrega un prerrequisito a una materia, con validaciones completas:
     * <ol>
     *   <li>Valida que no sea auto-referencia</li>
     *   <li>Valida que ambas materias existan en la BD</li>
     *   <li>Detecta ciclos mediante DFS desde la materia requisito</li>
     *   <li>Crea la relacion si pasa todas las validaciones</li>
     * </ol>
     *
     * @param subjectId             ID de la materia que requiere el prerrequisito
     * @param prerequisiteSubjectId ID de la materia requisito
     * @param type                  Tipo de condicion (REGULAR o APROBADA)
     * @return PrerequisiteDTO con los datos de la relacion creada
     * @throws IllegalArgumentException si alguna validacion falla
     */
    public PrerequisiteDTO addPrerequisite(int subjectId, int prerequisiteSubjectId, ConditionType type) {
        // Validacion 1: auto-referencia
        if (subjectId == prerequisiteSubjectId) {
            throw new IllegalArgumentException("Una materia no puede ser requisito de si misma");
        }

        // Validacion 2a: existe la materia origen
        Subject subject = Subject.findById(subjectId);
        if (subject == null) {
            throw new IllegalArgumentException(
                    "La materia con ID " + subjectId + " no existe");
        }

        // Validacion 2b: existe la materia requisito
        Subject prereqSubject = Subject.findById(prerequisiteSubjectId);
        if (prereqSubject == null) {
            throw new IllegalArgumentException(
                    "La materia requisito con ID " + prerequisiteSubjectId + " no existe");
        }

        // Validacion 3: deteccion de ciclos via DFS
        if (hasCycle(prerequisiteSubjectId, subjectId, new HashSet<>())) {
            throw new IllegalArgumentException(
                    "La relacion crearia un ciclo de correlatividades");
        }

        // Creacion de la relacion
        Condition condition = conditionRepository.create(subjectId, prerequisiteSubjectId, type);
        return toDTO(condition);
    }

    /**
     * Elimina un prerrequisito por su ID.
     *
     * @param conditionId ID de la condicion a eliminar
     * @return true si se elimino correctamente, false si no existia
     */
    public boolean removePrerequisite(int conditionId) {
        return conditionRepository.delete(conditionId);
    }

    /**
     * Deteccion de ciclos mediante DFS (Depth-First Search).
     * <p>
     * Parte desde la materia requisito y recorre el grafo de correlatividades
     * hacia adelante. Si encuentra la materia destino, hay un ciclo.
     * </p>
     * <pre>
     * hasCycle(startSubjectId, targetSubjectId, visited):
     *   if startSubjectId == targetSubjectId: return true
     *   if startSubjectId in visited: return false
     *   add startSubjectId to visited
     *   for each condition where subject_id == startSubjectId:
     *     if hasCycle(condition.prerequisiteSubjectId, targetSubjectId, visited): return true
     *   return false
     * </pre>
     *
     * @param startSubjectId ID de la materia desde la que se inicia la busqueda
     * @param targetSubjectId ID de la materia que se busca (la que seria materia origen)
     * @param visited Conjunto de IDs ya visitados para evitar ciclos infinitos
     * @return true si se encuentra un ciclo, false si no
     */
    private boolean hasCycle(int startSubjectId, int targetSubjectId, Set<Integer> visited) {
        if (startSubjectId == targetSubjectId) {
            return true;
        }
        if (visited.contains(startSubjectId)) {
            return false;
        }
        visited.add(startSubjectId);

        List<Condition> conditions = conditionRepository.findBySubject(startSubjectId);
        for (Condition condition : conditions) {
            if (hasCycle(condition.getPrerequisiteSubjectId(), targetSubjectId, visited)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convierte un Condition (modelo ActiveJDBC) a PrerequisiteDTO,
     * cargando el nombre de la materia requisito desde Subject.
     *
     * @param condition La condicion a convertir
     * @return PrerequisiteDTO con los datos mapeados
     */
    private PrerequisiteDTO toDTO(Condition condition) {
        PrerequisiteDTO dto = new PrerequisiteDTO();
        dto.setId(condition.getInteger("id"));
        dto.setSubjectId(condition.getSubjectId());
        dto.setPrerequisiteSubjectId(condition.getPrerequisiteSubjectId());
        dto.setType(condition.getType());

        // Cargar el nombre de la materia requisito desde Subject
        Subject prereqSubject = Subject.findById(condition.getPrerequisiteSubjectId());
        if (prereqSubject != null) {
            dto.setPrerequisiteSubjectName(prereqSubject.getSubjectName());
        }

        return dto;
    }
}
