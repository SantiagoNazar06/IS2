package com.is1.proyecto.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para ConditionType enum.
 * Sigue el patrón de RoleTest.
 */
class ConditionTypeTest {

    @Test
    void valuesExist() {
        assertNotNull(ConditionType.REGULAR);
        assertNotNull(ConditionType.APROBADA);
    }

    @Test
    void valuesAreDistinct() {
        assertNotEquals(ConditionType.REGULAR, ConditionType.APROBADA);
    }

    @Test
    void fromString_validValues() {
        assertEquals(ConditionType.REGULAR, ConditionType.fromString("REGULAR"));
        assertEquals(ConditionType.APROBADA, ConditionType.fromString("APROBADA"));
    }

    @Test
    void fromString_caseInsensitive() {
        assertEquals(ConditionType.REGULAR, ConditionType.fromString("regular"));
        assertEquals(ConditionType.REGULAR, ConditionType.fromString("Regular"));
        assertEquals(ConditionType.APROBADA, ConditionType.fromString("aprobada"));
        assertEquals(ConditionType.APROBADA, ConditionType.fromString("Aprobada"));
    }

    @Test
    void fromString_withWhitespace() {
        assertEquals(ConditionType.REGULAR, ConditionType.fromString("  REGULAR  "));
        assertEquals(ConditionType.APROBADA, ConditionType.fromString("\tAPROBADA\n"));
    }

    @Test
    void fromString_nullReturnsNull() {
        assertNull(ConditionType.fromString(null));
    }

    @Test
    void fromString_emptyReturnsNull() {
        assertNull(ConditionType.fromString(""));
        assertNull(ConditionType.fromString("   "));
    }

    @Test
    void fromString_invalidReturnsNull() {
        assertNull(ConditionType.fromString("APROBADO"));
        assertNull(ConditionType.fromString("DESAPROBADA"));
        assertNull(ConditionType.fromString("INVALID"));
    }
}
