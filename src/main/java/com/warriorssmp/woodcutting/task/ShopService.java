package com.warriorssmp.woodcutting.task;

import com.warriorssmp.woodcutting.data.DataStore;
import com.warriorssmp.woodcutting.data.PlayerGatherData;
import com.warriorssmp.woodcutting.economy.EconomyService;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ShopService {

    private final GatherConfig config;
    private final DataStore dataStore;
    private final EconomyService economy;
    private final PremiumService premium;

    public ShopService(GatherConfig config, DataStore dataStore, EconomyService economy, PremiumService premium) {
        this.config = config;
        this.dataStore = dataStore;
        this.economy = economy;
        this.premium = premium;
    }

    public enum Result {SUCCESS, NO_PERMISSION, ALREADY_OWNED, INSUFFICIENT_FUNDS, UNKNOWN_ITEM}

    public Result purchase(Player player, PlayerGatherData data, String itemId) {
        GatherConfig.ShopItem item = config.shopItem(itemId);
        if (item == null) return Result.UNKNOWN_ITEM;

        if (item.premium() && !premium.isPremium(player)) {
            return Result.NO_PERMISSION;
        }

        int key = itemId.hashCode();
        if (item.limit() > 0 && data.purchasedOneTimeItems.contains(key)) {
            return Result.ALREADY_OWNED;
        }

        long cost = Math.round(item.cost());
        if (data.points < cost) {
            return Result.INSUFFICIENT_FUNDS;
        }
        data.points -= cost;

        applyEffect(player, data, itemId);

        if (item.limit() > 0) {
            data.purchasedOneTimeItems.add(key);
        }

        return Result.SUCCESS;
    }

    private void applyEffect(Player player, PlayerGatherData data, String itemId) {
        long now = System.currentTimeMillis();
        switch (itemId) {
            case "task_skip" -> {} // handled by TaskService.skipTask directly, not routed through here
            case "xp_boost" -> data.xpBoostExpiry = Math.max(data.xpBoostExpiry, now) + 3_600_000L;
            case "point_boost" -> data.pointBoostExpiry = Math.max(data.pointBoostExpiry, now) + 3_600_000L;
            case "better_tasks" -> data.betterTasksExpiry = Math.max(data.betterTasksExpiry, now) + 3_600_000L;
            case "master_teleport" -> data.masterTeleportUnlocked = true;
            case "master_axe" -> giveMasterTool(player, org.bukkit.Material.NETHERITE_AXE, "Master Woodcutter Axe", "Woodcutting");
            default -> {
                // Consumables (potions, enchanted books, etc.) — give the physical item.
                GatherConfig.ShopItem item = config.shopItem(itemId);
                if (item != null) {
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(
                            com.warriorssmp.woodcutting.model.IconUtil.safeIcon(item.material())));
                }
            }
        }
    }

    /** There's no real "Ancient Tools" tier in vanilla Minecraft — Tier 7's tool
     *  requirement is this enchanted Netherite Axe, sold only here at the
     *  Woodcutter Shop so it's identifiable and can't be substituted with a plain
     *  enchanted axe from elsewhere. */
    private void giveMasterTool(Player player, org.bukkit.Material material, String name, String path) {
        ItemStack tool = new ItemStack(material);
        ItemMeta meta = tool.getItemMeta();
        meta.displayName(Component.text("§d§l" + name));
        meta.lore(List.of(
                Component.text("§7Tier 7 tool requirement — " + path),
                Component.text("§7Only available from the Woodcutter Shop")
        ));
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.FORTUNE, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        tool.setItemMeta(meta);
        player.getInventory().addItem(tool);
    }
}
