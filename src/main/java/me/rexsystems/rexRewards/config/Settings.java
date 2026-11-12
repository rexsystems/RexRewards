package me.rexsystems.rexRewards.config;

import me.rexsystems.rexRewards.RexRewards;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Settings {

    private final RexRewards plugin;
    private FileConfiguration config;

    public Settings(RexRewards plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        new ConfigAutoUpdater(plugin).update();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration config() {
        return config;
    }

    public int refreshTicks() {
        return Math.max(1, config.getInt("settings.refresh-ticks", 20));
    }

    public String prefix() {
        return string("messages.prefix", "");
    }

    public String message(String path) {
        return string(path, "").replace("{prefix}", prefix());
    }

    public List<String> messageList(String path) {
        String prefix = prefix();
        List<String> lines = new ArrayList<>();
        for (String line : config.getStringList(path)) {
            lines.add(line == null ? "" : line.replace("{prefix}", prefix));
        }
        return lines;
    }

    public String string(String path, String fallback) {
        String value = config.getString(path);
        return value == null ? fallback : value;
    }

    public boolean enabled(String path, boolean fallback) {
        return config.getBoolean(path, fallback);
    }

    public int number(String path, int fallback) {
        return config.getInt(path, fallback);
    }

    public double decimal(String path, double fallback) {
        return config.getDouble(path, fallback);
    }

    public StorageSettings storage() {
        String type = string("storage.type", "sqlite").trim().toLowerCase(Locale.ROOT);
        if (!type.equals("mysql")) {
            type = "sqlite";
        }
        int port = number("storage.mysql.port", 3306);
        if (port < 1 || port > 65535) {
            port = 3306;
        }
        int pool = number("storage.mysql.pool-size", 4);
        if (pool < 1) {
            pool = 1;
        } else if (pool > 20) {
            pool = 20;
        }
        return new StorageSettings(
                type,
                string("storage.sqlite.file", "rewards.db"),
                string("storage.mysql.host", "127.0.0.1"),
                port,
                string("storage.mysql.database", "rexrewards"),
                string("storage.mysql.username", "rexrewards"),
                string("storage.mysql.password", ""),
                pool,
                enabled("storage.mysql.use-ssl", false)
        );
    }
}
