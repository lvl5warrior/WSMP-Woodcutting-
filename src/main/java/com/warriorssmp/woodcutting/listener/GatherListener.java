package com.warriorssmp.woodcutting.listener;

import com.warriorssmp.woodcutting.WoodcuttingPlugin;
import com.warriorssmp.woodcutting.data.PlayerGatherData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;

public final class GatherListener implements Listener {

    private final WoodcuttingPlugin plugin;
    /** Locations of player-placed blocks, so breaking your own placed blocks doesn't
     *  farm task progress or Lucky Strike rolls (the "no cheesy afk farms" requirement). */
    private final Set<String> placedBlocks = new HashSet<>();

    public GatherListener(WoodcuttingPlugin plugin) {
        this.plugin = plugin;
    }

    private String key(org.bukkit.block.Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.getConfig().getBoolean("settings.block-cheesy-farms", true)) {
            placedBlocks.add(key(event.getBlock()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        boolean wasPlaced = placedBlocks.remove(key(event.getBlock()));
        if (wasPlaced) return; // anti-cheese: self-placed blocks don't count

        PlayerGatherData data = plugin.dataStore().get(player.getUniqueId());

        // Legendary Requests (Tier 7) track independently of the normal tier pool —
        // e.g. Chorus Flower (Void Orchard) isn't in any tier's resource list at all.
        plugin.legendaryRequestService().addProgress(player, data, material, 1);

        int tier = plugin.gatherConfig().tierOfMaterial(material);
        if (tier == -1) return; // not part of the normal Tier 1-6 pool

        // Lucky Strike rolls on every relevant break, independent of the active task.
        plugin.luckyStrikeService().roll(player, data, material, tier);
        plugin.taskService().addProgress(player, data, material, 1);
    }

    /**
     * Kept as a general hook in case a future resource ever comes from a mob drop —
     * every current Tier 1-6 resource and Legendary Request is a block, but this
     * routes the same way as onBreak so it's a one-line change to add one later.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        PlayerGatherData data = plugin.dataStore().get(killer.getUniqueId());
        for (var drop : event.getDrops()) {
            plugin.legendaryRequestService().addProgress(killer, data, drop.getType(), drop.getAmount());
            int tier = plugin.gatherConfig().tierOfMaterial(drop.getType());
            if (tier == -1) continue;
            plugin.luckyStrikeService().roll(killer, data, drop.getType(), tier);
            plugin.taskService().addProgress(killer, data, drop.getType(), drop.getAmount());
        }
    }

    /**
     * Some Tier 5-7 resources are "processed" (e.g. Netherite Scrap, smelted from
     * Ancient Debris) — they never fire BlockBreakEvent, only this, when the player
     * takes the smelted result out of a furnace.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material material = event.getItemType();
        int tier = plugin.gatherConfig().tierOfMaterial(material);
        if (tier == -1) return;

        PlayerGatherData data = plugin.dataStore().get(player.getUniqueId());
        plugin.luckyStrikeService().roll(player, data, material, tier);
        plugin.taskService().addProgress(player, data, material, event.getItemAmount());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Pre-warm the cache so menus open instantly.
        plugin.dataStore().get(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.dataStore().unload(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractMasterNpc(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        NamespacedKey npcKey = new NamespacedKey(plugin, "master_woodcutter_npc");
        if (Boolean.TRUE.equals(entity.getPersistentDataContainer().get(npcKey, PersistentDataType.BOOLEAN))) {
            event.setCancelled(true);
            plugin.menuManager().openMasterMenu(event.getPlayer());
        }
    }
}
