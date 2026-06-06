package com.is1.proyecto.services;

import com.is1.proyecto.dto.StudyPlanDTO;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.models.Career;
import com.is1.proyecto.models.StudyPlan;
import com.is1.proyecto.repositories.StudyPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StudyPlanService.
 * Uses @Mock StudyPlanRepository + MockedStatic<Career> for Career.findById calls.
 * Follows the ConditionServiceTest pattern.
 */
@ExtendWith(MockitoExtension.class)
class StudyPlanServiceTest {

    @Mock
    private StudyPlanRepository repository;

    @InjectMocks
    private StudyPlanService service;

    // =====================================================================
    // registerStudyPlan — validation errors
    // =====================================================================

    @Test
    void registerStudyPlan_nullName_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.registerStudyPlan(null, 2024, 1));
    }

    @Test
    void registerStudyPlan_emptyName_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.registerStudyPlan("", 2024, 1));
    }

    @Test
    void registerStudyPlan_blankName_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.registerStudyPlan("   ", 2024, 1));
    }

    @Test
    void registerStudyPlan_invalidYear_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.registerStudyPlan("Plan 2024", 0, 1));
    }

    @Test
    void registerStudyPlan_negativeYear_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.registerStudyPlan("Plan 2024", -1, 1));
    }

    // =====================================================================
    // registerStudyPlan — career validation
    // =====================================================================

    @Test
    void registerStudyPlan_careerNotFound_throwsValidationException() {
        try (MockedStatic<Career> careerMock = mockStatic(Career.class)) {
            careerMock.when(() -> Career.findById(any())).thenReturn(null);

            assertThrows(ValidationException.class,
                    () -> service.registerStudyPlan("Plan 2024", 2024, 999));
        }
    }

    // =====================================================================
    // registerStudyPlan — success
    // =====================================================================

    @Test
    void registerStudyPlan_success_returnsDTO() {
        Career mockCareer = mock(Career.class);
        when(mockCareer.getCareerName()).thenReturn("Ingenieria");

        try (MockedStatic<Career> careerMock = mockStatic(Career.class);
             MockedConstruction<StudyPlan> spConst = mockConstruction(StudyPlan.class,
                     (plan, ctx) -> {
                         when(plan.getId()).thenReturn(1);
                         when(plan.getName()).thenReturn("Plan 2024");
                         when(plan.getYear()).thenReturn(2024);
                         when(plan.getCareerId()).thenReturn(1);
                         when(plan.getCareer()).thenReturn(mockCareer);
                     })) {

            careerMock.when(() -> Career.findById(any())).thenReturn(mockCareer);
            when(repository.create(any(StudyPlan.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            StudyPlanDTO result = service.registerStudyPlan("Plan 2024", 2024, 1);

            assertNotNull(result);
            assertEquals(1, result.getId());
            assertEquals("Plan 2024", result.getName());
            assertEquals(2024, result.getYear());
            assertEquals(1, result.getCareerId());
            assertEquals("Ingenieria", result.getCareerName());
            verify(repository).create(any(StudyPlan.class));
        }
    }

    // =====================================================================
    // updateStudyPlan
    // =====================================================================

    @Test
    void updateStudyPlan_notFound_throwsValidationException() {
        when(repository.findById(1)).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> service.updateStudyPlan(1, "New Name", 2025, 1));
    }

    @Test
    void updateStudyPlan_success_returnsDTO() {
        StudyPlan mockPlan = mock(StudyPlan.class);
        when(mockPlan.getId()).thenReturn(1);
        when(mockPlan.getName()).thenReturn("Updated Plan");
        when(mockPlan.getYear()).thenReturn(2025);
        when(mockPlan.getCareerId()).thenReturn(1);
        when(repository.findById(1)).thenReturn(mockPlan);

        try (MockedStatic<Career> careerMock = mockStatic(Career.class)) {
            Career mockCareer = mock(Career.class);
            when(mockCareer.getCareerName()).thenReturn("Ingenieria");
            careerMock.when(() -> Career.findById(any())).thenReturn(mockCareer);
            when(mockPlan.getCareer()).thenReturn(mockCareer);

            StudyPlanDTO result = service.updateStudyPlan(1, "Updated Plan", 2025, 1);

            assertNotNull(result);
            assertEquals(1, result.getId());
            assertEquals("Updated Plan", result.getName());
            verify(repository).update(mockPlan);
        }
    }

    @Test
    void updateStudyPlan_partialUpdate_onlyName() {
        StudyPlan mockPlan = mock(StudyPlan.class);
        when(mockPlan.getId()).thenReturn(1);
        when(mockPlan.getName()).thenReturn("Only Name Update");
        when(mockPlan.getYear()).thenReturn(2024);
        when(mockPlan.getCareerId()).thenReturn(1);
        when(repository.findById(1)).thenReturn(mockPlan);

        try (MockedStatic<Career> careerMock = mockStatic(Career.class)) {
            Career mockCareer = mock(Career.class);
            careerMock.when(() -> Career.findById(any())).thenReturn(mockCareer);
            when(mockPlan.getCareer()).thenReturn(mockCareer);

            StudyPlanDTO result = service.updateStudyPlan(1, "Only Name Update", null, null);

            assertNotNull(result);
            assertEquals("Only Name Update", result.getName());
            assertEquals(2024, result.getYear()); // unchanged
            // Should not set year or careerId since they are null
            verify(mockPlan, never()).setYear(anyInt());
            verify(mockPlan, never()).setCareerId(any());
            verify(repository).update(mockPlan);
        }
    }

    @Test
    void updateStudyPlan_careerNotFound_throwsValidationException() {
        StudyPlan mockPlan = mock(StudyPlan.class);
        when(repository.findById(1)).thenReturn(mockPlan);

        try (MockedStatic<Career> careerMock = mockStatic(Career.class)) {
            careerMock.when(() -> Career.findById(any())).thenReturn(null);

            assertThrows(ValidationException.class,
                    () -> service.updateStudyPlan(1, null, null, 999));
        }
    }

    // =====================================================================
    // deleteStudyPlan
    // =====================================================================

    @Test
    void deleteStudyPlan_notFound_throwsValidationException() {
        when(repository.deleteById(1)).thenReturn(false);

        assertThrows(ValidationException.class,
                () -> service.deleteStudyPlan(1));
    }

    @Test
    void deleteStudyPlan_success() {
        when(repository.deleteById(1)).thenReturn(true);

        assertDoesNotThrow(() -> service.deleteStudyPlan(1));
        verify(repository).deleteById(1);
    }

    // =====================================================================
    // getStudyPlanById
    // =====================================================================

    @Test
    void getStudyPlanById_notFound_throwsValidationException() {
        when(repository.findById(1)).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> service.getStudyPlanById(1));
    }

    @Test
    void getStudyPlanById_success_returnsDTO() {
        StudyPlan mockPlan = mock(StudyPlan.class);
        when(mockPlan.getId()).thenReturn(1);
        when(mockPlan.getName()).thenReturn("Plan 2024");
        when(mockPlan.getYear()).thenReturn(2024);
        when(mockPlan.getCareerId()).thenReturn(1);
        when(repository.findById(1)).thenReturn(mockPlan);

        try (MockedStatic<Career> careerMock = mockStatic(Career.class)) {
            Career mockCareer = mock(Career.class);
            when(mockCareer.getCareerName()).thenReturn("Ingenieria");
            careerMock.when(() -> Career.findById(any())).thenReturn(mockCareer);
            when(mockPlan.getCareer()).thenReturn(mockCareer);

            StudyPlanDTO result = service.getStudyPlanById(1);

            assertNotNull(result);
            assertEquals(1, result.getId());
            assertEquals("Plan 2024", result.getName());
            assertEquals(2024, result.getYear());
        }
    }

    // =====================================================================
    // getAllStudyPlans
    // =====================================================================

    @Test
    void getAllStudyPlans_withCareerId_returnsFilteredList() {
        StudyPlan p1 = mock(StudyPlan.class);
        when(p1.getId()).thenReturn(1);
        when(p1.getName()).thenReturn("Plan A");
        when(p1.getYear()).thenReturn(2024);
        when(p1.getCareerId()).thenReturn(1);
        when(repository.findByCareerId(1)).thenReturn(Arrays.asList(p1));

        try (MockedStatic<Career> careerMock = mockStatic(Career.class)) {
            Career mockCareer = mock(Career.class);
            when(mockCareer.getCareerName()).thenReturn("Ingenieria");
            careerMock.when(() -> Career.findById(any())).thenReturn(mockCareer);
            when(p1.getCareer()).thenReturn(mockCareer);

            List<StudyPlanDTO> result = service.getAllStudyPlans(1);

            assertEquals(1, result.size());
            assertEquals("Plan A", result.get(0).getName());
            verify(repository).findByCareerId(1);
            verify(repository, never()).findAll();
        }
    }

    @Test
    void getAllStudyPlans_withoutCareerId_returnsAll() {
        StudyPlan p1 = mock(StudyPlan.class);
        when(p1.getId()).thenReturn(1);
        when(p1.getName()).thenReturn("Plan A");
        when(p1.getYear()).thenReturn(2024);
        when(p1.getCareerId()).thenReturn(1);
        when(repository.findAll()).thenReturn(Arrays.asList(p1));

        try (MockedStatic<Career> careerMock = mockStatic(Career.class)) {
            Career mockCareer = mock(Career.class);
            when(mockCareer.getCareerName()).thenReturn("Ingenieria");
            careerMock.when(() -> Career.findById(any())).thenReturn(mockCareer);
            when(p1.getCareer()).thenReturn(mockCareer);

            List<StudyPlanDTO> result = service.getAllStudyPlans(null);

            assertEquals(1, result.size());
            verify(repository).findAll();
            verify(repository, never()).findByCareerId(anyInt());
        }
    }

    @Test
    void getAllStudyPlans_emptyList() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<StudyPlanDTO> result = service.getAllStudyPlans(null);

        assertTrue(result.isEmpty());
        verify(repository).findAll();
    }
}
