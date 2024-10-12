package me.sabin.farmingContest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JacobMain extends JavaPlugin implements Listener {
    public final ConcurrentHashMap<UUID, Integer> contestScores = new ConcurrentHashMap<>();
    public final HashMap<UUID, Material> playerCrops = new HashMap<>();
    public boolean contestActive = false;
    private JacobTimer timer;
    private List<Material> activeCrops = new ArrayList<>();

    private int lowestDiamondScore;
    private int lowestPlatinumScore;
    private int lowestGoldScore;
    private int lowestSilverScore;
    private int lowestBronzeScore;

    private final List<Material> crops = Arrays.asList(

            Material.CACTUS,
            Material.POTATOES,
            Material.WHEAT,
            Material.CARROTS,
            Material.COCOA,
            Material.PUMPKIN,
            Material.MELON,
            Material.NETHER_WART,
            Material.SUGAR_CANE
            // Add more crops as desired
    );

    private final String none = ChatColor.WHITE + "None";
    private final String bronze = ChatColor.RED + "BRONZE";
    private final String silver = ChatColor.GRAY + "SILVER";
    private final String gold = ChatColor.YELLOW + "GOLD";
    private final String platinum = ChatColor.AQUA + "PLATINUM";
    private final String diamond = ChatColor.BLUE + "DIAMOND";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new JacobListener(this), this);
        JacobCommands commands = new JacobCommands(this);
        Objects.requireNonNull(getCommand("startcontest")).setExecutor(commands);
        Objects.requireNonNull(getCommand("endcontest")).setExecutor(commands);
        Objects.requireNonNull(getCommand("checkscore")).setExecutor(commands);

        getLogger().info("Commands registered successfully.");
    }

    private List<Material> selectRandomCrops() {
        Collections.shuffle(crops); // Shuffle to randomize
        return crops.subList(0, Math.min(crops.size(), 3)); // Select 3 random crops
    }
    public String getCropName(Material crop) {
        return switch (crop) {
            case POTATOES -> "Potatoes";
            case WHEAT -> "Wheat";
            case CARROTS -> "Carrots";
            case MELON -> "Melon";
            case PUMPKIN-> "Pumpkin";
            case NETHER_WART -> "Nether Wart";
            case COCOA -> "Coca";
            case CACTUS -> "Cactus";
            case SUGAR_CANE -> "Sugar Cane";
            // Add more crops as necessary
            default -> crop.name();
        };
    }

    public void startContest() {
        contestActive = true;
        contestScores.clear();
        playerCrops.clear(); // Clear previous participant crops
        activeCrops = selectRandomCrops(); // Update the class-level variable
        String cropNames = activeCrops.stream()
                .map(this::getCropName)
                .collect(Collectors.joining(", "));
        Bukkit.broadcastMessage("Jacob's Farming Contest has started with crops: " + cropNames);

        timer = new JacobTimer(this);
        timer.runTaskTimer(this, 0, 20); // Runs every second

        new BukkitRunnable() {
            @Override
            public void run() {
                if (contestActive) {
                    recalculateBrackets();
                    updateAllScoreboards();
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(this, 0, 20);
    }
    public List<Material> getActiveCrops() {
        return activeCrops;
    }

    public void endContest() {
        contestActive = false;
        if (timer != null) {
            timer.cancel();
        }
        Bukkit.broadcastMessage("Jacob's Farming Contest has ended!");
    }

    public void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }


    public void recalculateBrackets() {
        List<Integer> sortedScores = contestScores.values().stream()
                .filter(score -> score >= 100) // Only include scores >= 100
                .sorted(Collections.reverseOrder()) // Sort in descending order (highest to lowest)
                .collect(Collectors.toList());
        int totalPlayers = sortedScores.size();
        if (totalPlayers < 1) {
            // If there are no players, set all lowest scores to -1 or some default
            lowestDiamondScore = -1;
            lowestPlatinumScore = -1;
            lowestGoldScore = -1;
            lowestSilverScore = -1;
            lowestBronzeScore = -1;
            return; // Exit early since there are no players
        }
        // Calculate the lowest score needed for each bracket
        lowestDiamondScore = getLowestScoreForBracket(0.02, sortedScores);   // Top 2%
        lowestPlatinumScore = getLowestScoreForBracket(0.07, sortedScores);  // Next 5%
        lowestGoldScore = getLowestScoreForBracket(0.17, sortedScores);      // Next 10%
        lowestSilverScore = getLowestScoreForBracket(0.47, sortedScores);    // Next 30%
        lowestBronzeScore = getLowestScoreForBracket(1.00, sortedScores);    // The rest 60%
    }

    public void updateScoreboard(Player player) {
        UUID playerUUID = player.getUniqueId();
        int score = contestScores.getOrDefault(playerUUID, 0);

        // Only update scoreboard for players with score >= 100 when the contest is active
        if (contestActive && score >= 100) {
            Material participatingCrop = playerCrops.get(playerUUID);

            // Create a new scoreboard
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null) {
                player.sendMessage(ChatColor.RED + "Scoreboard manager not available!");
                return;
            }

            Scoreboard board = manager.getNewScoreboard();
            Objective objective = board.registerNewObjective("contestScore", Criteria.DUMMY, ChatColor.YELLOW + "Jacob's Contest");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            // Display crop or "None" if not assigned yet
            String cropDisplayName = (participatingCrop != null) ? ChatColor.GREEN + getCropName(participatingCrop) : ChatColor.RED + "None";
            String displayString = ChatColor.YELLOW + "○ " + cropDisplayName + " " + ChatColor.GREEN + (timer.getRemainingTime() / 60 + "m" + String.format("%02d", timer.getRemainingTime() % 60) + "s");
            objective.getScore(displayString).setScore(2);

            // Player's current bracket and score
            String playerBracket = getBracket(playerUUID);
            if (!playerBracket.equals(none)) {
                objective.getScore(playerBracket + ChatColor.WHITE + " with " + ChatColor.YELLOW + score).setScore(1);
            }

            // Check for the next bracket and display if applicable
            String nextBracket = getNextBracket(playerUUID);
            int lowestScoreForNextBracket = getLowestScoreForNextBracket(playerBracket);
            if (!nextBracket.equals(none) && lowestScoreForNextBracket != -1) {
                objective.getScore(ChatColor.GRAY + nextBracket + " has " + lowestScoreForNextBracket).setScore(0);
            }

            // Set the player's scoreboard
            player.setScoreboard(board);
        } else if (!contestActive && score < 100) {
            // If contest is inactive and score is less than 100, reset scoreboard to a default one
            player.setScoreboard(Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard());
        }
    }



    public String getBracket(UUID playerUUID) {
        int score = contestScores.getOrDefault(playerUUID, 0);
        if (score < 100) return none; // No bracket if score < 100

        List<Integer> sortedScores = contestScores.values().stream()
                .filter(s -> s >= 100)
                .sorted(Collections.reverseOrder())
                .toList();

        int playerRank = sortedScores.indexOf(score) + 1; // 1-based rank
        int totalPlayers = sortedScores.size();
        double percentile = (double) playerRank / totalPlayers; // Player's percentile rank

        // Determine the bracket based on the player's percentile rank
        if (percentile <= 0.02) return diamond;     // Top 2%
        if (percentile <= 0.07) return platinum;    // Next 5%
        if (percentile <= 0.17) return gold;        // Next 10%
        if (percentile <= 0.47) return silver;      // Next 30%
        if (percentile <= 1.00) return bronze;      // Remaining 60%

        return none; // Bottom 40% get no rank
    }

    public String getNextBracket(UUID playerUUID) {
        String playerBracket = getBracket(playerUUID);

        if (playerBracket.equals(none)) {
            return "bronze";  // Move from None to Bronze
        } else if (playerBracket.equals(bronze)) {
            return "silver";  // Move from Bronze to Silver
        } else if (playerBracket.equals(silver)) {
            return "gold";    // Move from Silver to Gold
        } else if (playerBracket.equals(gold)) {
            return "platinum"; // Move from Gold to Platinum
        } else if (playerBracket.equals(platinum)) {
            return "diamond";  // Move from Platinum to Diamond
        } else {
            return "none";     // Default case if no valid bracket
        }
    }

    public int getLowestScoreForNextBracket(String playerBracket) {
            if(playerBracket.equals(none)) {
                return lowestBronzeScore;
            }
            if (playerBracket.equals(bronze)) {
                return lowestSilverScore;
            }
            if (playerBracket.equals(silver)) {
                return lowestGoldScore;
            }
            if (playerBracket.equals(gold)) {
                return lowestPlatinumScore;
            }
            if (playerBracket.equals(platinum)) {
                return lowestDiamondScore;
            }
                return -1;
    }

    public int getLowestScoreForBracket(double percentile, List<Integer> sortedScores) {
        int totalPlayers = sortedScores.size();
        if (totalPlayers == 0) return -1; // No players? Return -1 to indicate no bracket

        // Calculate the index based on the percentile
        int index = (int) Math.ceil(percentile * totalPlayers) - 1;

        // Return the score at the calculated index, or -1 if out of bounds
        return (index < 0 || index >= sortedScores.size()) ? -1 : sortedScores.get(index);
    }
}
