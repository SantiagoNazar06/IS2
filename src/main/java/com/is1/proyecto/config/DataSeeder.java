package com.is1.proyecto.config;

import com.is1.proyecto.models.*;
import com.is1.proyecto.security.PasswordEncoder;
import org.javalite.activejdbc.Base;

/**
 * Puebla la base de datos con datos iniciales si está vacía.
 * <p>
 * Idempotente: solo ejecuta si no existen los registros semilla.
 */
public class DataSeeder {

    private static final PasswordEncoder PASSWORD_ENCODER = new PasswordEncoder();

    private DataSeeder() {
        // Utility class
    }

    /**
     * Crea las 3 cuentas seed si no existen por username.
     * Luego crea datos de dominio semilla (carrera, plan de estudio, materias).
     * Idempotente: cada entidad se crea solo si no existe.
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

        // ── Datos de dominio semilla ──
        System.out.println("[Seed] Verificando datos de dominio...");
        seedDomainData();

        System.out.println("[Seed] Seed completo.");
    }

    /**
     * Crea datos de dominio iniciales: carrera, plan de estudio y materias de ejemplo.
     * Idempotente: verifica existencia antes de crear.
     */
    private static void seedDomainData() {
        // ── Carrera ──
        Career career;
        if (Career.findFirst("career_name = ?", "Ingeniería en Sistemas") != null) {
            System.out.println("[Seed] Carrera 'Ingeniería en Sistemas' ya existe, omitiendo.");
            career = Career.findFirst("career_name = ?", "Ingeniería en Sistemas");
        } else {
            System.out.println("[Seed] Creando carrera 'Ingeniería en Sistemas'...");
            career = new Career();
            career.setCareerName("Ingeniería en Sistemas");
            career.setCareerDuration(5);
            career.saveIt();
        }

        // ── Plan de Estudio ──
        StudyPlan studyPlan;
        if (StudyPlan.findFirst("name = ? AND id_career = ?", "Plan 2024", career.getId()) != null) {
            System.out.println("[Seed] Plan de estudio 'Plan 2024' ya existe, omitiendo.");
            studyPlan = StudyPlan.findFirst("name = ? AND id_career = ?", "Plan 2024", career.getId());
        } else {
            System.out.println("[Seed] Creando plan de estudio 'Plan 2024'...");
            studyPlan = new StudyPlan();
            studyPlan.setName("Plan 2024");
            studyPlan.setYear(2024);
            studyPlan.setCareerId(career.getId());
            studyPlan.saveIt();
        }

        // ── Materias de ejemplo ──
        seedSubjectIfNotExists("MAT101", "Análisis Matemático I", studyPlan);
        seedSubjectIfNotExists("ALG101", "Álgebra", studyPlan);
        seedSubjectIfNotExists("PROG101", "Programación I", studyPlan);
    }

    /**
     * Crea una materia si no existe una con el mismo código.
     */
    private static void seedSubjectIfNotExists(String code, String name, StudyPlan studyPlan) {
        if (Subject.findFirst("code = ?", code) != null) {
            System.out.println("[Seed] Materia '" + code + "' ya existe, omitiendo.");
            return;
        }
        System.out.println("[Seed] Creando materia '" + code + " - " + name + "'...");
        Subject subject = new Subject();
        subject.setCode(code);
        subject.setSubjectName(name);
        subject.setStudyPlanId(studyPlan.getId());
        subject.saveIt();
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
