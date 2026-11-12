package me.rexsystems.rexRewards.feedback;

import me.rexsystems.rexRewards.RexRewards;
import me.rexsystems.rexRewards.config.Settings;
import me.rexsystems.rexRewards.util.MessageService;
import me.rexsystems.rexRewards.util.Placeholders;
import me.rexsystems.rexRewards.util.TextParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Feedback {

    private final RexRewards plugin;
    private final Settings settings;
    private final Set<String> unknownSounds = new HashSet<>();

    public Feedback(RexRewards plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void send(CommandSender sender, String path, Map<String, String> values) {
        deliver(sender, settings.message(path), values);
    }

    public void sendRaw(CommandSender sender, String template, Map<String, String> values) {
        deliver(sender, template, values);
    }

    public void claimed(Player player, Map<String, String> values) {
        send(player, "messages.claimed", values);
        action(player, "actionbar.claim", values);
        play(player, "sounds.claim");
        title(player, values);
    }

    public void alreadyClaimed(Player player, Map<String, String> values) {
        send(player, "messages.already-claimed", values);
        action(player, "actionbar.already-claimed", values);
        play(player, "sounds.already-claimed");
    }

    public void inventoryFull(Player player, Map<String, String> values) {
        send(player, "messages.inventory-full", values);
        action(player, "actionbar.inventory-full", values);
        play(player, "sounds.inventory-full");
    }

    public void unavailable(Player player, Map<String, String> values) {
        send(player, "messages.unavailable", values);
        action(player, "actionbar.unavailable", values);
        play(player, "sounds.unavailable");
    }

    public void storageError(Player player) {
        send(player, "messages.storage-error", Map.of());
    }

    private void deliver(CommandSender sender, String template, Map<String, String> values) {
        if (sender == null || template == null || template.isBlank()) {
            return;
        }
        if (sender instanceof Player player) {
            String applied = Placeholders.apply(player, TextParser.apply(template, values));
            if (!TextParser.blank(applied)) {
                player.sendMessage(TextParser.parse(applied));
            }
            return;
        }
        MessageService.send(sender, template, values);
    }

    private void action(Player player, String path, Map<String, String> values) {
        String template = settings.string(path, "");
        String applied = Placeholders.apply(player, TextParser.apply(template, values));
        if (TextParser.blank(applied)) {
            return;
        }
        player.sendActionBar(TextParser.parse(applied));
    }

    private void title(Player player, Map<String, String> values) {
        if (!settings.enabled("title.claim.enabled", false)) {
            return;
        }
        Component title = component(player, settings.string("title.claim.title", ""), values);
        Component subtitle = component(player, settings.string("title.claim.subtitle", ""), values);
        int fadeIn = clampTicks(settings.number("title.claim.fade-in", 5));
        int stay = clampTicks(settings.number("title.claim.stay", 25));
        int fadeOut = clampTicks(settings.number("title.claim.fade-out", 8));
        player.showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))
        ));
    }

    private Component component(Player player, String template, Map<String, String> values) {
        String applied = Placeholders.apply(player, TextParser.apply(template, values));
        if (TextParser.blank(applied)) {
            return Component.empty();
        }
        return TextParser.parse(applied);
    }

    private void play(Player player, String path) {
        if (!settings.enabled(path + ".enabled", false)) {
            return;
        }
        String name = settings.string(path + ".sound", "");
        Sound sound = sound(name);
        if (sound == null) {
            if (!name.isBlank() && unknownSounds.add(name)) {
                plugin.getLogger().warning("Unknown sound '" + name + "'.");
            }
            return;
        }
        float volume = (float) settings.decimal(path + ".volume", 1.0D);
        float pitch = (float) settings.decimal(path + ".pitch", 1.0D);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static Sound sound(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String text = name.trim().toLowerCase(Locale.ROOT);
        NamespacedKey key;
        try {
            if (text.indexOf(':') >= 0) {
                key = NamespacedKey.fromString(text);
            } else {
                key = NamespacedKey.minecraft(text.replace('_', '.'));
            }
        } catch (IllegalArgumentException exception) {
            return null;
        }
        if (key == null) {
            return null;
        }
        return Registry.SOUNDS.get(key);
    }

    private static int clampTicks(int ticks) {
        return Math.max(0, Math.min(200, ticks));
    }
}
