package org.example.pans.aINPC;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.example.pans.aINPC.jobsystem.JobManager;

import java.util.HashMap;
import java.util.Map;

public class ShopInteractionHandler implements Listener {

    private final Map<Integer, Material> npcWants = new HashMap<>();
    private final Map<Integer, Double> npcPrices = new HashMap<>();
    private final Map<Integer, Player> npcToPlayer = new HashMap<>();

    public void setWantedItem(NPC npc, Material material, Player player, double price) {
        npcWants.put(npc.getId(), material);
        npcPrices.put(npc.getId(), price);
        npcToPlayer.put(npc.getId(), player);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item dropped = event.getItemDrop();

        Bukkit.getScheduler().runTaskLater(AINPC.getInstance(), () -> {
            for (Entity entity : dropped.getNearbyEntities(2, 2, 2)) {
                if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
                    NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
                    Material wanted = npcWants.get(npc.getId());
                    if (wanted != null && dropped.getItemStack().getType() == wanted) {


                        JobManager jobManager = AINPC.getInstance().getJobManager();
                        String job = jobManager.getCurrentJob(player);

                        if (job == null || !job.equalsIgnoreCase("sklepikarz")) {
                            player.sendMessage("§cTylko sklepikarz może sprzedawać przedmioty klientom!");
                            return;
                        }


                        dropped.remove();

                        npcWants.remove(npc.getId());
                        double price = npcPrices.remove(npc.getId());
                        Player buyer = npcToPlayer.remove(npc.getId());

                        buyer.sendMessage("§aNPC: Dziękuję za " + wanted.name().toLowerCase());

                        if (AINPC.getEconomy() != null) {
                            AINPC.getEconomy().depositPlayer(buyer, price);
                            buyer.sendMessage("§a+" + price + " monet");
                        }


                        jobManager.getOrLoadData(buyer).addTransaction(job);
                        jobManager.reapplyBossBar(buyer, job);


                        AINPC.getStatusManager().removeStatus(npc);
                        AINPC.getInstance().priceNegotiationHandler.cancelDeliveryTimer(npc);
                        AINPC.getInstance().movingNPCManager.finishClient(npc);
                    }
                }
            }
        }, 20L);
    }
}
