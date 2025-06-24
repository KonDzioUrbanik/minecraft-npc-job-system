// SetNPCRoleCommand.java
package org.example.pans.aINPC;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SetNPCRoleCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Użycie: /setnpcrole <id> <rola>");
            return true;
        }

        try {
            int npcId = Integer.parseInt(args[0]);
            String role = args[1].toUpperCase();
            NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);

            if (npc == null) {
                sender.sendMessage("Nie znaleziono NPC o ID " + npcId);
                return true;
            }

            AINPC.getInstance().getNPCDataManager().setRole(npc, role);
            sender.sendMessage("Ustawiono rolę " + role + " dla NPC " + npc.getName());
        } catch (NumberFormatException e) {
            sender.sendMessage("ID NPC musi być liczbą.");
        }

        return true;
    }
}
