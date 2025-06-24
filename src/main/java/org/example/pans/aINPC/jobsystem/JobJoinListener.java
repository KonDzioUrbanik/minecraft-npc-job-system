package org.example.pans.aINPC.jobsystem;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JobJoinListener implements Listener {

    private final JobManager jobManager;

    public JobJoinListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String job = jobManager.getCurrentJob(event.getPlayer());
        if (job != null) {
            jobManager.reapplyBossBar(event.getPlayer(), job);
        }
    }
}
