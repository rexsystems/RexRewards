package me.rexsystems.rexRewards.menu;

import java.util.List;

public record RewardDefinition(
        String id,
        int slot,
        double chance,
        boolean oneTime,
        List<String> commands,
        List<ItemDefinition> items
) {
    public RewardDefinition {
        commands = commands == null ? List.of() : List.copyOf(commands);
        items = items == null ? List.of() : List.copyOf(items);
        if (chance < 0.0D) {
            chance = 0.0D;
        } else if (chance > 100.0D) {
            chance = 100.0D;
        }
    }
}
