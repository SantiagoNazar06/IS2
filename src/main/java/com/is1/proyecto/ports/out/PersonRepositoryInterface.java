package com.is1.proyecto.ports.out;

import java.util.Map;
import java.util.List;
import com.is1.proyecto.models.Person;

public interface PersonRepositoryInterface {
    Person create(Person person);
    Person findById(Long id);
    List<Person> findAll();
    boolean update(Long id, Map<String, Object> data);
    boolean delete(Long id);
    Person findByDni(String dni);
}
