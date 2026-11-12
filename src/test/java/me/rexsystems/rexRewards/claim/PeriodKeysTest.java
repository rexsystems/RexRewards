package me.rexsystems.rexRewards.claim;

import me.rexsystems.rexRewards.menu.MenuType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PeriodKeysTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");

    @Test
    void dailyKeyFlipsAtTheZonesOwnMidnight() {
        Instant beforeBerlin = Instant.parse("2026-01-14T22:59:59Z");
        Instant afterBerlin = Instant.parse("2026-01-14T23:00:00Z");
        assertEquals("2026-01-14", PeriodKeys.daily(beforeBerlin, BERLIN));
        assertEquals("2026-01-15", PeriodKeys.daily(afterBerlin, BERLIN));

        Instant beforeLosAngeles = Instant.parse("2026-01-16T07:59:59Z");
        Instant afterLosAngeles = Instant.parse("2026-01-16T08:00:00Z");
        assertEquals("2026-01-15", PeriodKeys.daily(beforeLosAngeles, LOS_ANGELES));
        assertEquals("2026-01-16", PeriodKeys.daily(afterLosAngeles, LOS_ANGELES));

        Instant sameMoment = Instant.parse("2026-01-15T23:30:00Z");
        assertEquals("2026-01-16", PeriodKeys.daily(sameMoment, BERLIN));
        assertEquals("2026-01-15", PeriodKeys.daily(sameMoment, LOS_ANGELES));
    }

    @Test
    void weeklyKeyFlipsOnTheConfiguredWeekStartNotUtc() {
        Instant sundayNight = Instant.parse("2026-01-19T07:30:00Z");
        Instant mondayMorning = Instant.parse("2026-01-19T08:00:00Z");

        assertEquals("2026-01-18", PeriodKeys.daily(sundayNight, LOS_ANGELES));
        assertEquals("2026-01-12", PeriodKeys.weekly(sundayNight, LOS_ANGELES, DayOfWeek.MONDAY));
        assertEquals("2026-01-19", PeriodKeys.daily(sundayNight, ZoneId.of("UTC")));
        assertEquals("2026-01-19", PeriodKeys.weekly(sundayNight, ZoneId.of("UTC"), DayOfWeek.MONDAY));

        assertEquals("2026-01-19", PeriodKeys.daily(mondayMorning, LOS_ANGELES));
        assertEquals("2026-01-19", PeriodKeys.weekly(mondayMorning, LOS_ANGELES, DayOfWeek.MONDAY));
    }

    @Test
    void sundayWeekStartFlipsAtLocalSaturdayMidnight() {
        Instant saturdayNight = Instant.parse("2026-01-18T07:59:59Z");
        Instant sundayMorning = Instant.parse("2026-01-18T08:00:00Z");

        assertEquals("2026-01-11", PeriodKeys.weekly(saturdayNight, LOS_ANGELES, DayOfWeek.SUNDAY));
        assertEquals("2026-01-18", PeriodKeys.weekly(sundayMorning, LOS_ANGELES, DayOfWeek.SUNDAY));
        assertEquals("2026-01-12", PeriodKeys.weekly(sundayMorning, LOS_ANGELES, DayOfWeek.MONDAY));
    }

    @Test
    void midnightInsideAWeekDoesNotChangeTheWeeklyKey() {
        Instant before = Instant.parse("2026-01-14T22:59:59Z");
        Instant after = Instant.parse("2026-01-14T23:00:00Z");
        assertEquals("2026-01-12", PeriodKeys.weekly(before, BERLIN, DayOfWeek.MONDAY));
        assertEquals("2026-01-12", PeriodKeys.weekly(after, BERLIN, DayOfWeek.MONDAY));
        assertEquals("2026-01-14", PeriodKeys.daily(before, BERLIN));
        assertEquals("2026-01-15", PeriodKeys.daily(after, BERLIN));
    }

    @Test
    void onceKeyStaysConstantAndOverridesTheMenuPeriod() {
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        Instant later = Instant.parse("2026-06-01T12:00:00Z");
        assertEquals("once", PeriodKeys.once());
        assertEquals("once", PeriodKeys.forReward(MenuType.ONCE, false, first, BERLIN, DayOfWeek.MONDAY));
        assertEquals("once", PeriodKeys.forReward(MenuType.DAILY, true, later, LOS_ANGELES, DayOfWeek.SUNDAY));
        assertEquals("once", PeriodKeys.forReward(MenuType.WEEKLY, true, later, LOS_ANGELES, DayOfWeek.MONDAY));
        assertEquals(PeriodKeys.daily(later, LOS_ANGELES),
                PeriodKeys.forReward(MenuType.DAILY, false, later, LOS_ANGELES, DayOfWeek.MONDAY));
        assertEquals(PeriodKeys.weekly(later, LOS_ANGELES, DayOfWeek.MONDAY),
                PeriodKeys.forReward(MenuType.WEEKLY, false, later, LOS_ANGELES, DayOfWeek.MONDAY));
    }
}
