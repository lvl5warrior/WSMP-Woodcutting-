package com.warriorssmp.woodcutting.model;

import org.bukkit.Material;

public final class ResourceDef {

    public enum GatherType { MINING, WOODCUTTING, FARMING, PROCESSED }

    private final Material material;
    private final int tier;
    private final GatherType type;
    private final int requiredLevel;
    private final int minAmount;
    private final int maxAmount;

    public ResourceDef(Material material, int tier, GatherType type, int requiredLevel, int minAmount, int maxAmount) {
        this.material = material;
        this.tier = tier;
        this.type = type;
        this.requiredLevel = requiredLevel;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public Material material() {
        return material;
    }

    public int tier() {
        return tier;
    }

    public GatherType type() {
        return type;
    }

    public int requiredLevel() {
        return requiredLevel;
    }

    public int minAmount() {
        return minAmount;
    }

    public int maxAmount() {
        return maxAmount;
    }

    public int rollAmount(java.util.random.RandomGenerator rng) {
        if (maxAmount <= minAmount) return minAmount;
        return minAmount + rng.nextInt(maxAmount - minAmount + 1);
    }

    public String displayName() {
        String raw = material.name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
