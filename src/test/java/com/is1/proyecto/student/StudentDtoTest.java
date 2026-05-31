package com.is1.proyecto.student;

import com.is1.proyecto.dto.EnrollmentDTO;
import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.dto.GradeDTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Phase 1 DTOs (EnrollmentDTO, SubjectDTO, GradeDTO).
 * These are structural tests — no logic, only constructor + getters.
 */
public class StudentDtoTest {

    @Test
    void testEnrollmentDTO_creationAndGetters() {
        EnrollmentDTO dto = new EnrollmentDTO(1L, "ENROLLED", 4.5, "2024-01");

        assertEquals(1L, dto.getSubjectId());
        assertEquals("ENROLLED", dto.getStatus());
        assertEquals(4.5, dto.getGrade());
        assertEquals("2024-01", dto.getPeriod());
    }

    @Test
    void testEnrollmentDTO_nullGrade() {
        EnrollmentDTO dto = new EnrollmentDTO(2L, "COMPLETED", null, "2024-02");

        assertEquals(2L, dto.getSubjectId());
        assertEquals("COMPLETED", dto.getStatus());
        assertNull(dto.getGrade());
        assertEquals("2024-02", dto.getPeriod());
    }

    @Test
    void testSubjectDTO_creationAndGetters() {
        SubjectDTO dto = new SubjectDTO(1L, "Matematica", "ENROLLED", null, "2024-01");

        assertEquals(1L, dto.getSubjectId());
        assertEquals("Matematica", dto.getSubjectName());
        assertEquals("ENROLLED", dto.getStatus());
        assertNull(dto.getGrade());
        assertEquals("2024-01", dto.getPeriod());
    }

    @Test
    void testSubjectDTO_withGrade() {
        SubjectDTO dto = new SubjectDTO(2L, "Fisica", "COMPLETED", 7.5, "2024-01");

        assertEquals(2L, dto.getSubjectId());
        assertEquals("Fisica", dto.getSubjectName());
        assertEquals("COMPLETED", dto.getStatus());
        assertEquals(7.5, dto.getGrade());
        assertEquals("2024-01", dto.getPeriod());
    }

    @Test
    void testGradeDTO_creationAndGetters() {
        GradeDTO dto = new GradeDTO("Matematica", 8.0, "2024-06-15");

        assertEquals("Matematica", dto.getSubjectName());
        assertEquals(8.0, dto.getGrade());
        assertEquals("2024-06-15", dto.getDate());
    }

    @Test
    void testGradeDTO_differentValues() {
        GradeDTO dto = new GradeDTO("Historia", 6.5, "2024-07-01");

        assertEquals("Historia", dto.getSubjectName());
        assertEquals(6.5, dto.getGrade());
        assertEquals("2024-07-01", dto.getDate());
    }
}
