package me.rexsystems.rexRewards.claim;

import java.util.List;

public final class InventoryFit {

    public static final int DEFAULT_STACK = 64;

    private InventoryFit() {
    }

    public static boolean shouldRollback(int freeSlots, List<Integer> amounts) {
        return shouldRollback(freeSlots, amounts, null);
    }

    public static boolean shouldRollback(int freeSlots, List<Integer> amounts, List<Integer> stackSizes) {
        long needed = slotsNeeded(amounts, stackSizes);
        if (needed <= 0) {
            return false;
        }
        return freeSlots < needed;
    }

    public static long slotsNeeded(List<Integer> amounts, List<Integer> stackSizes) {
        if (amounts == null || amounts.isEmpty()) {
            return 0L;
        }
        long needed = 0L;
        for (int index = 0; index < amounts.size(); index++) {
            Integer amountValue = amounts.get(index);
            if (amountValue == null || amountValue <= 0) {
                continue;
            }
            int stack = DEFAULT_STACK;
            if (stackSizes != null && index < stackSizes.size()) {
                Integer configured = stackSizes.get(index);
                if (configured != null && configured > 0) {
                    stack = configured;
                }
            }
            needed += ((long) amountValue + stack - 1L) / stack;
        }
        return needed;
    }
}
