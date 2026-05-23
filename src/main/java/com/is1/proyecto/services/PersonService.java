package com.is1.proyecto.services;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.ports.in.PersonServiceInterface;
import com.is1.proyecto.ports.out.PersonRepositoryInterface;
import com.is1.proyecto.dto.PersonDTO;
import com.is1.proyecto.exceptions.PersonNotFoundException;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.factories.PersonFactoryInterface;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

public class PersonService implements PersonServiceInterface{
    private PersonRepositoryInterface repository;
    private PersonFactoryInterface personFactory;
    //Define el patrón posible para los emails.
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    public PersonService(PersonRepositoryInterface repository, PersonFactoryInterface personFactory){
        this.repository = repository;
        this.personFactory = personFactory;
    }

    
    /**
     * Crea una nueva persona en el sistema a partir de los datos proporcionados.
     * Realiza las siguientes validaciones antes de persistir:
     * - El DNI no puede ser nulo ni vacío, y debe tener un formato válido.
     * - El DNI no debe estar registrado previamente en el sistema.
     * - El email, si se proporciona, debe tener un formato válido.
     * - El nombre y el apellido son obligatorios.
     *
     * @param data DTO con los datos de la persona a registrar.
     * @return La entidad {@link Person} creada y persistida.
     * @throws ValidationException si algún dato obligatorio falta o es inválido,
     *                             o si el DNI ya se encuentra registrado.
     */
    @Override
    public Person create(PersonDTO data) {
        // Validar que el DNI no sea nulo ni esté vacío
        if (data.getDni() == null || data.getDni().isBlank()) {
            throw new ValidationException("Datos inválidos", "Se requiere dni");
        }
        // Validar formato del DNI
        validateDni(data.getDni());
        // Verificar que no exista una persona registrada con el mismo DNI
        if (repository.findByDni(data.getDni()) != null) {
            throw new ValidationException("Datos inválidos", "No es posible realizar el registro.");
        }
        // Validar formato del email (si fue proporcionado)
        validateEmail(data.getEmail());
        // Validar que el nombre no sea nulo ni esté vacío
        if (data.getFirstName() == null || data.getFirstName().isBlank()) {
            throw new ValidationException("Datos inválidos", "Se requiere nombre");
        }
        // Validar que el apellido no sea nulo ni esté vacío
        if (data.getLastName() == null || data.getLastName().isBlank()) {
            throw new ValidationException("Datos inválidos", "Se requiere apellido");
        }
        // Construir la entidad Person con los datos validados
        Person currPerson = personFactory.create();
        currPerson.setDni(data.getDni());
        currPerson.setFirstName(data.getFirstName());
        currPerson.setLastName(data.getLastName());
        // Asignar campos opcionales solo si fueron proporcionados
        if (data.getEmail() != null) currPerson.setEmail(data.getEmail());
        if (data.getPhone() != null) currPerson.setPhone(data.getPhone());
        // Persistir la nueva persona en el repositorio
        repository.create(currPerson);
        return currPerson;
    }

    /**
     * Actualiza parcialmente los datos de una persona existente identificada por su ID.
     * Solo se actualizan los campos que estén presentes (no nulos) en el DTO recibido.
     * Se requiere que al menos un campo sea proporcionado para realizar la operación.
     *
     * @param id   Identificador único de la persona a actualizar.
     * @param data DTO con los campos a modificar. Los campos nulos se ignoran.
     * @return La entidad {@link Person} con los datos actualizados.
     * @throws PersonNotFoundException si no existe una persona con el ID proporcionado.
     * @throws ValidationException     si el DTO no contiene ningún campo a actualizar.
     */
    @Override
    public Person update(Long id, PersonDTO data) {
        // Verificar que exista una persona con el ID indicado
        if (repository.findById(id) == null) {
            throw new PersonNotFoundException("Persona no existente en el sistema");
        }
        // Construir el mapa de campos a actualizar incluyendo solo los no nulos
        Map<String, Object> fields = new HashMap<>();
        if (data.getFirstName() != null) fields.put("firstname", data.getFirstName());
        if (data.getLastName()  != null) fields.put("lastname",  data.getLastName());
        if (data.getEmail()     != null) fields.put("email",     data.getEmail());
        if (data.getPhone()     != null) fields.put("phone",     data.getPhone());
        // Asegurarse de que haya al menos un campo para actualizar
        if (fields.isEmpty()) {
            throw new ValidationException("Datos inválidos", "Se requieren datos para realizar update");
        }
        // Ejecutar la actualización parcial en el repositorio
        repository.update(id, fields);
        // Retornar la entidad actualizada
        return repository.findById(id);
    }

    /**
     * Elimina del sistema a la persona identificada por el ID proporcionado.
     * Retorna {@code false} si la persona no existe, sin lanzar excepción,
     * lo que permite un comportamiento idempotente ante eliminaciones repetidas.
     *
     * @param id Identificador único de la persona a eliminar. No puede ser nulo ni negativo.
     * @return {@code true} si la persona fue eliminada exitosamente;
     *         {@code false} si no existía en el sistema.
     * @throws PersonNotFoundException si el ID es nulo o tiene un valor negativo.
     */
    @Override
    public boolean delete(Long id) {
        // Validar que el ID sea un valor aceptable (no nulo y no negativo)
        if (id == null || id < 0) {
            throw new PersonNotFoundException("El id no es valido");
        }
        // Si la persona no existe, retornar false sin lanzar excepción
        if (repository.findById(id) == null) {
            return false;
        }
        // Ejecutar la eliminación y retornar el resultado del repositorio
        return repository.delete(id);
    }

    /**
     * Método que realiza la validacion de email por medio de regex.
     * El email no debe ser nulo.
     * @param email
     */
    private void validateEmail(String email){
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Datos inválidos", "Formato de email inválido");
        }
    }

    /**
     * Método que realiza la validación de dni.
     * EL dni debe contener solo dígitos.
     * El dni no debe ser menor a 10000000 ni mayor a 99999999.
     * @param dni
     * @return dni si éste es válido
     */
    public static String validateDni(String dni) {
        //(sin letras, guiones, espacios internos)
        if (!dni.matches("\\d+")) {
            throw new IllegalArgumentException("El DNI solo debe contener dígitos.");
        }

        //(boundary superior de caracteres)
        if (dni.length() > 8) {
            throw new IllegalArgumentException("El DNI no puede tener más de 8 dígitos.");
        }

        int valor = Integer.parseInt(dni);

        // Boundary inferior
        if (valor < 10000000) {
            throw new ValidationException("Dato inválido","El DNI debe ser mayor a 9.999.999.");
        }

        // Boundary superior
        if (valor > 99999999) {
            throw new ValidationException("Dato inválido","El DNI no puede superar 99.999.999.");
        }

        return dni;
    }
}
