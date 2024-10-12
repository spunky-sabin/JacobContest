package me.sabin.farmingContest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

public class JacobTimer extends BukkitRunnable {
    private final JacobMain plugin;
    private int countdown = 300; // 300 seconds (5 minutes)

    public JacobTimer(JacobMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (countdown > 0) {
            // Broadcast the remaining time every minute or when close to finishing
            if (countdown % 60 == 0 || countdown <= 10) {
                Bukkit.broadcastMessage(ChatColor.GREEN + "Time remaining: " + (countdown / 60) + " minute(s) " + (countdown % 60) + " second(s).");
            }
            countdown--;
        } else {
            // Time is up, end the contest
            plugin.endContest();
            cancel();
        }
    }

    // Method to get the remaining time in seconds
    public int getRemainingTime() {
        return countdown;
    }
}
