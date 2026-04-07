package com.is1.proyecto.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para Role enum.
 */
class RoleTest {

    @Test
    void fromString_validRoles() {
        assertEquals(Role.ADMIN, Role.fromString("ADMIN"));
        assertEquals(Role.ADMIN, Role.fromString("admin"));
        assertEquals(Role.ADMIN, Role.fromString("Admin"));

        assertEquals(Role.STUDENT, Role.fromString("STUDENT"));
        assertEquals(Role.STUDENT, Role.fromString("student"));
        assertEquals(Role.STUDENT, Role.fromString("Student"));

        assertEquals(Role.TEACHER, Role.fromString("TEACHER"));
        assertEquals(Role.TEACHER, Role.fromString("teacher"));
        assertEquals(Role.TEACHER, Role.fromString("Teacher"));
    }

    @Test
    void fromString_withWhitespace() {
        assertEquals(Role.ADMIN, Role.fromString("  ADMIN  "));
        assertEquals(Role.STUDENT, Role.fromString("\tSTUDENT\n"));
    }

    @Test
    void fromString_nullReturnsNull() {
        assertNull(Role.fromString(null));
    }

    @Test
    void fromString_emptyStringReturnsNull() {
        assertNull(Role.fromString(""));
    }

    @Test
    void fromString_invalidRoleReturnsNull() {
        assertNull(Role.fromString("SUPERADMIN"));
        assertNull(Role.fromString("ROOT"));
        assertNull(Role.fromString("guest"));
        assertNull(Role.fromString("random"));
    }

    @Test
    void allRolesExist() {
        assertNotNull(Role.ADMIN);
        assertNotNull(Role.STUDENT);
        assertNotNull(Role.TEACHER);
    }

    @Test
    void roleValuesAreDistinct() {
        assertNotEquals(Role.ADMIN, Role.STUDENT);
        assertNotEquals(Role.ADMIN, Role.TEACHER);
        assertNotEquals(Role.STUDENT, Role.TEACHER);
    }
}
