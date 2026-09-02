package com.warriorssmp.woodcutting.task;

import com.warriorssmp.woodcutting.WoodcuttingPlugin;
import com.warriorssmp.woodcutting.data.DataStore;
import com.warriorssmp.woodcutting.data.PlayerGatherData;
import com.warriorssmp.woodcutting.economy.EconomyService;
import com.warriorssmp.woodcutting.model.GatherTask;
import com.warriorssmp.woodcutting.model.GatherTier;
import com.warriorssmp.woodcutting.model.PointsUtil;
import com.warriorssmp.woodcutting.model.ResourceDef;
import com.warriorssmp.woodcutting.model.XpTable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TaskService {

    private final WoodcuttingPlugin plugin;
    private final GatherConfig config;
    private final DataStore dataStore;
    private final EconomyService economy;
    private final PremiumService premium;
    private final Random random = new Random();

    public TaskService(WoodcuttingPlugin plugin, GatherConfig config, DataStore dataStore, EconomyService economy, PremiumService premium) {
        this.plugin = plugin;
        this.config = config;
        this.dataStore = dataStore;
        this.economy = economy;
        this.premium = premium;
    }

    public int levelOf(PlayerGatherData data) {
        return XpTable.levelForXp(data.totalXp);
    }

    /**
     * Picks the tier a new task should be generated from: usually the player's
     * current tier, but with a chance (per the design doc's level-banded table)
     * of rolling one tier higher — boosted further if "Better Tasks" is active.
     * Tier 7 has no normal resource pool (Legendary Requests cover it instead),
     * so this never rolls into or lands on an empty-resource tier.
     */
    public GatherTier rollTaskTier(PlayerGatherData data, Player player) {
        int level = levelOf(data);
        GatherTier currentTier = config.tierForLevel(level);
        if (currentTier.resources().isEmpty()) {
            GatherTier fallback = config.tier(currentTier.number() - 1);
            currentTier = fallback != null ? fallback : currentTier;
        }

        double chance = XpTable.higherTierChance(level, currentTier.number(), currentTier.minLevel());
        if (data.betterTasksExpiry > System.currentTimeMillis()) {
            chance += 0.05;
        }

        GatherTier nextTier = config.tier(currentTier.number() + 1);
        if (nextTier == null || nextTier.resources().isEmpty()) return currentTier;

        if (nextTier.premium() && !premium.isPremium(player)) {
            return currentTier;
        }

        return random.nextDouble() < chance ? nextTier : currentTier;
    }

    /** Generates a brand new task for the player and sets it as their active task. */
    public GatherTask generateTask(Player player, PlayerGatherData data) {
        GatherTier tier = rollTaskTier(data, player);
        List<ResourceDef> pool = new ArrayList<>();
        for (ResourceDef def : tier.resources()) {
            if (!data.blockedResources.contains(def.material())) {
                pool.add(def);
            }
        }
        if (pool.isEmpty()) {
            pool.addAll(tier.resources()); // fall back if everything in the tier is blocked
        }

        ResourceDef chosen = pool.get(random.nextInt(pool.size()));
        int amount = chosen.rollAmount(random);
        GatherTask task = new GatherTask(chosen.material(), tier.number(), amount, 0);
        data.activeTask = task;
        return task;
    }

    /** Ensures the player has an active task, generating one if needed. */
    public GatherTask ensureTask(Player player, PlayerGatherData data) {
        if (data.activeTask == null) {
            return generateTask(player, data);
        }
        return data.activeTask;
    }

    public boolean skipTask(Player player, PlayerGatherData data) {
        long cost = Math.round(config.skipCost());
        if (data.points < cost) {
            player.sendMessage("§cYou need " + PointsUtil.format(cost) + " to skip your task.");
            return false;
        }
        data.points -= cost;
        if (data.activeTask != null) {
            data.history.add(new PlayerGatherData.TaskHistoryEntry(
                    data.activeTask.displayName(), 0, 0, -cost, true, System.currentTimeMillis()));
        }
        generateTask(player, data);
        return true;
    }

    /** Call whenever a tracked block is broken; grants small passive XP on every
     *  qualifying break (not just task turn-in), updates task progress if the
     *  material matches the active task, and shows one combined action bar. */
    public void addProgress(Player player, PlayerGatherData data, org.bukkit.Material material, int amount) {
        int tier = config.tierOfMaterial(material);
        long xpGained = tier >= 1 ? grantPassiveXp(player, data, tier, amount) : 0;

        GatherTask task = data.activeTask;
        boolean taskMatched = task != null && task.material() == material;
        if (taskMatched) {
            task.addProgress(amount);
            data.lifetimeResourcesGathered += amount;
            if (task.isComplete()) {
                completeTask(player, data);
                return; // completeTask already sends its own message; skip the passive bar this swing
            }
        }

        showXpActionBar(player, data, xpGained, taskMatched ? task : null);
    }

    /** Small XP-per-block table (separate from the much larger task-completion
     *  XP) — scales with tier so higher-tier resources feel worth more even
     *  outside of finishing a task. */
    private static final int[] XP_PER_BLOCK_TIER = {0, 1, 2, 3, 4, 5, 6, 8};

    private long grantPassiveXp(Player player, PlayerGatherData data, int tierNumber, int amount) {
        long perBlock = tierNumber >= 0 && tierNumber < XP_PER_BLOCK_TIER.length ? XP_PER_BLOCK_TIER[tierNumber] : 1;
        long xpGain = perBlock * amount;
        if (data.xpBoostExpiry > System.currentTimeMillis()) {
            xpGain = Math.round(xpGain * 1.10);
        }

        int levelBefore = levelOf(data);
        data.totalXp += xpGain;
        int levelAfter = levelOf(data);
        if (levelAfter > levelBefore) {
            announceLevelUp(player, levelAfter);
        }
        return xpGain;
    }

    /** The "how much XP did I just get, and how much more to level up" popup —
     *  fires on every qualifying block break, not just task turn-in. Folds the
     *  task progress bar into the same line when a task also updated, since an
     *  action bar can only show one message at a time. */
    private void showXpActionBar(Player player, PlayerGatherData data, long xpGained, GatherTask taskForDisplay) {
        int level = levelOf(data);
        long xpIntoLevel = data.totalXp - XpTable.xpForLevel(level);
        long xpForNextLevel = XpTable.xpForNextLevel(level) - XpTable.xpForLevel(level);
        long xpRemaining = Math.max(0, xpForNextLevel - xpIntoLevel);

        int barLength = 15;
        double fraction = xpForNextLevel > 0 ? (double) xpIntoLevel / xpForNextLevel : 1.0;
        int filled = (int) Math.round(Math.max(0, Math.min(1, fraction)) * barLength);
        String xpBar = "§b" + "█".repeat(filled) + "§7" + "░".repeat(barLength - filled);

        String xpPart = level >= XpTable.MAX_LEVEL
                ? "§b+" + xpGained + " XP §7(MAX LEVEL)"
                : "§b+" + xpGained + " XP §7(" + xpRemaining + " to next level) " + xpBar;

        if (taskForDisplay != null) {
            int taskBarLength = 15;
            double taskFraction = (double) taskForDisplay.progress() / taskForDisplay.required();
            int taskFilled = (int) Math.round(Math.max(0, Math.min(1, taskFraction)) * taskBarLength);
            String taskBar = "§a" + "█".repeat(taskFilled) + "§7" + "░".repeat(taskBarLength - taskFilled);
            player.sendActionBar(Component.text("§e" + taskForDisplay.displayName() + " §7"
                    + taskForDisplay.progress() + "/" + taskForDisplay.required() + " " + taskBar + "  §f| " + xpPart));
        } else {
            player.sendActionBar(Component.text(xpPart));
        }
    }

    private void completeTask(Player player, PlayerGatherData data) {
        GatherTask task = data.activeTask;
        if (task == null) return;

        GatherTier tier = config.tier(task.tier());
        int baseCoins = tier != null ? tier.baseCoins() : 1;

        data.streak++;
        double streakMult = config.streakMultiplier(data.streak);

        // XP intentionally does NOT scale with the tier's points yield — leveling
        // pace stays tied to tier number alone, independent of any future points
        // rebalancing. (40/80/160/280/480/800/2400 XP for tiers 1-7.)
        double xpGain = xpForTier(tier != null ? tier.number() : task.tier());
        double pointsGain = baseCoins * (1 + streakMult);

        if (data.xpBoostExpiry > System.currentTimeMillis()) {
            xpGain *= 1.10;
        }
        if (data.pointBoostExpiry > System.currentTimeMillis()) {
            pointsGain *= 1.10;
        }

        int levelBefore = levelOf(data);
        data.totalXp += Math.round(xpGain);
        int levelAfter = levelOf(data);

        long pointsRounded = Math.round(pointsGain);
        data.points += pointsRounded;
        data.lastTaskCompletedAt = System.currentTimeMillis();
        data.lifetimeTasksCompleted++;

        data.history.add(new PlayerGatherData.TaskHistoryEntry(
                task.displayName(), task.required(), Math.round(xpGain), pointsRounded, false, System.currentTimeMillis()));

        player.sendMessage("§a§lTASK COMPLETE §7— §f" + task.displayName()
                + " §7(+" + Math.round(xpGain) + " XP, +" + PointsUtil.format(pointsRounded) + ")");

        if (levelAfter > levelBefore) {
            announceLevelUp(player, levelAfter);
        }

        double newMult = config.streakMultiplier(data.streak);
        if (newMult > streakMult) {
            player.sendMessage("§6§lSTREAK MILESTONE! §7Streak " + data.streak + " — +" + (int) (newMult * 100) + "% Points");
        }

        data.activeTask = null;
        generateTask(player, data);
    }

    /** Level-up notification — a title popup plus sound, separate from the plain
     *  chat message task completion already sends, so it stands out on its own. */
    private void announceLevelUp(Player player, int newLevel) {
        GatherTier tier = config.tierForLevel(newLevel);
        player.showTitle(Title.title(
                Component.text("§6§lLEVEL UP!"),
                Component.text("§eWoodcutter Level " + newLevel + " §7— " + tier.display()),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(500))
        ));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        player.sendMessage("§6§lLEVEL UP! §7You are now Woodcutter Level §f" + newLevel + " §7(" + tier.display() + "§7)");
    }

    private static final int[] XP_PER_TIER = {0, 40, 80, 160, 280, 480, 800, 2400};

    private double xpForTier(int tierNumber) {
        if (tierNumber < 1 || tierNumber >= XP_PER_TIER.length) return 40;
        return XP_PER_TIER[tierNumber];
    }

    public GatherConfig config() {
        return config;
    }
}
