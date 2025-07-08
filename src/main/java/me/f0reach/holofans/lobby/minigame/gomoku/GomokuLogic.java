package me.f0reach.holofans.lobby.minigame.gomoku;

public class GomokuLogic {
    // 盤面の状態を表す2次元配列
    // 0: 空, 1: プレイヤー (黒), 2: CPU/プレイヤー (白)
    private final int[][] board;

    // 現在の手番のプレイヤー
    // 1: プレイヤー (黒), 2: CPU/プレイヤー (白)
    private int currentPlayer;

    // ゲームが終了したかどうか
    private boolean isGameOver;

    // 勝者
    // 0: 勝敗未定, 1: プレイヤー (黒), 2: CPU/プレイヤー (白), 3: 引き分け
    private int winner;

    /**
     * コンストラクタ
     */
    public GomokuLogic(int size) {
        board = new int[size][size];
        currentPlayer = 0; // プレイヤーから開始
        isGameOver = false;
        winner = 0;
    }

    /**
     * 盤面のある位置の石の状態を取得する
     *
     * @param x X座標
     * @param y Y座標
     * @return 0: 空, 1: プレイヤー (黒), 2: CPU/プレイヤー (白)
     */
    public int getStone(int x, int y) {
        if (isValid(x, y)) {
            return board[y][x];
        }
        return -1; // 無効な座標
    }

    /**
     * 盤面の大きさを取得する
     *
     * @return 盤面のサイズ (NxN)
     */
    public int getBoardSize() {
        return board.length;
    }

    /**
     * 現在の手番のプレイヤーを取得する
     *
     * @return 1: プレイヤー, 2: CPU
     */
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * ゲームが終了したかを取得する
     *
     * @return trueなら終了
     */
    public boolean isGameOver() {
        return isGameOver;
    }

    /**
     * 勝者を取得する
     *
     * @return 0: 勝敗未定, 1: プレイヤー, 2: CPU, 3: 引き分け
     */
    public int getWinner() {
        return winner;
    }

    /**
     * 指定された場所に石を置く
     *
     * @param x X座標
     * @param y Y座標
     * @return 石を置けた場合はtrue, それ以外はfalse
     */
    public boolean placeStone(int x, int y) {
        // ゲームが終了しているか、座標が範囲外か、既に石が置かれている場合は置けない
        if (isGameOver || !isValid(x, y) || board[y][x] != 0) {
            return false;
        }

        // 石を置く
        board[y][x] = currentPlayer;

        // 勝敗判定
        if (checkWin(x, y)) {
            isGameOver = true;
            winner = currentPlayer;
        } else if (isBoardFull()) {
            isGameOver = true;
            winner = 3; // 引き分け
        } else {
            // 手番を交代
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
        }

        return true;
    }

    /**
     * 石を置いた場所を中心に、勝利条件を満たしたかを判定する
     *
     * @param x 石を置いたX座標
     * @param y 石を置いたY座標
     * @return 勝利した場合はtrue
     */
    private boolean checkWin(int x, int y) {
        int player = board[y][x];

        // 4方向（縦, 横, 右斜め, 左斜め）をチェック
        // 方向を示すベクトル (dx, dy)
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

        for (int[] dir : directions) {
            int count = 1;
            // 正の方向
            for (int i = 1; i < 5; i++) {
                int nx = x + dir[0] * i;
                int ny = y + dir[1] * i;
                if (isValid(nx, ny) && board[ny][nx] == player) {
                    count++;
                } else {
                    break;
                }
            }
            // 負の方向
            for (int i = 1; i < 5; i++) {
                int nx = x - dir[0] * i;
                int ny = y - dir[1] * i;
                if (isValid(nx, ny) && board[ny][nx] == player) {
                    count++;
                } else {
                    break;
                }
            }
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }

    /**
     * 盤面が全て埋まったか（引き分け）を判定する
     *
     * @return 全て埋まっていればtrue
     */
    private boolean isBoardFull() {
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board.length; x++) {
                if (board[y][x] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 座標が盤面の範囲内かを確認する
     *
     * @param x X座標
     * @param y Y座標
     * @return 範囲内ならtrue
     */
    private boolean isValid(int x, int y) {
        return x >= 0 && x < board.length && y >= 0 && y < board.length;
    }

    public void resetGame() {
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board.length; x++) {
                board[y][x] = 0; // 全てのセルを空にする
            }
        }
        currentPlayer = 0; // プレイヤーから開始
        isGameOver = false;
        winner = 0; // 勝者なし
    }

    public void startGame() {
        resetGame();
        isGameOver = false;
        currentPlayer = 1; // プレイヤーから開始
    }

    public boolean isGameStarted() {
        return !isGameOver && (currentPlayer == 1 || currentPlayer == 2);
    }
}
