package me.f0reach.holofans.lobby.minigame.othello;

import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class OthelloConfig {
    private final Plugin plugin;

    private String world;
    private Vector pos1, pos2;
    private BoundingBox boundingBox;

    public OthelloConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        var section = plugin.getConfig().getConfigurationSection("othello");
        if (section == null) {
            plugin.getLogger().warning("No othello section found in config");
            return;
        }

        world = section.getString("world");
        pos1 = section.getVector("pos1");
        pos2 = section.getVector("pos2");
        boundingBox = BoundingBox.of(pos1, pos2).expand(5.0);
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public String getWorld() {
        return world;
    }

    public Vector getPos1() {
        return pos1;
    }

    public Vector getPos2() {
        return pos2;
    }
}
