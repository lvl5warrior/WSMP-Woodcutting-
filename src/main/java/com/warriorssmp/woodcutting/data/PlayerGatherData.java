package com.warriorssmp.woodcutting.data;

import com.warriorssmp.woodcutting.model.GatherTask;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerGatherData {

    public final UUID uuid;
    public long totalXp = 0;
    public long points = 0;
    public int streak = 0;
    public long lastTaskCompletedAt = 0;
    public GatherTask activeTask = null;
    public final Set<Material> blockedResources = new HashSet<>();
    public final List<TaskHistoryEntry> history = new ArrayList<>();
    public final Set<Integer> purchasedOneTimeItems = new HashSet<>();
    public boolean masterTeleportUnlocked = false;
    public long lastMasterTeleport = 0;
    public long lastGuideBookAt = 0;
    public long luckyStrikeBoostExpiry = 0; // reserved; Lucky Strike itself is chance-only now
    public long xpBoostExpiry = 0;
    public long pointBoostExpiry = 0;
    public long betterTasksExpiry = 0;
    public int lifetimeTasksCompleted = 0;
    public int lifetimeResourcesGathered = 0;
    public int lifetimeLuckyStrikes = 0;
    public int lifetimeLegendaryCompleted = 0;

    /** requestId -> rolled target amount for the current cycle (ancient_vein, sculk_harvest, void_orchard). */
    public final Map<String, Integer> legendaryTarget = new HashMap<>();
    /** requestId -> current progress toward legendaryTarget. */
    public final Map<String, Integer> legendaryProgress = new HashMap<>();
    /** requestId -> timestamp when that request becomes available again (0 = available now). */
    public final Map<String, Long> legendaryReadyAt = new HashMap<>();

    // The Triad Trial is a fixed three-part structure (one objective per gathering
    // path), so it's tracked with its own fields rather than the generic maps above.
    public int triadAncientDebrisProgress = 0;
    public int triadStemsProgress = 0;
    public int triadChorusProgress = 0;
    public long triadReadyAt = 0;

    public PlayerGatherData(UUID uuid) {
        this.uuid = uuid;
    }

    /** Wipes every stat/progress field back to a brand-new player's state.
     *  Used by the Admin Panel's "Reset Player" button. */
    public void resetAll() {
        totalXp = 0;
        points = 0;
        streak = 0;
        lastTaskCompletedAt = 0;
        activeTask = null;
        blockedResources.clear();
        history.clear();
        purchasedOneTimeItems.clear();
        masterTeleportUnlocked = false;
        lastMasterTeleport = 0;
        lastGuideBookAt = 0;
        luckyStrikeBoostExpiry = 0;
        xpBoostExpiry = 0;
        pointBoostExpiry = 0;
        betterTasksExpiry = 0;
        lifetimeTasksCompleted = 0;
        lifetimeResourcesGathered = 0;
        lifetimeLuckyStrikes = 0;
        lifetimeLegendaryCompleted = 0;
        legendaryTarget.clear();
        legendaryProgress.clear();
        legendaryReadyAt.clear();
        triadAncientDebrisProgress = 0;
        triadStemsProgress = 0;
        triadChorusProgress = 0;
        triadReadyAt = 0;
    }

    public record TaskHistoryEntry(String materialName, int amount, long xpGained,
                                    double coinsGained, boolean skipped, long timestamp) {}
}
