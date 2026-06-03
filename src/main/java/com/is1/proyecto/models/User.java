package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("users") // Esta anotación asocia explícitamente el modelo 'User' con la tabla 'users' en la DB.
public class User extends Model {

    // ActiveJDBC mapea automáticamente las columnas de la tabla 'users'
    // (como 'id', 'name', 'password', etc.) a los atributos de esta clase.
    // No necesitas declarar los campos (id, name, password) aquí como variables de instancia,
    // ya que la clase Model base se encarga de la interacción con la base de datos.

    // Opcional: Puedes agregar métodos getters y setters si prefieres un acceso más tipado,
    // aunque los métodos genéricos de Model (getString(), set(), getInteger(), etc.) ya funcionan.

    public String getName() {
        return getString("name"); // Obtiene el valor de la columna 'name'
    }

    public void setName(String name) {
        set("name", name); // Establece el valor para la columna 'name'
    }

    public String getPassword() {
        return getString("password"); // Obtiene el valor de la columna 'password'
    }

    public void setPassword(String password) {
        set("password", password); // Establece el valor para la columna 'password'
    }

    /**
     * Obtiene el rol del usuario.
     * 
     * @return Rol del usuario (ADMIN, STUDENT, TEACHER) o "STUDENT" por defecto
     */
    public String getRole() {
        String role = getString("role");
        return (role != null && !role.isEmpty()) ? role : "STUDENT";
    }

    /**
     * Establece el rol del usuario.
     * 
     * @param role Rol del usuario (ADMIN, STUDENT, TEACHER)
     */
    public void setRole(String role) {
        set("role", role);
    }

    public Long getStudentId() {
        return getLong("student_id");
    }

    public void setStudentId(Long studentId) {
        set("student_id", studentId);
    }

    /**
     * Obtiene el ID del profesor asociado, o null si no es un TEACHER.
     *
     * @return ID del profesor o null
     */
    public Long getTeacherId() {
        return getLong("teacher_id");
    }

    /**
     * Establece el ID del profesor asociado.
     *
     * @param teacherId ID del profesor
     */
    public void setTeacherId(Long teacherId) {
        set("teacher_id", teacherId);
    }

}