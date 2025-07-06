package me.f0reach.holofans.lobby.minigame.gomoku;

import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class GomokuConfig {
    private final Plugin plugin;
    private String world;
    private Vector posMin, posMax;
    private BoundingBox boardArea;
    private BoundingBox playableArea;
    private Vector direction1, direction2;
    private Vector rayTraceOffset;
    private int boardSize;
    private boolean isValid;

    public GomokuConfig(Plugin plugin) {
        this.plugin = plugin;
        this.isValid = false;
        this.boardSize = 15;
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


        var board = posMax.clone().subtract(posMin);
        if (board.getBlockX() == 0) {
            direction1 = new Vector(0, 0, 1);
            direction2 = new Vector(0, 1, 0);
        } else if (board.getBlockZ() == 0) {
            direction1 = new Vector(1, 0, 0);
            direction2 = new Vector(0, 1, 0);
        } else if (board.getBlockY() == 0) {
            direction1 = new Vector(1, 0, 0);
            direction2 = new Vector(0, 0, 1);
        } else {
            plugin.getLogger().warning("Gomoku positions must be aligned to axes");
            return;
        }

        boardArea = BoundingBox.of(posMin, posMax.clone().add(direction1).add(direction2));

        if (!setBoardSize(board)) {
            plugin.getLogger().warning("Gomoku board size must be aligned to axes");
            return;
        }

        var playableArea1 = section.getVector("playableArea1");
        var playableArea2 = section.getVector("playableArea2");
        if (posMin == null || posMax == null || playableArea1 == null || playableArea2 == null) {
            plugin.getLogger().warning("Gomoku positions not set in config");
            return;
        }

        playableArea = BoundingBox.of(playableArea1, playableArea2);
        if (!playableArea.contains(posMin) || !playableArea.contains(posMax)) {
            plugin.getLogger().warning("Gomoku playable area must contain the board positions");
            return;
        }

        rayTraceOffset = section.getVector("rayTraceOffset");
        if (rayTraceOffset == null) {
            rayTraceOffset = new Vector(0, 0, 0);
        }

        this.isValid = true;

        plugin.getLogger().info("Gomoku board size: " + boardSize);
    }

    private boolean setBoardSize(Vector boardSize) {
        this.boardSize = (Math.max(boardSize.getBlockX(), Math.max(boardSize.getBlockY(), boardSize.getBlockZ())) / 2) + 1;
        var xValid = boardSize.getBlockX() == 0 && boardSize.getBlockY() == boardSize.getBlockZ();
        var yValid = boardSize.getBlockY() == 0 && boardSize.getBlockX() == boardSize.getBlockZ();
        var zValid = boardSize.getBlockZ() == 0 && boardSize.getBlockX() == boardSize.getBlockY();
        if (!xValid && !yValid && !zValid) {
            plugin.getLogger().warning("Gomoku board size must be aligned to axes");
            return false;
        }
        return true;
    }

    public boolean isValid() {
        return isValid;
    }

    public BoundingBox getPlayableArea() {
        return playableArea;
    }

    public Vector getRayTraceOffset() {
        return rayTraceOffset;
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

    public BoundingBox getBoardArea() {
        return boardArea;
    }

    public Vector getDirection1() {
        return direction1;
    }

    public Vector getDirection2() {
        return direction2;
    }

    public int getBoardSize() {
        return boardSize;
    }
}
