package com.warriorssmp.woodcutting;

import com.warriorssmp.woodcutting.data.PlayerGatherData;
import com.warriorssmp.woodcutting.model.GatherTier;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Registers %woodcutting_level%, %woodcutting_xp%, %woodcutting_tier%, %woodcutting_streak%.
 * Useful for TAB / scoreboard integration alongside your other skill plugins.
 */
public final class WoodcuttingPlaceholders extends PlaceholderExpansion {

    private final WoodcuttingPlugin plugin;

    public WoodcuttingPlaceholders(WoodcuttingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "woodcutting";
    }

    @Override
    public @NotNull String getAuthor() {
        return "WarriorsSMP";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        PlayerGatherData data = plugin.dataStore().get(player.getUniqueId());
        int level = plugin.taskService().levelOf(data);

        return switch (params.toLowerCase()) {
            case "level" -> String.valueOf(level);
            case "xp" -> String.valueOf(data.totalXp);
            case "streak" -> String.valueOf(data.streak);
            case "tier" -> {
                GatherTier tier = plugin.gatherConfig().tierForLevel(level);
                yield tier == null ? "" : tier.rawName();
            }
            default -> null;
        };
    }
}
