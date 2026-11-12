package me.rexsystems.rexRewards.menu;

import me.rexsystems.rexRewards.RexRewards;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Menus {

    private final RexRewards plugin;
    private volatile Map<String, MenuDefinition> byId = Map.of();

    public Menus(RexRewards plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "menus.yml");
        if (!file.exists()) {
            plugin.saveResource("menus.yml", false);
        }
        // menus.yml belongs to the server. Reload reads it and never writes it back.
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("menus");
        Map<String, MenuDefinition> loaded = new LinkedHashMap<>();
        if (root == null) {
            plugin.getLogger().warning("menus.yml has no menus section.");
            this.byId = Map.of();
            return;
        }
        for (String id : root.getKeys(false)) {
            if (id == null || id.isBlank() || id.length() > 64) {
                plugin.getLogger().warning("Skipping menu id '" + id + "'. Ids must be 1 to 64 characters.");
                continue;
            }
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                plugin.getLogger().warning("Skipping menu '" + id + "' because it is not a section.");
                continue;
            }
            MenuDefinition menu = readMenu(id, section);
            if (menu != null) {
                loaded.put(id, menu);
            }
        }
        this.byId = Collections.unmodifiableMap(loaded);
        plugin.getLogger().info("Loaded " + loaded.size() + " reward menu(s).");
    }

    public MenuDefinition find(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        MenuDefinition direct = byId.get(id);
        if (direct != null) {
            return direct;
        }
        for (MenuDefinition menu : byId.values()) {
            if (menu.id().equalsIgnoreCase(id)) {
                return menu;
            }
        }
        return null;
    }

    public Collection<MenuDefinition> all() {
        return byId.values();
    }

    private MenuDefinition readMenu(String id, ConfigurationSection section) {
        MenuType type = MenuType.parse(section.getString("type"));
        if (type == null) {
            plugin.getLogger().warning("Skipping menu '" + id + "'. type must be daily, weekly, or once.");
            return null;
        }
        ZoneId zone = zone(id, section.getString("timezone", "UTC"));
        DayOfWeek weekStart = weekStart(id, section.getString("week-start", "MONDAY"));
        String permission = section.getString("permission", "");
        if (permission == null) {
            permission = "";
        }
        ConfigurationSection gui = section.getConfigurationSection("gui");
        int rows = 3;
        String title = id;
        ItemDefinition filler = null;
        Map<Integer, ItemDefinition> decorations = new LinkedHashMap<>();
        if (gui != null) {
            rows = Math.max(1, Math.min(6, gui.getInt("rows", 3)));
            title = gui.getString("title", id);
            filler = item(gui.getConfigurationSection("filler"));
            decorations = decorations(id, gui.getConfigurationSection("slots"), rows);
        }
        Map<String, RewardDefinition> rewards = new LinkedHashMap<>();
        Map<Integer, RewardDefinition> bySlot = new LinkedHashMap<>();
        ConfigurationSection rewardSection = section.getConfigurationSection("rewards");
        if (rewardSection != null) {
            for (String rewardId : rewardSection.getKeys(false)) {
                if (rewardId == null || rewardId.isBlank() || rewardId.length() > 64) {
                    plugin.getLogger().warning("Skipping reward '" + rewardId + "' in " + id + ". Ids must be 1 to 64 characters.");
                    continue;
                }
                ConfigurationSection rewardCfg = rewardSection.getConfigurationSection(rewardId);
                if (rewardCfg == null) {
                    continue;
                }
                RewardDefinition reward = readReward(id, rewardId, rewardCfg, rows, bySlot);
                if (reward != null) {
                    rewards.put(rewardId, reward);
                    bySlot.put(reward.slot(), reward);
                }
            }
        }
        decorations.keySet().removeIf(slot -> {
            if (!bySlot.containsKey(slot)) {
                return false;
            }
            plugin.getLogger().warning("Decoration slot " + slot + " in " + id + " is covered by a reward.");
            return true;
        });
        return new MenuDefinition(
                id,
                type,
                zone,
                weekStart,
                permission.trim(),
                rows,
                title == null ? id : title,
                filler,
                Collections.unmodifiableMap(decorations),
                Collections.unmodifiableMap(rewards),
                Collections.unmodifiableMap(bySlot)
        );
    }

    private RewardDefinition readReward(String menuId, String rewardId, ConfigurationSection section, int rows, Map<Integer, RewardDefinition> taken) {
        if (!section.contains("slot")) {
            plugin.getLogger().warning("Skipping reward '" + rewardId + "' in " + menuId + ". slot is missing.");
            return null;
        }
        int slot = section.getInt("slot", -1);
        int size = rows * 9;
        if (slot < 0 || slot >= size) {
            plugin.getLogger().warning("Skipping reward '" + rewardId + "' in " + menuId + ". slot " + slot + " is outside the menu.");
            return null;
        }
        if (taken.containsKey(slot)) {
            plugin.getLogger().warning("Skipping reward '" + rewardId + "' in " + menuId + ". slot " + slot + " is already used.");
            return null;
        }
        double chance = section.getDouble("chance", 100.0D);
        boolean oneTime = section.getBoolean("one-time", false);
        List<String> commands = section.getStringList("commands");
        List<ItemDefinition> items = items(section);
        return new RewardDefinition(rewardId, slot, chance, oneTime, commands, items);
    }

    private Map<Integer, ItemDefinition> decorations(String menuId, ConfigurationSection section, int rows) {
        Map<Integer, ItemDefinition> slots = new LinkedHashMap<>();
        if (section == null) {
            return slots;
        }
        int size = rows * 9;
        for (String key : section.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("Skipping decoration '" + key + "' in " + menuId + ". The key must be a slot number.");
                continue;
            }
            if (slot < 0 || slot >= size) {
                plugin.getLogger().warning("Skipping decoration slot " + slot + " in " + menuId + ".");
                continue;
            }
            ItemDefinition item = item(section.getConfigurationSection(key));
            if (item != null) {
                slots.put(slot, item);
            }
        }
        return slots;
    }

    private List<ItemDefinition> items(ConfigurationSection section) {
        List<Map<?, ?>> maps = section.getMapList("items");
        List<ItemDefinition> items = new ArrayList<>();
        for (Map<?, ?> map : maps) {
            ItemDefinition item = item(map);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private ItemDefinition item(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        return item(
                section.getString("material"),
                section.contains("amount") ? section.getInt("amount") : 1,
                section.getString("name"),
                section.getStringList("lore"),
                section.contains("custom-model-data") ? section.getInt("custom-model-data") : null
        );
    }

    private ItemDefinition item(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Object model = map.containsKey("custom-model-data") ? map.get("custom-model-data") : null;
        return item(
                text(map.get("material")),
                map.containsKey("amount") ? number(map.get("amount"), 1) : 1,
                text(map.get("name")),
                stringList(map.get("lore")),
                model == null ? null : number(model, 0)
        );
    }

    private ItemDefinition item(String material, int amount, String name, List<String> lore, Integer customModelData) {
        if (material == null || material.isBlank()) {
            return null;
        }
        Material matched = Material.matchMaterial(material.trim());
        if (matched == null || matched.isAir()) {
            plugin.getLogger().warning("Unknown material '" + material + "'.");
            return null;
        }
        int clamped = amount;
        if (clamped > 2304) {
            plugin.getLogger().warning("Amount for " + material + " was capped at 2304.");
            clamped = 2304;
        }
        return new ItemDefinition(matched.name(), Math.max(1, clamped), name, lore, customModelData);
    }

    private ZoneId zone(String menuId, String raw) {
        String text = raw == null || raw.isBlank() ? "UTC" : raw.trim();
        try {
            return ZoneId.of(text);
        } catch (DateTimeException exception) {
            plugin.getLogger().warning("Menu '" + menuId + "' has an invalid timezone '" + text + "'. Using UTC.");
            return ZoneId.of("UTC");
        }
    }

    private DayOfWeek weekStart(String menuId, String raw) {
        String text = raw == null || raw.isBlank() ? "MONDAY" : raw.trim().toUpperCase(Locale.ROOT);
        try {
            return DayOfWeek.valueOf(text);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Menu '" + menuId + "' has an invalid week-start '" + raw + "'. Using MONDAY.");
            return DayOfWeek.MONDAY;
        }
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int number(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> lines = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    lines.add(String.valueOf(entry));
                }
            }
            return lines;
        }
        if (value == null) {
            return List.of();
        }
        return List.of(String.valueOf(value));
    }
}
