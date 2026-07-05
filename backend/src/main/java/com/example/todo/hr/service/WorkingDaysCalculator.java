package com.example.todo.hr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Calcule le nombre de jours ouvrés (lun-ven, hors jours fériés français fixes)
 * entre deux dates incluses.
 */
@Component
public class WorkingDaysCalculator {

    private static final Logger log = LoggerFactory.getLogger(WorkingDaysCalculator.class);

    /**
     * Jours fériés français à date fixe (on ignore les fériés mobiles type Pâques
     * pour rester simple et déterministe dans le cadre de la démo).
     * Format: "MM-dd"
     */
    private static final Set<String> FIXED_HOLIDAYS = Set.of(
            "01-01", // Jour de l'an
            "05-01", // Fête du travail
            "05-08", // Victoire 1945
            "07-14", // Fête nationale
            "08-15", // Assomption
            "11-01", // Toussaint
            "11-11", // Armistice
            "12-25"  // Noël
    );

    public int compute(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Dates de début et de fin obligatoires");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début");
        }

        int workingDays = 0;
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            if (isWorkingDay(cursor)) {
                workingDays++;
            }
            cursor = cursor.plusDays(1);
        }

        log.info("[LEAVE][CALC] Jours ouvrés du {} au {} = {} jour(s)", start, end, workingDays);
        return workingDays;
    }

    private boolean isWorkingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        String key = String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
        return !FIXED_HOLIDAYS.contains(key);
    }
}
