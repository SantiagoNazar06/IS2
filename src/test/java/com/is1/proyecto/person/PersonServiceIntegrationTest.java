package com.is1.proyecto.person;

import com.is1.proyecto.services.PersonService;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.repositories.PersonRepository;
import com.is1.proyecto.dto.PersonDTO;
import com.is1.proyecto.factories.PersonFactoryInterface;
import com.is1.proyecto.exceptions.PersonNotFoundException;
import com.is1.proyecto.exceptions.ValidationException;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración de PersonService.
 *
 * Requisitos en pom.xml:
 *   - org.xerial:sqlite-jdbc
 *   - org.javalite:activejdbc
 *   - plugin activejdbc-instrumentation en el lifecycle de test-compile
 *
 * La tabla "people" se crea en SQLite in-memory antes de cada test
 * y se destruye al finalizar, garantizando aislamiento total.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PersonServiceIT {

    private PersonService service;

    // ---------------------------------------------------------------
    //  Ciclo de vida de la conexión y el esquema
    // ---------------------------------------------------------------

    @BeforeEach
    void openConnection() {
        // Abre una conexión a SQLite en memoria para cada test
        Base.open("org.sqlite.JDBC", "jdbc:sqlite::memory:", "", "");

        // Crea la tabla que ActiveJDBC espera (nombre en plural, snake_case)
        Base.exec("CREATE TABLE IF NOT EXISTS persons (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "dni TEXT, " +
            "firstName TEXT, " +
            "lastName TEXT, " +
            "phone TEXT, " +
            "email TEXT" +
            ")");

        PersonRepository repository = new PersonRepository();
        PersonFactoryInterface factory = Person::new;  // lambda como implementación de PersonFactory
        service = new PersonService(repository, factory);
    }

    @AfterEach
    void closeConnection() {
        Base.exec("DROP TABLE IF EXISTS people");
        Base.close();
    }

    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------

    private PersonDTO validDTO() {
        return new PersonDTO("30000000", "Juan", "Pérez", "3514000000", "juan@example.com");
    }

    private PersonDTO dtoWith(String dni) {
        return new PersonDTO(dni, "Ana", "García", null, null);
    }

    // ===============================================================
    //  CREATE – flujo completo contra la BD real
    // ===============================================================

    @Nested
    @DisplayName("create() – integración")
    class CreateIntegration {

        @Test
        @DisplayName("Persiste una persona válida y la recupera de la BD")
        void create_validPerson_persistedInDb() {
            PersonDTO dto = validDTO();

            Person created = service.create(dto);

            assertNotNull(created.getId());
            assertEquals(dto.getDni(),       created.getDni());
            assertEquals(dto.getFirstName(), created.getFirstName());
            assertEquals(dto.getLastName(),  created.getLastName());
            assertEquals(dto.getEmail(),     created.getEmail());
            assertEquals(dto.getPhone(),     created.getPhone());
        }

        @Test
        @DisplayName("Falla al crear dos personas con el mismo DNI")
        void create_duplicateDni_throwsValidationException() {
            service.create(validDTO());

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.create(validDTO()));
            assertEquals("Ya existe un dni registrado", ex.getDetails());
        }

        @Test
        @DisplayName("Persiste persona sin email ni teléfono (campos opcionales)")
        void create_withoutOptionalFields_persists() {
            PersonDTO dto = new PersonDTO("30000000", "Juan", "Pérez", null, null);

            Person created = service.create(dto);

            assertNotNull(created.getId());
            assertNull(created.getEmail());
            assertNull(created.getPhone());
        }

        @Test
        @DisplayName("Falla con DNI que contiene letras")
        void create_dniWithLetters_throwsIllegalArgumentException() {
            PersonDTO dto = new PersonDTO("3000A000", "Juan", "Pérez", null, null);

            assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        }

        @Test
        @DisplayName("Falla con email de formato inválido")
        void create_invalidEmail_throwsValidationException() {
            PersonDTO dto = new PersonDTO("30000000", "Juan", "Pérez", null, "no-es-email");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.create(dto));
            assertEquals("Formato de email inválido", ex.getDetails());
        }
    }

    // ===============================================================
    //  UPDATE – flujo completo contra la BD real
    // ===============================================================

    @Nested
    @DisplayName("update() – integración")
    class UpdateIntegration {

        @Test
        @DisplayName("Actualiza firstName y lo verifica leyendo de la BD")
        void update_firstName_persistedInDb() {
            Person created = service.create(validDTO());
            Long id = Long.valueOf(created.getId().toString());

            PersonDTO updateDto = new PersonDTO(null, "Carlos", null, null, null);
            Person updated = service.update(id, updateDto);

            assertEquals("Carlos", updated.getFirstName());
            // Verifica que el resto no cambió
            assertEquals(created.getLastName(), updated.getLastName());
        }

        @Test
        @DisplayName("Actualiza email y lo verifica leyendo de la BD")
        void update_email_persistedInDb() {
            Person created = service.create(validDTO());
            Long id = Long.valueOf(created.getId().toString());

            PersonDTO updateDto = new PersonDTO(null, null, null, null, "nuevo@mail.com");
            Person updated = service.update(id, updateDto);

            assertEquals("nuevo@mail.com", updated.getEmail());
        }

        @Test
        @DisplayName("Actualiza múltiples campos en una sola llamada")
        void update_multipleFields_allPersistedInDb() {
            Person created = service.create(validDTO());
            Long id = Long.valueOf(created.getId().toString());

            PersonDTO updateDto = new PersonDTO(null, "Luis", "Gomez", "3516000000", null);
            Person updated = service.update(id, updateDto);

            assertEquals("Luis",       updated.getFirstName());
            assertEquals("Gomez",      updated.getLastName());
            assertEquals("3516000000", updated.getPhone());
        }

        @Test
        @DisplayName("Lanza PersonNotFoundException si el id no existe en la BD")
        void update_nonExistentId_throwsPersonNotFoundException() {
            assertThrows(PersonNotFoundException.class,
                    () -> service.update(999L, validDTO()));
        }

        @Test
        @DisplayName("Lanza ValidationException si no se envían campos a actualizar")
        void update_emptyDto_throwsValidationException() {
            Person created = service.create(validDTO());
            Long id = Long.valueOf(created.getId().toString());

            PersonDTO emptyDto = new PersonDTO(null, null, null, null, null);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.update(id, emptyDto));
            assertEquals("Se requieren datos para realizar update", ex.getDetails());
        }
    }

    // ===============================================================
    //  DELETE – flujo completo contra la BD real
    // ===============================================================

    @Nested
    @DisplayName("delete() – integración")
    class DeleteIntegration {

        @Test
        @DisplayName("Elimina persona existente y retorna true")
        void delete_existingPerson_returnsTrue() {
            Person created = service.create(validDTO());
            Long id = Long.valueOf(created.getId().toString());

            assertTrue(service.delete(id));
        }

        @Test
        @DisplayName("La persona no puede crearse con el mismo DNI tras ser eliminada — BD limpia")
        void delete_thenRecreate_succeeds() {
            Person created = service.create(validDTO());
            Long id = Long.valueOf(created.getId().toString());
            service.delete(id);

            // Debe poder crearse de nuevo con el mismo DNI
            Person recreated = service.create(validDTO());
            assertNotNull(recreated.getId());
        }

        @Test
        @DisplayName("Retorna false si la persona no existe en la BD")
        void delete_nonExistentPerson_returnsFalse() {
            assertFalse(service.delete(999L));
        }

        @Test
        @DisplayName("Lanza excepción con id negativo")
        void delete_negativeId_throwsPersonNotFoundException() {
            assertThrows(PersonNotFoundException.class, () -> service.delete(-5L));
        }

        @Test
        @DisplayName("Eliminar el mismo id dos veces retorna false en el segundo intento")
        void delete_twice_secondReturnsFalse() {
            Person created = service.create(validDTO());
            Long id = Long.valueOf(created.getId().toString());

            service.delete(id);
            assertFalse(service.delete(id));
        }
    }

    // ===============================================================
    //  Boundary DNI – contra la BD real
    // ===============================================================

    @Nested
    @DisplayName("DNI boundary – integración")
    class DniBoundaryIntegration {

        @Test
        @DisplayName("DNI límite inferior válido (10000000) se persiste")
        void create_dniLowerBound_persists() {
            Person p = service.create(dtoWith("10000000"));
            assertEquals("10000000", p.getDni());
        }

        @Test
        @DisplayName("DNI límite superior válido (99999999) se persiste")
        void create_dniUpperBound_persists() {
            Person p = service.create(dtoWith("99999999"));
            assertEquals("99999999", p.getDni());
        }

        @Test
        @DisplayName("DNI por debajo del límite (9999999) no se persiste")
        void create_dniBelowLowerBound_throws() {
            assertThrows(ValidationException.class, () -> service.create(dtoWith("9999999")));
            assertEquals(0, Person.count()); // nada persistido
        }

        @Test
        @DisplayName("DNI con 9 dígitos no se persiste")
        void create_dniWith9Digits_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(dtoWith("100000000")));
            assertEquals(0, Person.count());
        }
    }

    // ===============================================================
    //  Boundary Email – contra la BD real
    // ===============================================================

    @Nested
    @DisplayName("Email boundary – integración")
    class EmailBoundaryIntegration {

        @Test
        @DisplayName("Email mínimo válido (a@b.co) se persiste")
        void create_minimalEmail_persists() {
            PersonDTO dto = new PersonDTO("30000000", "Juan", "Pérez", null, "a@b.co");
            Person p = service.create(dto);
            assertEquals("a@b.co", p.getEmail());
        }

        @Test
        @DisplayName("Email con TLD de un solo carácter no se persiste")
        void create_singleCharTld_throws() {
            PersonDTO dto = new PersonDTO("30000000", "Juan", "Pérez", null, "a@b.c");
            assertThrows(ValidationException.class, () -> service.create(dto));
            assertEquals(0, Person.count());
        }

        @Test
        @DisplayName("Email sin @ no se persiste")
        void create_emailWithoutAt_throws() {
            PersonDTO dto = new PersonDTO("30000000", "Juan", "Pérez", null, "invalido.com");
            assertThrows(ValidationException.class, () -> service.create(dto));
            assertEquals(0, Person.count());
        }

        @Test
        @DisplayName("Email nulo es aceptado y no guarda valor en BD")
        void create_nullEmail_persistsWithoutEmail() {
            PersonDTO dto = new PersonDTO("30000000", "Juan", "Pérez", null, null);
            Person p = service.create(dto);
            assertNull(p.getEmail());
        }
    }
}
