package me.rexsystems.rexRewards.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SchedulerUtils {

    private SchedulerUtils() {
    }

    public static void runSync(Plugin plugin, Runnable task) {
        if (plugin == null || task == null) {
            return;
        }
        Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (plugin == null || task == null) {
            return;
        }
        Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    public static void runForPlayer(Plugin plugin, Player player, Runnable task) {
        if (plugin == null || player == null || task == null) {
            return;
        }
        player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }

    public static void runForPlayer(Plugin plugin, Player player, Runnable task, Runnable retired) {
        if (plugin == null || player == null || task == null) {
            return;
        }
        player.getScheduler().run(plugin, scheduledTask -> task.run(), retired);
    }
}
