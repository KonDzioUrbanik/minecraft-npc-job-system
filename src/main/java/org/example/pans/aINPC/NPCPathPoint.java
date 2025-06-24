package org.example.pans.aINPC;

import org.bukkit.Location;

public class NPCPathPoint {
    private final Location location;
    private final String name;

    public NPCPathPoint(Location location, String name) {
        this.location = location;
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }
}
