package com.is1.proyecto.config;

import com.is1.proyecto.models.*;
import com.is1.proyecto.security.PasswordEncoder;
import org.javalite.activejdbc.Base;

/**
 * Puebla la base de datos con datos iniciales si está vacía.
 * <p>
 * Idempotente: solo ejecuta si la tabla {@code users} está vacía.
 */
public class DataSeeder {

    private static final PasswordEncoder PASSWORD_ENCODER = new PasswordEncoder();

    private DataSeeder() {
        // Utility class
    }

    /**
     * Crea las 3 cuentas seed si no existen por username.
     * Idempotente: cada cuenta se crea solo si su username no existe.
     */
    public static void seed() {
        System.out.println("[Seed] Verificando cuentas seed...");

        if (userExists("admin")) {
            System.out.println("[Seed] admin ya existe, omitiendo.");
        } else {
            System.out.println("[Seed] Creando admin...");
            seedAdmin();
        }

        if (userExists("teacher")) {
            System.out.println("[Seed] teacher ya existe, omitiendo.");
        } else {
            System.out.println("[Seed] Creando teacher...");
            seedTeacher();
        }

        if (userExists("student")) {
            System.out.println("[Seed] student ya existe, omitiendo.");
        } else {
            System.out.println("[Seed] Creando student...");
            seedStudent();
        }

        System.out.println("[Seed] Seed completo.");
    }

    private static boolean userExists(String username) {
        return User.where("name = ?", username).size() > 0;
    }

    private static void seedAdmin() {
        // Dummy person with dni "0" for admin
        Person adminPerson = new Person();
        adminPerson.setDni("0");
        adminPerson.setFirstName("Admin");
        adminPerson.setLastName("System");
        adminPerson.setPhone("0000000000");
        adminPerson.setEmail("admin@sistema.com");
        adminPerson.saveIt();

        User adminUser = new User();
        adminUser.setName("admin");
        adminUser.set("password", PASSWORD_ENCODER.encode("admin123"));
        adminUser.set("role", "ADMIN");
        adminUser.saveIt();
    }

    private static void seedTeacher() {
        Person teacherPerson = new Person();
        teacherPerson.setDni("12345678");
        teacherPerson.setFirstName("Carlos");
        teacherPerson.setLastName("Garcia");
        teacherPerson.setPhone("1111111111");
        teacherPerson.setEmail("carlos.garcia@escuela.com");
        teacherPerson.saveIt();

        Teacher teacherRecord = new Teacher();
        teacherRecord.setPerson(teacherPerson);
        teacherRecord.setNroLegajo("LEG-T-001");
        teacherRecord.saveIt();

        User teacherUser = new User();
        teacherUser.setName("teacher");
        teacherUser.set("password", PASSWORD_ENCODER.encode("teacher123"));
        teacherUser.set("role", "TEACHER");
        teacherUser.setTeacherId(teacherRecord.getLongId());
        teacherUser.saveIt();
    }

    private static void seedStudent() {
        Person studentPerson = new Person();
        studentPerson.setDni("87654321");
        studentPerson.setFirstName("Maria");
        studentPerson.setLastName("Lopez");
        studentPerson.setPhone("2222222222");
        studentPerson.setEmail("maria.lopez@escuela.com");
        studentPerson.saveIt();

        Student studentRecord = new Student();
        studentRecord.setPerson(studentPerson);
        studentRecord.setType("Avanzado");
        studentRecord.saveIt();

        User studentUser = new User();
        studentUser.setName("student");
        studentUser.set("password", PASSWORD_ENCODER.encode("student123"));
        studentUser.set("role", "STUDENT");
        studentUser.setStudentId(studentRecord.getLongId());
        studentUser.saveIt();
    }
}
