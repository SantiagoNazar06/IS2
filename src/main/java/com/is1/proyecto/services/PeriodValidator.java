package com.is1.proyecto.services;

import java.time.LocalDate;
import java.util.regex.Pattern;

public class PeriodValidator {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-[12]$");

    public static boolean isValidFormat(String period) {
        return period != null && PERIOD_PATTERN.matcher(period).matches();
    }

    public static boolean isPastPeriod(String period) {
        int year = Integer.parseInt(period.substring(0, 4));
        int semester = Integer.parseInt(period.substring(5));
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentSemester = now.getMonthValue() <= 6 ? 1 : 2;
        if (year < currentYear) return true;
        if (year == currentYear) return semester < currentSemester;
        return false;
    }
}
