package me.f0reach.holofans.lobby.minigame.othello;

import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class OthelloLogic {
    private final OthelloDisk[][] board = new OthelloDisk[8][8];
    private OthelloPlayer currentPlayer = OthelloPlayer.BLACK;
    private static final List<Vector2i> DIRECTIONS = List.of(
            new Vector2i(-1, -1),
            new Vector2i(-1, 0),
            new Vector2i(-1, 1),
            new Vector2i(0, -1),
            new Vector2i(0, 1),
            new Vector2i(1, -1),
            new Vector2i(1, 0),
            new Vector2i(1, 1)
    );

    public OthelloLogic() {
        reset();
    }

    public void reset() {
        currentPlayer = OthelloPlayer.BLACK;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = OthelloDisk.EMPTY;
            }
        }

        board[3][3] = OthelloDisk.WHITE;
        board[3][4] = OthelloDisk.BLACK;
        board[4][3] = OthelloDisk.BLACK;
        board[4][4] = OthelloDisk.WHITE;
    }

    public OthelloDisk getDisk(int x, int y) {
        return board[x][y];
    }

    public OthelloPlayer getCurrentPlayer() {
        return currentPlayer;
    }

    public void nextTurn() {
        currentPlayer = currentPlayer == OthelloPlayer.BLACK ? OthelloPlayer.WHITE : OthelloPlayer.BLACK;
    }

    private List<Vector2i> getValidDirections(Vector2i pos, OthelloDisk disk) {
        List<Vector2i> validDirections = new ArrayList<>();

        for (var dir : DIRECTIONS) {
            if (isValidDirection(pos, disk, dir)) {
                validDirections.add(dir);
            }
        }

        return validDirections;
    }

    private boolean isValidDirection(Vector2i pos, OthelloDisk disk, Vector2i dir) {
        var next = new Vector2i(pos);
        next.add(dir);
        var begin = new Vector2i(next);

        while (next.x >= 0 && next.x < 8 && next.y >= 0 && next.y < 8) {
            if (board[next.x][next.y] == OthelloDisk.EMPTY) {
                return false;
            }

            if (board[next.x][next.y] == disk) {
                return !begin.equals(next);
            }

            next.add(dir);
        }

        return false;
    }

    private void flipDirection(Vector2i pos, OthelloDisk disk, Vector2i dir) {
        var next = new Vector2i(pos).add(dir);

        // Expect valid direction
        while (board[next.x][next.y] != disk) {
            board[next.x][next.y] = disk;
            next.add(dir);
        }
    }

    public boolean placeDisk(Vector2i pos) {
        var disk = currentPlayer == OthelloPlayer.BLACK ? OthelloDisk.BLACK : OthelloDisk.WHITE;
        var directions = getValidDirections(pos, disk);

        if (directions.isEmpty()) {
            return false;
        }

        board[pos.x][pos.y] = disk;

        for (var dir : directions) {
            flipDirection(pos, disk, dir);
        }

        return true;
    }

    public boolean isGameOver() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == OthelloDisk.EMPTY) {
                    if (!getValidDirections(new Vector2i(i, j), OthelloDisk.BLACK).isEmpty() ||
                            !getValidDirections(new Vector2i(i, j), OthelloDisk.WHITE).isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean shouldSkipTurn() {
        var disk = currentPlayer == OthelloPlayer.BLACK ? OthelloDisk.BLACK : OthelloDisk.WHITE;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] != OthelloDisk.EMPTY) {
                    continue;
                }
                if (!getValidDirections(new Vector2i(i, j), disk).isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    public OthelloPlayer getWinner() {
        int blackCount = 0;
        int whiteCount = 0;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == OthelloDisk.BLACK) {
                    blackCount++;
                } else if (board[i][j] == OthelloDisk.WHITE) {
                    whiteCount++;
                }
            }
        }

        return blackCount > whiteCount ? OthelloPlayer.BLACK : OthelloPlayer.WHITE;
    }

    public boolean nextByCPU() {
        var disk = currentPlayer == OthelloPlayer.BLACK ? OthelloDisk.BLACK : OthelloDisk.WHITE;
        List<Vector2i> validPositions = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] != OthelloDisk.EMPTY) {
                    continue;
                }
                var directions = getValidDirections(new Vector2i(i, j), disk);
                if (!directions.isEmpty()) {
                    validPositions.add(new Vector2i(i, j));
                }
            }
        }

        if (validPositions.isEmpty()) {
            return true; // Skip turn
        }

        var pos = validPositions.get((int) (Math.random() * validPositions.size()));
        return placeDisk(pos);
    }
}
