package com.warriorssmp.woodcutting.task;

import com.warriorssmp.woodcutting.WoodcuttingPlugin;
import com.warriorssmp.woodcutting.data.DataStore;
import com.warriorssmp.woodcutting.data.PlayerGatherData;
import com.warriorssmp.woodcutting.economy.EconomyService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Handles the Master Woodcutter NPC's location and its teleport perk. Home
 * teleports were removed from this system entirely per the redesign — the
 * Master Woodcutter teleport (purchased from the shop) is the only teleport
 * that remains.
 */
public final class MasterNpcService {

    private final WoodcuttingPlugin plugin;
    private final GatherConfig config;
    private final DataStore dataStore;
    private final EconomyService economy;

    private Location masterLocation;

    public MasterNpcService(WoodcuttingPlugin plugin, GatherConfig config, DataStore dataStore, EconomyService economy) {
        this.plugin = plugin;
        this.config = config;
        this.dataStore = dataStore;
        this.economy = economy;
    }

    public void setMasterLocation(Location loc) {
        this.masterLocation = loc.clone();
    }

    public Location masterLocation() {
        return masterLocation;
    }

    public boolean teleportToMaster(Player player, PlayerGatherData data) {
        if (!data.masterTeleportUnlocked) {
            player.sendMessage("§cUnlock the Master Woodcutter teleport from the Woodcutter Shop first.");
            return false;
        }
        if (masterLocation == null) {
            player.sendMessage("§cThe Master Woodcutter hasn't been placed yet — ask an admin to run /woodmaster set.");
            return false;
        }
        long cooldownMs = config.masterTeleportCooldownMinutes() * 60_000L;
        long remaining = (data.lastMasterTeleport + cooldownMs) - System.currentTimeMillis();
        if (remaining > 0) {
            player.sendMessage("§cMaster Woodcutter teleport is on cooldown for " + formatDuration(remaining) + ".");
            return false;
        }
        data.lastMasterTeleport = System.currentTimeMillis();
        player.teleport(masterLocation);
        player.sendMessage("§aTeleported to the Master Woodcutter.");
        return true;
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + "m " + seconds + "s";
    }
}
