package me.f0reach.holofans.lobby.minigame.gomoku;

import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class GomokuConfig {
    private final Plugin plugin;

    // 設定ファイルに基づく変数
    private String world;
    private Vector posMin, posMax;
    private int boardSize;
    private boolean isValid;

    public GomokuConfig(Plugin plugin) {
        this.plugin = plugin;
        this.isValid = false;
        this.boardSize = 19;
        load();
    }

    public void load() {
        this.isValid = false;
        var section = plugin.getConfig().getConfigurationSection("gomoku");
        if (section == null) {
            plugin.getLogger().warning("No gomoku section found in config");
            return;
        }

        world = section.getString("world");
        var pos1Raw = section.getVector("pos1");
        var pos2Raw = section.getVector("pos2");
        if (pos1Raw == null || pos2Raw == null) {
            plugin.getLogger().warning("Gomoku positions not set in config");
            return;
        }

        posMin = new Vector(Math.min(pos1Raw.getX(), pos2Raw.getX()),
                Math.min(pos1Raw.getY(), pos2Raw.getY()),
                Math.min(pos1Raw.getZ(), pos2Raw.getZ())).toBlockVector();
        posMax = new Vector(Math.max(pos1Raw.getX(), pos2Raw.getX()),
                Math.max(pos1Raw.getY(), pos2Raw.getY()),
                Math.max(pos1Raw.getZ(), pos2Raw.getZ())).toBlockVector();

        // Y座標は同じでないといけない
        if (posMin.getY() != posMax.getY()) {
            plugin.getLogger().warning("Gomoku positions must have the same Y coordinate.");
            return;
        }

        // X,Zの大きさは同じでないといけない
        if (posMax.getX() - posMin.getX() != posMax.getZ() - posMin.getZ()) {
            plugin.getLogger().warning("Gomoku positions must have the same X and Z size.");
            return;
        }

        boardSize = section.getInt("boardSize");

        if (boardSize < 5) {
            plugin.getLogger().warning("Invalid gomoku board size: " + boardSize + ". Using default size 19.");
            boardSize = 19;
        }

        this.isValid = true;

        plugin.getLogger().info("Gomoku board size: " + boardSize);
    }

    public boolean isValid() {
        return isValid;
    }

    public String getWorld() {
        return world;
    }

    public Vector getPosMin() {
        return posMin;
    }

    public Vector getPosMax() {
        return posMax;
    }

    public int getBoardSize() {
        return boardSize;
    }
}
