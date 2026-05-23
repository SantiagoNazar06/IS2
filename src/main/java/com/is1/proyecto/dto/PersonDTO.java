package com.is1.proyecto.dto;

public class PersonDTO {
    private final String dni;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String email;

    public PersonDTO(String dni, String firstName, String lastName, String phone, String email) {
        this.dni = dni;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
    }

    public String getDni() { return dni; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}