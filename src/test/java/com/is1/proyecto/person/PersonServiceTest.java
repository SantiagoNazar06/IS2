package com.is1.proyecto.person;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.repositories.PersonRepository;
import com.is1.proyecto.services.PersonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository repository;

    private PersonService service;

    @BeforeEach
    void setUp() {
        service = new PersonService(repository);
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private PersonService.PersonDTO validDTO() {
        return new PersonService.PersonDTO(
                "30000000", "Juan", "Pérez", "3514000000", "juan@example.com"
        );
    }

    private Person personFrom(PersonService.PersonDTO dto) {
        Person p = new Person();
        p.setDni(dto.dni);
        p.setFirstName(dto.firstName);
        p.setLastName(dto.lastName);
        p.setEmail(dto.email);
        p.setPhone(dto.phone);
        return p;
    }

    // =========================================================
    //  CREATE – validaciones unitarias
    // =========================================================

    @Nested
    @DisplayName("create() – validaciones unitarias")
    class CreateValidations {

        @Test
        @DisplayName("Crea persona con datos válidos")
        void create_validData_returnsPerson() {
            PersonService.PersonDTO dto = validDTO();
            when(repository.findByDni(dto.dni)).thenReturn(null);

            Person result = service.create(dto);

            assertNotNull(result);
            assertEquals(dto.dni, result.getDni());
            assertEquals(dto.firstName, result.getFirstName());
            assertEquals(dto.lastName, result.getLastName());
            verify(repository).create(any(Person.class));
        }

        @Test
        @DisplayName("Lanza excepción si DNI es null")
        void create_nullDni_throwsValidationException() {
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    null, "Juan", "Pérez", null, null);

            PersonService.ValidationException ex = assertThrows(
                    PersonService.ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere dni", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si DNI está en blanco")
        void create_blankDni_throwsValidationException() {
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    "   ", "Juan", "Pérez", null, null);

            PersonService.ValidationException ex = assertThrows(
                    PersonService.ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere dni", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si DNI ya existe")
        void create_duplicateDni_throwsValidationException() {
            PersonService.PersonDTO dto = validDTO();
            when(repository.findByDni(dto.dni)).thenReturn(personFrom(dto));

            PersonService.ValidationException ex = assertThrows(
                    PersonService.ValidationException.class, () -> service.create(dto));
            assertEquals("Ya existe un dni registrado", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si firstName es null")
        void create_nullFirstName_throwsValidationException() {
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    "30000000", null, "Pérez", null, null);
            when(repository.findByDni(dto.dni)).thenReturn(null);

            PersonService.ValidationException ex = assertThrows(
                    PersonService.ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere nombre", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si firstName está en blanco")
        void create_blankFirstName_throwsValidationException() {
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    "30000000", "  ", "Pérez", null, null);
            when(repository.findByDni(dto.dni)).thenReturn(null);

            PersonService.ValidationException ex = assertThrows(
                    PersonService.ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere nombre", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si lastName es null")
        void create_nullLastName_throwsValidationException() {
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    "30000000", "Juan", null, null, null);
            when(repository.findByDni(dto.dni)).thenReturn(null);

            PersonService.ValidationException ex = assertThrows(
                    PersonService.ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere apellido", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si lastName está en blanco")
        void create_blankLastName_throwsValidationException() {
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    "30000000", "Juan", "  ", null, null);
            when(repository.findByDni(dto.dni)).thenReturn(null);

            PersonService.ValidationException ex = assertThrows(
                    PersonService.ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere apellido", ex.getDetails());
        }

        @Test
        @DisplayName("Crea persona sin email (email es opcional)")
        void create_noEmail_succeeds() {
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    "30000000", "Juan", "Pérez", null, null);
            when(repository.findByDni(dto.dni)).thenReturn(null);

            Person result = service.create(dto);

            assertNotNull(result);
            verify(repository).create(any(Person.class));
        }

        @Test
        @DisplayName("Lanza excepción si email tiene formato inválido")
        void create_invalidEmailFormat_throwsValidationException() {
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    "30000000", "Juan", "Pérez", null, "not-an-email");
            when(repository.findByDni(dto.dni)).thenReturn(null);

            PersonService.ValidationException ex = assertThrows(
                    PersonService.ValidationException.class, () -> service.create(dto));
            assertEquals("Formato de email inválido", ex.getDetails());
        }
    }

    // =========================================================
    //  UPDATE – validaciones unitarias
    // =========================================================

    @Nested
    @DisplayName("update() – validaciones unitarias")
    class UpdateValidations {

        @Test
        @DisplayName("Lanza PersonNotFoundException si id no existe")
        void update_nonExistentId_throwsPersonNotFoundException() {
            when(repository.findById(99L)).thenReturn(null);

            assertThrows(PersonService.PersonNotFoundException.class,
                    () -> service.update(99L, validDTO()));
        }

        @Test
        @DisplayName("Lanza ValidationException si no hay campos para actualizar")
        void update_noFields_throwsValidationException() {
            Person existing = personFrom(validDTO());
            when(repository.findById(1L)).thenReturn(existing);

            PersonService.PersonDTO emptyDto = new PersonService.PersonDTO(
                    null, null, null, null, null);

            PersonService.ValidationException ex = assertThrows(
                    PersonService.ValidationException.class,
                    () -> service.update(1L, emptyDto));
            assertEquals("Se requieren datos para realizar update", ex.getDetails());
        }

        @Test
        @DisplayName("Actualiza correctamente con campos válidos")
        void update_validFields_returnsUpdatedPerson() {
            Person existing = personFrom(validDTO());
            when(repository.findById(1L)).thenReturn(existing);
            when(repository.findById(1L)).thenReturn(existing); // post-update fetch

            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    null, "Carlos", null, null, null);

            Person result = service.update(1L, dto);

            assertNotNull(result);
            verify(repository).update(eq(1L), argThat(m -> "Carlos".equals(m.get("firstname"))));
        }
    }

    // =========================================================
    //  DELETE – validaciones unitarias
    // =========================================================

    @Nested
    @DisplayName("delete() – validaciones unitarias")
    class DeleteValidations {

        @Test
        @DisplayName("Retorna false si la persona no existe")
        void delete_nonExistentPerson_returnsFalse() {
            when(repository.findById(1L)).thenReturn(null);

            boolean result = service.delete(1L);

            assertFalse(result);
            verify(repository, never()).delete(anyLong());
        }

        @Test
        @DisplayName("Retorna true si la persona existe y se elimina")
        void delete_existingPerson_returnsTrue() {
            when(repository.findById(1L)).thenReturn(personFrom(validDTO()));
            when(repository.delete(1L)).thenReturn(true);

            assertTrue(service.delete(1L));
        }

        @Test
        @DisplayName("Lanza excepción si id es negativo")
        void delete_negativeId_throwsPersonNotFoundException() {
            assertThrows(PersonService.PersonNotFoundException.class,
                    () -> service.delete(-1L));
        }
    }

    // =========================================================
    //  validateDni() – boundary tests
    // =========================================================

    @Nested
    @DisplayName("validateDni() – boundary tests")
    class DniBoundaryTests {

        @Test
        @DisplayName("DNI mínimo válido: 10000000")
        void validateDni_minimumValid_ok() {
            assertDoesNotThrow(() -> PersonService.validateDni("10000000"));
        }

        @Test
        @DisplayName("DNI máximo válido: 99999999")
        void validateDni_maximumValid_ok() {
            assertDoesNotThrow(() -> PersonService.validateDni("99999999"));
        }

        @Test
        @DisplayName("DNI por debajo del límite inferior: 9999999 (7 dígitos)")
        void validateDni_belowLowerBound_throwsValidationException() {
            PersonService.ValidationException ex = assertThrows(
                    PersonService.ValidationException.class,
                    () -> PersonService.validateDni("9999999"));
            assertTrue(ex.getDetails().contains("mayor"));
        }

        @Test
        @DisplayName("DNI justo por debajo del límite inferior: 10000000 - 1")
        void validateDni_justBelowLowerBound_throwsValidationException() {
            // 9999999 es el valor inmediatamente inferior a 10000000
            assertThrows(PersonService.ValidationException.class,
                    () -> PersonService.validateDni("9999999"));
        }

        @Test
        @DisplayName("DNI por encima del límite superior: 100000000 (9 dígitos)")
        void validateDni_aboveUpperBound_throwsIllegalArgumentException() {
            // 9 dígitos: supera el length check primero
            assertThrows(IllegalArgumentException.class,
                    () -> PersonService.validateDni("100000000"));
        }

        @Test
        @DisplayName("DNI con letras lanza IllegalArgumentException")
        void validateDni_withLetters_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> PersonService.validateDni("3000A000"));
        }

        @Test
        @DisplayName("DNI con guion lanza IllegalArgumentException")
        void validateDni_withDash_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> PersonService.validateDni("30-000000"));
        }

        @Test
        @DisplayName("DNI con espacio interno lanza IllegalArgumentException")
        void validateDni_withInternalSpace_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> PersonService.validateDni("300 00000"));
        }

        @Test
        @DisplayName("DNI válido de 8 dígitos (longitud exacta)")
        void validateDni_exactLength8_ok() {
            assertDoesNotThrow(() -> PersonService.validateDni("50000000"));
        }
    }

    // =========================================================
    //  validateEmail() – boundary tests (a través de create)
    // =========================================================

    @Nested
    @DisplayName("validateEmail() – boundary tests")
    class EmailBoundaryTests {

        private void stubFindByDni(String dni) {
            when(repository.findByDni(dni)).thenReturn(null);
        }

        @Test
        @DisplayName("Email mínimo válido: a@b.co")
        void email_minimal_valid() {
            String dni = "30000001";
            stubFindByDni(dni);
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    dni, "Juan", "Pérez", null, "a@b.co");
            assertDoesNotThrow(() -> service.create(dto));
        }

        @Test
        @DisplayName("Email con subdominio válido")
        void email_subdomain_valid() {
            String dni = "30000002";
            stubFindByDni(dni);
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    dni, "Juan", "Pérez", null, "user@mail.example.com");
            assertDoesNotThrow(() -> service.create(dto));
        }

        @Test
        @DisplayName("Email con caracteres especiales permitidos en local-part")
        void email_specialCharsInLocal_valid() {
            String dni = "30000003";
            stubFindByDni(dni);
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    dni, "Juan", "Pérez", null, "user.name+tag@example.org");
            assertDoesNotThrow(() -> service.create(dto));
        }

        @Test
        @DisplayName("Email sin @ es inválido")
        void email_missingAt_invalid() {
            String dni = "30000004";
            stubFindByDni(dni);
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    dni, "Juan", "Pérez", null, "userexample.com");

            assertThrows(PersonService.ValidationException.class,
                    () -> service.create(dto));
        }

        @Test
        @DisplayName("Email sin dominio es inválido")
        void email_missingDomain_invalid() {
            String dni = "30000005";
            stubFindByDni(dni);
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    dni, "Juan", "Pérez", null, "user@");

            assertThrows(PersonService.ValidationException.class,
                    () -> service.create(dto));
        }

        @Test
        @DisplayName("Email sin TLD es inválido")
        void email_missingTld_invalid() {
            String dni = "30000006";
            stubFindByDni(dni);
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    dni, "Juan", "Pérez", null, "user@example");

            assertThrows(PersonService.ValidationException.class,
                    () -> service.create(dto));
        }

        @Test
        @DisplayName("Email con TLD de un solo caracter es inválido")
        void email_singleCharTld_invalid() {
            String dni = "30000007";
            stubFindByDni(dni);
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    dni, "Juan", "Pérez", null, "user@example.c");

            assertThrows(PersonService.ValidationException.class,
                    () -> service.create(dto));
        }

        @Test
        @DisplayName("Email null se acepta (campo opcional)")
        void email_null_accepted() {
            String dni = "30000008";
            stubFindByDni(dni);
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    dni, "Juan", "Pérez", null, null);
            assertDoesNotThrow(() -> service.create(dto));
        }

        @Test
        @DisplayName("Email con doble @ es inválido")
        void email_doubleAt_invalid() {
            String dni = "30000009";
            stubFindByDni(dni);
            PersonService.PersonDTO dto = new PersonService.PersonDTO(
                    dni, "Juan", "Pérez", null, "user@@example.com");

            assertThrows(PersonService.ValidationException.class,
                    () -> service.create(dto));
        }
    }
}
