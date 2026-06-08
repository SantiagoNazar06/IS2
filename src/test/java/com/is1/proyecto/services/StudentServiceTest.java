package com.is1.proyecto.services;

import com.is1.proyecto.dto.StudentListDTO;
import com.is1.proyecto.ports.out.StudentRepositoryInterface;
import com.is1.proyecto.repositories.EnrollmentRepository;
import com.is1.proyecto.repositories.EvaluationRepository;
import com.is1.proyecto.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StudentService.getStudents().
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepositoryInterface repository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(repository, enrollmentRepository, evaluationRepository);
    }

    @Test
    void testGetStudents_adminDelegatesWithIsAdminTrue() {
        List<StudentListDTO> expected = Arrays.asList(
                new StudentListDTO(java.util.Map.of("student_id", 1L, "full_name", "Admin Test", "dni", "1", "email", "a@t.com", "careers", "Ing."))
        );
        when(repository.findStudents(null, null, null, true)).thenReturn(expected);

        List<StudentListDTO> result = studentService.getStudents(null, null, null, "ADMIN");

        assertEquals(1, result.size());
        assertEquals("Admin Test", result.get(0).getFullName());
        verify(repository).findStudents(null, null, null, true);
    }

    @Test
    void testGetStudents_teacherDelegatesWithTeacherId() {
        List<StudentListDTO> expected = Arrays.asList(
                new StudentListDTO(java.util.Map.of("student_id", 2L, "full_name", "Teacher Student", "dni", "2", "email", "t@t.com", "careers", "Ing."))
        );
        when(repository.findStudents(null, null, 5L, false)).thenReturn(expected);

        List<StudentListDTO> result = studentService.getStudents(null, null, 5L, "TEACHER");

        assertEquals(1, result.size());
        assertEquals("Teacher Student", result.get(0).getFullName());
        verify(repository).findStudents(null, null, 5L, false);
    }

    @Test
    void testGetStudents_passesFiltersThrough() {
        when(repository.findStudents(10L, 20L, null, true)).thenReturn(Collections.emptyList());

        List<StudentListDTO> result = studentService.getStudents(10L, 20L, null, "ADMIN");

        assertTrue(result.isEmpty());
        verify(repository).findStudents(10L, 20L, null, true);
    }

    @Test
    void testGetStudents_teacherWithNullTeacherId() {
        when(repository.findStudents(null, null, null, false)).thenReturn(Collections.emptyList());

        List<StudentListDTO> result = studentService.getStudents(null, null, null, "TEACHER");

        assertTrue(result.isEmpty());
        verify(repository).findStudents(null, null, null, false);
    }

}
