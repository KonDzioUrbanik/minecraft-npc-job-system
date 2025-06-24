package org.example.pans.aINPC.jobsystem;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.HologramTrait;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class JobNPCListener implements Listener {

    private final JobGUIHandler guiHandler;

    public JobNPCListener(JobGUIHandler guiHandler) {
        this.guiHandler = guiHandler;
    }

    @EventHandler
    public void onNPCClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();

        if (!npc.getName().equalsIgnoreCase("Urzędnik")) return;


        if (!npc.hasTrait(HologramTrait.class)) {
            npc.addTrait(HologramTrait.class);
        }

        HologramTrait hologram = npc.getTrait(HologramTrait.class);


        if (hologram.getLines().isEmpty()) {
            hologram.addLine("§eKliknij mnie, aby sprawdzić dostępne prace");
        }

        guiHandler.openJobGUI(event.getClicker());
    }
}
