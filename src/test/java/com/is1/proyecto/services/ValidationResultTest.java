package com.is1.proyecto.services;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {

    @Test
    void ok_isAllowed() {
        ValidationResult result = ValidationResult.ok();
        assertTrue(result.isAllowed());
    }

    @Test
    void ok_hasNullFields() {
        ValidationResult result = ValidationResult.ok();
        assertNull(result.getReason());
        assertNull(result.getMessage());
        assertNull(result.getMissingPrerequisites());
    }

    @Test
    void fail_isNotAllowed() {
        ValidationResult result = ValidationResult.fail("MISSING_PREREQUISITES", "Faltan correlativas", List.of());
        assertFalse(result.isAllowed());
    }

    @Test
    void fail_containsReasonAndMessage() {
        ValidationResult result = ValidationResult.fail("MISSING_PREREQUISITES", "Faltan correlativas", List.of());
        assertEquals("MISSING_PREREQUISITES", result.getReason());
        assertEquals("Faltan correlativas", result.getMessage());
    }

    @Test
    void fail_containsMissingPrerequisitesList() {
        var missing = List.of(
            new ValidationResult.MissingPrerequisite(1L, "Matemática I"),
            new ValidationResult.MissingPrerequisite(2L, "Física I")
        );
        ValidationResult result = ValidationResult.fail("MISSING_PREREQUISITES", "msg", missing);

        assertEquals(2, result.getMissingPrerequisites().size());
        assertEquals(1L, result.getMissingPrerequisites().get(0).id);
        assertEquals("Matemática I", result.getMissingPrerequisites().get(0).name);
        assertEquals(2L, result.getMissingPrerequisites().get(1).id);
        assertEquals("Física I", result.getMissingPrerequisites().get(1).name);
    }

    @Test
    void missingPrerequisite_storesIdAndName() {
        var mp = new ValidationResult.MissingPrerequisite(42L, "Álgebra");
        assertEquals(42L, mp.id);
        assertEquals("Álgebra", mp.name);
    }
}
