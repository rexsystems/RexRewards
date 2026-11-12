package me.rexsystems.rexRewards.gui;

import me.rexsystems.rexRewards.RexRewards;
import me.rexsystems.rexRewards.claim.ChanceRoll;
import me.rexsystems.rexRewards.claim.ClaimService;
import me.rexsystems.rexRewards.config.Settings;
import me.rexsystems.rexRewards.menu.ItemDefinition;
import me.rexsystems.rexRewards.menu.MenuDefinition;
import me.rexsystems.rexRewards.menu.Menus;
import me.rexsystems.rexRewards.menu.RewardDefinition;
import me.rexsystems.rexRewards.storage.Database;
import me.rexsystems.rexRewards.util.Placeholders;
import me.rexsystems.rexRewards.util.SchedulerUtils;
import me.rexsystems.rexRewards.util.TextParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardMenus {

    private final RexRewards plugin;
    private final Menus menus;
    private final Database database;
    private final ItemFactory items;
    private final Map<UUID, RewardMenuHolder> open = new ConcurrentHashMap<>();
    private ClaimService claims;

    public RewardMenus(RexRewards plugin, Menus menus, Database database, Settings settings) {
        this.plugin = plugin;
        this.menus = menus;
        this.database = database;
        this.items = new ItemFactory(settings);
    }

    public void bind(ClaimService claims) {
        this.claims = claims;
    }

    public void open(Player player, MenuDefinition menu) {
        UUID uuid = player.getUniqueId();
        String menuId = menu.id();
        database.claimKeys(uuid, menuId).whenComplete((keys, error) -> SchedulerUtils.runSync(plugin, () -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online == null) {
                return;
            }
            if (error != null) {
                SchedulerUtils.runForPlayer(plugin, online, () -> plugin.feedback().storageError(online), null);
                plugin.getLogger().warning("Could not read claims for " + online.getName() + ": " + error.getMessage());
                return;
            }
            SchedulerUtils.runForPlayer(plugin, online, () -> show(online, menuId, keys), null);
        }));
    }

    public void refreshIfDue(Player player) {
        refresh(player, false);
    }

    public void refreshNow(Player player) {
        refresh(player, true);
    }

    public void reopenAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            SchedulerUtils.runForPlayer(plugin, player, () -> {
                RewardMenuHolder holder = open.get(player.getUniqueId());
                if (holder == null) {
                    return;
                }
                MenuDefinition menu = menus.find(holder.menuId());
                if (menu == null) {
                    player.closeInventory();
                    return;
                }
                open(player, menu);
            }, null);
        }
    }

    public void closed(RewardMenuHolder holder) {
        RewardMenuHolder current = open.get(holder.playerId());
        if (current == holder) {
            open.remove(holder.playerId());
        }
    }

    public void click(Player player, RewardMenuHolder holder, int slot) {
        MenuDefinition menu = menus.find(holder.menuId());
        if (menu == null || claims == null) {
            return;
        }
        RewardDefinition reward = menu.rewardAt(slot);
        if (reward == null) {
            return;
        }
        claims.claim(player, menu, reward, holder);
    }

    public boolean remembered(RewardMenuHolder holder, String key) {
        return holder != null && holder.claimed(key);
    }

    private void refresh(Player player, boolean force) {
        RewardMenuHolder holder = open.get(player.getUniqueId());
        if (holder == null || holder.getInventory() == null) {
            return;
        }
        if (!holder.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            open.remove(player.getUniqueId(), holder);
            return;
        }
        MenuDefinition menu = menus.find(holder.menuId());
        if (menu == null) {
            player.closeInventory();
            return;
        }
        if (holder.getInventory().getSize() != menu.size()) {
            open(player, menu);
            return;
        }
        Instant now = Instant.now();
        if (!force && menu.signature(now).equals(holder.signature())) {
            return;
        }
        if (!holder.tryBeginRefresh()) {
            return;
        }
        int token = holder.bumpToken();
        UUID uuid = player.getUniqueId();
        database.claimKeys(uuid, menu.id()).whenComplete((keys, error) -> SchedulerUtils.runSync(plugin, () -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online == null) {
                holder.endRefresh();
                return;
            }
            SchedulerUtils.runForPlayer(plugin, online, () -> {
                holder.endRefresh();
                if (error != null || holder.token() != token) {
                    return;
                }
                RewardMenuHolder current = open.get(uuid);
                if (current != holder || holder.getInventory() == null) {
                    return;
                }
                if (!holder.getInventory().equals(online.getOpenInventory().getTopInventory())) {
                    return;
                }
                MenuDefinition currentMenu = menus.find(holder.menuId());
                if (currentMenu == null) {
                    online.closeInventory();
                    return;
                }
                apply(online, holder, currentMenu, keys, Instant.now());
            }, holder::endRefresh);
        }));
    }

    private void show(Player player, String menuId, Set<String> keys) {
        MenuDefinition menu = menus.find(menuId);
        if (menu == null) {
            return;
        }
        RewardMenuHolder holder = new RewardMenuHolder(player.getUniqueId(), menu.id());
        Inventory inventory = Bukkit.createInventory(holder, menu.size(), TextParser.parse(Placeholders.apply(player, menu.title())));
        holder.attach(inventory);
        apply(player, holder, menu, keys, Instant.now());
        open.put(player.getUniqueId(), holder);
        player.openInventory(inventory);
    }

    public void noteClaim(Player player, String menuId, String rewardId, String period) {
        RewardMenuHolder holder = open.get(player.getUniqueId());
        if (holder == null || !holder.menuId().equals(menuId)) {
            return;
        }
        holder.bumpToken();
        holder.addClaim(rewardId + "\0" + period);
        MenuDefinition menu = menus.find(menuId);
        if (menu == null || holder.getInventory() == null) {
            return;
        }
        if (!holder.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            return;
        }
        paint(player, holder, menu, Instant.now());
    }

    private void apply(Player player, RewardMenuHolder holder, MenuDefinition menu, Set<String> keys, Instant instant) {
        holder.setClaims(keys);
        paint(player, holder, menu, instant);
    }

    private void paint(Player player, RewardMenuHolder holder, MenuDefinition menu, Instant instant) {
        holder.setSignature(menu.signature(instant));
        Inventory inventory = holder.getInventory();
        ItemStack filler = menu.filler() == null ? null : items.decoration(player, menu, menu.filler());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemDefinition decoration = menu.decorations().get(slot);
            RewardDefinition reward = menu.rewardAt(slot);
            if (reward != null) {
                String period = menu.periodKey(reward, instant);
                boolean claimed = holder.claimed(reward.id() + "\0" + period);
                boolean available = ChanceRoll.available(player.getUniqueId(), menu.id(), reward.id(), period, reward.chance());
                inventory.setItem(slot, items.icon(player, menu, reward, period, claimed, available));
            } else if (decoration != null) {
                inventory.setItem(slot, items.decoration(player, menu, decoration));
            } else {
                inventory.setItem(slot, filler == null ? null : filler.clone());
            }
        }
    }
}
