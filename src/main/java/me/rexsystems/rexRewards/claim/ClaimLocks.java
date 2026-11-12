package me.rexsystems.rexRewards.claim;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public final class ClaimLocks {

    private final Set<String> held = ConcurrentHashMap.newKeySet();

    public boolean tryLock(UUID player, String menuId, String rewardId) {
        return held.add(key(player, menuId, rewardId));
    }

    public void unlock(UUID player, String menuId, String rewardId) {
        held.remove(key(player, menuId, rewardId));
    }

    private static String key(UUID player, String menuId, String rewardId) {
        return player + "\0" + menuId + "\0" + rewardId;
    }
}
