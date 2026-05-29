package com.is1.proyecto.person;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.repositories.PersonRepository;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PersonRepositoryIntegrationTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-person.db";

    private PersonRepository repository;

    @BeforeAll
    void setupDatabase() {
        // Le indicamos al singleton que use la DB en memoria
        System.setProperty("db.url", JDBC_URL);

        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec(
            "CREATE TABLE IF NOT EXISTS persons (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
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
        repository = new PersonRepository();
    }

    @AfterEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @AfterAll
    void teardown() {
        new File("target/test-person.db").delete();
    }

    // =============================
    // CRUD COMPLETO
    // =============================

    @Test
    void testCRUDCycle() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Person person = new Person();
        person.set("firstname", "Juan");
        person.set("lastname", "Perez");
        person.set("dni", "123");
        Base.close();


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