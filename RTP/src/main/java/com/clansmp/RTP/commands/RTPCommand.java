package com.clansmp.RTP.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.clansmp.RTP.RandomTeleportGP; // <-- ADD THIS IMPORT
import com.clansmp.RTP.managers.RTPManager;

public class RTPCommand implements CommandExecutor {

    private final RandomTeleportGP plugin;
    private final RTPManager rtpManager;

    public RTPCommand(RandomTeleportGP plugin, RTPManager rtpManager) { 
        this.plugin = plugin; 
        this.rtpManager = rtpManager;
    }

    @SuppressWarnings("deprecation")
	@Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle /reloadrtp command
        if (label.equalsIgnoreCase("reloadrtp")) { // Check for the command name first
            if (!sender.hasPermission("rtp.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to reload the RTP plugin.");
                return true;
            }
            this.plugin.reloadPluginConfig(); // <-- Use 'this.plugin' to call the method
            sender.sendMessage(ChatColor.GREEN + "SecureRTP configuration reloaded.");
            return true;
        }

        // Handle /rtp command
        if (label.equalsIgnoreCase("rtp")) { // Check for the command name
            if (!sender.hasPermission("rtp.use")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use the RTP command.");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;
            rtpManager.teleportPlayerRandomly(player);
            return true;
        }

        return false; // This line should ideally not be reached if the command is correctly registered
    }
}
