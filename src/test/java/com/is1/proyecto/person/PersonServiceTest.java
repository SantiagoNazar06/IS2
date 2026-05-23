package com.is1.proyecto.person;

import com.is1.proyecto.dto.PersonDTO;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.factories.PersonFactory;
import com.is1.proyecto.exceptions.PersonNotFoundException;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.ports.out.PersonRepositoryInterface;
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
    private PersonRepositoryInterface repository;

    @Mock
    private PersonFactory personFactory;

    private PersonService service;

    @BeforeEach
    void setUp() {
        // Sin stubs globales: cada test declara solo lo que necesita
        service = new PersonService(repository, personFactory);
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private PersonDTO validDTO() {
        return new PersonDTO("30000000", "Juan", "Pérez", "3514000000", "juan@example.com");
    }

    /**
     * Crea un mock de Person con los getters configurados.
     * IMPORTANTE: nunca llamar esta función dentro de un thenReturn() en curso.
     * Siempre asignar a una variable primero.
     */
    private Person personFrom(PersonDTO dto) {
        Person p = mock(Person.class);
        lenient().when(p.getDni()).thenReturn(dto.getDni());
        lenient().when(p.getFirstName()).thenReturn(dto.getFirstName());
        lenient().when(p.getLastName()).thenReturn(dto.getLastName());
        lenient().when(p.getEmail()).thenReturn(dto.getEmail());
        lenient().when(p.getPhone()).thenReturn(dto.getPhone());
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
            PersonDTO dto = validDTO();
            Person personMock = mock(Person.class);
            when(personFactory.create()).thenReturn(personMock);
            when(personMock.getDni()).thenReturn(dto.getDni());
            when(personMock.getFirstName()).thenReturn(dto.getFirstName());
            when(personMock.getLastName()).thenReturn(dto.getLastName());
            when(repository.findByDni(dto.getDni())).thenReturn(null);

            Person result = service.create(dto);

            assertNotNull(result);
            assertEquals(dto.getDni(), result.getDni());
            assertEquals(dto.getFirstName(), result.getFirstName());
            assertEquals(dto.getLastName(), result.getLastName());
            verify(repository).create(any(Person.class));
        }

        @Test
        @DisplayName("Lanza excepción si DNI es null")
        void create_nullDni_throwsValidationException() {
            PersonDTO dto = new PersonDTO(null, "Juan", "Pérez", null, null);

            ValidationException ex = assertThrows(
                    ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere dni", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si DNI está en blanco")
        void create_blankDni_throwsValidationException() {
            PersonDTO dto = new PersonDTO("   ", "Juan", "Pérez", null, null);

            ValidationException ex = assertThrows(
                    ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere dni", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si DNI ya existe")
        void create_duplicateDni_throwsValidationException() {
            PersonDTO dto = validDTO();
            Person existing = personFrom(dto); // variable primero, nunca inline en thenReturn
            when(repository.findByDni(dto.getDni())).thenReturn(existing);

            ValidationException ex = assertThrows(
                    ValidationException.class, () -> service.create(dto));
            assertEquals("No es posible realizar el registro.", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si firstName es null")
        void create_nullFirstName_throwsValidationException() {
            PersonDTO dto = new PersonDTO("30000000", null, "Pérez", null, null);
            when(repository.findByDni(dto.getDni())).thenReturn(null);

            ValidationException ex = assertThrows(
                    ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere nombre", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si firstName está en blanco")
        void create_blankFirstName_throwsValidationException() {
            PersonDTO dto = new PersonDTO("30000000", "  ", "Pérez", null, null);
            when(repository.findByDni(dto.getDni())).thenReturn(null);

            ValidationException ex = assertThrows(
                    ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere nombre", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si lastName es null")
        void create_nullLastName_throwsValidationException() {
            PersonDTO dto = new PersonDTO("30000000", "Juan", null, null, null);
            when(repository.findByDni(dto.getDni())).thenReturn(null);

            ValidationException ex = assertThrows(
                    ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere apellido", ex.getDetails());
        }

        @Test
        @DisplayName("Lanza excepción si lastName está en blanco")
        void create_blankLastName_throwsValidationException() {
            PersonDTO dto = new PersonDTO("30000000", "Juan", "  ", null, null);
            when(repository.findByDni(dto.getDni())).thenReturn(null);

            ValidationException ex = assertThrows(
                    ValidationException.class, () -> service.create(dto));
            assertEquals("Se requiere apellido", ex.getDetails());
        }

        @Test
        @DisplayName("Crea persona sin email (email es opcional)")
        void create_noEmail_succeeds() {
            PersonDTO dto = new PersonDTO("30000000", "Juan", "Pérez", null, null);
            when(personFactory.create()).thenReturn(mock(Person.class));
            when(repository.findByDni(dto.getDni())).thenReturn(null);

            Person result = service.create(dto);

            assertNotNull(result);
            verify(repository).create(any(Person.class));
        }

        @Test
        @DisplayName("Lanza excepción si email tiene formato inválido")
        void create_invalidEmailFormat_throwsValidationException() {
            PersonDTO dto = new PersonDTO("30000000", "Juan", "Pérez", null, "not-an-email");
            when(repository.findByDni(dto.getDni())).thenReturn(null);

            ValidationException ex = assertThrows(
                    ValidationException.class, () -> service.create(dto));
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

            assertThrows(PersonNotFoundException.class,
                    () -> service.update(99L, validDTO()));
        }

        @Test
        @DisplayName("Lanza ValidationException si no hay campos para actualizar")
        void update_noFields_throwsValidationException() {
            Person existing = personFrom(validDTO());
            when(repository.findById(1L)).thenReturn(existing);

            PersonDTO emptyDto = new PersonDTO(null, null, null, null, null);

            ValidationException ex = assertThrows(
                    ValidationException.class,
                    () -> service.update(1L, emptyDto));
            assertEquals("Se requieren datos para realizar update", ex.getDetails());
        }

        @Test
        @DisplayName("Actualiza correctamente con campos válidos")
        void update_validFields_returnsUpdatedPerson() {
            Person existing = personFrom(validDTO());
            when(repository.findById(1L)).thenReturn(existing);

            PersonDTO dto = new PersonDTO(null, "Carlos", null, null, null);

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
            Person existing = personFrom(validDTO()); // variable primero
            when(repository.findById(1L)).thenReturn(existing);
            when(repository.delete(1L)).thenReturn(true);

            assertTrue(service.delete(1L));
        }

        @Test
        @DisplayName("Lanza excepción si id es negativo")
        void delete_negativeId_throwsPersonNotFoundException() {
            assertThrows(PersonNotFoundException.class,
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
            ValidationException ex = assertThrows(
                    ValidationException.class,
                    () -> PersonService.validateDni("9999999"));
            assertTrue(ex.getDetails().contains("mayor"));
        }

        @Test
        @DisplayName("DNI justo por debajo del límite inferior: 10000000 - 1")
        void validateDni_justBelowLowerBound_throwsValidationException() {
            assertThrows(ValidationException.class,
                    () -> PersonService.validateDni("9999999"));
        }

        @Test
        @DisplayName("DNI por encima del límite superior: 100000000 (9 dígitos)")
        void validateDni_aboveUpperBound_throwsIllegalArgumentException() {
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
    //  validateEmail() – boundary tests
    // =========================================================

    @Nested
    @DisplayName("validateEmail() – boundary tests")
    class EmailBoundaryTests {

        private Person setupCreateMocks(String dni) {
            Person personMock = mock(Person.class);
            when(personFactory.create()).thenReturn(personMock);
            when(repository.findByDni(dni)).thenReturn(null);
            return personMock;
        }

        @Test
        @DisplayName("Email mínimo válido: a@b.co")
        void email_minimal_valid() {
            setupCreateMocks("30000001");
            PersonDTO dto = new PersonDTO("30000001", "Juan", "Pérez", null, "a@b.co");
            assertDoesNotThrow(() -> service.create(dto));
        }

        @Test
        @DisplayName("Email con subdominio válido")
        void email_subdomain_valid() {
            setupCreateMocks("30000002");
            PersonDTO dto = new PersonDTO("30000002", "Juan", "Pérez", null, "user@mail.example.com");
            assertDoesNotThrow(() -> service.create(dto));
        }

        @Test
        @DisplayName("Email con caracteres especiales permitidos en local-part")
        void email_specialCharsInLocal_valid() {
            setupCreateMocks("30000003");
            PersonDTO dto = new PersonDTO("30000003", "Juan", "Pérez", null, "user.name+tag@example.org");
            assertDoesNotThrow(() -> service.create(dto));
        }

        @Test
        @DisplayName("Email sin @ es inválido")
        void email_missingAt_invalid() {
            when(repository.findByDni("30000004")).thenReturn(null);
            PersonDTO dto = new PersonDTO("30000004", "Juan", "Pérez", null, "userexample.com");
            assertThrows(ValidationException.class, () -> service.create(dto));
        }

        @Test
        @DisplayName("Email sin dominio es inválido")
        void email_missingDomain_invalid() {
            when(repository.findByDni("30000005")).thenReturn(null);
            PersonDTO dto = new PersonDTO("30000005", "Juan", "Pérez", null, "user@");
            assertThrows(ValidationException.class, () -> service.create(dto));
        }

        @Test
        @DisplayName("Email sin TLD es inválido")
        void email_missingTld_invalid() {
            when(repository.findByDni("30000006")).thenReturn(null);
            PersonDTO dto = new PersonDTO("30000006", "Juan", "Pérez", null, "user@example");
            assertThrows(ValidationException.class, () -> service.create(dto));
        }

        @Test
        @DisplayName("Email con TLD de un solo caracter es inválido")
        void email_singleCharTld_invalid() {
            when(repository.findByDni("30000007")).thenReturn(null);
            PersonDTO dto = new PersonDTO("30000007", "Juan", "Pérez", null, "user@example.c");
            assertThrows(ValidationException.class, () -> service.create(dto));
        }

        @Test
        @DisplayName("Email null se acepta (campo opcional)")
        void email_null_accepted() {
            setupCreateMocks("30000008");
            PersonDTO dto = new PersonDTO("30000008", "Juan", "Pérez", null, null);
            assertDoesNotThrow(() -> service.create(dto));
        }

        @Test
        @DisplayName("Email con doble @ es inválido")
        void email_doubleAt_invalid() {
            when(repository.findByDni("30000009")).thenReturn(null);
            PersonDTO dto = new PersonDTO("30000009", "Juan", "Pérez", null, "user@@example.com");
            assertThrows(ValidationException.class, () -> service.create(dto));
        }
    }
}