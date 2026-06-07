package com.is1.proyecto.config;

import com.is1.proyecto.models.*;
import com.is1.proyecto.security.PasswordEncoder;
import java.util.*;

/**
 * Puebla la base de datos con datos iniciales si está vacía.
 * <p>
 * Idempotente: las cuentas seed se verifican individualmente.
 * El demo masivo se ejecuta solo si Student.count() &lt;= 1 (solo el seed original).
 */
public class DataSeeder {

    private static final PasswordEncoder PASSWORD_ENCODER = new PasswordEncoder();
    private static final Random RANDOM = new Random(42); // fixed seed for reproducibility
    private static final String PERIOD = "2025";

    private DataSeeder() {}

    // ─────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────

    public static void seed() {
        System.out.println("[Seed] Verificando cuentas seed...");
        seedCoreAccounts();

        if (Student.count() <= 1) {
            System.out.println("[Seed] Sembrando dataset demo masivo...");
            seedDemoData();
            System.out.println("[Seed] Dataset demo completo.");
        } else {
            System.out.println("[Seed] Dataset demo ya existe, omitiendo.");
        }

        System.out.println("[Seed] Seed completo.");
    }

    // ─────────────────────────────────────────────────────────────────
    //  Core accounts (admin, teacher, student) — always present
    // ─────────────────────────────────────────────────────────────────

