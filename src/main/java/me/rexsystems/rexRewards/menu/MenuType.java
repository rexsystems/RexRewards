package me.rexsystems.rexRewards.menu;

import java.util.Locale;

public enum MenuType {
    DAILY,
    WEEKLY,
    ONCE;

    public static MenuType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "daily" -> DAILY;
            case "weekly" -> WEEKLY;
            case "once", "one-time", "onetime" -> ONCE;
            default -> null;
        };
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
