package me.sabin.farmingContest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.stream.Collectors;

public class JacobMain extends JavaPlugin {
    public final HashMap<UUID, Integer> contestScores = new HashMap<>();
    public boolean contestActive = false;
    private JacobTimer timer; // Timer instance

    private int lowestDiamondScore;
    private int lowestPlatinumScore;
    private int lowestGoldScore;
    private int lowestSilverScore;
    private int lowestBronzeScore;

    String none = ChatColor.WHITE + "None";
    String bronze = ChatColor.RED + "BRONZE";
    String silver = ChatColor.GRAY + "SILVER";
    String gold = ChatColor.YELLOW + "GOLD";
    String platinum = ChatColor.AQUA + "PLATINUM";
    String diamond = ChatColor.BLUE + "DIAMOND";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new JacobListener(this), this);

        JacobCommands commands = new JacobCommands(this);
        Objects.requireNonNull(getCommand("startcontest")).setExecutor(commands);
        Objects.requireNonNull(getCommand("endcontest")).setExecutor(commands);
        Objects.requireNonNull(getCommand("checkscore")).setExecutor(commands);

        // Log to confirm registration
        getLogger().info("Commands registered successfully.");
    }

    // Method to start the contest and schedule recalculation every 20 ticks
    public void startContest() {
        contestActive = true;
        contestScores.clear();
        Bukkit.broadcastMessage("Jacob's Farming Contest has started!");

        // Start the timer for 5 minutes
        timer = new JacobTimer(this);
        timer.runTaskTimer(this, 0, 20); // Runs every second

        new BukkitRunnable() {
            @Override
            public void run() {
                if (contestActive) {
                    recalculateBrackets(); // Recalculate brackets every 20 ticks
                    updateAllScoreboards(); // Update all scoreboards
                } else {
                    cancel(); // Stop the task when contest ends
                }
            }
        }.runTaskTimer(this, 0, 20); // Runs every 20 ticks
    }


    public void endContest() {
        contestActive = false;
        timer.cancel(); // Stop the timer when contest ends
        Bukkit.broadcastMessage("Jacob's Farming Contest has ended!");
    }

    // Method to update all players' scoreboards
    public void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }
    public void recalculateBrackets() {
        List<Integer> sortedScores = contestScores.values().stream()
                .filter(score -> score >= 100) // Only include scores >= 100
                .sorted(Collections.reverseOrder()) // Sort in descending order
                .collect(Collectors.toList());

        lowestDiamondScore = getLowestScoreForBracket(0.02, sortedScores);
        lowestPlatinumScore = getLowestScoreForBracket(0.07, sortedScores); // 2% + 5%
        lowestGoldScore = getLowestScoreForBracket(0.17, sortedScores); // 2% + 5% + 10%
        lowestSilverScore = getLowestScoreForBracket(0.47, sortedScores); // 2% + 5% + 10% + 30%
        lowestBronzeScore = getLowestScoreForBracket(0.53, sortedScores); // Rest for bronze (60%+)
    }

    // Update scoreboard for a specific player
    public void updateScoreboard(Player player) {
        UUID playerUUID = player.getUniqueId();
        int score = contestScores.get(playerUUID);

        if (!(contestActive) && score < 100) {
            return;
        }
        if(contestActive && score> 100) {

            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null) {
                player.sendMessage(ChatColor.RED + "Scoreboard manager not available!");
                return; // Handle the error, maybe log it
            }

            Scoreboard board = manager.getNewScoreboard();
            Objective objective = board.registerNewObjective("contestScore", "dummy", ChatColor.YELLOW + "Jacob's Contest");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            // Contest info
            objective.getScore(ChatColor.WHITE + "Potato").setScore(3);
            objective.getScore(ChatColor.GREEN + "Time: " + (timer.getRemainingTime() / 60) + ":" + String.format("%02d", timer.getRemainingTime() % 60)).setScore(2); // Update time

            // Player's bracket
            String playerBracket = getBracket(playerUUID);
            String PlayerNextBracket = getNextBracket(playerUUID);

                objective.getScore("Collected: " + ChatColor.YELLOW + score);

            if (playerBracket.equals(bronze)||playerBracket.equals(silver)||playerBracket.equals(gold)||playerBracket.equals(platinum)||playerBracket.equals(diamond)){
                objective.getScore(playerBracket + ChatColor.WHITE + " with " + score).setScore(1); // Score position for player's score
            }

            // Display the lowest score for the player's bracket
            int lowestScoreForBracket = -1;
              if (playerBracket.equals(platinum)) {
                lowestScoreForBracket = lowestDiamondScore;
            } else if (playerBracket.equals(gold)) {
                lowestScoreForBracket = lowestPlatinumScore;
            } else if (playerBracket.equals(silver)) {
                lowestScoreForBracket = lowestGoldScore;
            } else if (playerBracket.equals(bronze)) {
                lowestScoreForBracket = lowestSilverScore;
            } else if (playerBracket.equals(none)) {
                lowestScoreForBracket=lowestBronzeScore;
            }

            if (lowestScoreForBracket != -1) {
                objective.getScore(ChatColor.WHITE + PlayerNextBracket + " has " + lowestScoreForBracket).setScore(0); // Score position for the lowest score
            }

            player.setScoreboard(board);
        }
    }

    // Helper method to get the lowest score for a bracket based on percentile
    public int getLowestScoreForBracket(double percentile, List<Integer> sortedScores) {
        int totalPlayers = sortedScores.size();
        if (totalPlayers == 0) return -1;

        int index = (int) Math.ceil(percentile * totalPlayers) - 1;
        return (index < 0 || index >= sortedScores.size()) ? -1 : sortedScores.get(index);
    }


    // Get the bracket of a player based on their score
    public String getBracket(UUID playerUUID) {
        int score = contestScores.getOrDefault(playerUUID, 0);
        if (score < 100) return none;

        List<Integer> sortedScores = contestScores.values().stream()
                .filter(s -> s >= 100)
                .sorted(Collections.reverseOrder())
                .toList();

        int playerRank = sortedScores.indexOf(score) + 1;
        int totalPlayers = sortedScores.size();
        double percentile = (double) playerRank / totalPlayers;

        if (percentile <= 0.02) return diamond;
        if (percentile <= 0.07) return platinum;
        if (percentile <= 0.17) return gold;
        if (percentile <= 0.47) return silver;
        if (percentile <= 0.53) return bronze;
        if (percentile<= 1.0) return none;

        return none;
    }
    public String getNextBracket(UUID playerUUID){
        String playerBracket = getBracket(playerUUID);
        if (playerBracket.equals(none)){
            String nextBronze = bronze;
        } else if (playerBracket.equals(bronze)) {
            String nextSilver = silver;
        }
        else if (playerBracket.equals(silver)) {
            String nextSilver = gold;
        }
        else if (playerBracket.equals(platinum)) {
            String nextSilver = diamond;
        }
        return none;
    }

    // Get the bracket name for a given score
    public String getBracketName(int score) {
        // Similar logic as getBracket but for displaying other players' bracket names
        return getBracketNameFromScore(score);
    }

    public String getBracketNameFromScore(int score) {
        List<Integer> sortedScores = contestScores.values().stream()
                .filter(s -> s >= 100)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());

        int totalPlayers = sortedScores.size();
        int index = sortedScores.indexOf(score);

        double percentile = (double) (index + 1) / totalPlayers;

        if (percentile <= 0.02) return diamond;
        if (percentile <= 0.07) return platinum;
        if (percentile <= 0.17) return gold;
        if (percentile <= 0.47) return silver;
        if (percentile <= 1.0) return bronze;

        return none;
    }


    public JacobTimer getTimer() {
        return timer;
    }
}
