package com.is1.proyecto.factories;

import com.is1.proyecto.models.Person;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para PersonFactory.
 * Verifica que create() devuelva instancias válidas de Person.
 * Requiere DB (SQLite) porque Person extiende Model de ActiveJDBC.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonFactoryTest {

    private static final String JDBC_URL = "jdbc:sqlite:target/test-personfactory.db";

    @BeforeAll
    void setupDatabase() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("CREATE TABLE persons (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "dni TEXT NOT NULL UNIQUE, " +
                "firstName TEXT NOT NULL, " +
                "lastName TEXT NOT NULL, " +
                "phone TEXT NOT NULL, " +
                "email TEXT NOT NULL)");
        Base.close();
    }

    @BeforeEach
    void openConnection() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
    }

    @AfterEach
    void closeConnection() {
        Base.close();
    }

    @Test
    void create_returnsPersonInstance() {
        PersonFactory factory = new PersonFactory();
        Person result = factory.create();
        assertNotNull(result);
    }

    @Test
    void create_returnsNewInstanceEachCall() {
        PersonFactory factory = new PersonFactory();
        assertNotSame(factory.create(), factory.create());
    }

    @Test
    void factoryImplementsInterface() {
        PersonFactory factory = new PersonFactory();
        assertTrue(factory instanceof PersonFactoryInterface);
    }

    @AfterAll
    void teardown() {
        Base.open("org.sqlite.JDBC", JDBC_URL, "", "");
        Base.exec("DROP TABLE IF EXISTS persons");
        Base.close();
        new java.io.File("target/test-personfactory.db").delete();
    }
}
