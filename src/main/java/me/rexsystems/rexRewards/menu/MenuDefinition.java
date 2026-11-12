package me.rexsystems.rexRewards.menu;

import me.rexsystems.rexRewards.claim.PeriodKeys;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;

public record MenuDefinition(
        String id,
        MenuType type,
        ZoneId zone,
        DayOfWeek weekStart,
        String permission,
        int rows,
        String title,
        ItemDefinition filler,
        Map<Integer, ItemDefinition> decorations,
        Map<String, RewardDefinition> rewards,
        Map<Integer, RewardDefinition> bySlot
) {

    public int size() {
        return rows * 9;
    }

    public String periodKey(RewardDefinition reward, Instant instant) {
        return PeriodKeys.forReward(type, reward.oneTime(), instant, zone, weekStart);
    }

    public String signature(Instant instant) {
        StringBuilder builder = new StringBuilder();
        for (RewardDefinition reward : rewards.values()) {
            builder.append(reward.id()).append('=').append(periodKey(reward, instant)).append(';');
        }
        return builder.toString();
    }

    public RewardDefinition rewardAt(int slot) {
        return bySlot.get(slot);
    }

    public Collection<RewardDefinition> rewardList() {
        return rewards.values();
    }

    public boolean permissionRequired() {
        return permission != null && !permission.isBlank();
    }
}
