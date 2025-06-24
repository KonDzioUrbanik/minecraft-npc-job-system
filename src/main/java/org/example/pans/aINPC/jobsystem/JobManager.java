package org.example.pans.aINPC.jobsystem;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.*;

public class JobManager {

    private final Map<UUID, JobPlayerData> playerDataMap = new HashMap<>();
    private final Map<UUID, BossBar> jobBossBars = new HashMap<>();

    public void setJob(Player player, String jobName) {
        JobPlayerData data = getOrLoadData(player);
        data.setCurrentJob(jobName);
        updateBossBar(player, jobName);
    }

    public String getCurrentJob(Player player) {
        return getOrLoadData(player).getCurrentJob();
    }

    public void addTransaction(Player player) {
        JobPlayerData data = getOrLoadData(player);
        data.addTransaction(data.getCurrentJob());
        updateBossBar(player, data.getCurrentJob());
    }

    public boolean canChangeJob(Player player) {
        return getOrLoadData(player).canChangeJob();
    }

    public JobPlayerData getOrLoadData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), uuid -> new JobPlayerData(player.getUniqueId()));
    }
    public void reapplyBossBar(Player player, String jobName) {
        updateBossBar(player, jobName);
    }


    private void updateBossBar(Player player, String jobName) {
        UUID uuid = player.getUniqueId();
        JobPlayerData data = getOrLoadData(player);

        int currentLevel = data.getLevel();
        int transactions = data.getTransactions();
        int nextRequirement = getNextRequirement(jobName, currentLevel + 1);

        String title = "Praca: " + jobName + " – Poziom " + currentLevel + " (" + transactions + "/" + nextRequirement + ")";
        double progress = Math.min(1.0, transactions / (double) nextRequirement);

        BossBar bar = jobBossBars.get(uuid);
        if (bar == null) {
            bar = Bukkit.createBossBar(title, BarColor.GREEN, BarStyle.SOLID);
            jobBossBars.put(uuid, bar);
        } else {
            bar.setTitle(title);
        }

        bar.setProgress(progress);
        bar.addPlayer(player);
        bar.setVisible(true);
    }

    private int getNextRequirement(String jobName, int nextLevel) {
        Map<Integer, Integer> levels = JobType.getLevelMap(jobName);
        return levels.getOrDefault(nextLevel, levels.values().stream().max(Integer::compareTo).orElse(1));
    }
    public void assignJob(Player player, String jobName) {
        JobPlayerData data = getOrLoadData(player);
        if (data.canChangeJob()) {
            data.setCurrentJob(jobName);
            player.sendMessage("§aZostałeś zatrudniony jako: §e" + jobName);
            updateBossBar(player, jobName);
        } else {
            player.sendMessage("§cMusisz poczekać przed zmianą pracy.");
        }
    }

}
