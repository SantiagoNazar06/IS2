package com.is1.proyecto.services;

import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.models.Career;
import com.is1.proyecto.models.StudyPlan;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.repositories.SubjectRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de Subject: Contiene la logica de negocio para la gestion de materias.
 * Se encarga de validar los datos antes de permitir que el repositorio toque la base de datos.
 * <p>
 * Relacion con SRS:
 * <ul>
 *   <li><b>SRS-FUN-002</b>: Gestion de la Oferta Academica (ABMC) — "las materias asociadas"</li>
 *   <li><b>SRS-FUN-004</b>: Inscripcion a Materias — materias disponibles para inscripcion</li>
 *   <li><b>SRS-FUN-003</b>: Gestion de Correlatividades — materias tienen requisitos</li>
 * </ul>
 *
 * <h3>Estructura de Relacion</h3>
 * <pre>
 * Subject → StudyPlan → Career
 * </pre>
 * Una materia pertenece a un plan de estudio, y dicho plan pertenece a una carrera.
 * </p>
 */
public class SubjectService {

    private final SubjectRepository repository;

    public SubjectService(SubjectRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra una nueva materia con validaciones previas.
     *
     * @param code        Codigo unico de la materia (ej: "ING101")
     * @param name        Nombre de la materia (obligatorio)
     * @param studyPlanId ID del plan de estudio al que pertenece (opcional, puede ser null)
     * @return SubjectDTO con los datos de la materia creada
     * @throws ValidationException si alguna validacion falla
     */
    public SubjectDTO registerSubject(String code, String name, Integer studyPlanId) {
        // Validacion: codigo obligatorio
        if (code == null || code.trim().isEmpty()) {
            throw new ValidationException("El codigo de la materia es obligatorio", "code");
        }

        // Validacion: nombre obligatorio
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("El nombre de la materia es obligatorio", "name");
        }

        // Validacion: codigo unico
        Subject existing = repository.findByCode(code.trim());
        if (existing != null) {
            throw new ValidationException("Ya existe una materia con el codigo " + code, "code");
        }

        // Validacion: plan de estudio existe (si se especifico)
        if (studyPlanId != null) {
            StudyPlan studyPlan = StudyPlan.findById(studyPlanId);
            if (studyPlan == null) {
                throw new ValidationException("El plan de estudio con ID " + studyPlanId + " no existe", "studyPlanId");
            }
        }

        Subject subject = new Subject();
        subject.setCode(code.trim().toUpperCase());
        subject.setSubjectName(name.trim());
        if (studyPlanId != null) {
            subject.setStudyPlanId(studyPlanId);
        }

        repository.create(subject);
        return toDTO(subject);
    }

    /**
     * Actualiza una materia existente.
     *
     * @param id          ID de la materia a actualizar
     * @param code        Nuevo codigo (opcional, null para no cambiar)
     * @param name        Nuevo nombre (opcional, null para no cambiar)
     * @param studyPlanId Nuevo plan de estudio (opcional, null para no cambiar, -1 para desasignar)
     * @return SubjectDTO con los datos actualizados
     * @throws ValidationException si alguna validacion falla
     */
    public SubjectDTO updateSubject(int id, String code, String name, Integer studyPlanId) {
        Subject subject = repository.findById(id);
        if (subject == null) {
            throw new ValidationException("La materia con ID " + id + " no existe", "id");
        }

        // Validar y actualizar codigo
        if (code != null && !code.trim().isEmpty()) {
            String trimmedCode = code.trim().toUpperCase();
            Subject existing = repository.findByCode(trimmedCode);
            if (existing != null && !existing.getId().equals(id)) {
                throw new ValidationException("Ya existe otra materia con el codigo " + code, "code");
            }
            subject.setCode(trimmedCode);
        }

        // Validar y actualizar nombre
        if (name != null && !name.trim().isEmpty()) {
            subject.setSubjectName(name.trim());
        }

        // Actualizar plan de estudio
        if (studyPlanId != null && studyPlanId == -1) {
            // Desasignar plan de estudio
            subject.setStudyPlanId(null);
        } else if (studyPlanId != null) {
            StudyPlan studyPlan = StudyPlan.findById(studyPlanId);
            if (studyPlan == null) {
                throw new ValidationException("El plan de estudio con ID " + studyPlanId + " no existe", "studyPlanId");
            }
            subject.setStudyPlanId(studyPlanId);
        }

        repository.update(subject);
        return toDTO(subject);
    }

    /**
     * Elimina una materia por su ID.
     *
     * @param id ID de la materia a eliminar
     * @throws ValidationException si la materia no existe
     */
    public void deleteSubject(int id) {
        boolean deleted = repository.deleteById(id);
        if (!deleted) {
            throw new ValidationException("La materia con ID " + id + " no existe", "id");
        }
    }

    /**
     * Obtiene todas las materias, opcionalmente filtradas por plan de estudio.
     *
     * @param studyPlanId ID de plan de estudio para filtrar (null para obtener todas)
     * @return Lista de SubjectDTO
     */
    public List<SubjectDTO> getAllSubjects(Integer studyPlanId) {
        List<Subject> subjects;
        if (studyPlanId != null) {
            subjects = repository.findByStudyPlanId(studyPlanId);
        } else {
            subjects = repository.findAll();
        }
        return subjects.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una materia por su ID.
     *
     * @param id ID de la materia
     * @return SubjectDTO
     * @throws ValidationException si la materia no existe
     */
    public SubjectDTO getSubjectById(int id) {
        Subject subject = repository.findById(id);
        if (subject == null) {
            throw new ValidationException("La materia con ID " + id + " no existe", "id");
        }
        return toDTO(subject);
    }

    /**
     * Convierte un modelo Subject a SubjectDTO, resolviendo la cadena:
     * Subject → StudyPlan → Career.
     */
    private SubjectDTO toDTO(Subject subject) {
        String studyPlanName = null;
        Integer studyPlanId = subject.getStudyPlanId();
        Integer careerId = null;
        String careerName = null;

        if (studyPlanId != null) {
            StudyPlan studyPlan = subject.getStudyPlan();
            if (studyPlan != null) {
                studyPlanName = studyPlan.getName();

                // Resolver Career a traves del StudyPlan
                Integer spCareerId = studyPlan.getCareerId();
                if (spCareerId != null) {
                    careerId = spCareerId;
                    Career career = studyPlan.getCareer();
                    if (career != null) {
                        careerName = career.getCareerName();
                    }
                }
            }
        }

        return new SubjectDTO(
                subject.getId(),
                subject.getCode(),
                subject.getSubjectName(),
                studyPlanId,
                studyPlanName,
                careerId,
                careerName
        );
    }
}
