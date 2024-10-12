package me.sabin.farmingContest;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class JacobListener implements Listener {
    private final JacobMain plugin;

    public JacobListener(JacobMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.contestActive) return; // Ensure contest is active

        // Get the block that was broken
        Material brokenBlockType = event.getBlock().getType();
        Collection<ItemStack> drops = event.getBlock().getDrops();
        // Ensure the broken block is one of the active crops
        List<Material> activeCrops = plugin.getActiveCrops();

        if (!activeCrops.contains(brokenBlockType)) {
            return; // If the block isn't part of the active crops, ignore it
        }

        // Get the player who broke the block
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Check if the player has an assigned crop
        Material assignedCrop = plugin.playerCrops.get(playerUUID);
        if (assignedCrop != null && !assignedCrop.equals(brokenBlockType)) {
            return; // Player is breaking a crop they are not assigned to
        }

        // If this is the first crop they broke, assign it to them
        if (assignedCrop == null) {
            plugin.playerCrops.put(playerUUID, brokenBlockType); // Assign the crop to the player
        }

        // Calculate the score
        int score = getScore(drops);


        // Update player's score
        plugin.contestScores.put(playerUUID, plugin.contestScores.getOrDefault(playerUUID, 0) + score);

        // Update scoreboard after breaking the block
        plugin.updateScoreboard(player);
    }

    private static int getScore(Collection<ItemStack> drops) {
        int score = 0;
        for (ItemStack drop : drops) {
            if (drop.getType() == Material.WHEAT) { // Check if the drop is wheat
                score += drop.getAmount(); // Add score for wheat only

            }if (drop.getType() == Material.POTATOES) { // Check if the drop is potatoes
                score += drop.getAmount(); // Add score for potatoes only
            } // Add other crops as needed
            if (drop.getType() == Material.CARROTS){
                score += drop.getAmount();
            }
            if (drop.getType()==Material.PUMPKIN){
                score += drop.getAmount();
            }
            if (drop.getType()==Material.MELON){
                score += drop.getAmount();
            }
            if (drop.getType()==Material.SUGAR_CANE){
                score += drop.getAmount();
            }
            if (drop.getType()==Material.COCOA){
                score += drop.getAmount();
            }
            if (drop.getType()==Material.NETHER_WART){
                score += drop.getAmount();
            }
            if (drop.getType()==Material.CACTUS){
                score += drop.getAmount();
            }
        }
        return score;
    }
}
