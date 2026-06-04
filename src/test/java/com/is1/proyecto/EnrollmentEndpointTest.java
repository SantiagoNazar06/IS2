package com.is1.proyecto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.security.PasswordEncoder;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static spark.Spark.*;

/**
 * Tests de integración HTTP para el endpoint POST /enrollments/student/:id.
 * Usa extracción manual de JSESSIONID para evitar el problema conocido de
 * java.net.CookieManager con cookies de 'localhost' en JVM modernas.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnrollmentEndpointTest {

    private static final String TEST_DB_PATH = "./target/endpoint-test.db";
    private static final String TEST_DB_URL  = "jdbc:sqlite:" + TEST_DB_PATH;
    private static final int    TEST_PORT    = 4568;
    private static final String BASE_URL     = "http://localhost:" + TEST_PORT;

    private final ObjectMapper mapper = new ObjectMapper();
    private final PasswordEncoder encoder = new PasswordEncoder();

    // IDs insertados en setUp
    private long studentId;
    private long otherStudentId;
    private long subjectNoPrereqs;
    private long subjectWithPrereqs;

    // Cookies de sesión extraídas manualmente del header Set-Cookie
    private String adminCookie;
    private String studentCookie;

    private final String validPeriod = futureOrCurrentPeriod();

    @BeforeAll
    void setUp() throws Exception {
        new File(TEST_DB_PATH).delete();
        System.setProperty("db.url", TEST_DB_URL);
        System.setProperty("server.port", String.valueOf(TEST_PORT));

        Base.open("org.sqlite.JDBC", TEST_DB_URL, "", "");
        createSchema();
        insertTestData();
        Base.close();

        App.main(new String[]{});
        awaitInitialization();

        // Login y extracción manual de JSESSIONID
        adminCookie   = loginAndGetCookie("admin_user",   "pass123");
        studentCookie = loginAndGetCookie("student_user", "pass456");

        assertNotNull(adminCookie,   "La sesión del admin debe establecerse");
        assertNotNull(studentCookie, "La sesión del estudiante debe establecerse");
    }

    @AfterAll
    void tearDown() throws Exception {
        stop();
        awaitStop();
        new File(TEST_DB_PATH).delete();
    }

    // -----------------------------------------------------------------------
    // AC-1 + AC-2: inscripción válida → 201 con enrollment en respuesta
    // -----------------------------------------------------------------------
    @Test
    @Order(1)
    void enrollAdmin_validRequest_returns201WithEnrollment() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "subjectId", subjectNoPrereqs,
                "period", validPeriod
        ));

        HttpResponse<String> res = post(adminCookie, "/enrollments/student/" + studentId, body);
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);

        assertEquals(201, res.statusCode(), "Debe retornar 201 para inscripción válida");
        assertTrue(json.containsKey("enrollment"), "La respuesta debe incluir el objeto 'enrollment'");
        Map<?, ?> enrollment = (Map<?, ?>) json.get("enrollment");
        assertEquals(validPeriod, enrollment.get("period"));
        assertEquals("ENROLLED", enrollment.get("status"));
    }

    // -----------------------------------------------------------------------
    // AC-2 (already enrolled): inscripción duplicada → 400
    // -----------------------------------------------------------------------
    @Test
    @Order(2)
    void enroll_alreadyEnrolled_returns400() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "subjectId", subjectNoPrereqs,
                "period", validPeriod
        ));

        HttpResponse<String> res = post(adminCookie, "/enrollments/student/" + studentId, body);
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);

        assertEquals(400, res.statusCode(), "Inscripción duplicada debe retornar 400");
        assertTrue(json.get("error").toString().contains("ya está inscripto"),
                "El mensaje de error debe indicar inscripción duplicada");
    }

    // -----------------------------------------------------------------------
    // AC-3 + AC-4: correlativas faltantes → 400 + missingPrerequisites [{id, name}]
    // -----------------------------------------------------------------------
    @Test
    @Order(3)
    void enroll_prerequisitesMissing_returns400WithMissingList() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "subjectId", subjectWithPrereqs,
                "period", validPeriod
        ));

        HttpResponse<String> res = post(adminCookie, "/enrollments/student/" + studentId, body);
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);

        assertEquals(400, res.statusCode(), "Correlativa faltante debe retornar 400");
        assertTrue(json.containsKey("missingPrerequisites"),
                "Debe incluir 'missingPrerequisites' en la respuesta");
        var missing = (java.util.List<?>) json.get("missingPrerequisites");
        assertFalse(missing.isEmpty(), "La lista de correlativas faltantes no puede estar vacía");
        Map<?, ?> firstMissing = (Map<?, ?>) missing.get(0);
        assertTrue(firstMissing.containsKey("id"),   "Cada item debe tener 'id'");
        assertTrue(firstMissing.containsKey("name"), "Cada item debe tener 'name'");
    }

    // -----------------------------------------------------------------------
    // AC-5: estudiante inscribiendo a OTRO → 403
    // -----------------------------------------------------------------------
    @Test
    @Order(4)
    void enroll_studentInscribingOther_returns403() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "subjectId", subjectNoPrereqs,
                "period", validPeriod
        ));

        // studentCookie corresponde a otherStudentId; intenta inscribir a studentId
        HttpResponse<String> res = post(studentCookie, "/enrollments/student/" + studentId, body);
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);

        assertEquals(403, res.statusCode(), "Un estudiante no puede inscribir a otro: debe retornar 403");
        assertNotNull(json.get("error"));
    }

    // -----------------------------------------------------------------------
    // AC-5 (propio): estudiante inscribiéndose a sí mismo → 201
    // -----------------------------------------------------------------------
    @Test
    @Order(5)
    void enroll_studentInscribingHimself_returns201() throws Exception {
        String nextPeriod = nextDifferentPeriod();
        String body = mapper.writeValueAsString(Map.of(
                "subjectId", subjectNoPrereqs,
                "period", nextPeriod
        ));

        // studentCookie tiene student_id = otherStudentId en sesión
        HttpResponse<String> res = post(studentCookie, "/enrollments/student/" + otherStudentId, body);

        assertEquals(201, res.statusCode(),
                "Un estudiante puede inscribirse a sí mismo: debe retornar 201");
    }

    // -----------------------------------------------------------------------
    // AC-6: formato de período inválido → 400
    // -----------------------------------------------------------------------
    @Test
    @Order(6)
    void enroll_invalidPeriodFormat_returns400() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "subjectId", subjectNoPrereqs,
                "period", "2024/1"
        ));

        HttpResponse<String> res = post(adminCookie, "/enrollments/student/" + studentId, body);
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);

        assertEquals(400, res.statusCode(), "Período con formato inválido debe retornar 400");
        assertTrue(json.get("error").toString().contains("período inválido"),
                "El mensaje debe indicar formato de período inválido");
    }

    // -----------------------------------------------------------------------
    // AC-7: período pasado → 400
    // -----------------------------------------------------------------------
    @Test
    @Order(7)
    void enroll_pastPeriod_returns400() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "subjectId", subjectNoPrereqs,
                "period", "2020-1"
        ));

        HttpResponse<String> res = post(adminCookie, "/enrollments/student/" + studentId, body);
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);

        assertEquals(400, res.statusCode(), "Período pasado debe retornar 400");
        assertTrue(json.get("error").toString().contains("período pasado"),
                "El mensaje debe indicar que el período es pasado");
    }

    // -----------------------------------------------------------------------
    // AC-5 (sin sesión): sin autenticación → 401
    // -----------------------------------------------------------------------
    @Test
    @Order(8)
    void enroll_notAuthenticated_returns401() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "subjectId", subjectNoPrereqs,
                "period", validPeriod
        ));

        // Petición sin cookie de sesión
        HttpResponse<String> res = post(null, "/enrollments/student/" + studentId, body);
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);

        assertEquals(401, res.statusCode(), "Sin sesión debe retornar 401");
        assertNotNull(json.get("error"));
    }

    // ============================= Helpers =================================

    /**
     * Envía un POST JSON al endpoint con la cookie de sesión dada.
     * Si sessionCookie es null, se envía sin autenticación.
     */
    private HttpResponse<String> post(String sessionCookie, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (sessionCookie != null) {
            builder.header("Cookie", sessionCookie);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Realiza el login y extrae el JSESSIONID del header Set-Cookie.
     * Esto evita el problema de CookieManager con 'localhost' en JVM modernas.
     */
    private String loginAndGetCookie(String username, String password) throws Exception {
        String form = "username=" + username + "&password=" + password;
        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(loginReq, HttpResponse.BodyHandlers.ofString());

        // Extraer el valor JSESSIONID del header Set-Cookie (ej. "JSESSIONID=abc; Path=/; HttpOnly")
        return response.headers().firstValue("Set-Cookie")
                .map(header -> header.split(";")[0].trim())
                .orElse(null);
    }

    private String futureOrCurrentPeriod() {
        LocalDate now = LocalDate.now();
        int semester = now.getMonthValue() <= 6 ? 1 : 2;
        return now.getYear() + "-" + semester;
    }

    private String nextDifferentPeriod() {
        LocalDate now = LocalDate.now();
        int semester = now.getMonthValue() <= 6 ? 1 : 2;
        if (semester == 1) return now.getYear() + "-2";
        return (now.getYear() + 1) + "-1";
    }

    private void createSchema() {
        Base.exec("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, password TEXT NOT NULL, role TEXT NOT NULL DEFAULT 'ADMIN' CHECK(role IN('ADMIN','STUDENT')), student_id INTEGER)");
        Base.exec("CREATE TABLE IF NOT EXISTS persons (id INTEGER PRIMARY KEY AUTOINCREMENT, dni TEXT NOT NULL UNIQUE, firstName TEXT NOT NULL, lastName TEXT NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS students (id INTEGER PRIMARY KEY AUTOINCREMENT, id_person INTEGER NOT NULL, student_type TEXT NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS subjects (id_subject INTEGER PRIMARY KEY AUTOINCREMENT, subject_name TEXT NOT NULL)");
        Base.exec("CREATE TABLE IF NOT EXISTS conditions (id INTEGER PRIMARY KEY AUTOINCREMENT, subject_id INTEGER NOT NULL, prerequisite_subject_id INTEGER NOT NULL, type VARCHAR(20) NOT NULL DEFAULT 'REGULAR')");
        Base.exec("CREATE TABLE IF NOT EXISTS evaluations (id_evaluations INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, subject_id INTEGER NOT NULL, evaluation_date DATE NOT NULL, evaluation_note INTEGER, condition_type TEXT)");
        Base.exec("CREATE TABLE IF NOT EXISTS enrollments (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, subject_id INTEGER NOT NULL, period TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'ENROLLED', created_at TEXT, updated_at TEXT, UNIQUE(student_id, subject_id, period))");
        Base.exec("CREATE TABLE IF NOT EXISTS teachers (id INTEGER PRIMARY KEY AUTOINCREMENT, id_persona INTEGER NOT NULL, nroLegajo VARCHAR(30) NOT NULL UNIQUE)");
        Base.exec("CREATE TABLE IF NOT EXISTS careers (id_careers INTEGER PRIMARY KEY AUTOINCREMENT, career_name TEXT NOT NULL, career_duration INTEGER NOT NULL)");
    }

    private void insertTestData() {
        String adminHash   = encoder.encode("pass123");
        String studentHash = encoder.encode("pass456");

        Base.exec("INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?,?,?,?,?)",
                "11111111", "Maria", "Lopez", "1234567890", "maria@test.com");
        long person1Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();
        Base.exec("INSERT INTO students (id_person, student_type) VALUES (?,?)", person1Id, "Avanzado");
        studentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO persons (dni, firstName, lastName, phone, email) VALUES (?,?,?,?,?)",
                "22222222", "Carlos", "Perez", "9876543210", "carlos@test.com");
        long person2Id = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();
        Base.exec("INSERT INTO students (id_person, student_type) VALUES (?,?)", person2Id, "Ingresante");
        otherStudentId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO users (name, password, role, student_id) VALUES (?,?,?,?)",
                "admin_user", adminHash, "ADMIN", null);
        Base.exec("INSERT INTO users (name, password, role, student_id) VALUES (?,?,?,?)",
                "student_user", studentHash, "STUDENT", otherStudentId);

        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Introduccion a la Programacion");
        subjectNoPrereqs = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        Base.exec("INSERT INTO subjects (subject_name) VALUES (?)", "Estructuras de Datos");
        subjectWithPrereqs = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();

        // subjectWithPrereqs requiere subjectNoPrereqs aprobada
        Base.exec("INSERT INTO conditions (subject_id, prerequisite_subject_id, type) VALUES (?,?,?)",
                subjectWithPrereqs, subjectNoPrereqs, "APROBADA");
        // El estudiante principal (studentId) NO tiene evaluaciones → falla la correlativa
    }
}
