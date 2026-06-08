package com.is1.proyecto.services;

import com.is1.proyecto.models.Condition;
import com.is1.proyecto.models.ConditionType;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.repositories.ConditionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ConditionService.
 * Sigue el mismo patron que ConditionRepositoryTest:
 * MockedStatic para Subject.findById en carga de nombres y deteccion de existencia.
 * Mock del ConditionRepository para evitar acceso a DB real.
 */
@ExtendWith(MockitoExtension.class)
class ConditionServiceTest {

    @Mock
    private ConditionRepository conditionRepository;

    @InjectMocks
    private ConditionService conditionService;

    // =====================================================================
    // getPrerequisites
    // =====================================================================

    @Test
    void testGetPrerequisites_returnsPrerequisiteDTOList() {
        // Arrange
        Condition condition1 = mock(Condition.class);
        when(condition1.getInteger("id")).thenReturn(1);
        when(condition1.getSubjectId()).thenReturn(10);
        when(condition1.getPrerequisiteSubjectId()).thenReturn(20);
        when(condition1.getType()).thenReturn(ConditionType.REGULAR);

        Condition condition2 = mock(Condition.class);
        when(condition2.getInteger("id")).thenReturn(2);
        when(condition2.getSubjectId()).thenReturn(10);
        when(condition2.getPrerequisiteSubjectId()).thenReturn(30);
        when(condition2.getType()).thenReturn(ConditionType.APROBADA);

        when(conditionRepository.findBySubject(10))
                .thenReturn(Arrays.asList(condition1, condition2));

        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            Subject prereqSubject1 = mock(Subject.class);
            when(prereqSubject1.getSubjectName()).thenReturn("Matematica");
            subjectMock.when(() -> Subject.findById(20)).thenReturn(prereqSubject1);

            Subject prereqSubject2 = mock(Subject.class);
            when(prereqSubject2.getSubjectName()).thenReturn("Fisica");
            subjectMock.when(() -> Subject.findById(30)).thenReturn(prereqSubject2);

            // Act
            List<PrerequisiteDTO> result = conditionService.getPrerequisites(10);

            // Assert
            assertEquals(2, result.size());

            PrerequisiteDTO dto1 = result.get(0);
            assertEquals(1, dto1.getId());
            assertEquals(10, dto1.getSubjectId());
            assertEquals(20, dto1.getPrerequisiteSubjectId());
            assertEquals("Matematica", dto1.getPrerequisiteSubjectName());
            assertEquals(ConditionType.REGULAR, dto1.getType());

            PrerequisiteDTO dto2 = result.get(1);
            assertEquals(2, dto2.getId());
            assertEquals(10, dto2.getSubjectId());
            assertEquals(30, dto2.getPrerequisiteSubjectId());
            assertEquals("Fisica", dto2.getPrerequisiteSubjectName());
            assertEquals(ConditionType.APROBADA, dto2.getType());

            verify(conditionRepository).findBySubject(10);
            subjectMock.verify(() -> Subject.findById(20));
            subjectMock.verify(() -> Subject.findById(30));
        }
    }

    @Test
    void testGetPrerequisites_returnsEmptyList() {
        // Arrange
        when(conditionRepository.findBySubject(99))
                .thenReturn(Collections.emptyList());

        // Act
        List<PrerequisiteDTO> result = conditionService.getPrerequisites(99);

        // Assert
        assertTrue(result.isEmpty());
        verify(conditionRepository).findBySubject(99);
    }

    // =====================================================================
    // addPrerequisite
    // =====================================================================

    @Test
    void testAddPrerequisite_happyPath_createsAndReturnsDTO() {
        // Arrange
        int subjectId = 1;
        int prereqSubjectId = 2;

        Subject subject = mock(Subject.class);
        Subject prereqSubject = mock(Subject.class);
        when(prereqSubject.getSubjectName()).thenReturn("Analisis");

        Condition createdCondition = mock(Condition.class);
        when(createdCondition.getInteger("id")).thenReturn(100);
        when(createdCondition.getSubjectId()).thenReturn(subjectId);
        when(createdCondition.getPrerequisiteSubjectId()).thenReturn(prereqSubjectId);
        when(createdCondition.getType()).thenReturn(ConditionType.REGULAR);

        when(conditionRepository.create(subjectId, prereqSubjectId, ConditionType.REGULAR))
                .thenReturn(createdCondition);

        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            subjectMock.when(() -> Subject.findById(subjectId)).thenReturn(subject);
            subjectMock.when(() -> Subject.findById(prereqSubjectId)).thenReturn(prereqSubject);

            // No conditions that would create a cycle
            when(conditionRepository.findBySubject(prereqSubjectId))
                    .thenReturn(Collections.emptyList());

            // Act
            PrerequisiteDTO result = conditionService.addPrerequisite(
                    subjectId, prereqSubjectId, ConditionType.REGULAR);

            // Assert
            assertNotNull(result);
            assertEquals(100, result.getId());
            assertEquals(subjectId, result.getSubjectId());
            assertEquals(prereqSubjectId, result.getPrerequisiteSubjectId());
            assertEquals("Analisis", result.getPrerequisiteSubjectName());
            assertEquals(ConditionType.REGULAR, result.getType());

            subjectMock.verify(() -> Subject.findById(subjectId));
            subjectMock.verify(() -> Subject.findById(prereqSubjectId), times(2));
            verify(conditionRepository).findBySubject(prereqSubjectId);
            verify(conditionRepository).create(subjectId, prereqSubjectId, ConditionType.REGULAR);
        }
    }

    @Test
    void testAddPrerequisite_selfReference_throwsIllegalArgumentException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> conditionService.addPrerequisite(1, 1, ConditionType.REGULAR));

        // Verify no repository interaction occurred
        verifyNoInteractions(conditionRepository);
    }

    @Test
    void testAddPrerequisite_subjectNotFound_throwsIllegalArgumentException() {
        // Arrange
        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            subjectMock.when(() -> Subject.findById(1)).thenReturn(null);

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> conditionService.addPrerequisite(1, 2, ConditionType.REGULAR));

            subjectMock.verify(() -> Subject.findById(1));
            subjectMock.verify(() -> Subject.findById(2), never());
            verify(conditionRepository, never()).create(anyInt(), anyInt(), any());
        }
    }

    @Test
    void testAddPrerequisite_prerequisiteSubjectNotFound_throwsIllegalArgumentException() {
        // Arrange
        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            Subject subject = mock(Subject.class);
            subjectMock.when(() -> Subject.findById(1)).thenReturn(subject);
            subjectMock.when(() -> Subject.findById(2)).thenReturn(null);

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> conditionService.addPrerequisite(1, 2, ConditionType.APROBADA));

            subjectMock.verify(() -> Subject.findById(1));
            subjectMock.verify(() -> Subject.findById(2));
            verify(conditionRepository, never()).create(anyInt(), anyInt(), any());
        }
    }

    @Test
    void testAddPrerequisite_directCycle_throwsIllegalArgumentException() {
        // Arrange
        int subjectA = 1; // A
        int subjectB = 2; // B
        // Existing: A -> B (subject A requires B)
        // Trying to add: B -> A (subject B requires A) → cycle A->B->A

        Condition existingCondition = mock(Condition.class);
        when(existingCondition.getPrerequisiteSubjectId()).thenReturn(subjectB);

        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            Subject subjectAobj = mock(Subject.class);
            Subject subjectBobj = mock(Subject.class);
            subjectMock.when(() -> Subject.findById(subjectA)).thenReturn(subjectAobj);
            subjectMock.when(() -> Subject.findById(subjectB)).thenReturn(subjectBobj);

            // Cycle detection: DFS from A (the prerequisite), check if B is reachable
            // findBySubject(B) is never called because DFS finds the terminal case first
            when(conditionRepository.findBySubject(subjectA))
                    .thenReturn(Arrays.asList(existingCondition)); // A -> B

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> conditionService.addPrerequisite(subjectB, subjectA, ConditionType.REGULAR));

            verify(conditionRepository).findBySubject(subjectA);
            // findBySubject(subjectB) is NOT called because DFS hits the
            // terminal condition (startSubjectId == targetSubjectId) first
            verify(conditionRepository, never()).create(anyInt(), anyInt(), any());
        }
    }

    @Test
    void testAddPrerequisite_indirectCycle_throwsIllegalArgumentException() {
        // Arrange
        int subjectA = 1; // A
        int subjectB = 2; // B
        int subjectC = 3; // C
        // Existing: A -> B, B -> C
        // Trying to add: C -> A → indirect cycle A->B->C->A

        Condition aToB = mock(Condition.class);
        when(aToB.getPrerequisiteSubjectId()).thenReturn(subjectB);

        Condition bToC = mock(Condition.class);
        when(bToC.getPrerequisiteSubjectId()).thenReturn(subjectC);

        try (MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {
            Subject subjectAobj = mock(Subject.class);
            Subject subjectBobj = mock(Subject.class);
            Subject subjectCobj = mock(Subject.class);
            subjectMock.when(() -> Subject.findById(subjectA)).thenReturn(subjectAobj);
            subjectMock.when(() -> Subject.findById(subjectB)).thenReturn(subjectBobj);
            subjectMock.when(() -> Subject.findById(subjectC)).thenReturn(subjectCobj);

            // Cycle detection: DFS from A (the prerequisite), check if C is reachable
            // findBySubject(A) → [aToB] with prereq = B
            // findBySubject(B) → [bToC] with prereq = C
            // findBySubject(C) is never called because C == target (terminal case)
            when(conditionRepository.findBySubject(subjectA)).thenReturn(Arrays.asList(aToB));
            when(conditionRepository.findBySubject(subjectB)).thenReturn(Arrays.asList(bToC));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> conditionService.addPrerequisite(subjectC, subjectA, ConditionType.APROBADA));

            verify(conditionRepository).findBySubject(subjectA);
            verify(conditionRepository).findBySubject(subjectB);
            // findBySubject(subjectC) is NOT called because DFS hits the
            // terminal condition (startSubjectId == targetSubjectId) first
            verify(conditionRepository, never()).create(anyInt(), anyInt(), any());
        }
    }

    // =====================================================================
    // removePrerequisite
    // =====================================================================

    @Test
    void testRemovePrerequisite_existing_returnsTrue() {
        // Arrange
        when(conditionRepository.delete(100)).thenReturn(true);

        // Act
        boolean result = conditionService.removePrerequisite(100);

        // Assert
        assertTrue(result);
        verify(conditionRepository).delete(100);
    }

    @Test
    void testRemovePrerequisite_nonExisting_returnsFalse() {
        // Arrange
        when(conditionRepository.delete(999)).thenReturn(false);

        // Act
        boolean result = conditionService.removePrerequisite(999);

        // Assert
        assertFalse(result);
        verify(conditionRepository).delete(999);
    }
}
