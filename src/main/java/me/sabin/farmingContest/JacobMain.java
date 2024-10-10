package me.sabin.farmingContest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JacobMain extends JavaPlugin {
    public HashMap<UUID, Integer> contestScores = new HashMap<>();
    public boolean contestActive = false;

    @Override
    public void onEnable() {
        new JacobCommands(this);
        new JacobListener(this);
    }
    String none = ChatColor.WHITE + "None";
    String bronze = ChatColor.RED + "BRONZE";
    String silver = ChatColor.GRAY + "SILVER";
    String gold = ChatColor.YELLOW + "GOLD";
    String platinum = ChatColor.AQUA + "PLATINUM";
    String diamond = ChatColor.BLUE + "DIAMOND";

    public String getBracket(UUID playerUUID) {
        int score = contestScores.getOrDefault(playerUUID, 0);

        // Players with a score below 100 won't get a bracket or scoreboard.
        if (score < 100) return "None";

        List<Integer> sortedScores = contestScores.values().stream()
                .filter(s -> s >= 100) // Include only scores above 100.
                .sorted((a, b) -> Integer.compare(b, a)) // Sort in descending order.
                .collect(Collectors.toList());

        int totalPlayers = sortedScores.size();
        int playerRank = sortedScores.indexOf(score) + 1; // 1-based rank.

        // Calculate percentile
        double percentile = (double) playerRank / totalPlayers;

        if (percentile <= 0.02) return "Diamond";
        if (percentile <= 0.05) return "Platinum";
        if (percentile <= 0.10) return "Gold";
        if (percentile <= 0.30) return "Silver";
        if (percentile <= 0.60) return "Bronze";
        return "None"; // Fallback, but should not be hit.
    }

    public void updateScoreboard(Player player) {
        UUID playerUUID = player.getUniqueId();
        int score = contestScores.getOrDefault(playerUUID, 0);

        // Only show scoreboard if score is above 100
        if (score <= 100) {
            return; // Do not display anything for players with score 100 or below
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        // Create objective to track scores
        Objective objective = board.registerNewObjective("contestScore", "dummy", ChatColor.YELLOW + "Jacob's Contest");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Empty line
        objective.getScore(ChatColor.RESET.toString()).setScore(4);

        // Contest type (assuming potato event)
        objective.getScore(ChatColor.WHITE + "Potato").setScore(3);

        // Time left (dummy data)
        objective.getScore(ChatColor.GREEN + "Time: 05:00").setScore(2);

        // Empty line
        objective.getScore(ChatColor.RESET.toString()).setScore(1);

        String playerBracket = getBracket(playerUUID);

        if (playerBracket.equals("None")) {
            objective.getScore(ChatColor.WHITE + "Collected: " + ChatColor.YELLOW + score).setScore(0);
        }

        if (!playerBracket.equals("None")) {
            // Show player's bracket if they have one
            objective.getScore(playerBracket + " with " + score).setScore(0);

            // Get next lowest score for the next bracket
            int nextLowestScore = getNextLowestScore(playerUUID);
            if (nextLowestScore > 0) {
                String nextBracket = getBracketName(nextLowestScore);
                objective.getScore(ChatColor.WHITE + nextBracket + " has " + ChatColor.YELLOW + (nextLowestScore - score)).setScore(-1);
            }
        }

        player.setScoreboard(board);
    }


    private int getNextLowestScore(UUID playerUUID) {
        String playerBracket = getBracket(playerUUID);
        List<Integer> sortedScores = contestScores.values().stream()
                .filter(s -> s >= 100) // Only consider players with scores >= 100.
                .sorted()
                .collect(Collectors.toList());

        // Check for the next lowest score in the higher bracket.
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
                return -1; // No higher bracket.
            default:
                return -1;
        }
    }
    private int getBracketPercentileScore(double percentile, List<Integer> sortedScores) {
        int totalPlayers = sortedScores.size();
        if (totalPlayers == 0) return -1; // No players to compare.

        int index = (int) Math.ceil(percentile * totalPlayers) - 1; // Percentile index.
        if (index < 0 || index >= sortedScores.size()) return -1;

        return sortedScores.get(index);
    }

    private int getLowestScoreInBracket(String bracket, List<Integer> scores) {
        int minScoreInBracket = -1;

        switch (bracket) {
            case "bronze":
                minScoreInBracket = getBracketPercentileScore(0.60, scores);
                break;
            case "silver":
                minScoreInBracket = getBracketPercentileScore(0.30, scores);
                break;
            case "gold":
                minScoreInBracket = getBracketPercentileScore(0.10, scores);
                break;
            case "platinum":
                minScoreInBracket = getBracketPercentileScore(0.05, scores);
                break;
            case "diamond":
                minScoreInBracket = getBracketPercentileScore(0.02, scores);
                break;
        }

        return minScoreInBracket;
    }

    private String getBracketName(int score) {
        List<Integer> sortedScores = contestScores.values().stream()
                .filter(s -> s > 100)
                .sorted((a, b) -> Integer.compare(b, a))
                .collect(Collectors.toList());

        int totalPlayers = sortedScores.size();
        int index = sortedScores.indexOf(score);

        if (index == -1) return "None"; // Score not found

        double percentage = (double) (index + 1) / totalPlayers;

        if (percentage <= 0.02) return diamond;
        if (percentage <= 0.05) return platinum;
        if (percentage <= 0.10) return gold;
        if (percentage <= 0.30) return silver;
        if (percentage <= 0.60) return bronze;
        return none;
    }
}
