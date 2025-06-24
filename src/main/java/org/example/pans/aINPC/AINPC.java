package org.example.pans.aINPC;

import net.citizensnpcs.api.CitizensAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.pans.aINPC.jobsystem.*;

import java.io.File;
import java.util.List;

public final class AINPC extends JavaPlugin {

    private static AINPC instance;

    public ShopInteractionHandler shopInteractionHandler;
    public PriceNegotiationHandler priceNegotiationHandler;
    public MovingNPCManager movingNPCManager;
    private NPCStatusDisplayManager npcStatusDisplayManager;
    private static Economy econ = null;

    private JobManager jobManager;
    private JobGUIHandler jobGUIHandler;
    private FileConfiguration priceConfig = null;
    public NPCDataManager npcDataManager;

    @Override
    public void onEnable() {
        instance = this;


        createFolder("npc_texts");
        createFolder("player_data");

        String[] dialogFiles = {
                "npc_texts/request_item.yml",
                "npc_texts/good_price.yml",
                "npc_texts/bad_price.yml",
                "npc_texts/no_response.yml"
        };
        for (String path : dialogFiles) {
            saveResource(path, false);
        }

        // === Prace ===
        File file = new File(getDataFolder(), "prace.yml");
        if (!file.exists()) saveResource("prace.yml", false);
        FileConfiguration praceConfig = YamlConfiguration.loadConfiguration(file);
        List<String> jobList = praceConfig.getStringList("prace");
        ConfigurationSection levelSection = praceConfig.getConfigurationSection("poziomy");
        if (levelSection == null) {
            // utwórz pustą sekcję żeby nie było nullpointera
            levelSection = new YamlConfiguration().createSection("poziomy");
        }
        JobType.loadFromConfig(jobList, levelSection);




        // === Ceny ===
        File pricesFile = new File(getDataFolder(), "prices.yml");
        if (!pricesFile.exists()) saveResource("prices.yml", false);
        priceConfig = YamlConfiguration.loadConfiguration(pricesFile);

        // === Vault ===
        if (!setupEconomy()) {
            getLogger().severe("Vault not found. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // === Dane NPC ===
        npcDataManager = new NPCDataManager(getDataFolder());

        // === Systemy ===
        npcStatusDisplayManager = new NPCStatusDisplayManager();
        shopInteractionHandler = new ShopInteractionHandler();
        priceNegotiationHandler = new PriceNegotiationHandler();
        movingNPCManager = new MovingNPCManager(this, shopInteractionHandler, priceNegotiationHandler);

        // === Komendy ===
        getCommand("setnpcrole").setExecutor(new SetNPCRoleCommand());
        getCommand("npcpath").setExecutor(new PathCommand(movingNPCManager)); // <-- po inicjalizacji

        // === Eventy ===
        getServer().getPluginManager().registerEvents(shopInteractionHandler, this);
        getServer().getPluginManager().registerEvents(priceNegotiationHandler, this);

        // === Prace: GUI i Eventy ===
        jobManager = new JobManager();
        jobGUIHandler = new JobGUIHandler(jobManager);

        getServer().getPluginManager().registerEvents(new JobJoinListener(jobManager), this);
        getServer().getPluginManager().registerEvents(new JobNPCListener(jobGUIHandler), this);
        getServer().getPluginManager().registerEvents(new JobGUIListener(jobGUIHandler), this);

        // === NPC z rolą "CLIENT" wracają do działania ===
        Bukkit.getScheduler().runTaskLater(this, () -> {
            CitizensAPI.getNPCRegistry().forEach(npc -> {
                String role = npcDataManager.getRole(npc);
                if ("CLIENT".equalsIgnoreCase(role)) {
                    // NPC wróci do systemu – ścieżki i kolejka zajmą się nim
                }
            });
        }, 20L);

        // === Ścieżki NPC ===
        File pathFile = new File(getDataFolder(), "npc_paths.yml");
        movingNPCManager.loadPathsFromFile(pathFile);
        movingNPCManager.startWalking();

        getLogger().info("✅ AINPC enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("❌ AINPC disabled.");
    }

    public static AINPC getInstance() {
        return instance;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static NPCStatusDisplayManager getStatusManager() {
        return getInstance().npcStatusDisplayManager;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public FileConfiguration getPriceConfig() {
        return priceConfig;
    }

    public NPCDataManager getNPCDataManager() {
        return npcDataManager;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void createFolder(String name) {
        File folder = new File(getDataFolder(), name);
        if (!folder.exists()) folder.mkdirs();
    }
}
