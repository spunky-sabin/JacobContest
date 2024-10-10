package me.sabin.farmingContest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class JacobCommands implements CommandExecutor {
    private final JacobMain plugin;

    public JacobCommands(JacobMain plugin) {
        this.plugin = plugin;
        plugin.getCommand("startcontest").setExecutor(this);
        plugin.getCommand("endcontest").setExecutor(this);
        plugin.getCommand("checkscore").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID playerUUID = player.getUniqueId();

            switch (label.toLowerCase()) {
                case "startcontest":
                    startNewContest();
                    player.sendMessage("You started a new contest.");
                    return true;

                case "endcontest":
                    endContest();
                    player.sendMessage("You ended the current contest.");
                    return true;

                case "checkscore":
                    checkScore(playerUUID, player);
                    return true;

                default:
                    return false;
            }
        }
        return false;
    }

    private void startNewContest() {
        plugin.contestActive = true;
        plugin.contestScores.clear();
        Bukkit.broadcastMessage("Jacob's Farming Contest has started!");
        Bukkit.getLogger().info("Contest started.");
    }

    private void endContest() {
        plugin.contestActive = false;
        Bukkit.broadcastMessage("Jacob's Farming Contest has ended!");
        Bukkit.getLogger().info("Contest ended.");
        // Additional logic for ranking and rewards can be added here
    }

    private void checkScore(UUID playerUUID, Player player) {
        int score = plugin.contestScores.getOrDefault(playerUUID, 0);
        player.sendMessage("Your current score: " + score);
    }
}
