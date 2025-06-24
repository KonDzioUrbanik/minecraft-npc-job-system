package org.example.pans.aINPC.jobsystem;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

public class JobGUIListener implements Listener {

    private final JobGUIHandler guiHandler;

    public JobGUIListener(JobGUIHandler guiHandler) {
        this.guiHandler = guiHandler;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        guiHandler.handleClick(event);
    }
}
