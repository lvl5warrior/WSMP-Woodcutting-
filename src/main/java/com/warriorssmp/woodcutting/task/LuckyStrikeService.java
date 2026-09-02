package com.warriorssmp.woodcutting.task;

import com.warriorssmp.woodcutting.WoodcuttingPlugin;
import com.warriorssmp.woodcutting.data.DataStore;
import com.warriorssmp.woodcutting.data.PlayerGatherData;
import com.warriorssmp.woodcutting.economy.EconomyService;
import com.warriorssmp.woodcutting.model.IconUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Map;
import java.util.Random;

public final class LuckyStrikeService {

    private final WoodcuttingPlugin plugin;
    private final GatherConfig config;
    private final DataStore dataStore;
    private final EconomyService economy;
    private final Random random = new Random();

    public LuckyStrikeService(WoodcuttingPlugin plugin, GatherConfig config, DataStore dataStore, EconomyService economy) {
        this.plugin = plugin;
        this.config = config;
        this.dataStore = dataStore;
        this.economy = economy;
    }

    /**
     * Rolls Lucky Strike for a tracked resource break. Returns true if it triggered.
     * This fires independently of the player's active task — every relevant block
     * break gets a shot, which is what makes ordinary gathering feel like it has
     * variance instead of being pure repetition. On trigger, it doubles the resource
     * you just gathered (gives you one extra of the same item) rather than a flat
     * coin payout.
     */
    public boolean roll(Player player, PlayerGatherData data, Material material, int tierOfMaterial) {
        double chance = config.luckyStrikeChance(tierOfMaterial);

        if (data.luckyStrikeBoostExpiry > System.currentTimeMillis()) {
            chance += 0.05;
        }

        if (random.nextDouble() >= chance) {
            return false;
        }

        data.lifetimeLuckyStrikes++;

        ItemStack bonus = new ItemStack(IconUtil.safeIcon(material), 1);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(bonus);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        String resourceName = IconUtil.safeIcon(material).name().toLowerCase().replace('_', ' ');

        player.sendActionBar(Component.text("§e⭐ LUCKY STRIKE! §7x2 " + resourceName));
        player.showTitle(Title.title(
                Component.text("§e⭐ LUCKY STRIKE!"),
                Component.text("§7x2 " + resourceName),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1200), Duration.ofMillis(300))
        ));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);

        return true;
    }
}
