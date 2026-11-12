package me.rexsystems.rexRewards.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextParser {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private static final Pattern HEX_AMP = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_HASH = Pattern.compile("(?<![<:])#([A-Fa-f0-9]{6})");

    private static final Map<Character, String> CODES = legacyCodes();

    private TextParser() {
    }

    public static String apply(String input, Map<String, String> placeholders) {
        String value = input == null ? "" : input;
        if (placeholders == null || placeholders.isEmpty()) {
            return value;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            String replacement = escapeValue(entry.getValue());
            value = value.replace("{" + key + "}", replacement);
            value = value.replace("%" + key + "%", replacement);
        }
        return value;
    }

    public static String literals(String input, Map<String, String> placeholders) {
        String value = input == null ? "" : input;
        if (placeholders == null || placeholders.isEmpty()) {
            return value;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            String replacement = entry.getValue() == null ? "" : entry.getValue();
            value = value.replace("{" + key + "}", replacement);
            value = value.replace("%" + key + "%", replacement);
        }
        return value;
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String normalized = normalize(input);
        try {
            return MINI.deserialize(normalized);
        } catch (Exception ignored) {
            return LEGACY.deserialize(input.replace('§', '&'));
        }
    }

    public static boolean blank(String input) {
        if (input == null || input.isBlank()) {
            return true;
        }
        return PLAIN.serialize(parse(input)).isBlank();
    }

    public static String escapeValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return MINI.escapeTags(value).replace('&', '＆').replace('§', ' ');
    }

    static String normalize(String input) {
        String value = input.replace('§', '&');
        value = replaceHex(HEX_AMP, value);
        // A hash that already sits in a MiniMessage tag or a namespace is not a legacy hex color.
        value = replaceHex(HEX_HASH, value);
        return replaceCodes(value);
    }

    private static String replaceHex(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String replaceCodes(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '&' && index + 1 < value.length()) {
                String tag = CODES.get(Character.toLowerCase(value.charAt(index + 1)));
                if (tag != null) {
                    out.append(tag);
                    index++;
                    continue;
                }
            }
            out.append(current);
        }
        return out.toString();
    }

    private static Map<Character, String> legacyCodes() {
        Map<Character, String> codes = new LinkedHashMap<>();
        codes.put('0', "<black>");
        codes.put('1', "<dark_blue>");
        codes.put('2', "<dark_green>");
        codes.put('3', "<dark_aqua>");
        codes.put('4', "<dark_red>");
        codes.put('5', "<dark_purple>");
        codes.put('6', "<gold>");
        codes.put('7', "<gray>");
        codes.put('8', "<dark_gray>");
        codes.put('9', "<blue>");
        codes.put('a', "<green>");
        codes.put('b', "<aqua>");
        codes.put('c', "<red>");
        codes.put('d', "<light_purple>");
        codes.put('e', "<yellow>");
        codes.put('f', "<white>");
        codes.put('k', "<obfuscated>");
        codes.put('l', "<bold>");
        codes.put('m', "<strikethrough>");
        codes.put('n', "<underlined>");
        codes.put('o', "<italic>");
        codes.put('r', "<reset>");
        return Map.copyOf(codes);
    }
}
