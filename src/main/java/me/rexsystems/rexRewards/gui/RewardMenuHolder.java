package me.rexsystems.rexRewards.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RewardMenuHolder implements InventoryHolder {

    private final UUID playerId;
    private final String menuId;
    private Inventory inventory;
    private String signature = "";
    private final Set<String> claims = new HashSet<>();
    private boolean refreshQueued;
    private int token;

    public RewardMenuHolder(UUID playerId, String menuId) {
        this.playerId = playerId;
        this.menuId = menuId;
    }

    public void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID playerId() {
        return playerId;
    }

    public String menuId() {
        return menuId;
    }

    public String signature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature == null ? "" : signature;
    }

    public void setClaims(Set<String> keys) {
        claims.clear();
        if (keys != null) {
            claims.addAll(keys);
        }
    }

    public void addClaim(String key) {
        claims.add(key);
    }

    public boolean claimed(String key) {
        return claims.contains(key);
    }

    public boolean tryBeginRefresh() {
        if (refreshQueued) {
            return false;
        }
        refreshQueued = true;
        return true;
    }

    public void endRefresh() {
        refreshQueued = false;
    }

    public int bumpToken() {
        return ++token;
    }

    public int token() {
        return token;
    }
}
