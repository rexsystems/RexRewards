package me.rexsystems.rexRewards.claim;

import me.rexsystems.rexRewards.RexRewards;
import me.rexsystems.rexRewards.feedback.Feedback;
import me.rexsystems.rexRewards.gui.ItemFactory;
import me.rexsystems.rexRewards.gui.RewardMenuHolder;
import me.rexsystems.rexRewards.gui.RewardMenus;
import me.rexsystems.rexRewards.menu.MenuDefinition;
import me.rexsystems.rexRewards.menu.RewardDefinition;
import me.rexsystems.rexRewards.storage.Database;
import me.rexsystems.rexRewards.util.Placeholders;
import me.rexsystems.rexRewards.util.SchedulerUtils;
import me.rexsystems.rexRewards.util.TextParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClaimService {

    private final RexRewards plugin;
    private final Database database;
    private final Feedback feedback;
    private final RewardMenus menus;
    private final ItemFactory items;
    private final ClaimLocks locks = new ClaimLocks();

    public ClaimService(RexRewards plugin, Database database, Feedback feedback, RewardMenus menus) {
        this.plugin = plugin;
        this.database = database;
        this.feedback = feedback;
        this.menus = menus;
        this.items = new ItemFactory(plugin.settings());
    }

    public void claim(Player player, MenuDefinition menu, RewardDefinition reward, RewardMenuHolder holder) {
        if (!database.accepting()) {
            return;
        }
        if (!player.hasPermission("rexrewards.use") || (menu.permissionRequired() && !player.hasPermission(menu.permission()))) {
            feedback.send(player, "messages.menu-denied", Map.of("menu", menu.id(), "player", player.getName()));
            return;
        }
        Instant now = Instant.now();
        String period = menu.periodKey(reward, now);
        Map<String, String> values = values(player, menu, reward, period);
        if (!ChanceRoll.available(player.getUniqueId(), menu.id(), reward.id(), period, reward.chance())) {
            feedback.unavailable(player, values);
            return;
        }
        String memoryKey = reward.id() + "\0" + period;
        if (menus.remembered(holder, memoryKey)) {
            feedback.alreadyClaimed(player, values);
            return;
        }
        if (!locks.tryLock(player.getUniqueId(), menu.id(), reward.id())) {
            return;
        }
        UUID uuid = player.getUniqueId();
        database.insert(uuid, menu.id(), reward.id(), period).whenComplete((inserted, error) ->
                SchedulerUtils.runSync(plugin, () -> afterInsert(uuid, menu, reward, period, values, inserted, error)));
    }

    private void afterInsert(UUID uuid, MenuDefinition menu, RewardDefinition reward, String period,
                             Map<String, String> values, Boolean inserted, Throwable error) {
        if (error != null) {
            locks.unlock(uuid, menu.id(), reward.id());
            plugin.getLogger().warning("Claim insert failed: " + error.getMessage());
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                SchedulerUtils.runForPlayer(plugin, online, () -> feedback.storageError(online), null);
            }
            return;
        }
        Player online = Bukkit.getPlayer(uuid);
        if (inserted == null || !inserted) {
            if (online == null) {
                locks.unlock(uuid, menu.id(), reward.id());
                return;
            }
            SchedulerUtils.runForPlayer(plugin, online, () -> {
                try {
                    feedback.alreadyClaimed(online, values);
                    menus.noteClaim(online, menu.id(), reward.id(), period);
                } finally {
                    locks.unlock(uuid, menu.id(), reward.id());
                }
            }, () -> locks.unlock(uuid, menu.id(), reward.id()));
            return;
        }
        if (online == null) {
            releaseAfterDelete(uuid, menu.id(), reward.id(), period);
            return;
        }
        SchedulerUtils.runForPlayer(plugin, online,
                () -> deliver(online, menu, reward, period, values),
                () -> {
                    // The player left before the give ran, so the inserted row is removed.
                    releaseAfterDelete(uuid, menu.id(), reward.id(), period);
                });
    }

    private void deliver(Player player, MenuDefinition menu, RewardDefinition reward, String period, Map<String, String> values) {
        UUID uuid = player.getUniqueId();
        boolean given = false;
        boolean deletePending = false;
        ItemStack[] before = null;
        try {
            if (!player.isOnline()) {
                releaseAfterDelete(uuid, menu.id(), reward.id(), period);
                deletePending = true;
                return;
            }
            List<ItemStack> stacks = items.grant(player, menu, reward, period);
            if (ItemSpace.needsRollback(player, stacks)) {
                rollback(player, menu, reward, period, values);
                deletePending = true;
                return;
            }
            before = snapshot(player);
            var leftover = player.getInventory().addItem(stacks.toArray(ItemStack[]::new));
            if (!leftover.isEmpty()) {
                player.getInventory().setStorageContents(before);
                rollback(player, menu, reward, period, values);
                deletePending = true;
                return;
            }
            given = true;
            feedback.claimed(player, values);
            runCommands(player, reward, values);
            menus.noteClaim(player, menu.id(), reward.id(), period);
        } catch (Throwable failure) {
            if (!given && !deletePending) {
                if (before != null) {
                    player.getInventory().setStorageContents(before);
                }
                plugin.getLogger().warning("Claim give failed for " + player.getName() + ": " + failure.getMessage());
                rollback(player, menu, reward, period, values);
                deletePending = true;
            } else if (given) {
                plugin.getLogger().warning("Reward command failed for " + player.getName() + ": " + failure.getMessage());
            }
        } finally {
            if (!deletePending) {
                locks.unlock(uuid, menu.id(), reward.id());
            }
        }
    }

    private void rollback(Player player, MenuDefinition menu, RewardDefinition reward, String period, Map<String, String> values) {
        feedback.inventoryFull(player, values);
        releaseAfterDelete(player.getUniqueId(), menu.id(), reward.id(), period);
    }

    private void releaseAfterDelete(UUID player, String menuId, String rewardId, String period) {
        database.delete(player, menuId, rewardId, period).whenComplete((ignored, error) -> {
            if (error != null) {
                plugin.getLogger().severe("Could not roll back claim "
                        + player + " " + menuId + " " + rewardId + " " + period
                        + ": " + error.getMessage());
            }
            locks.unlock(player, menuId, rewardId);
        });
    }

    private void runCommands(Player player, RewardDefinition reward, Map<String, String> values) {
        // Folia rejects player edits from the global region, so reward commands stay on this thread.
        for (String command : reward.commands()) {
            String line = Placeholders.apply(player, TextParser.literals(command, values));
            line = line.replace('\n', ' ').replace('\r', ' ').trim();
            if (line.startsWith("/")) {
                line = line.substring(1).trim();
            }
            if (line.isEmpty()) {
                continue;
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
            } catch (Throwable failure) {
                plugin.getLogger().warning("Reward command failed for " + player.getName() + ": " + failure.getMessage());
            }
        }
    }

    private static ItemStack[] snapshot(Player player) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        ItemStack[] copy = new ItemStack[contents.length];
        for (int index = 0; index < contents.length; index++) {
            ItemStack stack = contents[index];
            copy[index] = stack == null ? null : stack.clone();
        }
        return copy;
    }

    private static Map<String, String> values(Player player, MenuDefinition menu, RewardDefinition reward, String period) {
        Map<String, String> values = new HashMap<>();
        values.put("player", player.getName());
        values.put("uuid", player.getUniqueId().toString());
        values.put("menu", menu.id());
        values.put("reward", reward.id());
        values.put("period", period);
        return values;
    }
}
