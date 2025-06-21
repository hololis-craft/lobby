package me.f0reach.holofans.lobby;

import org.bukkit.util.Vector;

public class LobbyConfig {
    private final HolofansLobby plugin;

    // ロビーの設定
    private String lobbyWorld = null;
    private Vector lobbyLocation = null;
    private double lobbyYaw = 0;
    private int teleportDelay = 20 * 3;

    public LobbyConfig(HolofansLobby plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        try {
            lobbyWorld = plugin.getConfig().getString("lobbyWorld");
            lobbyLocation = plugin.getConfig().getVector("lobbyPos");
            lobbyYaw = plugin.getConfig().getDouble("lobbyYaw");
            teleportDelay = plugin.getConfig().getInt("teleportDelay");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload lobby location from config " + e.getMessage());
        }
    }

    public String getLobbyWorld() {
        return lobbyWorld;
    }

    public Vector getLobbyLocation() {
        return lobbyLocation;
    }

    public double getLobbyYaw() {
        return lobbyYaw;
    }

    public int getTeleportDelay() {
        return teleportDelay;
    }

}
