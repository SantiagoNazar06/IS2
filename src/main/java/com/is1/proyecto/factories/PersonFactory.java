package com.is1.proyecto.factories;

import com.is1.proyecto.models.Person;

public class PersonFactory implements PersonFactoryInterface{
    @Override
    public Person create() {
        return new Person();
    }
}
