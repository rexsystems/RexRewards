package me.rexsystems.rexRewards.command;

import me.rexsystems.rexRewards.RexRewards;
import me.rexsystems.rexRewards.menu.MenuDefinition;
import me.rexsystems.rexRewards.util.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RewardsCommand implements CommandExecutor, TabCompleter {

    private final RexRewards plugin;

    public RewardsCommand(RexRewards plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return help(sender, label);
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "open" -> open(sender, label, args);
            case "list" -> list(sender, label);
            case "reload" -> reload(sender, label);
            case "admin" -> admin(sender, label, args);
            default -> usage(sender, label);
        };
    }

    private boolean help(CommandSender sender, String label) {
        Map<String, String> values = Map.of("label", label);
        plugin.feedback().send(sender, "messages.help-header", values);
        for (String line : plugin.settings().messageList("messages.help")) {
            plugin.feedback().sendRaw(sender, line, values);
        }
        return true;
    }

    private boolean usage(CommandSender sender, String label) {
        plugin.feedback().send(sender, "messages.usage", Map.of("label", label));
        return true;
    }

    private boolean open(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.feedback().send(sender, "messages.players-only", Map.of("label", label));
            return true;
        }
        if (!player.hasPermission("rexrewards.use")) {
            plugin.feedback().send(player, "messages.no-permission", Map.of("label", label));
            return true;
        }
        if (args.length < 2) {
            plugin.feedback().send(player, "messages.open-usage", Map.of("label", label));
            return true;
        }
        MenuDefinition menu = plugin.menus().find(args[1]);
        if (menu == null) {
            plugin.feedback().send(player, "messages.unknown-menu", Map.of("label", label, "menu", args[1]));
            return true;
        }
        if (menu.permissionRequired() && !player.hasPermission(menu.permission())) {
            plugin.feedback().send(player, "messages.menu-denied", Map.of("label", label, "menu", menu.id()));
            return true;
        }
        plugin.rewardMenus().open(player, menu);
        return true;
    }

    private boolean list(CommandSender sender, String label) {
        if (sender instanceof Player player && !player.hasPermission("rexrewards.use")) {
            plugin.feedback().send(sender, "messages.no-permission", Map.of("label", label));
            return true;
        }
        List<MenuDefinition> visible = new ArrayList<>();
        for (MenuDefinition menu : plugin.menus().all()) {
            if (!menu.permissionRequired() || sender.hasPermission(menu.permission())) {
                visible.add(menu);
            }
        }
        if (visible.isEmpty()) {
            plugin.feedback().send(sender, "messages.list-empty", Map.of("label", label));
            return true;
        }
        plugin.feedback().send(sender, "messages.list-header", Map.of("label", label));
        for (MenuDefinition menu : visible) {
            plugin.feedback().sendRaw(sender, plugin.settings().message("messages.list-line"), Map.of(
                    "label", label,
                    "menu", menu.id(),
                    "type", menu.type().id()
            ));
        }
        return true;
    }

    private boolean reload(CommandSender sender, String label) {
        if (!sender.hasPermission("rexrewards.reload")) {
            plugin.feedback().send(sender, "messages.no-permission", Map.of("label", label));
            return true;
        }
        try {
            plugin.reloadPlugin();
            plugin.feedback().send(sender, "messages.reload", Map.of("label", label));
        } catch (RuntimeException failure) {
            plugin.getLogger().warning("Reload failed: " + failure.getMessage());
            plugin.feedback().send(sender, "messages.reload-failed", Map.of("label", label));
        }
        return true;
    }

    private boolean admin(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("rexrewards.admin")) {
            plugin.feedback().send(sender, "messages.no-permission", Map.of("label", label));
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("reset")) {
            plugin.feedback().send(sender, "messages.reset-usage", Map.of("label", label));
            return true;
        }
        if (args.length < 4) {
            plugin.feedback().send(sender, "messages.reset-usage", Map.of("label", label));
            return true;
        }
        MenuDefinition menu = plugin.menus().find(args[3]);
        if (menu == null) {
            plugin.feedback().send(sender, "messages.unknown-menu", Map.of("label", label, "menu", args[3]));
            return true;
        }
        String playerName = args[2];
        String menuId = menu.id();
        SchedulerUtils.runSync(plugin, () -> resolveAndReset(sender, playerName, menuId));
        return true;
    }

    private void resolveAndReset(CommandSender sender, String playerName, String menuId) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online == null) {
            for (Player candidate : Bukkit.getOnlinePlayers()) {
                if (candidate.getName().equalsIgnoreCase(playerName)) {
                    online = candidate;
                    break;
                }
            }
        }
        if (online != null) {
            deleteClaims(sender, online.getUniqueId(), online.getName(), menuId);
            return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
        if (!offline.hasPlayedBefore()) {
            plugin.feedback().send(sender, "messages.unknown-player", Map.of("player", playerName));
            return;
        }
        String resolved = offline.getName() == null ? playerName : offline.getName();
        deleteClaims(sender, offline.getUniqueId(), resolved, menuId);
    }

    private void deleteClaims(CommandSender sender, UUID uuid, String name, String menuId) {
        plugin.database().deleteMenu(uuid, menuId).whenComplete((count, error) -> SchedulerUtils.runSync(plugin, () -> {
            if (error != null) {
                plugin.getLogger().warning("Reset failed: " + error.getMessage());
                reply(sender, "messages.reset-failed", Map.of("player", name, "menu", menuId));
                return;
            }
            reply(sender, "messages.reset-done", Map.of(
                    "count", Integer.toString(count == null ? 0 : count),
                    "player", name,
                    "menu", menuId
            ));
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                SchedulerUtils.runForPlayer(plugin, target, () -> plugin.rewardMenus().refreshNow(target), null);
            }
        }));
    }

    private void reply(CommandSender sender, String path, Map<String, String> values) {
        if (sender instanceof Player player) {
            SchedulerUtils.runForPlayer(plugin, player, () -> plugin.feedback().send(player, path, values), null);
            return;
        }
        plugin.feedback().send(sender, path, values);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("help");
            options.add("list");
            options.add("open");
            if (sender.hasPermission("rexrewards.reload")) {
                options.add("reload");
            }
            if (sender.hasPermission("rexrewards.admin")) {
                options.add("admin");
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return filter(visibleMenus(sender), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return sender.hasPermission("rexrewards.admin") ? filter(List.of("reset"), args[1]) : List.of();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("rexrewards.admin")) {
                return List.of();
            }
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filter(names, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("rexrewards.admin")) {
                return List.of();
            }
            List<String> ids = new ArrayList<>();
            for (MenuDefinition menu : plugin.menus().all()) {
                ids.add(menu.id());
            }
            return filter(ids, args[3]);
        }
        return List.of();
    }

    private List<String> visibleMenus(CommandSender sender) {
        List<String> ids = new ArrayList<>();
        for (MenuDefinition menu : plugin.menus().all()) {
            if (!menu.permissionRequired() || sender.hasPermission(menu.permission())) {
                ids.add(menu.id());
            }
        }
        return ids;
    }

    private static List<String> filter(List<String> options, String prefix) {
        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(needle)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
