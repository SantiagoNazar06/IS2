package com.is1.proyecto.repositories;

import java.util.List;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import com.is1.proyecto.models.User;

public class UserRepository {

    /**
     * Busca un usuario por su nombre de usuario.
     * Útil para el proceso de Login.
     */
    public User findByUsername(String username) {
        return User.findFirst("username = ?", username); //
    }

    /**
     * Crea un usuario hasheando la contraseña antes de persistir.
     */
    public User create(User user) {
        String hashed = hashPassword(user.getPassword());
        user.setPassword(hashed); 
        user.saveIt();
        return user;
    }

    public User findById(Object id) {
        return User.findById(id);
    }

    public List<User> findAll() {
        return User.findAll(); 
    }

    /**
     * Actualiza campos específicos de un usuario mediante un Map.
     */
    public boolean update(Long id, Map<String, Object> data) {
        User user = findById(id);
        if (user != null) {
            // Si el mapa contiene "password", lo hasheamos antes de actualizar
            if (data.containsKey("password")) {
                data.put("password", hashPassword(data.get("password").toString()));
            }
            user.fromMap(data);
            return user.saveIt();
        }
        return false;
    }

    public boolean delete(Long id) {
        User user = findById(id);
        return user != null && user.delete(); 
    }

    /**
     * Retorna una lista de usuarios filtrados por su rol (ADMIN, STUDENT, TEACHER).
     */
    public List<User> findByRole(String role) {
        return User.where("role = ?", role); 
    }

    /**
     * Helper para generar el hash de la contraseña.
     */
    private String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt());  
    }
}