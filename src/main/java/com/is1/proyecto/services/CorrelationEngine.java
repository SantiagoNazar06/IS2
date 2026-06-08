package com.is1.proyecto.services;

import com.is1.proyecto.models.Condition;
import com.is1.proyecto.models.ConditionType;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.services.ValidationResult.MissingPrerequisite;

import org.javalite.activejdbc.Base;

import java.util.ArrayList;
import java.util.List;

public class CorrelationEngine {

    /**
     * Verifica si un estudiante cumple todas las correlatividades para inscribirse a una materia.
     * Consulta la tabla conditions para obtener los prerequisitos de la materia y
     * verifica en evaluations que el estudiante los haya completado con la condición requerida.
     */
    public ValidationResult canEnroll(long studentId, long subjectId) {
        List<Condition> prerequisites = Condition.where("subject_id = ?", subjectId);
        List<MissingPrerequisite> missing = new ArrayList<>();

        for (Condition prerequisite : prerequisites) {
            Integer prereqSubjectId = prerequisite.getPrerequisiteSubjectId();
            ConditionType conditionType = prerequisite.getType();

            // La correlativa se resuelve uniendo evaluations -> enrollments y mirando la condición:
            //   - APROBADA: la previa debe estar APROBADA o PROMOCION (materia cerrada con nota).
            //   - REGULAR:  alcanza con REGULAR, APROBADA o PROMOCION.
            String inClause = (conditionType == ConditionType.APROBADA)
                ? "('APROBADA', 'PROMOCION')"
                : "('REGULAR', 'APROBADA', 'PROMOCION')";
            String sql = "SELECT COUNT(*) FROM evaluations ev "
                + "JOIN enrollments en ON en.id = ev.enrollment_id "
                + "WHERE en.student_id = ? AND en.subject_id = ? "
                + "AND ev.condition_type IN " + inClause;
            long passed = ((Number) Base.firstCell(sql, studentId, prereqSubjectId)).longValue();

            if (passed == 0) {
                Subject requiredSubject = Subject.findById(prereqSubjectId);
                String subjectName = requiredSubject != null
                    ? requiredSubject.getSubjectName()
                    : "Materia " + prereqSubjectId;
                missing.add(new MissingPrerequisite(prereqSubjectId, subjectName));
            }
        }

        if (missing.isEmpty()) {
            return ValidationResult.ok();
        }

        return ValidationResult.fail(
            "MISSING_PREREQUISITES",
            "El estudiante no cumple con las correlatividades requeridas",
            missing
        );
    }
}
