package com.is1.proyecto.factories;

import com.is1.proyecto.models.Person;

/**
 * Abstracción para la creación de instancias de Person.
 * Permite reemplazar "new Person()" por un mock en tests unitarios,
 * evitando que ActiveJDBC intente conectarse a la base de datos
 * al momento de cargar la clase.
 */
public interface PersonFactoryInterface {
    Person create();
}