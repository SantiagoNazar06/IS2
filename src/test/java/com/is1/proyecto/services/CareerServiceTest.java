package com.is1.proyecto.services;

import com.is1.proyecto.models.Career;
import com.is1.proyecto.repositories.CareerRepository;
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
 * Unit tests for CareerService.
 * Follows the ConditionServiceTest pattern: @ExtendWith(MockitoExtension.class),
 * @Mock CareerRepository, @InjectMocks CareerService.
 */
@ExtendWith(MockitoExtension.class)
class CareerServiceTest {

    @Mock
    private CareerRepository repository;

    @InjectMocks
    private CareerService service;

    // =====================================================================
    // registerCareer
    // =====================================================================

    @Test
    void registerCareer_duplicateName_returnsDuplicateResult() {
        Career existing = mock(Career.class);
        when(repository.findByName("Ingenieria")).thenReturn(existing);

        CareerService.CareerRegisterResult result = service.registerCareer("Ingenieria", 5);

        assertFalse(result.success);
        assertEquals(400, result.statusCode);
        assertTrue(result.message.contains("Ya existe"));
        verify(repository).findByName("Ingenieria");
        verify(repository, never()).create(any());
    }

    @Test
    void registerCareer_invalidDuration_returnsError() {
        when(repository.findByName(anyString())).thenReturn(null);

        CareerService.CareerRegisterResult result = service.registerCareer("Ingenieria", 0);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("mayor a cero"));
        verify(repository, never()).create(any());
    }

    @Test
    void registerCareer_negativeDuration_returnsError() {
        when(repository.findByName(anyString())).thenReturn(null);

        CareerService.CareerRegisterResult result = service.registerCareer("Ingenieria", -1);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("mayor a cero"));
    }

    @Test
    void registerCareer_success_returnsOk() {
        when(repository.findByName(anyString())).thenReturn(null);

        try (MockedConstruction<Career> careerConst = mockConstruction(Career.class)) {
            CareerService.CareerRegisterResult result = service.registerCareer("Ingenieria", 5);

            assertTrue(result.success);
            assertEquals(201, result.statusCode);
            verify(repository).create(any(Career.class));
        }
    }

    @Test
    void registerCareer_repositoryThrows_returnsError() {
        when(repository.findByName(anyString())).thenThrow(new RuntimeException("DB error"));

        CareerService.CareerRegisterResult result = service.registerCareer("Ingenieria", 5);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
    }

    // =====================================================================
    // updateCareer
    // =====================================================================

    @Test
    void updateCareer_notFound_returnsError() {
        when(repository.findById(1)).thenReturn(null);

        CareerService.CareerRegisterResult result = service.updateCareer(1, "Nuevo", 5);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("no encontrada"));
    }

    @Test
    void updateCareer_invalidDuration_returnsError() {
        Career career = mock(Career.class);
        when(repository.findById(1)).thenReturn(career);

        CareerService.CareerRegisterResult result = service.updateCareer(1, "Nuevo", 0);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("mayor a cero"));
    }

    @Test
    void updateCareer_duplicateName_returnsError() {
        Career career = mock(Career.class);
        when(repository.findById(1)).thenReturn(career);

        Career other = mock(Career.class);
        when(other.getId()).thenReturn(2);
        when(repository.findByName("Nuevo")).thenReturn(other);

        CareerService.CareerRegisterResult result = service.updateCareer(1, "Nuevo", 5);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("Ya existe"));
    }

    @Test
    void updateCareer_sameNameAllowed_returnsOk() {
        Career career = mock(Career.class);
        when(career.getId()).thenReturn(1);
        when(repository.findById(1)).thenReturn(career);

        // findByName returns the SAME career (same id) — allowed
        when(repository.findByName("Ingenieria")).thenReturn(career);

        CareerService.CareerRegisterResult result = service.updateCareer(1, "Ingenieria", 5);

        assertTrue(result.success);
        assertEquals(201, result.statusCode);
        verify(repository).update(career);
    }

    @Test
    void updateCareer_success_returnsOk() {
        Career career = mock(Career.class);
        when(repository.findById(1)).thenReturn(career);
        when(repository.findByName("Nuevo")).thenReturn(null);

        CareerService.CareerRegisterResult result = service.updateCareer(1, "Nuevo", 5);

        assertTrue(result.success);
        assertEquals(201, result.statusCode);
        verify(repository).update(career);
    }

    @Test
    void updateCareer_repositoryThrows_returnsError() {
        Career career = mock(Career.class);
        when(repository.findById(1)).thenReturn(career);
        doThrow(new RuntimeException("DB error")).when(repository).update(any());

        CareerService.CareerRegisterResult result = service.updateCareer(1, "Nuevo", 5);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
    }

    // =====================================================================
    // deleteCareer
    // =====================================================================

    @Test
    void deleteCareer_notFound_returnsError() {
        when(repository.findById(1)).thenReturn(null);

        CareerService.CareerRegisterResult result = service.deleteCareer(1);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
        assertTrue(result.message.contains("no existe"));
    }

    @Test
    void deleteCareer_success_returnsOk() {
        Career career = mock(Career.class);
        when(repository.findById(1)).thenReturn(career);

        CareerService.CareerRegisterResult result = service.deleteCareer(1);

        assertTrue(result.success);
        assertEquals(201, result.statusCode);
        verify(repository).delete(career);
    }

    @Test
    void deleteCareer_repositoryThrows_returnsError() {
        Career career = mock(Career.class);
        when(repository.findById(1)).thenReturn(career);
        doThrow(new RuntimeException("DB error")).when(repository).delete(any());

        CareerService.CareerRegisterResult result = service.deleteCareer(1);

        assertFalse(result.success);
        assertEquals(500, result.statusCode);
    }

    // =====================================================================
    // getAllCareers
    // =====================================================================

    @Test
    void getAllCareers_returnsList() {
        Career c1 = mock(Career.class);
        Career c2 = mock(Career.class);
        when(repository.findAll()).thenReturn(Arrays.asList(c1, c2));

        List<Career> result = service.getAllCareers();

        assertEquals(2, result.size());
        verify(repository).findAll();
    }

    @Test
    void getAllCareers_emptyList() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Career> result = service.getAllCareers();

        assertTrue(result.isEmpty());
        verify(repository).findAll();
    }
}
