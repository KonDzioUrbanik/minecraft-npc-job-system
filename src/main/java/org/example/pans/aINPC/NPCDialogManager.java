package org.example.pans.aINPC;

import org.bukkit.configuration.file.YamlConfiguration;


import java.io.File;
import java.util.List;
import java.util.Random;

public class NPCDialogManager {
    private static final String DIALOG_FOLDER = "npc_texts";


    public static String getRandomLine(String category) {
        File file = new File(AINPC.getInstance().getDataFolder(), DIALOG_FOLDER + "/" + category + ".yml");

        if (!file.exists()) {
            AINPC.getInstance().getLogger().warning("Brak pliku dialogowego: " + category);
            return "...";
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> lines = config.getStringList("lines");

        if (lines == null || lines.isEmpty()) {
            AINPC.getInstance().getLogger().warning("Brak linii dialogu w: " + category);
            return "...";
        }

        return lines.get(new Random().nextInt(lines.size()));
    }


    public static String getFormattedLine(String category, String itemName) {
        String line = getRandomLine(category);
        return line.replace("{item}", itemName);
    }
}
