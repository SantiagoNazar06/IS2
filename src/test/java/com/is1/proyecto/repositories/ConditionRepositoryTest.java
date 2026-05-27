package com.is1.proyecto.repositories;

import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.models.Condition;
import com.is1.proyecto.models.ConditionType;
import org.javalite.activejdbc.LazyList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ConditionRepository.
 * Sigue el mismo patrón que PersonRepositoryTest:
 * MockedStatic para métodos estáticos de ActiveJDBC,
 * MockedConstruction para new Condition() en create.
 */
@ExtendWith(MockitoExtension.class)
public class ConditionRepositoryTest {

    @Mock
    private DBConfigSingleton dbMock;

    @InjectMocks
    private ConditionRepository repository;

    // =============================
    // findBySubject
    // =============================
    @Test
    void testFindBySubject_returnsConditionList() {
        try (MockedStatic<Condition> conditionMock = mockStatic(Condition.class)) {
            LazyList<Condition> expectedList = mock(LazyList.class);
            conditionMock.when(() -> Condition.where(anyString(), anyInt()))
                    .thenReturn(expectedList);

            List<Condition> result = repository.findBySubject(1);

            assertSame(expectedList, result);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    // =============================
    // create
    // =============================
    @Test
    void testCreate_regularType_savesAndReturns() {
        try (MockedConstruction<Condition> mocked = mockConstruction(Condition.class,
                (mock, context) -> when(mock.saveIt()).thenReturn(true))) {

            Condition result = repository.create(1, 2, ConditionType.REGULAR);

            assertNotNull(result);
            Condition conditionMock = mocked.constructed().get(0);
            verify(conditionMock).setSubjectId(1);
            verify(conditionMock).setPrerequisiteSubjectId(2);
            verify(conditionMock).setType(ConditionType.REGULAR);
            verify(conditionMock).saveIt();
            assertSame(conditionMock, result);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testCreate_aprobadaType_savesAndReturns() {
        try (MockedConstruction<Condition> mocked = mockConstruction(Condition.class,
                (mock, context) -> when(mock.saveIt()).thenReturn(true))) {

            Condition result = repository.create(5, 6, ConditionType.APROBADA);

            assertNotNull(result);
            Condition conditionMock = mocked.constructed().get(0);
            verify(conditionMock).setSubjectId(5);
            verify(conditionMock).setPrerequisiteSubjectId(6);
            verify(conditionMock).setType(ConditionType.APROBADA);
            verify(conditionMock).saveIt();
            assertSame(conditionMock, result);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    // =============================
    // delete
    // =============================
    @Test
    void testDelete_existingCondition_returnsTrue() {
        try (MockedStatic<Condition> conditionMock = mockStatic(Condition.class)) {
            Condition conditionMocked = mock(Condition.class);
            conditionMock.when(() -> Condition.findById(10))
                    .thenReturn(conditionMocked);

            boolean result = repository.delete(10);

            assertTrue(result);
            verify(conditionMocked).delete(true);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testDelete_nonExistingCondition_returnsFalse() {
        try (MockedStatic<Condition> conditionMock = mockStatic(Condition.class)) {
            conditionMock.when(() -> Condition.findById(999))
                    .thenReturn(null);

            boolean result = repository.delete(999);

            assertFalse(result);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }
}
