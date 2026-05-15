package com.is1.proyecto.services;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.repositories.PersonRepository;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

public class PersonService {
    private PersonRepository repository;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    public PersonService(PersonRepository repository){
        this.repository = repository;
    }

    public static class PersonDTO{
        public final String dni;
        public final String firstName;
        public final String lastName;
        public final String phone;
        public final String email;

        public PersonDTO(String dni, String firstName, String lastName, String phone, String email) {
            this.dni = dni;
            this.firstName = firstName;
            this.lastName = lastName;
            this.phone = phone;
            this.email = email;
        }
    }

    public Person create(PersonDTO data){
        if(data.dni == null || data.dni.isBlank()){
            throw new ValidationException("Datos inválidos", "Se requiere dni");
        }
        validateDni(data.dni);
        if(repository.findByDni(data.dni) != null){
            throw new ValidationException("Datos inválidos", "Ya existe un dni registrado");
        }

        validateEmail(data.email);
        
        if(data.firstName == null || data.firstName.isBlank()){
            throw new ValidationException("Datos inválidos", "Se requiere nombre");
        }
        if(data.lastName == null || data.lastName.isBlank()){
            throw new ValidationException("Datos inválidos", "Se requiere apellido");
        }

        Person currPerson = new Person();
        currPerson.setDni(data.dni);
        currPerson.setFirstName(data.firstName);
        currPerson.setLastName(data.lastName);
        if(data.email != null)currPerson.setEmail(data.email);
        if(data.phone != null)currPerson.setPhone(data.phone);
        repository.create(currPerson);

        return currPerson;
    }

    private void validateEmail(String email){
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Datos inválidos", "Formato de email inválido");
        }
    }

    public Person update(Long id, PersonDTO data){
        if(repository.findById(id) == null){
            throw new PersonNotFoundException("Persona no existente en el sistema");
        }
        Map<String, Object> fields = new HashMap<>();
        if (data.firstName != null) fields.put("firstname", data.firstName);
        if (data.lastName  != null) fields.put("lastname",  data.lastName);
        if (data.email     != null) fields.put("email",     data.email);
        if (data.phone    != null) fields.put("phone",     data.phone);

        if(fields.isEmpty()){
            throw new ValidationException("Datos inválidos", "Se requieren datos para realizar update");
        }

        repository.update(id, fields);
        return repository.findById(id);
    }

    public boolean delete(Long id){
        return false;
    }

    public static class ValidationException extends RuntimeException {
        private String details;
        
        public ValidationException(String message, String details) {
            super(message);
            this.details = details;
        }

        public String getDetails(){
            return details;
        }
    }

    public static class PersonNotFoundException extends RuntimeException {
        public PersonNotFoundException(String message) {
            super(message);
                
        }
    }

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
