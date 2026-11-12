package me.rexsystems.rexRewards.util;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class MessageService {

    private MessageService() {
    }

    public static Component parse(String input) {
        return TextParser.parse(input);
    }

    public static Component parse(String input, Map<String, String> placeholders) {
        return TextParser.parse(TextParser.apply(input, placeholders));
    }

    public static void send(CommandSender sender, String message) {
        if (sender == null || TextParser.blank(message)) {
            return;
        }
        sender.sendMessage(parse(message));
    }

    public static void send(CommandSender sender, String message, Map<String, String> placeholders) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }
        String applied = TextParser.apply(message, placeholders);
        if (TextParser.blank(applied)) {
            return;
        }
        sender.sendMessage(parse(applied));
    }

    public static void actionBar(Player player, String message, Map<String, String> placeholders) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }
        String applied = TextParser.apply(message, placeholders);
        if (TextParser.blank(applied)) {
            return;
        }
        player.sendActionBar(parse(applied));
    }
}
