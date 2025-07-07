package me.f0reach.holofans.lobby.minigame.gomoku;

import java.awt.*;
import java.awt.image.BufferedImage;

public class GomokuRenderer {
    // --- 設定値 ---
    private int imageSize = 384; // 画像のサイズ (ピクセル)
    private int gridCount = 19;  // 格子の数 (19x19)
    private Color boardColor = new Color(222, 184, 135); // 碁盤の色
    private Color lineColor = new Color(0, 0, 0); // 格子の色

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
     */
    public void createBoardImage() {
        // 画像を生成
        this.boardImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) this.boardImage.getGraphics();

        // アンチエイリアス設定
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // 碁盤の描画
        drawBoard(g2d);

        // グラフィックスを解放
        g2d.dispose();
    }

    /**
     * 碁盤（背景、格子、星）を描画します。
     *
     * @param g2d Graphics2Dオブジェクト
     */
    private void drawBoard(Graphics2D g2d) {
        // 盤面の余白と格子1マスのサイズを計算
        double cellSize = (double) imageSize / (gridCount + 1);
        int startPos = (int) cellSize;
        int endPos = (int) (cellSize * gridCount);

        // 背景を塗りつぶし
        g2d.setColor(boardColor);
        g2d.fillRect(0, 0, imageSize, imageSize);

        // 格子を描画
        g2d.setColor(lineColor);
        g2d.setStroke(new BasicStroke(1));
        for (int i = 1; i <= gridCount; i++) {
            int pos = (int) (cellSize * i);
            g2d.drawLine(pos, startPos, pos, endPos); // 垂直線
            g2d.drawLine(startPos, pos, endPos, pos); // 水平線
        }
    }
}
