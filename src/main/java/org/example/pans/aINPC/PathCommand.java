package org.example.pans.aINPC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PathCommand implements CommandExecutor {

    private final MovingNPCManager manager;
    private final Map<UUID, List<NPCPathPoint>> tempPaths = new HashMap<>();

    public PathCommand(MovingNPCManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return false;

        UUID uuid = p.getUniqueId();

        if (args.length == 1 && args[0].equalsIgnoreCase("start")) {
            tempPaths.put(uuid, new ArrayList<>());
            p.sendMessage("§aRozpoczęto tworzenie ścieżki.");
            return true;

        } else if (args.length >= 1 && args[0].equalsIgnoreCase("add")) {
            List<NPCPathPoint> path = tempPaths.get(uuid);
            if (path == null) {
                p.sendMessage("§cNajpierw wpisz /npcpath start");
                return true;
            }

            String name = args.length >= 2 ? args[1] : "punkt";
            path.add(new NPCPathPoint(p.getLocation(), name));
            p.sendMessage("§aDodano punkt: " + name);
            return true;

        } else if (args.length == 2 && args[0].equalsIgnoreCase("save")) {
            List<NPCPathPoint> path = tempPaths.get(uuid);
            if (path == null || path.isEmpty()) {
                p.sendMessage("§cBrak zapisanych punktów.");
                return true;
            }

            try {
                int npcId = Integer.parseInt(args[1]);
                manager.addPathForNPC(npcId, path);
                manager.startWalking();
                p.sendMessage("§aZapisano ścieżkę dla NPC o ID " + npcId);
                tempPaths.remove(uuid);


                File file = new File(Bukkit.getPluginManager().getPlugin("AINPC").getDataFolder(), "npc_paths.yml");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                List<String> lines = path.stream().map(point -> {
                    Location loc = point.getLocation();
                    return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + point.getName();
                }).collect(Collectors.toList());

                config.set(String.valueOf(npcId) + ".path", lines);
                config.save(file);

            } catch (NumberFormatException e) {
                p.sendMessage("§cNiepoprawny ID NPC.");
            } catch (IOException e) {
                p.sendMessage("§cBłąd podczas zapisu ścieżki do pliku.");
                e.printStackTrace();
            }

            return true;
        }

        p.sendMessage("§cUżycie: /npcpath start | add <nazwa> | save <npcId>");
        return true;
    }
}
