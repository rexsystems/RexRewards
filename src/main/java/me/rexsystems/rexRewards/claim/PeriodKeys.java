package me.rexsystems.rexRewards.claim;

import me.rexsystems.rexRewards.menu.MenuType;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class PeriodKeys {

    public static final String ONCE = "once";

    private PeriodKeys() {
    }

    public static String daily(Instant instant, ZoneId zone) {
        return localDate(instant, zone).toString();
    }

    public static String weekly(Instant instant, ZoneId zone, DayOfWeek weekStart) {
        DayOfWeek start = weekStart == null ? DayOfWeek.MONDAY : weekStart;
        LocalDate date = localDate(instant, zone);
        int shift = Math.floorMod(date.getDayOfWeek().getValue() - start.getValue(), 7);
        return date.minusDays(shift).toString();
    }

    public static String once() {
        return ONCE;
    }

    public static String forReward(MenuType type, boolean oneTime, Instant instant, ZoneId zone, DayOfWeek weekStart) {
        if (oneTime || type == MenuType.ONCE) {
            return once();
        }
        if (type == MenuType.WEEKLY) {
            return weekly(instant, zone, weekStart);
        }
        return daily(instant, zone);
    }

    private static LocalDate localDate(Instant instant, ZoneId zone) {
        ZoneId resolved = zone == null ? ZoneId.of("UTC") : zone;
        return instant.atZone(resolved).toLocalDate();
    }
}
