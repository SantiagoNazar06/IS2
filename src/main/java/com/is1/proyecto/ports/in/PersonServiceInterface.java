package com.is1.proyecto.ports.in;

import com.is1.proyecto.dto.PersonDTO;
import com.is1.proyecto.models.Person;

//Interface necesaria para seguir la hexagonal architecture.
public interface PersonServiceInterface {
    Person create(PersonDTO data);

    Person update(Long id, PersonDTO data);

    boolean delete(Long id);
}
