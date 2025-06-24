package org.example.pans.aINPC.jobsystem;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class JobPlayerData {
    private final UUID playerUUID;
    private String currentJob;
    private int level = 1;
    private long lastJobChange = 0;
    private int transactions = 0;
    private int poziom = 1;

    public JobPlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        load();
    }

    public String getCurrentJob() {
        return currentJob;
    }

    public void setCurrentJob(String jobName) {
        this.currentJob = jobName.toUpperCase();
        this.level = 1;
        this.poziom = 1;
        this.transactions = 0;
        this.lastJobChange = System.currentTimeMillis();
        save();
    }

    public int getLevel() {
        return level;
    }

    public int getTransactions() {
        return transactions;
    }

    public long getLastJobChange() {
        return lastJobChange;
    }

    public boolean canChangeJob() {
        return (System.currentTimeMillis() - lastJobChange) >= 30_000;
    }

    public void addTransaction(String job) {
        this.transactions++;
        updatePoziom(job);
        save();
    }

    private void updatePoziom(String job) {
        Map<Integer, Integer> levelMap = JobType.getLevelMap(job);
        int newLevel = poziom;

        for (Map.Entry<Integer, Integer> entry : levelMap.entrySet()) {
            if (transactions >= entry.getValue() && entry.getKey() > newLevel) {
                newLevel = entry.getKey();
            }
        }

        if (newLevel > poziom) {
            poziom = newLevel;
            this.level = newLevel;

            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                player.sendMessage("§a⬆ Awansowałeś na poziom §e" + newLevel + " §aw pracy §e" + currentJob + "§a!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0), 30);
            }
        }
    }

    private void save() {
        File file = getDataFile();
        YamlConfiguration config = new YamlConfiguration();
        config.set("job", currentJob);
        config.set("level", level);
        config.set("transactions", transactions);
        config.set("lastJobChange", lastJobChange);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        File file = getDataFile();
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String jobName = config.getString("job");
        if (jobName != null && JobType.isValidJob(jobName)) {
            this.currentJob = jobName.toUpperCase();
        }
        this.level = config.getInt("level", 1);
        this.transactions = config.getInt("transactions", 0);
        this.lastJobChange = config.getLong("lastJobChange", 0);
        this.poziom = level;
    }

    private File getDataFile() {
        File dataFolder = new File(Bukkit.getPluginManager().getPlugin("AINPC").getDataFolder(), "player_data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return new File(dataFolder, playerUUID + ".yml");
    }
}
