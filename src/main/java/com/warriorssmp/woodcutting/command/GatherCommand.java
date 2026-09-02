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
            case "woodshop" -> plugin.menuManager().openShop(player, 1);
            default -> {return false;}
        }
        return true;
    }
}
