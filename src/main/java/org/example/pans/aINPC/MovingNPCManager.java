
package org.example.pans.aINPC;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class MovingNPCManager {
    private final JavaPlugin plugin;
    private final Map<Integer, List<NPCPathPoint>> npcPaths = new HashMap<>();
    private final Map<Integer, Integer> npcCurrentIndex = new HashMap<>();
    private final Set<Integer> npcWaiting = new HashSet<>();
    private final Queue<NPC> clientQueue = new LinkedList<>();
    private final List<Material> availableItems = new ArrayList<>();

    private final ShopInteractionHandler shopInteractionHandler;
    private final PriceNegotiationHandler priceNegotiationHandler;

    private final Map<Integer, BukkitRunnable> countdownTasks = new HashMap<>();

    public MovingNPCManager(JavaPlugin plugin, ShopInteractionHandler shopInteractionHandler, PriceNegotiationHandler priceNegotiationHandler) {
        this.plugin = plugin;
        this.shopInteractionHandler = shopInteractionHandler;
        this.priceNegotiationHandler = priceNegotiationHandler;
        loadAvailableItems();
    }

    public void addPathForNPC(int npcId, List<NPCPathPoint> path) {
        npcPaths.put(npcId, path);
        npcCurrentIndex.put(npcId, 0);
    }

    public void setNPCWaiting(NPC npc, boolean waiting) {
        if (waiting) {
            npcWaiting.add(npc.getId());
            npc.getNavigator().cancelNavigation();
        } else {
            npcWaiting.remove(npc.getId());
        }
    }

    public void startWalking() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Integer npcId : npcPaths.keySet()) {
                    NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
                    if (npc == null || !npc.isSpawned()) continue;
                    if (npcWaiting.contains(npcId)) continue;

                    List<NPCPathPoint> path = npcPaths.get(npcId);
                    int currentIndex = npcCurrentIndex.get(npcId);
                    NPCPathPoint point = path.get(currentIndex);
                    Location nextLoc = point.getLocation();

                    if (npc.getEntity().getLocation().distance(nextLoc) < 2) {
                        handleInteraction(npc, point);
                        currentIndex = (currentIndex + 1) % path.size();
                        npcCurrentIndex.put(npcId, currentIndex);
                    } else {
                        npc.getNavigator().setTarget(nextLoc);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    private void handleInteraction(NPC npc, NPCPathPoint point) {
        if (!point.getName().equalsIgnoreCase("sklep")) return;
        if (clientQueue.contains(npc)) return;

        clientQueue.add(npc);
        setNPCWaiting(npc, true);
        AINPC.getStatusManager().showStatus(npc, "Czekam w kolejce", 40);
        tryNextClient();
    }

    private void tryNextClient() {
        if (clientQueue.isEmpty()) return;

        NPC current = clientQueue.peek();
        if (!npcWaiting.contains(current.getId())) return;


        AINPC.getStatusManager().removeStatus(current);
        startCountdown(current, 40);
    }

    private void startCountdown(NPC npc, int time) {
        AINPC.getStatusManager().showStatus(npc, "Czekam na wejście", time);

        BukkitRunnable countdownTask = new BukkitRunnable() {
            int secondsLeft = time;

            @Override
            public void run() {
                if (!clientQueue.contains(npc)) {
                    cancel();
                    return;
                }

                if (secondsLeft <= 0) {
                    AINPC.getStatusManager().removeStatus(npc);
                    finishClient(npc);
                    cancel();
                    return;
                }

                AINPC.getStatusManager().showStatus(npc, "Czekam na wejście", secondsLeft);
                secondsLeft--;
            }
        };

        countdownTask.runTaskTimer(plugin, 0L, 20L);
        countdownTasks.put(npc.getId(), countdownTask);


        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (clientQueue.peek() != npc) return;

            Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (player != null && !availableItems.isEmpty()) {
                Material item = availableItems.get(new Random().nextInt(availableItems.size()));
                priceNegotiationHandler.forceStartNegotiation(player, npc, item);
            }
        }, 20L);
    }

    public void stopCountdown(NPC npc) {
        BukkitRunnable task = countdownTasks.remove(npc.getId());
        if (task != null) task.cancel();
    }

    public void finishClient(NPC npc) {
        clientQueue.remove(npc);
        setNPCWaiting(npc, false);
        AINPC.getStatusManager().removeStatus(npc);
        stopCountdown(npc);
        Bukkit.getScheduler().runTaskLater(plugin, this::tryNextClient, 20L);
    }

    private void loadAvailableItems() {
        File configFile = new File(plugin.getDataFolder(), "prices.yml");
        if (!configFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (String key : config.getKeys(false)) {
            try {
                availableItems.add(Material.valueOf(key.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void loadPathsFromFile(File file) {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String npcIdStr : config.getKeys(false)) {
            int npcId = Integer.parseInt(npcIdStr);
            List<String> pathStrings = config.getStringList(npcIdStr + ".path");
            List<NPCPathPoint> path = new ArrayList<>();

            for (String line : pathStrings) {
                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                String world = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                String name = parts[4];

                Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                path.add(new NPCPathPoint(loc, name));
            }

            addPathForNPC(npcId, path);
        }
    }
}
