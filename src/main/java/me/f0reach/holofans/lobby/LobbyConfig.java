package me.f0reach.holofans.lobby;

import org.bukkit.util.Vector;

public class LobbyConfig {
    private final HolofansLobby plugin;

    // ロビーの設定
    private String lobbyWorld = null;
    private Vector lobbyLocation = null;
    private double lobbyYaw = 0;
    private int teleportDelay = 20 * 3;
    private double lobbyPrice = 0.0;
    private boolean lobbyDynamicPrice = false;
    private double lobbyPricePerBlock = 0.0;

    public LobbyConfig(HolofansLobby plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        try {
            lobbyWorld = plugin.getConfig().getString("lobbyWorld");
            lobbyLocation = plugin.getConfig().getVector("lobbyPos");
            lobbyYaw = plugin.getConfig().getDouble("lobbyYaw");
            teleportDelay = plugin.getConfig().getInt("teleportDelay", 20 * 3);
            lobbyPrice = plugin.getConfig().getDouble("lobbyPrice", 0.0);
            lobbyDynamicPrice = plugin.getConfig().getBoolean("lobbyDynamicPrice", false);
            lobbyPricePerBlock = plugin.getConfig().getDouble("lobbyPricePerBlock", 0.0);
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

    public double getLobbyPrice() {
        return lobbyPrice;
    }

    public boolean isLobbyDynamicPrice() {
        return lobbyDynamicPrice;
    }

    public double getLobbyPricePerBlock() {
        return lobbyPricePerBlock;
    }
}
