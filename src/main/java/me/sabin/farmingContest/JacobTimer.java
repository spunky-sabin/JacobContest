package me.sabin.farmingContest;
import org.bukkit.scheduler.BukkitRunnable;

public class JacobTimer extends BukkitRunnable {
    private final JacobMain plugin;
    private int countdown = 1200; // 1200 seconds (20 minutes)

    public JacobTimer(JacobMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (countdown > 0) {

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
