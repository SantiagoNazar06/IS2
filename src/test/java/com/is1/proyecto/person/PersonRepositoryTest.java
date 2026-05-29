package com.is1.proyecto.person;

import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.repositories.PersonRepository;
import com.is1.proyecto.models.Person;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PersonRepositoryTest {

    @Mock
    private DBConfigSingleton dbMock;

    @InjectMocks
    private PersonRepository repository;

    @BeforeEach
    void setup() {
        
    }

    // =============================
    // personsTableExists
    // =============================
    @Test
    void testPersonsTableExists_true() {
        try (MockedStatic<Base> baseMock = Mockito.mockStatic(Base.class)) {

            baseMock.when(() -> Base.firstCell(anyString(), any()))
                    .thenReturn("persons");

            boolean result = repository.personsTableExists();

            assertTrue(result);
            verify(dbMock).openConnection();
            verify(dbMock).closeConnection();
        }
    }

    @Test
    void testPersonsTableExists_false() {
        try (MockedStatic<Base> baseMock = Mockito.mockStatic(Base.class)) {

            baseMock.when(() -> Base.firstCell(anyString(), any()))
                    .thenReturn(null);

            boolean result = repository.personsTableExists();

            assertFalse(result);
        }
    }

    // =============================
    // create
    // =============================
    @Test
    void testCreate() {
        Person personMock = mock(Person.class);

        when(personMock.saveIt()).thenReturn(true);

        Person result = repository.create(personMock);

        assertEquals(personMock, result);
        verify(personMock).saveIt();
        verify(dbMock).openConnection();
        verify(dbMock).closeConnection();
    }

    // =============================
    // findById
    // =============================
    @Test
    void testFindById_found() {
        try (MockedStatic<Person> personMockedStatic = Mockito.mockStatic(Person.class)) {

            Person personMock = mock(Person.class);

            personMockedStatic.when(() -> Person.findById(1L))
                    .thenReturn(personMock);

            Person result = repository.findById(1L);

            assertNotNull(result);
            assertEquals(personMock, result);
        }
    }

    // =============================
    // findAll
    // =============================
    @Test
    void testFindAll() {
        try (MockedStatic<Person> personMockedStatic = Mockito.mockStatic(Person.class)) {

            LazyList<Person> list = Mockito.mock(LazyList.class);
            doReturn(list).when(list).load();

            personMockedStatic.when(Person::findAll)
                    .thenReturn(list);

            List<Person> result = repository.findAll();

            assertEquals(list, result);
            verify(list).load(); 
        }
    }

    // =============================
    // update
    // =============================
    @Test
    void testUpdate_success() {
        try (MockedStatic<Person> personMockedStatic = Mockito.mockStatic(Person.class)) {

            Person personMock = mock(Person.class);

            Map<String, Object> data = new HashMap<>();
            data.put("firstname", "Juan");

            personMockedStatic.when(() -> Person.findById(1L))
                    .thenReturn(personMock);

            when(personMock.saveIt()).thenReturn(true);

            boolean result = repository.update(1L, data);

            assertTrue(result);
            verify(personMock).set("firstname", "Juan");
            verify(personMock).saveIt();
        }
    }

    @Test
    void testUpdate_personNotFound() {
        try (MockedStatic<Person> personMockedStatic = Mockito.mockStatic(Person.class)) {

            Map<String, Object> data = new HashMap<>();
            data.put("firstname", "Juan");

            personMockedStatic.when(() -> Person.findById(1L))
                    .thenReturn(null);

            boolean result = repository.update(1L, data);

            assertFalse(result);
        }
    }

    @Test
    void testUpdate_invalidData() {
        boolean result = repository.update(0L, null);
        assertFalse(result);
    }

    // =============================
    // delete
    // =============================
    @Test
    void testDelete_success() {
        try (MockedStatic<Person> personMockedStatic = Mockito.mockStatic(Person.class)) {

            Person personMock = mock(Person.class);

            personMockedStatic.when(() -> Person.findById(1L))
                    .thenReturn(personMock);

            boolean result = repository.delete(1L);

            assertTrue(result);
            verify(personMock).delete(true);
        }
    }

    @Test
    void testDelete_notFound() {
        try (MockedStatic<Person> personMockedStatic = Mockito.mockStatic(Person.class)) {

            personMockedStatic.when(() -> Person.findById(1L))
                    .thenReturn(null);

            boolean result = repository.delete(1L);

            assertFalse(result);
        }
    }

    // =============================
    // findByDni
    // =============================
    @Test
    void testFindByDni() {
        try (MockedStatic<Person> personMockedStatic = Mockito.mockStatic(Person.class)) {

            Person personMock = mock(Person.class);

            personMockedStatic.when(() -> Person.findFirst(anyString(), any()))
                    .thenReturn(personMock);

            Person result = repository.findByDni("123");

            assertNotNull(result);
            assertEquals(personMock, result);
        }
    }
}