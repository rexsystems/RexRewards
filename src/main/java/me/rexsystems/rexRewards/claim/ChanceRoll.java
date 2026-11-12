package me.rexsystems.rexRewards.claim;

import java.util.UUID;

public final class ChanceRoll {

    private ChanceRoll() {
    }

    public static boolean available(UUID player, String menuId, String rewardId, String periodKey, double chance) {
        if (chance >= 100.0D) {
            return true;
        }
        if (chance <= 0.0D || player == null) {
            return false;
        }
        long mixed = player.getMostSignificantBits() ^ Long.rotateLeft(player.getLeastSignificantBits(), 17);
        mixed = mix(mixed, menuId);
        mixed = mix(mixed, rewardId);
        mixed = mix(mixed, periodKey);
        int roll = (int) Long.remainderUnsigned(mixed, 10_000L);
        int threshold = (int) Math.round(chance * 100.0D);
        return roll < threshold;
    }

    private static long mix(long value, String text) {
        long mixed = value;
        String source = text == null ? "" : text;
        for (int index = 0; index < source.length(); index++) {
            mixed = (mixed ^ source.charAt(index)) * 0x100000001B3L;
        }
        return mixed;
    }
}
