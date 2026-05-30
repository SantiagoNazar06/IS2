package com.is1.proyecto.services;

import com.is1.proyecto.dto.StudentWithGradeDTO;
import com.is1.proyecto.models.Enrollment;
import com.is1.proyecto.models.Evaluation;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.TeacherSubject;
import org.javalite.activejdbc.LazyList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para TeacherService.getStudentsBySubject.
 * Utiliza MockedStatic para los metodos estaticos de ActiveJDBC
 * y mocks para las instancias de los modelos.
 */
@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    private final TeacherService teacherService = new TeacherService();

    // =====================================================================
    // getStudentsBySubject — validacion de errores
    // =====================================================================

    @Test
    void testGetStudentsBySubject_teacherNotFound_throwsIllegalArgumentException() {
        try (MockedStatic<Teacher> teacherMock = mockStatic(Teacher.class)) {
            teacherMock.when(() -> Teacher.findById(1)).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teacherService.getStudentsBySubject(1, 5));

            assertEquals("Teacher not found", ex.getMessage());
            teacherMock.verify(() -> Teacher.findById(1));
        }
    }

    @Test
    void testGetStudentsBySubject_teacherNotAssigned_throwsIllegalArgumentException() {
        try (MockedStatic<Teacher> teacherMock = mockStatic(Teacher.class);
             MockedStatic<TeacherSubject> tsMock = mockStatic(TeacherSubject.class)) {

            Teacher mockTeacher = mock(Teacher.class);
            teacherMock.when(() -> Teacher.findById(1)).thenReturn(mockTeacher);
            tsMock.when(() -> TeacherSubject.findFirst(anyString(), anyInt(), anyInt()))
                    .thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teacherService.getStudentsBySubject(1, 5));

            assertEquals("Teacher not assigned to this subject", ex.getMessage());
            teacherMock.verify(() -> Teacher.findById(1));
            tsMock.verify(() -> TeacherSubject.findFirst(anyString(), anyInt(), anyInt()));
        }
    }

    @Test
    void testGetStudentsBySubject_subjectNotFound_throwsIllegalArgumentException() {
        try (MockedStatic<Teacher> teacherMock = mockStatic(Teacher.class);
             MockedStatic<TeacherSubject> tsMock = mockStatic(TeacherSubject.class);
             MockedStatic<Subject> subjectMock = mockStatic(Subject.class)) {

            Teacher mockTeacher = mock(Teacher.class);
            teacherMock.when(() -> Teacher.findById(1)).thenReturn(mockTeacher);

            TeacherSubject mockAssignment = mock(TeacherSubject.class);
            tsMock.when(() -> TeacherSubject.findFirst(anyString(), anyInt(), anyInt()))
                    .thenReturn(mockAssignment);

            subjectMock.when(() -> Subject.findById(5)).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teacherService.getStudentsBySubject(1, 5));

            assertEquals("Subject not found", ex.getMessage());
            teacherMock.verify(() -> Teacher.findById(1));
            tsMock.verify(() -> TeacherSubject.findFirst(anyString(), anyInt(), anyInt()));
            subjectMock.verify(() -> Subject.findById(5));
        }
    }

    // =====================================================================
    // getStudentsBySubject — casos con datos
    // =====================================================================

    @Test
    void testGetStudentsBySubject_noEnrollments_returnsEmptyList() {
        try (MockedStatic<Teacher> teacherMock = mockStatic(Teacher.class);
             MockedStatic<TeacherSubject> tsMock = mockStatic(TeacherSubject.class);
             MockedStatic<Subject> subjectMock = mockStatic(Subject.class);
             MockedStatic<Enrollment> enrollmentMock = mockStatic(Enrollment.class)) {

            Teacher mockTeacher = mock(Teacher.class);
            teacherMock.when(() -> Teacher.findById(1)).thenReturn(mockTeacher);

            TeacherSubject mockAssignment = mock(TeacherSubject.class);
            tsMock.when(() -> TeacherSubject.findFirst(anyString(), anyInt(), anyInt()))
                    .thenReturn(mockAssignment);

            Subject mockSubject = mock(Subject.class);
            subjectMock.when(() -> Subject.findById(5)).thenReturn(mockSubject);

            LazyList<Enrollment> emptyList = mock(LazyList.class);
            when(emptyList.iterator()).thenReturn(Collections.emptyIterator());
            enrollmentMock.when(() -> Enrollment.where(anyString(), any(), any()))
                    .thenReturn(emptyList);

            List<StudentWithGradeDTO> result = teacherService.getStudentsBySubject(1, 5);

            assertTrue(result.isEmpty());
            enrollmentMock.verify(() -> Enrollment.where(anyString(), any(), any()));
        }
    }

    @Test
    void testGetStudentsBySubject_mixedGrades_returnsCorrectData() {
        try (MockedStatic<Teacher> teacherMock = mockStatic(Teacher.class);
             MockedStatic<TeacherSubject> tsMock = mockStatic(TeacherSubject.class);
             MockedStatic<Subject> subjectMock = mockStatic(Subject.class);
             MockedStatic<Enrollment> enrollmentMock = mockStatic(Enrollment.class);
             MockedStatic<Student> studentMock = mockStatic(Student.class);
             MockedStatic<Evaluation> evalMock = mockStatic(Evaluation.class)) {

            // --- Setup: teacher, assignment, subject ---
            Teacher mockTeacher = mock(Teacher.class);
            teacherMock.when(() -> Teacher.findById(1)).thenReturn(mockTeacher);

            TeacherSubject mockAssignment = mock(TeacherSubject.class);
            tsMock.when(() -> TeacherSubject.findFirst(anyString(), anyInt(), anyInt()))
                    .thenReturn(mockAssignment);

            Subject mockSubject = mock(Subject.class);
            subjectMock.when(() -> Subject.findById(5)).thenReturn(mockSubject);

            // --- Setup: 2 enrollments ---
            Enrollment enrollment1 = mock(Enrollment.class);
            when(enrollment1.getStudentId()).thenReturn(10L);
            when(enrollment1.getId()).thenReturn(100L);
            when(enrollment1.getCreatedAt()).thenReturn("2025-03-01");

            Enrollment enrollment2 = mock(Enrollment.class);
            when(enrollment2.getStudentId()).thenReturn(20L);
            when(enrollment2.getId()).thenReturn(200L);
            when(enrollment2.getCreatedAt()).thenReturn("2025-03-15");

            LazyList<Enrollment> mockList = mock(LazyList.class);
            when(mockList.iterator()).thenReturn(Arrays.asList(enrollment1, enrollment2).iterator());
            enrollmentMock.when(() -> Enrollment.where(anyString(), any(), any()))
                    .thenReturn(mockList);

            // --- Setup: student 1 (with person) ---
            Student student1 = mock(Student.class);
            Person person1 = mock(Person.class);
            when(person1.getFirstName()).thenReturn("Juan");
            when(person1.getLastName()).thenReturn("Perez");
            when(student1.getPerson()).thenReturn(person1);
            when(student1.getId()).thenReturn(10L);

            // --- Setup: student 2 (with person) ---
            Student student2 = mock(Student.class);
            Person person2 = mock(Person.class);
            when(person2.getFirstName()).thenReturn("Maria");
            when(person2.getLastName()).thenReturn("Lopez");
            when(student2.getPerson()).thenReturn(person2);
            when(student2.getId()).thenReturn(20L);

            studentMock.when(() -> Student.findById(10L)).thenReturn(student1);
            studentMock.when(() -> Student.findById(20L)).thenReturn(student2);

            // --- Setup: evaluation for enrollment 1 only ---
            Evaluation eval1 = mock(Evaluation.class);
            when(eval1.getEvaluationGrade()).thenReturn(8.5);
            when(eval1.getEvaluationDate()).thenReturn(Date.valueOf("2025-06-15"));

            evalMock.when(() -> Evaluation.findFirst(eq("enrollment_id = ?"), eq(100L)))
                    .thenReturn(eval1);
            evalMock.when(() -> Evaluation.findFirst(eq("enrollment_id = ?"), eq(200L)))
                    .thenReturn(null);

            // --- Act ---
            List<StudentWithGradeDTO> result = teacherService.getStudentsBySubject(1, 5);

            // --- Assert ---
            assertEquals(2, result.size());

            // Student 1: with grade
            StudentWithGradeDTO dto1 = result.get(0);
            assertEquals(Long.valueOf(10L), dto1.getStudentId());
            assertEquals("Juan Perez", dto1.getStudentName());
            assertEquals("2025-03-01", dto1.getEnrollmentDate());
            assertEquals(Double.valueOf(8.5), dto1.getGrade());
            assertEquals("2025-06-15", dto1.getGradeDate());

            // Student 2: without grade
            StudentWithGradeDTO dto2 = result.get(1);
            assertEquals(Long.valueOf(20L), dto2.getStudentId());
            assertEquals("Maria Lopez", dto2.getStudentName());
            assertEquals("2025-03-15", dto2.getEnrollmentDate());
            assertNull(dto2.getGrade());
            assertNull(dto2.getGradeDate());

            // Verify interactions
            teacherMock.verify(() -> Teacher.findById(1));
            tsMock.verify(() -> TeacherSubject.findFirst(anyString(), anyInt(), anyInt()));
            subjectMock.verify(() -> Subject.findById(5));
            enrollmentMock.verify(() -> Enrollment.where(anyString(), any(), any()));
            studentMock.verify(() -> Student.findById(10L));
            studentMock.verify(() -> Student.findById(20L));
            evalMock.verify(() -> Evaluation.findFirst(eq("enrollment_id = ?"), eq(100L)));
            evalMock.verify(() -> Evaluation.findFirst(eq("enrollment_id = ?"), eq(200L)));
        }
    }
}
