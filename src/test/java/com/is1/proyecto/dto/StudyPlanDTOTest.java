package com.is1.proyecto.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StudyPlanDTO.
 * DTO with constructor and getters only (all fields are final).
 */
class StudyPlanDTOTest {

    @Test
    void constructor_shouldSetAllFields() {
        StudyPlanDTO dto = new StudyPlanDTO(1, "Plan 2024", 2024, 10, "Ingeniería");

        assertEquals(Integer.valueOf(1), dto.getId());
        assertEquals("Plan 2024", dto.getName());
        assertEquals(Integer.valueOf(2024), dto.getYear());
        assertEquals(Integer.valueOf(10), dto.getCareerId());
        assertEquals("Ingeniería", dto.getCareerName());
    }

    @Test
    void constructor_shouldAcceptNullValues() {
        StudyPlanDTO dto = new StudyPlanDTO(null, null, null, null, null);

        assertNull(dto.getId());
        assertNull(dto.getName());
        assertNull(dto.getYear());
        assertNull(dto.getCareerId());
        assertNull(dto.getCareerName());
    }
}
