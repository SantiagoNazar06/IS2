package com.is1.proyecto.repositories;

import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.models.Condition;
import com.is1.proyecto.models.ConditionType;
import java.util.List;

/**
 * Repositorio de Condition: encargado de la comunicacion directa con la base de datos.
 * Centraliza todas las consultas usando el modelo ActiveJDBC.
 * Sigue el mismo patron que PersonRepository con DBConfigSingleton para manejo de conexiones.
 */
public class ConditionRepository {

    private DBConfigSingleton db;

    public ConditionRepository() {
        this.db = DBConfigSingleton.getInstance();
    }

    /**
     * Busca todas las condiciones/correlatividades para una materia dada.
     *
     * @param subjectId ID de la materia
     * @return Lista de condiciones que tienen a subjectId como materia
     */
    public List<Condition> findBySubject(int subjectId) {
        db.openConnection();
        try {
            return Condition.where("subject_id = ?", subjectId);
        } finally {
            db.closeConnection();
        }
    }

    /**
     * Crea una nueva condicion/correlatividad entre dos materias.
     *
     * @param subjectId              ID de la materia que tiene el requisito
     * @param prerequisiteSubjectId  ID de la materia requisito
     * @param type                   Tipo de condicion (REGULAR o APROBADA)
     * @return La condicion creada y persistida
     */
    public Condition create(int subjectId, int prerequisiteSubjectId, ConditionType type) {
        db.openConnection();
        try {
            Condition condition = new Condition();
            condition.setSubjectId(subjectId);
            condition.setPrerequisiteSubjectId(prerequisiteSubjectId);
            condition.setType(type);
            condition.saveIt();
            return condition;
        } finally {
            db.closeConnection();
        }
    }

    /**
     * Elimina una condicion por su ID.
     *
     * @param conditionId ID de la condicion a eliminar
     * @return true si se elimino, false si no existia
     */
    public boolean delete(int conditionId) {
        db.openConnection();
        try {
            Condition condition = Condition.findById(conditionId);
            if (condition == null) {
                return false;
            }
            condition.delete(true);
            return true;
        } finally {
            db.closeConnection();
        }
    }
}
