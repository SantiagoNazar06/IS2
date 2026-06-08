package com.is1.proyecto.services;

import com.is1.proyecto.models.*;
import com.is1.proyecto.repositories.TeacherRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TeacherService.
 * Uses MockedStatic for ActiveJDBC static methods (Person.findFirst, Teacher.findFirst,
 * TeacherAssignment.findById, Subject.findAll) and MockedConstruction for
 * new Person() / new Teacher() / new TeacherAssignment().
 * Delegation methods (getAllTeachers, getTeacherWithPerson, etc.) mock TeacherRepository only.
 */
@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private TeacherService service;

    private TeacherService.TeacherData createSampleData() {
        return new TeacherService.TeacherData("12345678", "L001", "John", "Doe", "555-0100", "john@test.com");
    }

    // =====================================================================
    // registerTeacher — existing person + existing teacher = duplicate
    // =====================================================================

    @Test
    void registerTeacher_personExists_teacherExists_returnsDuplicate() {
        TeacherService.TeacherData data = createSampleData();

        try (MockedStatic<Person> personMock = mockStatic(Person.class);
             MockedStatic<Teacher> teacherMock = mockStatic(Teacher.class)) {

            Person mockPerson = mock(Person.class);
            when(mockPerson.getId()).thenReturn(1);
            personMock.when(() -> Person.findFirst(anyString(), any())).thenReturn(mockPerson);

            Teacher mockTeacher = mock(Teacher.class);
            teacherMock.when(() -> Teacher.findFirst(anyString(), any())).thenReturn(mockTeacher);

            TeacherService.TeacherRegisterResult result = service.registerTeacher(data);

            assertFalse(result.success);
            assertEquals(400, result.statusCode);
            assertTrue(result.redirectUrl.contains("Ya existe"));
            verify(mockPerson).saveIt(); // person was updated in else branch
        }
    }

    // =====================================================================
    // registerTeacher — existing person + new teacher = success
    // =====================================================================

    @Test
    void registerTeacher_personExists_teacherDoesNotExist_returnsOk() {
        TeacherService.TeacherData data = createSampleData();

        try (MockedStatic<Person> personMock = mockStatic(Person.class);
             MockedStatic<Teacher> teacherMock = mockStatic(Teacher.class);
             MockedConstruction<Teacher> teacherConst = mockConstruction(Teacher.class)) {

            Person mockPerson = mock(Person.class);
            when(mockPerson.getId()).thenReturn(1);
            personMock.when(() -> Person.findFirst(anyString(), any())).thenReturn(mockPerson);

            teacherMock.when(() -> Teacher.findFirst(anyString(), any())).thenReturn(null);

            TeacherService.TeacherRegisterResult result = service.registerTeacher(data);

            assertTrue(result.success);
            assertEquals(201, result.statusCode);
            verify(mockPerson).saveIt();

            List<Teacher> constructed = teacherConst.constructed();
            assertEquals(1, constructed.size());
            Teacher mockT = constructed.get(0);
            verify(mockT).set("id_persona", 1);
            verify(mockT).set("nroLegajo", "L001");
            verify(mockT).saveIt();
        }
    }

    // =====================================================================
    // registerTeacher — new person + new teacher = success (full create)
    // =====================================================================

    @Test
    void registerTeacher_personDoesNotExist_teacherDoesNotExist_returnsOk() {
        TeacherService.TeacherData data = createSampleData();

        try (MockedStatic<Person> personMock = mockStatic(Person.class);
             MockedStatic<Teacher> teacherMock = mockStatic(Teacher.class);
             MockedConstruction<Person> personConst = mockConstruction(Person.class);
             MockedConstruction<Teacher> teacherConst = mockConstruction(Teacher.class)) {

            personMock.when(() -> Person.findFirst(anyString(), any())).thenReturn(null);
            teacherMock.when(() -> Teacher.findFirst(anyString(), any())).thenReturn(null);

            TeacherService.TeacherRegisterResult result = service.registerTeacher(data);

            assertTrue(result.success);
            assertEquals(201, result.statusCode);

            List<Person> persons = personConst.constructed();
            assertEquals(1, persons.size());
            Person mockPerson = persons.get(0);
            verify(mockPerson).set("dni", "12345678");
            verify(mockPerson).set("firstName", "John");
            verify(mockPerson).set("lastName", "Doe");
            verify(mockPerson).saveIt();

            List<Teacher> teachers = teacherConst.constructed();
            assertEquals(1, teachers.size());
            Teacher mockTeacher = teachers.get(0);
            verify(mockTeacher).saveIt();
        }
    }

    // =====================================================================
    // registerTeacher — exception path
    // =====================================================================

    @Test
    void registerTeacher_personFindThrows_returnsError() {
        TeacherService.TeacherData data = createSampleData();

        try (MockedStatic<Person> personMock = mockStatic(Person.class)) {
            personMock.when(() -> Person.findFirst(anyString(), any())).thenThrow(new RuntimeException("DB error"));

            TeacherService.TeacherRegisterResult result = service.registerTeacher(data);

            assertFalse(result.success);
            assertEquals(500, result.statusCode);
        }
    }

    // =====================================================================
    // verifyAssignment
    // =====================================================================

    @Test
    void verifyAssignment_exists_returnsTrue() {
        when(teacherRepository.existsAssignment(1L, 2L)).thenReturn(true);

        boolean result = service.verifyAssignment(1L, 2L);

        assertTrue(result);
        verify(teacherRepository).existsAssignment(1L, 2L);
    }

    @Test
    void verifyAssignment_notExists_returnsFalse() {
        when(teacherRepository.existsAssignment(1L, 2L)).thenReturn(false);

        boolean result = service.verifyAssignment(1L, 2L);

        assertFalse(result);
        verify(teacherRepository).existsAssignment(1L, 2L);
    }

    // =====================================================================
    // getAllTeachers
    // =====================================================================

    @SuppressWarnings("unchecked")
    @Test
    void getAllTeachers_returnsList() {
        List<Map<String, Object>> expected = Arrays.asList(Map.of("teacherId", 1));
        when(teacherRepository.findAllWithPersons()).thenReturn(expected);

        List<Map<String, Object>> result = service.getAllTeachers();

        assertEquals(1, result.size());
        verify(teacherRepository).findAllWithPersons();
    }

    // =====================================================================
    // getTeacherWithPerson
    // =====================================================================

    @Test
    void getTeacherWithPerson_returnsMap() {
        Map<String, Object> expected = Map.of("teacherId", 1, "firstName", "John");
        when(teacherRepository.findWithPerson(1L)).thenReturn(expected);

        Map<String, Object> result = service.getTeacherWithPerson(1L);

        assertEquals(1, result.get("teacherId"));
        verify(teacherRepository).findWithPerson(1L);
    }

    // =====================================================================
    // getAssignedSubjects
    // =====================================================================

    @SuppressWarnings("unchecked")
    @Test
    void getAssignedSubjects_returnsList() {
        List<Map<String, Object>> expected = Arrays.asList(Map.of("subjectId", 1));
        when(teacherRepository.findAssignedSubjectsWithCount(1L)).thenReturn(expected);

        List<Map<String, Object>> result = service.getAssignedSubjects(1L);

        assertEquals(1, result.size());
        verify(teacherRepository).findAssignedSubjectsWithCount(1L);
    }

    // =====================================================================
    // getSubjectStudents / getGrades
    // =====================================================================

    @Test
    void getSubjectStudents_verified_returnsList() {
        when(teacherRepository.existsAssignment(1L, 2L)).thenReturn(true);
        List<Map<String, Object>> students = Arrays.asList(Map.of("studentId", 1));
        when(teacherRepository.findSubjectStudents(2L)).thenReturn(students);

        List<Map<String, Object>> result = service.getSubjectStudents(1L, 2L);

        assertEquals(1, result.size());
        verify(teacherRepository).findSubjectStudents(2L);
    }

    @Test
    void getSubjectStudents_notVerified_returnsEmptyList() {
        when(teacherRepository.existsAssignment(1L, 2L)).thenReturn(false);

        List<Map<String, Object>> result = service.getSubjectStudents(1L, 2L);

        assertTrue(result.isEmpty());
        verify(teacherRepository, never()).findSubjectStudents(any());
    }

    @Test
    void getGrades_delegatesToGetSubjectStudents() {
        when(teacherRepository.existsAssignment(1L, 2L)).thenReturn(true);
        when(teacherRepository.findSubjectStudents(2L)).thenReturn(Arrays.asList(Map.of("grade", 8.5)));

        List<Map<String, Object>> result = service.getGrades(1L, 2L);

        assertEquals(1, result.size());
        assertEquals(8.5, result.get(0).get("grade"));
    }

    // =====================================================================
    // getAllAssignments
    // =====================================================================

    @SuppressWarnings("unchecked")
    @Test
    void getAllAssignments_enrichesWithRoleFlags() {
        Map<String, Object> assignment = new HashMap<>();
        assignment.put("role", "RESPONSABLE");
        assignment.put("id", 1);
        when(teacherRepository.findAllAssignmentsWithDetails())
                .thenReturn(Arrays.asList(assignment));

        List<Map<String, Object>> result = service.getAllAssignments();

        assertEquals(1, result.size());
        Map<String, Object> item = result.get(0);
        assertTrue((Boolean) item.get("isResponsable"));
        assertFalse((Boolean) item.get("isJtp"));
        assertFalse((Boolean) item.get("isAyudante"));
    }

    // =====================================================================
    // createAssignment
    // =====================================================================

    @Test
    void createAssignment_success() {
        try (MockedConstruction<TeacherAssignment> taConst = mockConstruction(TeacherAssignment.class)) {

            TeacherAssignment result = service.createAssignment(1L, 2L, "2024", "RESPONSABLE");

            verifyInteractionWithFirstConstructed(taConst);
            assertSame(taConst.constructed().get(0), result);
        }
    }

    private void verifyInteractionWithFirstConstructed(MockedConstruction<TeacherAssignment> taConst) {
        List<TeacherAssignment> constructed = taConst.constructed();
        assertEquals(1, constructed.size());
        TeacherAssignment mockTa = constructed.get(0);
        verify(mockTa).setTeacherId(1L);
        verify(mockTa).setSubjectId(2L);
        verify(mockTa).setPeriod("2024");
        verify(mockTa).setRole(TeacherRole.RESPONSABLE);
        verify(mockTa).saveIt();
    }

    // =====================================================================
    // deleteAssignment
    // =====================================================================

    @Test
    void deleteAssignment_exists_returnsTrue() {
        try (MockedStatic<TeacherAssignment> taMock = mockStatic(TeacherAssignment.class)) {
            TeacherAssignment mockTa = mock(TeacherAssignment.class);
            taMock.when(() -> TeacherAssignment.findById(anyLong())).thenReturn(mockTa);

            boolean result = service.deleteAssignment(1L);

            assertTrue(result);
            verify(mockTa).delete();
        }
    }

    @Test
    void deleteAssignment_notExists_returnsFalse() {
        try (MockedStatic<TeacherAssignment> taMock = mockStatic(TeacherAssignment.class)) {
            taMock.when(() -> TeacherAssignment.findById(anyLong())).thenReturn(null);

            boolean result = service.deleteAssignment(1L);

            assertFalse(result);
        }
    }

    // =====================================================================
    // getAllSubjectsSimple
    // =====================================================================

    // =====================================================================
    // getAssignedSubjectsSimple
    // =====================================================================

    @SuppressWarnings("unchecked")
    @Test
    void getAssignedSubjectsSimple_enrichesWithRoleFlags() {
        Map<String, Object> subject = new HashMap<>();
        subject.put("subjectId", 1);
        subject.put("role", "JTP");
        when(teacherRepository.getAssignedSubjects(1L)).thenReturn(Arrays.asList(subject));

        List<Map<String, Object>> result = service.getAssignedSubjectsSimple(1L);

        assertEquals(1, result.size());
        Map<String, Object> item = result.get(0);
        assertFalse((Boolean) item.get("isResponsable"));
        assertTrue((Boolean) item.get("isJtp"));
        assertFalse((Boolean) item.get("isAyudante"));
    }
}
