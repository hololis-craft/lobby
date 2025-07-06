package me.f0reach.holofans.lobby.minigame.gomoku;

import java.awt.*;
import java.awt.image.BufferedImage;

public class GomokuRenderer {
    // --- 設定値 ---
    private int imageSize = 384; // 画像のサイズ (ピクセル)
    private int gridCount = 19;  // 格子の数 (19x19)
    private int margin = 24; // 盤面の余白 (ピクセル)
    private Color boardColor = new Color(222, 184, 135); // 碁盤の色
    private Color lineColor = new Color(139, 69, 19); // 格子の色

    // 石の定義 (0:なし, 1:黒, 2:白)
    public static final int EMPTY = 0;
    public static final int BLACK_STONE = 1;
    public static final int WHITE_STONE = 2;

    // --- 状態 ---
    private Image boardImage; // 生成された盤面の画像

    /**
     * GomokuRendererのコンストラクタ
     */
    public GomokuRenderer() {
        // 初期化処理が必要な場合はここに記述
    }

    /**
     * 盤面の画像サイズを設定します。
     *
     * @param size 画像のサイズ (ピクセル)
     */
    public void setImageSize(int size) {
        this.imageSize = size;
    }

    /**
     * 格子の数を設定します。
     *
     * @param count 格子の数 (例: 19)
     */
    public void setGridCount(int count) {
        this.gridCount = count;
    }

    /**
     * 盤面の余白を設定します。
     *
     * @param margin 盤面の余白 (ピクセル)
     */
    public void setMargin(int margin) {
        this.margin = margin;
    }

    /**
     * 盤面の色を設定します。
     *
     * @param color 盤面の色
     */
    public void setBoardColor(Color color) {
        this.boardColor = color;
    }

    /**
     * 格子の色を設定します。
     *
     * @param color 格子の色
     */
    public void setLineColor(Color color) {
        this.lineColor = color;
    }

    /**
     * 盤面の画像を取得します。
     *
     * @return 盤面の画像
     */
    public Image getBoardImage() {
        return boardImage;
    }

    /**
     * 盤面の状態を更新します。
     *
     * @param positions 石の配置情報を持つ2次元配列
     */
    public void update(int[][] positions) {
        // 盤面の画像を生成
        this.boardImage = createBoardImage(positions);
    }

    /**
     * 盤面のレンダラーを生成します。
     *
     * @param offsetX X方向のオフセット
     * @param offsetY Y方向のオフセット
     * @return 生成されたGomokuMapRenderer
     */
    public GomokuMapRenderer createMapRenderer(int offsetX, int offsetY) {
        return new GomokuMapRenderer(this, offsetX, offsetY);
    }

    /**
     * 盤面の画像を生成します。
     *
     * @param positions 石の配置情報を持つ2次元配列
     * @return 生成された画像
     */
    public Image createBoardImage(int[][] positions) {
        // 盤面のサイズと格子の数をチェック
        if (positions == null || positions.length != gridCount || positions[0].length != gridCount) {
            throw new IllegalArgumentException("Invalid positions array. Must be a " + gridCount + "x" + gridCount + " array.");
        }

        // 画像を生成
        Image image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();

        // アンチエイリアス設定
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // 碁盤の描画
        drawBoard(g2d);

        // 石の描画
        drawStones(g2d, positions);

        // グラフィックスを解放
        g2d.dispose();

        return image;
    }

    /**
     * 碁盤（背景、格子、星）を描画します。
     *
     * @param g2d Graphics2Dオブジェクト
     */
    private void drawBoard(Graphics2D g2d) {
        // 盤面の余白と格子1マスのサイズを計算
        int boardArea = imageSize - (margin * 2);
        int cellSize = boardArea / (gridCount - 1);

        // 背景を塗りつぶし
        g2d.setColor(boardColor);
        g2d.fillRect(0, 0, imageSize, imageSize);

        // 格子を描画
        g2d.setColor(lineColor);
        g2d.setStroke(new BasicStroke(1));
        for (int i = 0; i < gridCount; i++) {
            int pos = margin + i * cellSize;
            // 縦線
            g2d.drawLine(pos, margin, pos, imageSize - margin);
            // 横線
            g2d.drawLine(margin, pos, imageSize - margin, pos);
        }
    }

    /**
     * 碁石を描画します。
     *
     * @param g2d       Graphics2Dオブジェクト
     * @param positions 石の配置情報を持つ2次元配列
     */
    private void drawStones(Graphics2D g2d, int[][] positions) {
        int boardArea = imageSize - (margin * 2);
        int cellSize = boardArea / (gridCount - 1);
        int stoneSize = cellSize - 2; // 石の直径

        for (int x = 0; x < gridCount; x++) {
            for (int y = 0; y < gridCount; y++) {
                int stoneType = positions[x][y];
                if (stoneType == EMPTY) {
                    continue;
                }

                int cx = margin + x * cellSize;
                int cy = margin + y * cellSize;
                int stoneX = cx - stoneSize / 2;
                int stoneY = cy - stoneSize / 2;

                if (stoneType == BLACK_STONE) {
                    g2d.setColor(Color.BLACK);
                    g2d.fillOval(stoneX, stoneY, stoneSize, stoneSize);
                } else if (stoneType == WHITE_STONE) {
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(stoneX, stoneY, stoneSize, stoneSize);
                    // 白石には見やすくするために黒い縁を描画
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawOval(stoneX, stoneY, stoneSize, stoneSize);
                }
            }
        }
    }
}
