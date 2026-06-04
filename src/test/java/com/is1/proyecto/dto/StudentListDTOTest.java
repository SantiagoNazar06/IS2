package com.is1.proyecto.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StudentListDTO.
 * DTO with constructor from Map row and getters.
 */
class StudentListDTOTest {

    @Test
    void testConstructorFromMap_withAllFields() {
        Map<String, Object> row = Map.of(
                "student_id", 1L,
                "full_name", "Juan Perez",
                "dni", "12345678",
                "email", "juan@test.com",
                "careers", "Ingenieria, Licenciatura"
        );

        StudentListDTO dto = new StudentListDTO(row);

        assertEquals(Long.valueOf(1L), dto.getStudentId());
        assertEquals("Juan Perez", dto.getFullName());
        assertEquals("12345678", dto.getDni());
        assertEquals("juan@test.com", dto.getEmail());
        assertEquals("Ingenieria, Licenciatura", dto.getCareers());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testConstructorFromMap_withNullCareers() {
        Map<String, Object> row = new java.util.HashMap<>();
        row.put("student_id", 2L);
        row.put("full_name", "Maria Gomez");
        row.put("dni", "87654321");
        row.put("email", "maria@test.com");
        row.put("careers", null);

        StudentListDTO dto = new StudentListDTO(row);

        assertEquals(Long.valueOf(2L), dto.getStudentId());
        assertEquals("Maria Gomez", dto.getFullName());
        assertEquals("87654321", dto.getDni());
        assertEquals("maria@test.com", dto.getEmail());
        assertNull(dto.getCareers());
    }

    @Test
    void testConstructorFromMap_withAllNumbers() {
        // Verify student_id works with Integer (SQLite returns Integer)
        Map<String, Object> row = Map.of(
                "student_id", 3,
                "full_name", "Carlos Lopez",
                "dni", "11223344",
                "email", "carlos@test.com",
                "careers", "Medicina"
        );

        StudentListDTO dto = new StudentListDTO(row);

        assertEquals(Long.valueOf(3L), dto.getStudentId());
        assertEquals("Carlos Lopez", dto.getFullName());
        assertEquals("11223344", dto.getDni());
        assertEquals("carlos@test.com", dto.getEmail());
        assertEquals("Medicina", dto.getCareers());
    }
}
