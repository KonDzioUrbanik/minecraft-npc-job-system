package org.example.pans.aINPC;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class NPCStatusDisplayManager {

    private final Map<Integer, ArmorStand> npcArmorStands = new HashMap<>();
    private final Map<Integer, Integer> taskIds = new HashMap<>();

    public void showStatus(NPC npc, String message, int seconds) {
        removeStatus(npc);

        Location loc = npc.getEntity().getLocation().add(0, 2.5, 0);
        ArmorStand stand = (ArmorStand) npc.getEntity().getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setCustomName("§e" + message + " (" + seconds + "s)");
        npcArmorStands.put(npc.getId(), stand);

        int taskId = new BukkitRunnable() {
            int timeLeft = seconds;
            @Override
            public void run() {
                if (!stand.isValid() || !npc.isSpawned()) {
                    cancel();
                    return;
                }
                if (timeLeft <= 0) {
                    removeStatus(npc);
                    cancel();
                    return;
                }
                stand.setCustomName("§e" + message + " (" + timeLeft + "s)");
                timeLeft--;
            }
        }.runTaskTimer(AINPC.getInstance(), 0L, 20L).getTaskId();

        taskIds.put(npc.getId(), taskId);
    }

    public void removeStatus(NPC npc) {
        Bukkit.getScheduler().runTask(AINPC.getInstance(), () -> {
            ArmorStand stand = npcArmorStands.remove(npc.getId());
            if (stand != null && stand.isValid()) stand.remove();

            Integer taskId = taskIds.remove(npc.getId());
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        });
    }

}
