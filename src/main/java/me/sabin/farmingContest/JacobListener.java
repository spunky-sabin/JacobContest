package me.sabin.farmingContest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.UUID;

public class JacobListener implements Listener {
    private final JacobMain plugin;

    public JacobListener(JacobMain plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.contestActive && event.getBlock().getType() == Material.POTATOES) {
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
            plugin.contestScores.put(playerUUID, plugin.contestScores.getOrDefault(playerUUID, 0) + totalPotatoes);

            // Update scoreboard after breaking the block
            plugin.updateScoreboard(player);
        }
    }
}
