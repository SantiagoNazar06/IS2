package com.is1.proyecto.services;

import com.is1.proyecto.models.Evaluation;
import com.is1.proyecto.repositories.EvaluationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EvaluationService.
 * Follows the ConditionServiceTest pattern: @ExtendWith(MockitoExtension.class),
 * @Mock EvaluationRepository, @InjectMocks EvaluationService.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationRepository repository;

    @InjectMocks
    private EvaluationService service;

    // =====================================================================
    // registerEvaluation — grade validation
    // =====================================================================

    @Test
    void registerEvaluation_nullGrade_returnsError() {
        EvaluationService.EvaluationRegisterResult result = service.registerEvaluation(1, null);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("0.00"));
    }

    @Test
    void registerEvaluation_gradeBelowZero_returnsError() {
        EvaluationService.EvaluationRegisterResult result = service.registerEvaluation(1, -1.0);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("0.00"));
    }

    @Test
    void registerEvaluation_gradeAboveTen_returnsError() {
        EvaluationService.EvaluationRegisterResult result = service.registerEvaluation(1, 10.5);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("0.00"));
    }

    // =====================================================================
    // registerEvaluation — duplicate enrollment
    // =====================================================================

    @Test
    void registerEvaluation_duplicateEnrollment_returnsError() {
        Evaluation existing = mock(Evaluation.class);
        when(repository.findByEnrollmentId(1)).thenReturn(existing);

        EvaluationService.EvaluationRegisterResult result = service.registerEvaluation(1, 8.5);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("ya cuenta"));
        verify(repository, never()).createEvaluation(any());
    }

    // =====================================================================
    // registerEvaluation — success paths
    // =====================================================================

    @Test
    void registerEvaluation_success_returnsOk() {
        when(repository.findByEnrollmentId(1)).thenReturn(null);

        try (MockedConstruction<Evaluation> evalConst = mockConstruction(Evaluation.class)) {
            EvaluationService.EvaluationRegisterResult result = service.registerEvaluation(1, 8.5);

            assertTrue(result.success);
            assertEquals(201, result.statusCode);
            verify(repository).createEvaluation(any(Evaluation.class));
        }
    }

    @Test
    void registerEvaluation_boundaryGradeMin_returnsOk() {
        when(repository.findByEnrollmentId(1)).thenReturn(null);

        try (MockedConstruction<Evaluation> evalConst = mockConstruction(Evaluation.class)) {
            EvaluationService.EvaluationRegisterResult result = service.registerEvaluation(1, 0.0);

            assertTrue(result.success);
            assertEquals(201, result.statusCode);
            verify(repository).createEvaluation(any(Evaluation.class));
        }
    }

    @Test
    void registerEvaluation_boundaryGradeMax_returnsOk() {
        when(repository.findByEnrollmentId(1)).thenReturn(null);

        try (MockedConstruction<Evaluation> evalConst = mockConstruction(Evaluation.class)) {
            EvaluationService.EvaluationRegisterResult result = service.registerEvaluation(1, 10.0);

            assertTrue(result.success);
            assertEquals(201, result.statusCode);
            verify(repository).createEvaluation(any(Evaluation.class));
        }
    }

    // =====================================================================
    // registerEvaluation — error propagation
    // =====================================================================

    @Test
    void registerEvaluation_repositoryThrows_returnsError() {
        when(repository.findByEnrollmentId(1)).thenThrow(new RuntimeException("DB error"));

        EvaluationService.EvaluationRegisterResult result = service.registerEvaluation(1, 8.5);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
    }

    // =====================================================================
    // updateEvaluation — always blocked
    // =====================================================================

    @Test
    void updateEvaluation_alwaysReturnsError() {
        EvaluationService.EvaluationRegisterResult result = service.updateEvaluation(1, 9.0);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("No se permite"));
    }

    // =====================================================================
    // deleteEvaluation — always blocked
    // =====================================================================

    @Test
    void deleteEvaluation_alwaysReturnsError() {
        EvaluationService.EvaluationRegisterResult result = service.deleteEvaluation(1);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("No se permite"));
    }

    // =====================================================================
    // getAllEvaluations
    // =====================================================================

    @Test
    void getAllEvaluations_returnsList() {
        Evaluation e1 = mock(Evaluation.class);
        Evaluation e2 = mock(Evaluation.class);
        when(repository.findAll()).thenReturn(Arrays.asList(e1, e2));

        List<Evaluation> result = service.getAllEvaluations();

        assertEquals(2, result.size());
        verify(repository).findAll();
    }

    @Test
    void getAllEvaluations_emptyList() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Evaluation> result = service.getAllEvaluations();

        assertTrue(result.isEmpty());
        verify(repository).findAll();
    }
}
