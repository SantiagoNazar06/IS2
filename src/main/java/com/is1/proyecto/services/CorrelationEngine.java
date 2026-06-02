package com.is1.proyecto.services;

import com.is1.proyecto.models.Condition;
import com.is1.proyecto.models.ConditionType;
import com.is1.proyecto.models.Evaluation;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.services.ValidationResult.MissingPrerequisite;

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
            // REGULAR -> "regular", APROBADA -> "aprobado" (matches evaluations.condition_type)
            String requiredCondition = conditionType == ConditionType.APROBADA ? "aprobado" : "regular";

            long passed = Evaluation.count(
                "student_id = ? AND subject_id = ? AND condition_type = ?",
                studentId, prereqSubjectId, requiredCondition
            );

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
