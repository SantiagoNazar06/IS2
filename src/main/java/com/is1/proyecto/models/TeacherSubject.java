package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo para la tabla intermedia teacher_subject.
 * Relaciona muchos a muchos entre profesores y materias.
 * Una materia puede tener muchos profesores asignados,
 * y un profesor puede estar asignado a muchas materias.
 */
@Table("teacher_subject")
public class TeacherSubject extends Model {

    public Long getTeacherId() {
        return getLong("teacher_id");
    }

    public void setTeacherId(Long teacherId) {
        set("teacher_id", teacherId);
    }

    public Long getSubjectId() {
        return getLong("subject_id");
    }

    public void setSubjectId(Long subjectId) {
        set("subject_id", subjectId);
    }

    public Teacher getTeacher() {
        return parent(Teacher.class);
    }

    public Subject getSubject() {
        return parent(Subject.class);
    }
}
