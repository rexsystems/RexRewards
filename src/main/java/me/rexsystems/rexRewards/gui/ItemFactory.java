package me.rexsystems.rexRewards.gui;

import me.rexsystems.rexRewards.RexRewards;
import me.rexsystems.rexRewards.config.Settings;
import me.rexsystems.rexRewards.menu.ItemDefinition;
import me.rexsystems.rexRewards.menu.MenuDefinition;
import me.rexsystems.rexRewards.menu.RewardDefinition;
import me.rexsystems.rexRewards.util.Placeholders;
import me.rexsystems.rexRewards.util.TextParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemFactory {

    private final Settings settings;

    public ItemFactory(Settings settings) {
        this.settings = settings;
    }

    public ItemStack icon(Player player, MenuDefinition menu, RewardDefinition reward, String periodKey, boolean claimed, boolean available) {
        String statusPath = claimed
                ? "messages.gui.claimed-lore"
                : available ? "messages.gui.click-lore" : "messages.gui.unavailable-lore";
        List<String> extra = List.of(settings.message(statusPath));
        if (reward.items().isEmpty()) {
            return named(new ItemStack(Material.PAPER), player, menu, reward, periodKey, reward.id(), List.of(), null, extra);
        }
        ItemDefinition first = reward.items().get(0);
        ItemStack stack = base(first);
        if (stack == null) {
            stack = new ItemStack(Material.PAPER);
        }
        stack = named(stack, player, menu, reward, periodKey, first.name(), first.lore(), first.customModelData(), extra);
        stack.setAmount(Math.min(first.amount(), Math.max(1, stack.getMaxStackSize())));
        return stack;
    }

    public ItemStack decoration(Player player, MenuDefinition menu, ItemDefinition definition) {
        ItemStack stack = base(definition);
        if (stack == null) {
            return null;
        }
        return named(stack, player, menu, null, "", definition.name(), definition.lore(), definition.customModelData(), List.of());
    }

    public List<ItemStack> grant(Player player, MenuDefinition menu, RewardDefinition reward, String periodKey) {
        List<ItemStack> stacks = new ArrayList<>();
        Map<String, String> values = values(player, menu, reward, periodKey);
        for (ItemDefinition definition : reward.items()) {
            ItemStack prototype = base(definition);
            if (prototype == null) {
                continue;
            }
            ItemStack shaped = named(prototype, player, values, definition.name(), definition.lore(), definition.customModelData(), List.of());
            int left = definition.amount();
            int max = Math.max(1, shaped.getMaxStackSize());
            while (left > 0) {
                ItemStack piece = shaped.clone();
                int amount = Math.min(left, max);
                piece.setAmount(amount);
                stacks.add(piece);
                left -= amount;
            }
        }
        return stacks;
    }

    private ItemStack named(ItemStack stack, Player player, MenuDefinition menu, RewardDefinition reward, String periodKey,
                            String name, List<String> lore, Integer customModelData, List<String> extraLore) {
        return named(stack, player, values(player, menu, reward, periodKey), name, lore, customModelData, extraLore);
    }

    private ItemStack named(ItemStack stack, Player player, Map<String, String> values, String name, List<String> lore,
                            Integer customModelData, List<String> extraLore) {
        ItemMeta meta = stack.getItemMeta();
        if (name == null || name.isBlank()) {
            meta.setHideTooltip(true);
        } else {
            meta.displayName(noItalic(parse(player, name, values)));
        }
        List<Component> lines = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) {
                if (line == null || TextParser.blank(line)) {
                    continue;
                }
                lines.add(noItalic(parse(player, line, values)));
            }
        }
        if (extraLore != null) {
            for (String line : extraLore) {
                if (line == null || TextParser.blank(line)) {
                    continue;
                }
                lines.add(noItalic(parse(player, line, values)));
            }
        }
        if (!lines.isEmpty()) {
            meta.lore(lines);
            meta.setHideTooltip(false);
        }
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private Component parse(Player player, String template, Map<String, String> values) {
        return TextParser.parse(Placeholders.apply(player, TextParser.apply(template, values)));
    }

    private static ItemStack base(ItemDefinition definition) {
        if (definition == null || definition.material() == null) {
            return null;
        }
        Material material = Material.matchMaterial(definition.material());
        if (material == null || material.isAir()) {
            return null;
        }
        return new ItemStack(material);
    }

    private static Map<String, String> values(Player player, MenuDefinition menu, RewardDefinition reward, String periodKey) {
        return Map.of(
                "player", player.getName(),
                "uuid", player.getUniqueId().toString(),
                "menu", menu.id(),
                "reward", reward == null ? "" : reward.id(),
                "period", periodKey == null ? "" : periodKey
        );
    }

    static Component noItalic(Component component) {
        // Item names are italic unless the component turns that decoration off.
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
