package com.is1.proyecto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static spark.Spark.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserApiRoutesTest {

    private static final String TEST_DB_PATH = "./target/user-api-test.db";
    private static final String TEST_DB_URL  = "jdbc:sqlite:" + TEST_DB_PATH;
    private static final int    TEST_PORT    = 4569;
    private static final String BASE_URL     = "http://localhost:" + TEST_PORT;

    private final ObjectMapper mapper = new ObjectMapper();

    private String adminCookie;
    private String studentCookie;
    private String teacherCookie;

    private Long createdUserId;

    @BeforeAll
    void setUp() throws Exception {
        new File(TEST_DB_PATH).delete();
        System.setProperty("db.url", TEST_DB_URL);
        System.setProperty("server.port", String.valueOf(TEST_PORT));

        // Dejamos que App.main() arranque Spark, ejecute bootstrap y
        // DataSeeder cree los 3 usuarios seed (admin, teacher, student)
        App.main(new String[]{});
        awaitInitialization();

        // Login con los usuarios seed que DataSeeder ya creo
        adminCookie   = loginAndGetCookie("admin",   "admin123");
        studentCookie = loginAndGetCookie("student", "student123");
        teacherCookie = loginAndGetCookie("teacher", "teacher123");

        assertNotNull(adminCookie,   "La sesion del admin debe establecerse");
        assertNotNull(studentCookie, "La sesion del estudiante debe establecerse");
        assertNotNull(teacherCookie, "La sesion del teacher debe establecerse");
    }

    @AfterAll
    void tearDown() throws Exception {
        stop();
        awaitStop();
        new File(TEST_DB_PATH).delete();
    }

    // ========================================================================
    // AUTHORIZATION TESTS
    // ========================================================================

    @Test
    @Order(1)
    void studentUser_gets403_onGetUsers() throws Exception {
        HttpResponse<String> res = get(studentCookie, "/api/users");
        assertEquals(403, res.statusCode(), "STUDENT debe recibir 403 en GET /api/users");
    }

    @Test
    @Order(2)
    void teacherUser_gets403_onGetUsers() throws Exception {
        HttpResponse<String> res = get(teacherCookie, "/api/users");
        assertEquals(403, res.statusCode(), "TEACHER debe recibir 403 en GET /api/users");
    }

    @Test
    @Order(3)
    void unauthenticatedUser_gets401_onGetUsers() throws Exception {
        HttpResponse<String> res = get(null, "/api/users");
        assertEquals(401, res.statusCode(), "Sin sesion debe recibir 401 en GET /api/users");
    }

    // ========================================================================
    // LIST TESTS
    // ========================================================================

    @Test
    @Order(4)
    void listUsers_returns200_withUserList() throws Exception {
        HttpResponse<String> res = get(adminCookie, "/api/users");

        assertEquals(200, res.statusCode());
        assertEquals("application/json", res.headers().firstValue("Content-Type").orElse(""));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = mapper.readValue(res.body(), List.class);
        assertTrue(users.size() >= 3, "Debe haber al menos 3 usuarios seed");
        assertFalse(users.get(0).containsKey("password"), "La respuesta NO debe incluir password");
    }

    // ========================================================================
    // GET BY ID TESTS
    // ========================================================================

    @Test
    @Order(5)
    void getUserById_existingUser_returns200() throws Exception {
        HttpResponse<String> res = get(adminCookie, "/api/users/1");

        assertEquals(200, res.statusCode());
        Map<?, ?> user = mapper.readValue(res.body(), Map.class);
        assertNotNull(user.get("id"));
        assertNotNull(user.get("username"));
        assertNotNull(user.get("role"));
        assertNull(user.get("password"), "La respuesta NO debe incluir password");
    }

    @Test
    @Order(6)
    void getUserById_nonExistentUser_returns404() throws Exception {
        HttpResponse<String> res = get(adminCookie, "/api/users/99999");

        assertEquals(404, res.statusCode());
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);
        assertNotNull(json.get("error"));
    }

    // ========================================================================
    // CREATE TESTS
    // ========================================================================

    @Test
    @Order(7)
    void createUser_validRequest_returns201() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "username", "nuevo_usuario",
                "password", "pass123",
                "role", "STUDENT"
        ));

        HttpResponse<String> res = post(adminCookie, "/api/users", body);

        assertEquals(201, res.statusCode());
        Map<?, ?> user = mapper.readValue(res.body(), Map.class);
        assertEquals("nuevo_usuario", user.get("username"));
        assertEquals("STUDENT", user.get("role"));
        assertNull(user.get("password"), "La respuesta NO debe incluir password");

        createdUserId = ((Number) user.get("id")).longValue();
    }

    @Test
    @Order(8)
    void createUser_withoutUsername_returns400() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "password", "pass123",
                "role", "STUDENT"
        ));

        HttpResponse<String> res = post(adminCookie, "/api/users", body);

        assertEquals(400, res.statusCode());
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);
        assertNotNull(json.get("error"));
    }

    @Test
    @Order(9)
    void createUser_withoutPassword_returns400() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "username", "sin_password",
                "role", "STUDENT"
        ));

        HttpResponse<String> res = post(adminCookie, "/api/users", body);

        assertEquals(400, res.statusCode());
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);
        assertNotNull(json.get("error"));
    }

    // ========================================================================
    // UPDATE TESTS
    // ========================================================================

    @Test
    @Order(10)
    void updateUser_existingUser_returns200() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "username", "admin_actualizado",
                "role", "ADMIN"
        ));

        HttpResponse<String> res = put(adminCookie, "/api/users/1", body);

        assertEquals(200, res.statusCode());
        Map<?, ?> user = mapper.readValue(res.body(), Map.class);
        assertEquals("admin_actualizado", user.get("username"));
        assertEquals("ADMIN", user.get("role"));
        assertNull(user.get("password"), "La respuesta NO debe incluir password");
    }

    @Test
    @Order(11)
    void updateUser_nonExistentUser_returns404() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "username", "no_existe"
        ));

        HttpResponse<String> res = put(adminCookie, "/api/users/99999", body);

        assertEquals(404, res.statusCode());
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);
        assertNotNull(json.get("error"));
    }

    // ========================================================================
    // DELETE TESTS
    // ========================================================================

    @Test
    @Order(12)
    void deleteUser_existingUser_returns204() throws Exception {
        assertNotNull(createdUserId, "Se necesita un usuario creado previamente");

        HttpResponse<String> res = delete(adminCookie, "/api/users/" + createdUserId);

        assertEquals(204, res.statusCode(), "DELETE debe retornar 204 sin contenido");
        assertTrue(res.body().isEmpty() || res.body().equals(""),
                "El body debe estar vacio para 204");
    }

    @Test
    @Order(13)
    void deleteUser_nonExistentUser_returns404() throws Exception {
        HttpResponse<String> res = delete(adminCookie, "/api/users/99999");

        assertEquals(404, res.statusCode());
        Map<?, ?> json = mapper.readValue(res.body(), Map.class);
        assertNotNull(json.get("error"));
    }

    @Test
    @Order(14)
    void listUsers_afterDelete_doesNotIncludeDeleted() throws Exception {
        HttpResponse<String> res = get(adminCookie, "/api/users");

        assertEquals(200, res.statusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = mapper.readValue(res.body(), List.class);
        boolean found = users.stream()
                .anyMatch(u -> createdUserId.equals(u.get("id")));
        assertFalse(found, "El usuario eliminado no debe aparecer en la lista");
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private HttpResponse<String> get(String sessionCookie, String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET();
        if (sessionCookie != null) {
            builder.header("Cookie", sessionCookie);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

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

    private HttpResponse<String> put(String sessionCookie, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (sessionCookie != null) {
            builder.header("Cookie", sessionCookie);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String sessionCookie, String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .DELETE();
        if (sessionCookie != null) {
            builder.header("Cookie", sessionCookie);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String loginAndGetCookie(String username, String password) throws Exception {
        String form = "username=" + username + "&password=" + password;
        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(loginReq, HttpResponse.BodyHandlers.ofString());

        return response.headers().firstValue("Set-Cookie")
                .map(header -> header.split(";")[0].trim())
                .orElse(null);
    }
}
