package me.f0reach.holofans.lobby.minigame.gomoku;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

public class GomokuMapRenderer extends MapRenderer {
    private final GomokuRenderer parent;
    private final int offsetX;
    private final int offsetY;

    public GomokuMapRenderer(GomokuRenderer parent, int offsetX, int offsetY) {
        this.parent = parent;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        // 盤面の画像を取得
        var image = parent.getBoardImage();
        if (image == null) {
            return; // 画像がまだ生成されていない場合は何もしない
        }

        // 盤面の画像を描画
        canvas.drawImage(offsetX, offsetY, image);
    }
}
