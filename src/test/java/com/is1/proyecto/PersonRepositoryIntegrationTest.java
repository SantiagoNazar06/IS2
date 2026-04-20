package com.is1.proyecto;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.repositories.PersonRepository;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PersonRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private PersonRepository repository;

    @BeforeAll
    void setupDatabase() {

        Base.open(
                "org.postgresql.Driver",
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );

        Base.exec(
            "CREATE TABLE persons (" +
            "id SERIAL PRIMARY KEY, " +
            "dni TEXT, " +
            "firstname TEXT, " +
            "lastname TEXT, " +
            "phone TEXT, " +
            "email TEXT" +
            ")"
        );

        Base.close();
    }

    @BeforeEach
    void setup() {
        Base.open(
                "org.postgresql.Driver",
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
        repository = new PersonRepository();
    }

    @AfterEach
    void cleanDatabase() {
        Base.exec("DELETE FROM persons");
        Base.close();
    }

    // =============================
    // CRUD COMPLETO
    // =============================
    @Test
    void testCRUDCycle() {

        Person person = new Person();
        person.set("firstname", "Juan");
        person.set("lastname", "Perez");
        person.set("dni", "123");

        Person created = repository.create(person);

        assertNotNull(created.getId());

        Person found = repository.findById(created.getLongId());

        assertNotNull(found);
        assertEquals("Juan", found.getString("firstname"));
        assertEquals("Perez", found.getString("lastname"));

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("firstname", "Carlos");

        boolean updated = repository.update(created.getLongId(), updateData);

        assertTrue(updated);

        Person updatedPerson = repository.findById(created.getLongId());

        assertEquals("Carlos", updatedPerson.getString("firstname"));

        List<Person> all = repository.findAll();

        assertEquals(1, all.size());

        Person byDni = repository.findByDni("123");

        assertNotNull(byDni);
        assertEquals(created.getId(), byDni.getId());

        boolean deleted = repository.delete(created.getLongId());

        assertTrue(deleted);

        Person deletedPerson = repository.findById(created.getLongId());

        assertNull(deletedPerson);
    }

    // =============================
    // TABLE EXISTS
    // =============================
    @Test
    void testPersonsTableExists() {
        boolean exists = repository.personsTableExists();
        assertTrue(exists);
    }
}