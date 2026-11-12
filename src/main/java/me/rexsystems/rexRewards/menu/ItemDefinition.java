package me.rexsystems.rexRewards.menu;

import java.util.List;

public record ItemDefinition(
        String material,
        int amount,
        String name,
        List<String> lore,
        Integer customModelData
) {
    public ItemDefinition {
        lore = lore == null ? List.of() : List.copyOf(lore);
        if (amount < 1) {
            amount = 1;
        }
    }
}
