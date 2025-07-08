package me.f0reach.holofans.lobby.minigame.gomoku;

import java.util.*;

public class SimpleCpu {
    private final GomokuLogic game;
    private final int cpuPlayerId;
    private final int humanPlayerId;
    private final Random random = new Random();

    public SimpleCpu(GomokuLogic game, int cpuPlayerId) {
        this.game = game;
        this.cpuPlayerId = cpuPlayerId;
        this.humanPlayerId = (cpuPlayerId == 1) ? 2 : 1;
    }

    /**
     * CPUの手を決定する（最適化版）
     *
     * @return {x, y} 座標の配列
     */
    public int[] findBestMove() {
        var boardSize = game.getBoardSize();
        var board = new int[boardSize][boardSize];
        // 盤面の状態を複製
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                board[y][x] = game.getStone(x, y);
            }
        }

        // 評価対象となる空きマスリストを取得
        Set<Integer> candidateMoves = findCandidateMoves(board);

        // 盤面に一つも石がない場合（初手）は、中央に置く
        if (candidateMoves.isEmpty()) {
            int center = boardSize / 2;
            return new int[]{center, center};
        }

        // 1. CPUが勝てる手を探す
        int[] winMove = findWinningMove(board, cpuPlayerId, candidateMoves);
        if (winMove != null) {
            return winMove;
        }

        // 2. プレイヤーが勝つ手（リーチ）を阻止する
        int[] blockMove = findWinningMove(board, humanPlayerId, candidateMoves);
        if (blockMove != null) {
            return blockMove;
        }

        // 3. スコアリングに基づいて最適な手を見つける
        return findMoveByScoring(board, candidateMoves);
    }

    /**
     * 評価対象となる候補手（いずれかの石に隣接する空きマス）のリストを作成する
     *
     * @param board 盤面
     * @return 候補となるマスのSet (キー: y * BOARD_SIZE + x)
     */
    private Set<Integer> findCandidateMoves(int[][] board) {
        Set<Integer> candidates = new HashSet<>();
        int boardSize = game.getBoardSize();

        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                // 石が置かれているマスは評価対象外
                if (board[y][x] == 0) continue;

                // 石の周囲8マスをチェック
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;

                        int ny = y + dy;
                        int nx = x + dx;

                        if (isValid(nx, ny) && board[ny][nx] == 0) {
                            candidates.add(ny * boardSize + nx);
                        }
                    }
                }
            }
        }
        return candidates;
    }


    /**
     * 特定のプレイヤーが勝てる（5つ並ぶ）マスを探す
     *
     * @param board      盤面
     * @param playerId   プレイヤーID
     * @param candidates 評価対象のマス
     * @return 見つかった場合は {x, y} 座標、見つからなければ null
     */
    private int[] findWinningMove(int[][] board, int playerId, Set<Integer> candidates) {
        int boardSize = game.getBoardSize();
        for (int moveKey : candidates) {
            int y = moveKey / boardSize;
            int x = moveKey % boardSize;

            // 仮に石を置いてみる
            board[y][x] = playerId;
            if (checkWinCondition(board, x, y, playerId)) {
                board[y][x] = 0; // 元に戻す
                return new int[]{x, y};
            }
            board[y][x] = 0; // 元に戻す
        }
        return null;
    }

    /**
     * スコアリングに基づいて最適な手を見つける
     */
    private int[] findMoveByScoring(int[][] board, Set<Integer> candidates) {
        int bestScore = -1;
        List<int[]> bestMoves = new ArrayList<>();
        int boardSize = game.getBoardSize();

        for (int moveKey : candidates) {
            int y = moveKey / boardSize;
            int x = moveKey % boardSize;

            int score = calculateScore(board, x, y);
            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(new int[]{x, y});
            } else if (score == bestScore) {
                bestMoves.add(new int[]{x, y});
            }
        }

        // 最高スコアのマスが複数ある場合はランダムに1つ選ぶ
        if (!bestMoves.isEmpty()) {
            return bestMoves.get(random.nextInt(bestMoves.size()));
        }

        // 万が一候補がない場合（通常は発生しない）、ランダムな手
        return findRandomMove(board);
    }

    /**
     * 指定したマスのスコアを計算する
     *
     * @param board 盤面
     * @param x     X座標
     * @param y     Y座標
     * @return スコア
     */
    private int calculateScore(int[][] board, int x, int y) {
        // 自分の手を評価するスコアと、相手の手を妨害するスコアを合算
        // 相手の妨害を少し高く評価するため、重みを1.2に設定
        return evaluatePosition(board, x, y, cpuPlayerId) +
                (int) (evaluatePosition(board, x, y, humanPlayerId) * 1.2);
    }

    /**
     * あるマスに石を置いた場合の、4方向の連続性を評価する
     */
    private int evaluatePosition(int[][] board, int x, int y, int playerId) {
        int totalScore = 0;
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

        // 仮に石を置いて評価
        board[y][x] = playerId;

        for (int[] dir : directions) {
            // 自分の石が何個連続するか
            int count = countConsecutiveStones(board, x, y, playerId, dir);
            // 連続数に応じたスコアを加算 (例: 4つ並びは非常に高く評価)
            totalScore += getScoreForCount(count);
        }

        // 盤面を元に戻す
        board[y][x] = 0;
        return totalScore;
    }

    /**
     * 連続した石の数を数える
     */
    private int countConsecutiveStones(int[][] board, int x, int y, int playerId, int[] dir) {
        int count = 0;
        // 正の方向
        for (int i = 1; i < 5; i++) {
            int nx = x + dir[0] * i;
            int ny = y + dir[1] * i;
            if (isValid(nx, ny) && board[ny][nx] == playerId) count++;
            else break;
        }
        // 負の方向
        for (int i = 1; i < 5; i++) {
            int nx = x - dir[0] * i;
            int ny = y - dir[1] * i;
            if (isValid(nx, ny) && board[ny][nx] == playerId) count++;
            else break;
        }
        return count;
    }

    /**
     * 連続数に応じたスコアを返す
     */
    private int getScoreForCount(int count) {
        switch (count) {
            case 0:
                return 1;    // リーチ(2)を作る
            case 1:
                return 5;    // リーチ(3)を作る
            case 2:
                return 25;   // リーチ(4)を作る (三)
            case 3:
                return 1000; // ダブルリーチ(四)を作る
            default:
                return 10000; // 五を作る
        }
    }


    /**
     * 勝利条件を満たしているか簡易チェック（findWinningMove用）
     */
    private boolean checkWinCondition(int[][] board, int x, int y, int playerId) {
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            if (countConsecutiveStones(board, x, y, playerId, dir) >= 4) {
                return true;
            }
        }
        return false;
    }

    /**
     * ランダムな空いているマスを見つける（フォールバック用）
     */
    private int[] findRandomMove(int[][] board) {
        List<int[]> emptyCells = new ArrayList<>();
        for (int y = 0; y < game.getBoardSize(); y++) {
            for (int x = 0; x < game.getBoardSize(); x++) {
                if (board[y][x] == 0) {
                    emptyCells.add(new int[]{x, y});
                }
            }
        }
        if (!emptyCells.isEmpty()) {
            return emptyCells.get(random.nextInt(emptyCells.size()));
        }
        return null;
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && x < game.getBoardSize() && y >= 0 && y < game.getBoardSize();
    }
}