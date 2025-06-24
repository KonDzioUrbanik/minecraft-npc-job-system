package org.example.pans.aINPC;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.util.*;

public class PriceNegotiationHandler implements Listener {

    private final Map<Player, NPC> activeNegotiations = new HashMap<>();
    private final Map<NPC, Material> npcNeeds = new HashMap<>();
    private final Map<NPC, Integer> deliveryTimeouts = new HashMap<>();
    private final Map<NPC, Integer> negotiationTimeouts = new HashMap<>();
    private final Map<String, List<String>> npcDialogues = new HashMap<>();

    public PriceNegotiationHandler() {
        loadNpcDialogues();
    }

    private void loadNpcDialogues() {
        String[] files = {"request_item.yml", "good_price.yml", "bad_price.yml", "no_response.yml"};
        for (String fileName : files) {
            File file = new File(AINPC.getInstance().getDataFolder(), "npc_texts/" + fileName);
            if (file.exists()) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                npcDialogues.put(fileName, cfg.getStringList("lines"));
            }
        }
    }

    public void forceStartNegotiation(Player player, NPC npc, Material material) {

        cancelTimeouts(npc);

        activeNegotiations.put(player, npc);
        npcNeeds.put(npc, material);

        List<String> lines = npcDialogues.get("request_item.yml");
        String msg = lines.get(new Random().nextInt(lines.size())).replace("{item}", material.name().toLowerCase().replace("_", " "));
        player.sendMessage("§eNPC: " + msg + " Ile chcesz? (np. .12)");

        AINPC.getStatusManager().showStatus(npc, "Czekam na cenę", 30);

        // ⏱ Timer: jeśli gracz nie odpowie z ceną
        int negotiationTimeout = Bukkit.getScheduler().runTaskLater(AINPC.getInstance(), () -> {
            if (activeNegotiations.containsKey(player) && activeNegotiations.get(player).equals(npc)) {
                List<String> timeoutLines = npcDialogues.get("no_response.yml");
                String timeoutMsg = timeoutLines.get(new Random().nextInt(timeoutLines.size())).replace("{item}", material.name().toLowerCase());
                player.sendMessage("§cNPC: " + timeoutMsg);
                cleanup(npc, player);
            }
        }, 600L).getTaskId();

        negotiationTimeouts.put(npc, negotiationTimeout);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();

        if (!msg.startsWith(".") || !activeNegotiations.containsKey(player)) return;
        event.setCancelled(true);

        NPC npc = activeNegotiations.get(player);
        Material item = npcNeeds.get(npc);

        String job = AINPC.getInstance().getJobManager().getCurrentJob(player);
        if (!"sklepikarz".equalsIgnoreCase(job)) {
            player.sendMessage("§cNPC: Nie jesteś sklepikarzem.");
            cleanup(npc, player);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(msg.substring(1));
        } catch (NumberFormatException e) {
            player.sendMessage("§cNiepoprawna cena.");
            cleanup(npc, player);
            return;
        }

        double min = AINPC.getInstance().getPriceConfig().getDouble(item.name() + ".min", -1);
        double max = AINPC.getInstance().getPriceConfig().getDouble(item.name() + ".max", -1);

        Bukkit.getScheduler().runTask(AINPC.getInstance(), () -> {
            if (price >= min && price <= max) {
                List<String> lines = npcDialogues.get("good_price.yml");
                String reply = lines.get(new Random().nextInt(lines.size()))
                        .replace("{item}", item.name().toLowerCase())
                        .replace("{price}", String.valueOf(price));
                player.sendMessage("§aNPC: " + reply);

                AINPC.getInstance().shopInteractionHandler.setWantedItem(npc, item, player, price);
                AINPC.getStatusManager().showStatus(npc, "Czekam na przedmiot", 30);


                cancelNegotiationTimer(npc);
                AINPC.getInstance().movingNPCManager.stopCountdown(npc);


                int timeoutId = Bukkit.getScheduler().runTaskLater(AINPC.getInstance(), () -> {
                    List<String> timeoutLines = npcDialogues.get("no_response.yml");
                    String timeoutMsg = timeoutLines.get(new Random().nextInt(timeoutLines.size())).replace("{item}", item.name().toLowerCase());
                    player.sendMessage("§cNPC: " + timeoutMsg);
                    cleanup(npc, player);
                }, 600L).getTaskId();

                deliveryTimeouts.put(npc, timeoutId);

            } else {
                List<String> lines = npcDialogues.get("bad_price.yml");
                String reply = lines.get(new Random().nextInt(lines.size())).replace("{item}", item.name().toLowerCase());
                player.sendMessage("§cNPC: " + reply);
                cleanup(npc, player);
            }
        });
    }

    public void cancelDeliveryTimer(NPC npc) {
        Integer task = deliveryTimeouts.remove(npc);
        if (task != null) Bukkit.getScheduler().cancelTask(task);
    }

    public void cancelNegotiationTimer(NPC npc) {
        Integer task = negotiationTimeouts.remove(npc);
        if (task != null) Bukkit.getScheduler().cancelTask(task);
    }

    private void cancelTimeouts(NPC npc) {
        cancelNegotiationTimer(npc);
        cancelDeliveryTimer(npc);
    }

    private void cleanup(NPC npc, Player player) {
        activeNegotiations.remove(player);
        npcNeeds.remove(npc);
        cancelTimeouts(npc);
        AINPC.getStatusManager().removeStatus(npc);
        AINPC.getInstance().movingNPCManager.finishClient(npc);
    }
}
