package com.is1.proyecto.services;

import java.util.Map;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.repositories.PersonRepository;

public class PersonService {
    private PersonRepository repository;

    public PersonService(PersonRepository repository){
        this.repository = repository;
    }

    public Person create(Map<String, Object> data){
        return null;
    }

    public Person update(Long id, Map<String, Object> data){
        return null;
    }

    public boolean delete(Long id){
        return false;
    }
}
