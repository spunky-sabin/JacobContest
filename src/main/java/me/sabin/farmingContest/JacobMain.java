package me.sabin.farmingContest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JacobMain extends JavaPlugin implements Listener, CommandExecutor {
    private final HashMap<UUID, Integer> contestScores = new HashMap<>();
    private boolean contestActive = false;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("startcontest").setExecutor(this);
        getCommand("endcontest").setExecutor(this);
        getCommand("checkscore").setExecutor(this);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (contestActive && event.getBlock().getType() == Material.POTATOES) {
            Player player = event.getPlayer();

            // Get the drops from the block after it has been broken
            Collection<ItemStack> drops = event.getBlock().getDrops();
            int totalPotatoes = 0;

            for (ItemStack drop : drops) {
                if (drop.getType() == Material.POTATO) {
                    totalPotatoes += drop.getAmount();
                }
            }

            // Update player's score
            UUID playerUUID = player.getUniqueId();
            contestScores.put(playerUUID, contestScores.getOrDefault(playerUUID, 0) + totalPotatoes);

            // Notify the player
            player.sendMessage("You broke a potato block and received " + totalPotatoes + " potatoes!");

            // Update scoreboard after breaking the block
            updateScoreboard(player);
        }
    }

    private void updateScoreboard(Player player) {
        UUID playerUUID = player.getUniqueId();
        int score = contestScores.getOrDefault(playerUUID, 0);
        if (score > 100) {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            Scoreboard board = manager.getNewScoreboard();

            // Create objective to track scores
            Objective objective = board.registerNewObjective("contestScore", "dummy", ChatColor.YELLOW + "Jacob's Contest");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            // Empty line
            objective.getScore(ChatColor.RESET.toString()).setScore(4);

            // Contest type
            objective.getScore(ChatColor.WHITE + "Potato").setScore(3);

            // Time left (fixed for this example)
            objective.getScore(ChatColor.GREEN + "Time: 05:00").setScore(2);

            // Empty line
            objective.getScore(ChatColor.RESET.toString()).setScore(1);

            // Collected score
            if (getBracket(playerUUID).equals("None")) {
                objective.getScore(ChatColor.WHITE + "Collected: " + ChatColor.YELLOW + score).setScore(0);
            } else {
                String playerBracket = getBracket(playerUUID);
                int nextLowestScore = getNextLowestScore(playerUUID);
                objective.getScore(ChatColor.WHITE + "Rank: " + playerBracket + " with " + score).setScore(0);
                if (nextLowestScore != -1) {
                    objective.getScore(ChatColor.WHITE + getBracketName(nextLowestScore) + " has next lowest: " + ChatColor.YELLOW + nextLowestScore).setScore(-1);
                }
            }

            // Assign the scoreboard to the player
            player.setScoreboard(board);
        }
    }

    private String getBracket(UUID playerUUID) {
        int score = contestScores.getOrDefault(playerUUID, 0);
        List<Integer> sortedScores = contestScores.values().stream()
                .filter(s -> s > 100) // Exclude players with less than 100 score
                .sorted((a, b) -> Integer.compare(b, a)) // Sort in descending order
                .collect(Collectors.toList());

        int totalPlayers = sortedScores.size();
        int index = sortedScores.indexOf(score);

        // Determine the bracket based on the player's position
        if (index == -1 || score < 100) return "None"; // Not qualified for brackets
        double percentage = (double) (index + 1) / totalPlayers; // Rank is index + 1 in 0-based index

        if (percentage <= 0.02) return "Diamond";
        if (percentage <= 0.05) return "Platinum";
        if (percentage <= 0.10) return "Gold";
        if (percentage <= 0.30) return "Silver";
        if (percentage <= 0.60) return "Bronze";
        return "None"; // This should not happen
    }

    private int getNextLowestScore(UUID playerUUID) {
        String playerBracket = getBracket(playerUUID);
        List<Integer> sortedScores = contestScores.values().stream()
                .filter(s -> s > 100) // Exclude players with less than 100 score
                .sorted()
                .collect(Collectors.toList());

        // Check which bracket to return the next lowest score
        switch (playerBracket) {
            case "None":
                return getLowestScoreInBracket("Bronze", sortedScores);
            case "Bronze":
                return getLowestScoreInBracket("Silver", sortedScores);
            case "Silver":
                return getLowestScoreInBracket("Gold", sortedScores);
            case "Gold":
                return getLowestScoreInBracket("Platinum", sortedScores);
            case "Platinum":
                return getLowestScoreInBracket("Diamond", sortedScores);
            case "Diamond":
                return -1; // No next bracket
            default:
                return -1;
        }
    }

    private int getLowestScoreInBracket(String bracket, List<Integer> scores) {
        switch (bracket) {
            case "Bronze":
                return scores.stream().filter(s -> s < 300).findFirst().orElse(-1);
            case "Silver":
                return scores.stream().filter(s -> s < 600).findFirst().orElse(-1);
            case "Gold":
                return scores.stream().filter(s -> s < 900).findFirst().orElse(-1);
            case "Platinum":
                return scores.stream().filter(s -> s < 1200).findFirst().orElse(-1);
            case "Diamond":
                return scores.stream().filter(s -> s >= 1200).findFirst().orElse(-1);
            default:
                return -1;
        }
    }

    private String getBracketName(int score) {
        if (score < 100) return "None";
        if (score < 300) return "Bronze";
        if (score < 600) return "Silver";
        if (score < 900) return "Gold";
        if (score < 1200) return "Platinum";
        return "Diamond"; // for scores 1200 and above
    }

    // Command handling logic
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

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
                    UUID playerUUID = player.getUniqueId();
                    int score = contestScores.getOrDefault(playerUUID, 0);
                    player.sendMessage("Your current score: " + score);
                    return true;

                default:
                    return false;
            }
        }
        return false;
    }

    private void startNewContest() {
        contestActive = true;
        contestScores.clear();
        Bukkit.broadcastMessage("Jacob's Farming Contest has started!");
        Bukkit.getLogger().info("Contest started.");
    }

    private void endContest() {
        contestActive = false;
        Bukkit.broadcastMessage("Jacob's Farming Contest has ended!");
        Bukkit.getLogger().info("Contest ended.");
        // Additional logic for ranking and rewards can be added here
    }
}
