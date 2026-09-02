package com.warriorssmp.woodcutting.command;

import com.warriorssmp.woodcutting.WoodcuttingPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GatherCommand implements CommandExecutor {

    private final WoodcuttingPlugin plugin;

    public GatherCommand(WoodcuttingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "woodmenu" -> plugin.menuManager().openMainMenu(player);
            case "woodtask" -> plugin.menuManager().openTaskDetails(player);
            case "woodleaderboards" -> plugin.menuManager().openLeaderboardHub(player);
            case "woodbuffs" -> plugin.menuManager().openBuffs(player);
            case "woodshop" -> {
                if (!player.hasPermission("woodcutting.admin") && !isNearWoodcutter(player)) {
                    var loc = plugin.masterNpcService().masterLocation();
                    if (loc == null) {
                        player.sendMessage("§cThe Master Woodcutter hasn't been placed yet — ask an admin.");
                    } else {
                        player.sendMessage("§cYou need to be near the Master Woodcutter to open the shop.");
                    }
                    return true;
                }
                plugin.menuManager().openShop(player, 1);
            }
            default -> {return false;}
        }
        return true;
    }

    /** /woodshop is proximity-gated to the Master Woodcutter NPC, matching the
     *  Fishing/Cooking plugins. Admins (woodcutting.admin, default: op) bypass
     *  this so the shop can be tested/edited without needing an NPC placed first. */
    private boolean isNearWoodcutter(Player player) {
        var loc = plugin.masterNpcService().masterLocation();
        if (loc == null) return false;
        if (loc.getWorld() == null || !loc.getWorld().equals(player.getWorld())) return false;
        double radius = plugin.getConfig().getDouble("settings.npc-shop-radius", 5.0);
        return loc.distanceSquared(player.getLocation()) <= radius * radius;
    }
}
