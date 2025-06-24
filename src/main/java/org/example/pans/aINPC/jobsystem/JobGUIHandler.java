package org.example.pans.aINPC.jobsystem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class JobGUIHandler {
    private final JobManager jobManager;

    public JobGUIHandler(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    public void openJobGUI(Player player) {
        List<String> jobs = JobType.getJobTypes();
        Inventory inv = Bukkit.createInventory(null, 9, "Wybierz pracę");

        for (String job : jobs) {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + job);
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Wybierz pracę")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String jobName = clicked.getItemMeta().getDisplayName().replace("§e", "").toUpperCase();
        if (JobType.isValidJob(jobName)) {
            jobManager.assignJob(player, jobName);
            player.closeInventory();
        }
    }
}
