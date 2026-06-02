package com.is1.proyecto.services;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PeriodValidatorTest {

    // --- isValidFormat ---

    @Test
    void isValidFormat_firstSemester_returnsTrue() {
        assertTrue(PeriodValidator.isValidFormat("2025-1"));
    }

    @Test
    void isValidFormat_secondSemester_returnsTrue() {
        assertTrue(PeriodValidator.isValidFormat("2025-2"));
    }

    @Test
    void isValidFormat_semesterThree_returnsFalse() {
        assertFalse(PeriodValidator.isValidFormat("2025-3"));
    }

    @Test
    void isValidFormat_missingYear_returnsFalse() {
        assertFalse(PeriodValidator.isValidFormat("25-1"));
    }

    @Test
    void isValidFormat_wrongSeparator_returnsFalse() {
        assertFalse(PeriodValidator.isValidFormat("2025/1"));
    }

    @Test
    void isValidFormat_null_returnsFalse() {
        assertFalse(PeriodValidator.isValidFormat(null));
    }

    @Test
    void isValidFormat_emptyString_returnsFalse() {
        assertFalse(PeriodValidator.isValidFormat(""));
    }

    @Test
    void isValidFormat_withSpaces_returnsFalse() {
        assertFalse(PeriodValidator.isValidFormat("2025 -1"));
    }

    // --- isPastPeriod ---

    @Test
    void isPastPeriod_pastYear_returnsTrue() {
        assertTrue(PeriodValidator.isPastPeriod("2020-1"));
    }

    @Test
    void isPastPeriod_currentYearPastSemester_returnsTrue() {
        int currentYear = LocalDate.now().getYear();
        int currentSemester = LocalDate.now().getMonthValue() <= 6 ? 1 : 2;
        if (currentSemester == 2) {
            // Estamos en el segundo semestre, el primero ya pasó
            assertTrue(PeriodValidator.isPastPeriod(currentYear + "-1"));
        } else {
            // Estamos en el primer semestre, no hay semestre pasado en el año actual
            // Este caso se cubre con el test de año anterior
            assertTrue(PeriodValidator.isPastPeriod((currentYear - 1) + "-2"));
        }
    }

    @Test
    void isPastPeriod_currentPeriod_returnsFalse() {
        int currentYear = LocalDate.now().getYear();
        int currentSemester = LocalDate.now().getMonthValue() <= 6 ? 1 : 2;
        assertFalse(PeriodValidator.isPastPeriod(currentYear + "-" + currentSemester));
    }

    @Test
    void isPastPeriod_futureYear_returnsFalse() {
        int futureYear = LocalDate.now().getYear() + 2;
        assertFalse(PeriodValidator.isPastPeriod(futureYear + "-1"));
    }

    @Test
    void isPastPeriod_nextSemesterSameYear_returnsFalse() {
        int currentYear = LocalDate.now().getYear();
        int currentSemester = LocalDate.now().getMonthValue() <= 6 ? 1 : 2;
        if (currentSemester == 1) {
            // El segundo semestre del año actual es futuro
            assertFalse(PeriodValidator.isPastPeriod(currentYear + "-2"));
        } else {
            // Ya estamos en el segundo semestre, el primero del año siguiente es futuro
            assertFalse(PeriodValidator.isPastPeriod((currentYear + 1) + "-1"));
        }
    }
}
