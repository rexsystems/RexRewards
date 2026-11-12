package me.rexsystems.rexRewards.claim;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ItemSpace {

    private ItemSpace() {
    }

    public static boolean needsRollback(Player player, List<ItemStack> stacks) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        ItemStack[] shadow = new ItemStack[contents.length];
        int free = 0;
        for (int index = 0; index < contents.length; index++) {
            ItemStack stack = contents[index];
            if (stack == null || stack.getType().isAir()) {
                free++;
                continue;
            }
            shadow[index] = stack.clone();
        }
        List<Integer> amounts = new ArrayList<>();
        List<Integer> limits = new ArrayList<>();
        if (stacks != null) {
            for (ItemStack giving : stacks) {
                if (giving == null || giving.getType().isAir() || giving.getAmount() <= 0) {
                    continue;
                }
                int left = giving.getAmount();
                for (ItemStack existing : shadow) {
                    if (existing == null || !existing.isSimilar(giving)) {
                        continue;
                    }
                    int room = existing.getMaxStackSize() - existing.getAmount();
                    if (room <= 0) {
                        continue;
                    }
                    int used = Math.min(room, left);
                    existing.setAmount(existing.getAmount() + used);
                    left -= used;
                    if (left <= 0) {
                        break;
                    }
                }
                if (left > 0) {
                    amounts.add(left);
                    limits.add(Math.max(1, giving.getMaxStackSize()));
                }
            }
        }
        return InventoryFit.shouldRollback(free, amounts, limits);
    }
}
