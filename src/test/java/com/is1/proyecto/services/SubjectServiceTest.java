package com.is1.proyecto.services;

import com.is1.proyecto.dto.SubjectDTO;
import com.is1.proyecto.exceptions.ValidationException;
import com.is1.proyecto.models.Career;
import com.is1.proyecto.models.StudyPlan;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.repositories.SubjectRepository;
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
 * Unit tests for SubjectService.
 * Uses @Mock SubjectRepository + MockedStatic<StudyPlan> for StudyPlan.findById calls.
 * Follows the ConditionServiceTest pattern.
 */
@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository repository;

    @InjectMocks
    private SubjectService service;

    // =====================================================================
    // registerSubject — validation errors
    // =====================================================================

    @Test
    void registerSubject_nullCode_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.registerSubject(null, "Matematica", 1));
    }

    @Test
    void registerSubject_emptyCode_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.registerSubject("", "Matematica", 1));
    }

    @Test
    void registerSubject_blankCode_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.registerSubject("   ", "Matematica", 1));
    }

    @Test
    void registerSubject_nullName_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.registerSubject("MAT101", null, 1));
    }

    @Test
    void registerSubject_emptyName_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.registerSubject("MAT101", "", 1));
    }

    // =====================================================================
    // registerSubject — duplicate code
    // =====================================================================

    @Test
    void registerSubject_duplicateCode_throwsValidationException() {
        Subject existing = mock(Subject.class);
        when(repository.findByCode("MAT101")).thenReturn(existing);

        assertThrows(ValidationException.class,
                () -> service.registerSubject("mat101", "Matematica", 1));
    }

    // =====================================================================
    // registerSubject — study plan validation
    // =====================================================================

    @Test
    void registerSubject_studyPlanNotFound_throwsValidationException() {
        when(repository.findByCode("MAT101")).thenReturn(null);

        try (MockedStatic<StudyPlan> spMock = mockStatic(StudyPlan.class)) {
            spMock.when(() -> StudyPlan.findById(any())).thenReturn(null);

            assertThrows(ValidationException.class,
                    () -> service.registerSubject("MAT101", "Matematica", 999));
        }
    }

    // =====================================================================
    // registerSubject — success paths
    // =====================================================================

    @Test
    void registerSubject_success_returnsDTO() {
        when(repository.findByCode("MAT101")).thenReturn(null);

        Career mockCareer = mock(Career.class);
        when(mockCareer.getCareerName()).thenReturn("Ingenieria");

        StudyPlan mockSP = mock(StudyPlan.class);
        when(mockSP.getName()).thenReturn("Plan 2024");
        when(mockSP.getCareer()).thenReturn(mockCareer);
        when(mockSP.getCareerId()).thenReturn(1);

        try (MockedStatic<StudyPlan> spMock = mockStatic(StudyPlan.class);
             MockedConstruction<Subject> subjConst = mockConstruction(Subject.class,
                     (subj, ctx) -> {
                         when(subj.getId()).thenReturn(1);
                         when(subj.getSubjectName()).thenReturn("Matematica");
                         when(subj.getCode()).thenReturn("MAT101");
                         when(subj.getStudyPlanId()).thenReturn(1);
                         when(subj.getStudyPlan()).thenReturn(mockSP);
                     })) {

            spMock.when(() -> StudyPlan.findById(any())).thenReturn(mockSP);
            when(repository.create(any(Subject.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            SubjectDTO result = service.registerSubject("MAT101", "Matematica", 1);

            assertNotNull(result);
            assertEquals("MAT101", result.getCode());
            assertEquals("Matematica", result.getSubjectName());
            assertEquals("Plan 2024", result.getStudyPlanName());
            assertEquals("Ingenieria", result.getCareerName());
            verify(repository).create(any(Subject.class));
        }
    }

    @Test
    void registerSubject_withoutStudyPlan_success() {
        when(repository.findByCode("MAT101")).thenReturn(null);

        try (MockedConstruction<Subject> subjConst = mockConstruction(Subject.class,
                (subj, ctx) -> {
                    when(subj.getId()).thenReturn(1);
                    when(subj.getSubjectName()).thenReturn("Matematica");
                    when(subj.getCode()).thenReturn("MAT101");
                    when(subj.getStudyPlanId()).thenReturn(null);
                })) {

            when(repository.create(any(Subject.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            SubjectDTO result = service.registerSubject("MAT101", "Matematica", null);

            assertNotNull(result);
            assertEquals("Matematica", result.getSubjectName());
            assertNull(result.getStudyPlanId());
            assertNull(result.getStudyPlanName());
            assertNull(result.getCareerId());
            verify(repository).create(any(Subject.class));
        }
    }

    // =====================================================================
    // updateSubject
    // =====================================================================

    @Test
    void updateSubject_notFound_throwsValidationException() {
        when(repository.findById(1)).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> service.updateSubject(1, "NEW101", "New Name", 1));
    }

    @Test
    void updateSubject_duplicateCode_throwsValidationException() {
        Subject subject = mock(Subject.class);
        when(repository.findById(1)).thenReturn(subject);

        Subject other = mock(Subject.class);
        when(other.getId()).thenReturn(2);
        when(repository.findByCode("NEW101")).thenReturn(other);

        assertThrows(ValidationException.class,
                () -> service.updateSubject(1, "NEW101", null, null));
    }

    @Test
    void updateSubject_studyPlanNotFound_throwsValidationException() {
        Subject subject = mock(Subject.class);
        when(repository.findById(1)).thenReturn(subject);
        when(repository.findByCode("NEW101")).thenReturn(null);

        try (MockedStatic<StudyPlan> spMock = mockStatic(StudyPlan.class)) {
            spMock.when(() -> StudyPlan.findById(any())).thenReturn(null);

            assertThrows(ValidationException.class,
                    () -> service.updateSubject(1, "NEW101", "New Name", 999));
        }
    }

    @Test
    void updateSubject_success_returnsDTO() {
        Subject subject = mock(Subject.class);
        when(subject.getId()).thenReturn(1);
        when(repository.findById(1)).thenReturn(subject);
        when(repository.findByCode("NEW101")).thenReturn(null);

        try (MockedStatic<StudyPlan> spMock = mockStatic(StudyPlan.class)) {
            StudyPlan mockSP = mock(StudyPlan.class);
            when(mockSP.getName()).thenReturn("Plan 2024");
            Career mockCareer = mock(Career.class);
            when(mockCareer.getCareerName()).thenReturn("Ingenieria");
            when(mockSP.getCareer()).thenReturn(mockCareer);
            when(mockSP.getCareerId()).thenReturn(1);
            spMock.when(() -> StudyPlan.findById(any())).thenReturn(mockSP);

            when(subject.getSubjectName()).thenReturn("New Name");
            when(subject.getCode()).thenReturn("NEW101");
            when(subject.getStudyPlanId()).thenReturn(1);
            when(subject.getStudyPlan()).thenReturn(mockSP);

            SubjectDTO result = service.updateSubject(1, "NEW101", "New Name", 1);

            assertNotNull(result);
            assertEquals("NEW101", result.getCode());
            assertEquals("New Name", result.getSubjectName());
            verify(repository).update(subject);
        }
    }

    @Test
    void updateSubject_codeSameAsExisting_allowed() {
        Subject subject = mock(Subject.class);
        when(subject.getId()).thenReturn(1);
        when(repository.findById(1)).thenReturn(subject);

        // findById for same code returns the SAME subject (by id)
        when(repository.findByCode("MAT101")).thenReturn(subject);

        try (MockedStatic<StudyPlan> spMock = mockStatic(StudyPlan.class)) {
            StudyPlan mockSP = mock(StudyPlan.class);
            spMock.when(() -> StudyPlan.findById(any())).thenReturn(mockSP);
            when(mockSP.getCareerId()).thenReturn(null);

            when(subject.getSubjectName()).thenReturn("Matematica");
            when(subject.getCode()).thenReturn("MAT101");
            when(subject.getStudyPlan()).thenReturn(mockSP);

            SubjectDTO result = service.updateSubject(1, "MAT101", "Matematica", 1);

            assertNotNull(result);
            assertEquals("MAT101", result.getCode());
            verify(repository).update(subject);
        }
    }

    @Test
    void updateSubject_desasociateStudyPlan_setsNull() {
        Subject subject = mock(Subject.class);
        when(subject.getId()).thenReturn(1);
        when(subject.getSubjectName()).thenReturn("Matematica");
        when(subject.getCode()).thenReturn("MAT101");
        when(subject.getStudyPlanId()).thenReturn(null);
        when(repository.findById(1)).thenReturn(subject);
        when(repository.findByCode("MAT101")).thenReturn(null);

        SubjectDTO result = service.updateSubject(1, "MAT101", "Matematica", -1);

        assertNotNull(result);
        verify(subject).setStudyPlanId(null);
        verify(repository).update(subject);
    }

    // =====================================================================
    // deleteSubject
    // =====================================================================

    @Test
    void deleteSubject_notFound_throwsValidationException() {
        when(repository.deleteById(1)).thenReturn(false);

        assertThrows(ValidationException.class,
                () -> service.deleteSubject(1));
    }

    @Test
    void deleteSubject_success() {
        when(repository.deleteById(1)).thenReturn(true);

        assertDoesNotThrow(() -> service.deleteSubject(1));
        verify(repository).deleteById(1);
    }

    // =====================================================================
    // getSubjectById
    // =====================================================================

    @Test
    void getSubjectById_notFound_throwsValidationException() {
        when(repository.findById(1)).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> service.getSubjectById(1));
    }

    @Test
    void getSubjectById_success_returnsDTO() {
        Subject subject = mock(Subject.class);
        when(subject.getId()).thenReturn(1);
        when(subject.getSubjectName()).thenReturn("Matematica");
        when(subject.getCode()).thenReturn("MAT101");
        when(repository.findById(1)).thenReturn(subject);

        SubjectDTO result = service.getSubjectById(1);

        assertNotNull(result);
        assertEquals("Matematica", result.getSubjectName());
        assertEquals("MAT101", result.getCode());
    }

    // =====================================================================
    // getAllSubjects
    // =====================================================================

    @Test
    void getAllSubjects_withStudyPlanId_returnsFilteredList() {
        Subject s1 = mock(Subject.class);
        when(s1.getId()).thenReturn(1);
        when(s1.getSubjectName()).thenReturn("Matematica");
        when(s1.getCode()).thenReturn("MAT101");
        when(repository.findByStudyPlanId(1)).thenReturn(Arrays.asList(s1));

        List<SubjectDTO> result = service.getAllSubjects(1);

        assertEquals(1, result.size());
        assertEquals("Matematica", result.get(0).getSubjectName());
        verify(repository).findByStudyPlanId(1);
        verify(repository, never()).findAll();
    }

    @Test
    void getAllSubjects_withoutStudyPlanId_returnsAll() {
        Subject s1 = mock(Subject.class);
        when(s1.getId()).thenReturn(1);
        when(s1.getSubjectName()).thenReturn("Matematica");
        when(s1.getCode()).thenReturn("MAT101");
        when(repository.findAll()).thenReturn(Arrays.asList(s1));

        List<SubjectDTO> result = service.getAllSubjects(null);

        assertEquals(1, result.size());
        verify(repository).findAll();
        verify(repository, never()).findByStudyPlanId(anyInt());
    }

    @Test
    void getAllSubjects_emptyList() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<SubjectDTO> result = service.getAllSubjects(null);

        assertTrue(result.isEmpty());
        verify(repository).findAll();
    }
}