    private static void seedCoreAccounts() {
        if (!userExists("admin")) {
            System.out.println("[Seed] Creando admin...");
            seedAdmin();
        }
        if (!userExists("teacher")) {
            System.out.println("[Seed] Creando teacher...");
            seedTeacher();
        }
        if (!userExists("student")) {
            System.out.println("[Seed] Creando student...");
            seedStudent();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Demo dataset
    // ─────────────────────────────────────────────────────────────────

    private static void seedDemoData() {
        // 1. Careers & study plans
        Map<String, CareerDef> careerDefs = buildCareerDefs();
        List<CareerEntry> careers = seedCareers(careerDefs);

        // 2. Subjects per career
        List<SubjectEntry> allSubjects = new ArrayList<>();
        for (CareerEntry ce : careers) {
            List<SubjectDef> defs = careerDefs.get(ce.career.getCareerName()).subjects;
            for (SubjectDef sd : defs) {
                Subject subj = seedSubject(sd.code, sd.name, ce.studyPlan);
                allSubjects.add(new SubjectEntry(subj, ce));
            }
        }

        // 3. Teachers
        List<TeacherEntry> teachers = seedTeachers();

        // 4. Teacher assignments (teacher_subject + teacher_assignments)
        seedTeacherAssignments(teachers, allSubjects);

        // 5. Students (~100)
        List<StudentEntry> students = seedStudents();

        // 6. Conditions (prerequisites)
        seedConditions(allSubjects);

        // 7. Enrollments
        List<EnrollmentEntry> enrollments = seedEnrollments(students, allSubjects);

        // 8. Evaluations (grades)
        seedEvaluations(enrollments);
    }

    // ─────────────────────────────────────────────────────────────────
    //  1. Careers
    // ─────────────────────────────────────────────────────────────────

    private static List<CareerEntry> seedCareers(Map<String, CareerDef> defs) {
        List<CareerEntry> result = new ArrayList<>();
        for (CareerDef def : defs.values()) {
            Career career = Career.findFirst("career_name = ?", def.name);
            if (career == null) {
                System.out.println("[Seed] Creando carrera '" + def.name + "'...");
                career = new Career();
                career.setCareerName(def.name);
                career.setCareerDuration(def.duration);
                career.saveIt();
            }

            StudyPlan plan = StudyPlan.findFirst("name = ? AND id_career = ?", "Plan 2024", career.getId());
            if (plan == null) {
                System.out.println("[Seed] Creando plan de estudio para '" + def.name + "'...");
                plan = new StudyPlan();
                plan.setName("Plan 2024");
                plan.setYear(2024);
                plan.setCareerId(career.getId());
                plan.saveIt();
            }

            result.add(new CareerEntry(career, plan));
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    //  2. Subjects
    // ─────────────────────────────────────────────────────────────────

    private static Subject seedSubject(String code, String name, StudyPlan plan) {
        Subject existing = Subject.findFirst("code = ?", code);
        if (existing != null) return existing;
        System.out.println("[Seed] Creando materia '" + code + " - " + name + "'...");
        Subject s = new Subject();
        s.setCode(code);
        s.setSubjectName(name);
        s.setStudyPlanId(plan.getId());
        s.saveIt();
        return s;
    }

    // ─────────────────────────────────────────────────────────────────
    //  3. Teachers (10)
    // ─────────────────────────────────────────────────────────────────

    private static List<TeacherEntry> seedTeachers() {
        String[][] data = {
            {"Luis", "Martinez", "30000001", "LEG-T-002"},
            {"Ana", "Fernandez", "30000002", "LEG-T-003"},
            {"Pedro", "Gonzalez", "30000003", "LEG-T-004"},
            {"Laura", "Rodriguez", "30000004", "LEG-T-005"},
            {"Diego", "Lopez",    "30000005", "LEG-T-006"},
            {"Sofia", "Diaz",     "30000006", "LEG-T-007"},
            {"Martin", "Perez",   "30000007", "LEG-T-008"},
            {"Valeria", "Torres", "30000008", "LEG-T-009"},
            {"Jorge", "Ramirez",  "30000009", "LEG-T-010"},
            {"Carla", "Sanchez",  "30000010", "LEG-T-011"},
        };

        List<TeacherEntry> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            String[] row = data[i];
            String dni = row[2];
            if (Person.findFirst("dni = ?", dni) != null) continue;

            Person p = new Person();
            p.setDni(dni);
            p.setFirstName(row[0]);
            p.setLastName(row[1]);
            p.setPhone("11555" + String.format("%05d", i + 1));
            p.setEmail(row[0].toLowerCase() + "." + row[1].toLowerCase() + "@escuela.com");
            p.saveIt();

            Teacher t = new Teacher();
            t.setPerson(p);
            t.setNroLegajo(row[3]);
            t.saveIt();

            String username = row[0].toLowerCase() + "." + row[1].toLowerCase();
            if (!userExists(username)) {
                User u = new User();
                u.setName(username);
                u.set("password", PASSWORD_ENCODER.encode("pass123"));
                u.set("role", "TEACHER");
                u.setTeacherId(t.getLongId());
                u.saveIt();
            }

            list.add(new TeacherEntry(t, p, row[0], row[1]));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────
    //  4. Teacher assignments
    // ─────────────────────────────────────────────────────────────────

    private static void seedTeacherAssignments(List<TeacherEntry> teachers, List<SubjectEntry> subjects) {
        if (TeacherAssignment.count() > 0) return;

        TeacherRole[] roles = {TeacherRole.RESPONSABLE, TeacherRole.JTP, TeacherRole.AYUDANTE};

        for (int i = 0; i < subjects.size(); i++) {
            SubjectEntry se = subjects.get(i);
            TeacherEntry primary = teachers.get(i % teachers.size());

            // Primary teacher (RESPONSABLE)
            Long teacherId = primary.teacher.getLongId();
            Long subjectId = se.subject.getLong("id_subject");

            if (TeacherAssignment.findFirst(
                    "teacher_id = ? AND subject_id = ? AND period = ?",
                    teacherId, subjectId, PERIOD) == null) {
                TeacherAssignment ta = new TeacherAssignment();
                ta.setTeacherId(teacherId);
                ta.setSubjectId(subjectId);
                ta.setRole(roles[0]);
                ta.setPeriod(PERIOD);
                ta.saveIt();
            }

            // teacher_subject join table
            if (TeacherSubject.findFirst("teacher_id = ? AND subject_id = ?",
                    teacherId, subjectId) == null) {
                TeacherSubject ts = new TeacherSubject();
                ts.setTeacherId(teacherId);
                ts.setSubjectId(subjectId);
                ts.saveIt();
            }

            // Optional secondary teacher (JTP) for some subjects
            if (i % 2 == 0) {
                TeacherEntry secondary = teachers.get((i + teachers.size() / 2) % teachers.size());
                Long secondaryId = secondary.teacher.getLongId();
                if (!secondaryId.equals(teacherId)
                        && TeacherAssignment.findFirst(
                            "teacher_id = ? AND subject_id = ? AND period = ?",
                            secondaryId, subjectId, PERIOD) == null) {
                    TeacherAssignment ta = new TeacherAssignment();
                    ta.setTeacherId(secondaryId);
                    ta.setSubjectId(subjectId);
                    ta.setRole(roles[1]);
                    ta.setPeriod(PERIOD);
                    ta.saveIt();
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  5. Students (~100)
    // ─────────────────────────────────────────────────────────────────

    private static List<StudentEntry> seedStudents() {
        String[] firstNames = {
            "Mateo", "Valentina", "Santiago", "Camila", "Benjamin",
            "Isabella", "Sebastian", "Luciana", "Facundo", "Martina",
            "Nicolas", "Julieta", "Agustin", "Florencia", "Tomas",
            "Catalina", "Federico", "Victoria", "Ian", "Antonella",
            "Lautaro", "Guadalupe", "Gael", "Abigail", "Thiago",
            "Emilia", "Bruno", "Renata", "Franco", "Mia",
            "Juan", "Lola", "Joaquin", "Chloe", "Emiliano",
            "Olivia", "Maximo", "Morena", "Bautista", "Ailen",
            "Simon", "Jazmin", "Santino", "Malena", "Lorenzo",
            "Zoe", "Matias", "Ambar", "Ignacio", "Lara",
            "Gino", "Clara", "Lisandro", "Luna", "Julian",
            "Pilar", "Alma", "Brunela", "Ciro", "Paloma",
            "Ramiro", "Elena", "Ezequiel", "Valeria", "Aaron",
            "Agostina", "Luca", "Ariana", "Nehuen", "Brisa",
            "Genaro", "Helena", "Tiziano", "Luana", "Alvaro",
            "Kiara", "Luciano", "Delfina", "Cristian", "Magali",
            "Kevin", "Selene", "Hector", "Ariadna", "Fabian",
            "Nahiara", "Rafael", "Oriana", "Pablo", "Milena",
            "Damian", "Josefina", "Alexis", "Tatiana", "Gaston",
            "Melina", "Leandro", "Alejandra", "Mauro", "Carolina"
        };

        String[] lastNames = {
            "Gimenez", "Castillo", "Rojas", "Molina", "Acosta",
            "Pereyra", "Sosa", "Medina", "Moreno", "Morales",
            "Ortiz", "Silva", "Ramos", "Paz", "Benitez",
            "Vega", "Cruz", "Flores", "Campos", "Herrera",
            "Aguirre", "Vargas", "Mendoza", "Peralta", "Godoy",
            "Ibañez", "Cardenas", "Soria", "Coronel", "Luna",
            "Ledesma", "Fernandez", "Gonzalez", "Rodriguez", "Lopez",
            "Diaz", "Perez", "Torres", "Ramirez", "Sanchez",
            "Romero", "Alvarez", "Ruiz", "Castillo", "Gutierrez",
            "Reyes", "Delgado", "Navarro", "Vazquez", "Chavez"
        };

        String[] studentTypes = {"Inicial", "Intermedio", "Avanzado", "Regular"};

        List<StudentEntry> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String firstName = firstNames[i % firstNames.length];
            String lastName = lastNames[i % lastNames.length];
            String dni = String.format("400%05d", i + 1);

            if (Person.findFirst("dni = ?", dni) != null) continue;

            Person p = new Person();
            p.setDni(dni);
            p.setFirstName(firstName);
            p.setLastName(lastName);
            p.setPhone("11600" + String.format("%05d", i + 1));
            p.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@alumno.escuela.com");
            p.saveIt();

            Student s = new Student();
            s.setPerson(p);
            s.setType(studentTypes[i % studentTypes.length]);
            s.saveIt();

            String username = "alumno." + (i + 1);
            if (!userExists(username)) {
                User u = new User();
                u.setName(username);
                u.set("password", PASSWORD_ENCODER.encode("alumno123"));
                u.set("role", "STUDENT");
                u.setStudentId(s.getLongId());
                u.saveIt();
            }

            list.add(new StudentEntry(s, p, firstName, lastName));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────
    //  6. Conditions (prerequisites per career)
    // ─────────────────────────────────────────────────────────────────

    private static void seedConditions(List<SubjectEntry> allSubjects) {
        if (Condition.count() > 0) return;

        // Group subjects by career
        Map<String, List<SubjectEntry>> byCareer = new LinkedHashMap<>();
        for (SubjectEntry se : allSubjects) {
            String careerName = se.career.career.getCareerName();
            byCareer.computeIfAbsent(careerName, k -> new ArrayList<>()).add(se);
        }

        String[][] chains = {
            // Sistemas: MAT101 → ALG101 → PROG101 → BD101 (in order)
            {"Ingeniería en Sistemas", "MAT101", "ALG101"},
            {"Ingeniería en Sistemas", "ALG101", "PROG101"},
            {"Ingeniería en Sistemas", "PROG101", "BD101"},
            // Administración: ADM101 → ECO101 → CON101
            {"Licenciatura en Administración", "ADM101", "ECO101"},
            {"Licenciatura en Administración", "ECO101", "CON101"},
            // Contador: CONT101 → IMP101 → AUD101
            {"Contador Público", "CONT101", "IMP101"},
            {"Contador Público", "IMP101", "AUD101"},
            // Industrial: IND101 → LOG101 → CAL101
            {"Ingeniería Industrial", "IND101", "LOG101"},
            {"Ingeniería Industrial", "LOG101", "CAL101"},
            // Marketing: MKT101 → PUB101 → INV101
            {"Licenciatura en Marketing", "MKT101", "PUB101"},
            {"Licenciatura en Marketing", "PUB101", "INV101"},
        };

        for (String[] chain : chains) {
            String careerName = chain[0];
            String fromCode = chain[1];
            String toCode = chain[2];

            Subject from = Subject.findFirst("code = ?", fromCode);
            Subject to = Subject.findFirst("code = ?", toCode);
            if (from == null || to == null) continue;

            Integer fromId = from.getId();
            Integer toId = to.getId();

            if (Condition.findFirst("subject_id = ? AND prerequisite_subject_id = ?", toId, fromId) == null) {
                Condition c = new Condition();
                c.setSubjectId(toId);
                c.setPrerequisiteSubjectId(fromId);
                c.set("type", "REGULAR");
                c.saveIt();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  7. Enrollments (3-6 per student)
    // ─────────────────────────────────────────────────────────────────

    private static List<EnrollmentEntry> seedEnrollments(List<StudentEntry> students, List<SubjectEntry> allSubjects) {
        List<EnrollmentEntry> result = new ArrayList<>();
        String[] statuses = {"ENROLLED", "COMPLETED", "DROPPED", "CANCELLED"};

        for (StudentEntry se : students) {
            int count = 3 + RANDOM.nextInt(4); // 3 to 6
            Set<Integer> usedIndices = new HashSet<>();

            for (int i = 0; i < count && i < allSubjects.size(); i++) {
                int subjIdx;
                do {
                    subjIdx = RANDOM.nextInt(allSubjects.size());
                } while (!usedIndices.add(subjIdx));

                SubjectEntry subj = allSubjects.get(subjIdx);
                String status = statuses[RANDOM.nextInt(statuses.length)];

                Long studentId = se.student.getLongId();
                Long subjectId = subj.subject.getLong("id_subject");

                if (Enrollment.findFirst(
                        "student_id = ? AND subject_id = ? AND period = ?",
                        studentId, subjectId, PERIOD) != null) continue;

                Enrollment e = new Enrollment();
                e.setStudentId(studentId);
                e.setSubjectId(subjectId);
                e.setPeriod(PERIOD);
                e.setStatus(status);
                e.saveIt();

                result.add(new EnrollmentEntry(e, status));
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    //  8. Evaluations (for COMPLETED enrollments)
    // ─────────────────────────────────────────────────────────────────

    private static void seedEvaluations(List<EnrollmentEntry> enrollments) {
        String[] conditions = {"REGULAR", "APROBADA", "PROMOCION"};

        for (EnrollmentEntry ee : enrollments) {
            if (!"COMPLETED".equals(ee.status)) continue;
            if (Evaluation.findFirst("enrollment_id = ?", ee.enrollment.getId()) != null) continue;

            Evaluation ev = new Evaluation();
            ev.setEvaluationEnrollementId(ee.enrollment.getInteger("id"));
            ev.setEvaluationGrade(4.0 + (RANDOM.nextDouble() * 6.0)); // 4.0 to 10.0
            ev.setCondition(conditions[RANDOM.nextInt(conditions.length)]);
            ev.setEvaluationDate(new java.sql.Date(System.currentTimeMillis()));
            ev.saveIt();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────

    private static boolean userExists(String username) {
        return User.where("name = ?", username).size() > 0;
    }

    private static void seedAdmin() {
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

    // ─────────────────────────────────────────────────────────────────
    //  Data definitions (plain classes, Java 11 compatible)
    // ─────────────────────────────────────────────────────────────────

    static class CareerDef {
        final String name;
        final int duration;
        final List<SubjectDef> subjects;
        CareerDef(String name, int duration, List<SubjectDef> subjects) {
            this.name = name; this.duration = duration; this.subjects = subjects;
        }
    }
    static class SubjectDef {
        final String code;
        final String name;
        SubjectDef(String code, String name) {
            this.code = code; this.name = name;
        }
    }
    static class CareerEntry {
        final Career career;
        final StudyPlan studyPlan;
        CareerEntry(Career career, StudyPlan studyPlan) {
            this.career = career; this.studyPlan = studyPlan;
        }
    }
    static class SubjectEntry {
        final Subject subject;
        final CareerEntry career;
        SubjectEntry(Subject subject, CareerEntry career) {
            this.subject = subject; this.career = career;
        }
    }
    static class TeacherEntry {
        final Teacher teacher;
        final Person person;
        final String firstName;
        final String lastName;
        TeacherEntry(Teacher teacher, Person person, String firstName, String lastName) {
            this.teacher = teacher; this.person = person;
            this.firstName = firstName; this.lastName = lastName;
        }
    }
    static class StudentEntry {
        final Student student;
        final Person person;
        final String firstName;
        final String lastName;
        StudentEntry(Student student, Person person, String firstName, String lastName) {
            this.student = student; this.person = person;
            this.firstName = firstName; this.lastName = lastName;
        }
    }
    static class EnrollmentEntry {
        final Enrollment enrollment;
        final String status;
        EnrollmentEntry(Enrollment enrollment, String status) {
            this.enrollment = enrollment; this.status = status;
        }
    }

    private static Map<String, CareerDef> buildCareerDefs() {
        Map<String, CareerDef> map = new LinkedHashMap<>();

        map.put("Ingeniería en Sistemas", new CareerDef("Ingeniería en Sistemas", 5, List.of(
            new SubjectDef("MAT101", "Análisis Matemático I"),
            new SubjectDef("ALG101", "Álgebra"),
            new SubjectDef("PROG101", "Programación I"),
            new SubjectDef("BD101",   "Base de Datos"),
            new SubjectDef("RED101",  "Redes")
        )));

        map.put("Licenciatura en Administración", new CareerDef("Licenciatura en Administración", 4, List.of(
            new SubjectDef("ADM101", "Administración General"),
            new SubjectDef("ECO101", "Economía"),
            new SubjectDef("CON101", "Contabilidad Gerencial"),
            new SubjectDef("LEG101", "Legislación Laboral")
        )));

        map.put("Contador Público", new CareerDef("Contador Público", 5, List.of(
            new SubjectDef("CONT101", "Contabilidad Básica"),
            new SubjectDef("IMP101",  "Impuestos"),
            new SubjectDef("AUD101",  "Auditoría"),
            new SubjectDef("FIN101",  "Finanzas")
        )));

        map.put("Ingeniería Industrial", new CareerDef("Ingeniería Industrial", 5, List.of(
            new SubjectDef("IND101", "Procesos Industriales"),
            new SubjectDef("LOG101", "Logística"),
            new SubjectDef("CAL101", "Control de Calidad"),
            new SubjectDef("SEG101", "Seguridad e Higiene")
        )));

        map.put("Licenciatura en Marketing", new CareerDef("Licenciatura en Marketing", 4, List.of(
            new SubjectDef("MKT101", "Marketing Digital"),
            new SubjectDef("PUB101", "Publicidad"),
            new SubjectDef("COM101", "Comunicación"),
            new SubjectDef("INV101", "Investigación de Mercado")
        )));

        return map;
    }
}
