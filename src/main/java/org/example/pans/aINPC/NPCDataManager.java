package org.example.pans.aINPC;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NPCDataManager {
    private final File file;
    private final YamlConfiguration config;
    private final Map<Integer, String> npcRoles = new HashMap<>();

    public NPCDataManager(File dataFolder) {
        this.file = new File(dataFolder, "npc_data.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadNPCData();
    }

    private void loadNPCData() {
        for (String key : config.getKeys(false)) {
            try {
                int npcId = Integer.parseInt(key);
                String role = config.getString(key + ".role");
                npcRoles.put(npcId, role);
            } catch (NumberFormatException e) {
                System.out.println("Błędne ID NPC w npc_data.yml: " + key);
            }
        }
    }

    public void saveNPCData() {
        for (Map.Entry<Integer, String> entry : npcRoles.entrySet()) {
            config.set(entry.getKey() + ".role", entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRole(NPC npc) {
        return npcRoles.get(npc.getId());
    }

    public void setRole(NPC npc, String role) {
        npcRoles.put(npc.getId(), role);
        config.set(npc.getId() + ".role", role);
        saveNPCData();
    }

    public boolean isClient(NPC npc) {
        String role = getRole(npc);
        return role != null && role.equalsIgnoreCase("CLIENT");
    }

    public boolean isUrzędnik(NPC npc) {
        String role = getRole(npc);
        return role != null && role.equalsIgnoreCase("URZEDNIK");
    }
}
