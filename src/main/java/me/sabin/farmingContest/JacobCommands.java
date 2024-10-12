package me.sabin.farmingContest;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;

public class JacobCommands implements CommandExecutor {
    private final JacobMain plugin;

    public JacobCommands(JacobMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            switch (label.toLowerCase()) {
                case "startcontest":
                    plugin.startContest();
                    player.sendMessage("You started a new contest.");
                    return true;

                case "endcontest":
                    plugin.endContest();
                    player.sendMessage("You ended the current contest.");
                    return true;

                case "checkscore":
                    int score = plugin.contestScores.getOrDefault(player.getUniqueId(), 0);
                    player.sendMessage(ChatColor.YELLOW + "Your current score is: " + ChatColor.GOLD + score);
                    return true;

                default:
                    return false;
            }
        }
        return false;
    }
}
