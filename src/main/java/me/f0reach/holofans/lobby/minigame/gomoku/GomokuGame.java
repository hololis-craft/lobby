package me.f0reach.holofans.lobby.minigame.gomoku;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GomokuGame implements CommandExecutor, Listener {
    private GomokuLogic logic;
    private final GomokuConfig config;
    private final Plugin plugin;
    private final GomokuRenderer gridRenderer;

    private final Material BOARD_MATERIAL = Material.BROWN_CONCRETE;
    private final Material EMPTY_MATERIAL = Material.GLASS;
    private final Material BLACK_MATERIAL = Material.BLACK_WOOL;
    private final Material WHITE_MATERIAL = Material.WHITE_WOOL;

    private final List<Player> players;

    private World world;

    public GomokuGame(Plugin plugin) {
        this.plugin = plugin;
        this.config = new GomokuConfig(plugin);
        this.players = new ArrayList<>();

        // Register command executor
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Objects.requireNonNull(plugin.getServer().getPluginCommand("gomoku")).setExecutor(this);

        this.logic = new GomokuLogic(config.getBoardSize());
        this.gridRenderer = new GomokuRenderer();

        reloadConfig();
    }

    public void reloadConfig() {
        if (!players.isEmpty()) {
            stopGame();
        }

        config.load();
        if (!config.isValid()) {
            plugin.getLogger().severe("Gomoku configuration is invalid. Please check your config.yml.");
            return;
        }
        this.world = plugin.getServer().getWorld(config.getWorld());
        this.logic = new GomokuLogic(config.getBoardSize());
        this.players.clear();

        plugin.getLogger().info("Gomoku configuration reloaded successfully.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }
        if (command.getName().equalsIgnoreCase("gomoku")) {
            if (args.length == 0) {
                startGame(player);
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
                resetGame();
                player.sendMessage(Component.text("ゲームをリセットしました。", TextColor.color(0xFFFFFF)));
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {
                stopGame();
                return true;
            }
        }
        return false;
    }

    private List<Player> getVisitingPlayers() {
        var center = config.getPosMin().clone()
                .add(config.getPosMax().clone())
                .multiply(0.5);
        var playableArea = BoundingBox.of(center, 15, 15, 15);
        return world.getPlayers().stream()
                .filter(player -> playableArea.contains(player.getLocation().toVector()))
                .collect(Collectors.toList());
    }

    private void sendGameStartMessage() {
        var isCPU = players.size() == 1;
        var playerText = players.stream().map(Player::getName).collect(Collectors.joining(" vs "));
        var playerVs = isCPU ? playerText + " vs CPU" : playerText;
        var title = Title.title(
                Component.text("五目並べ", TextColor.color(0xFFFFFF)),
                Component.text(playerVs)
        );

        for (Player player : getVisitingPlayers()) {
            player.showTitle(title);
        }
    }

    private void sendGuide(Player player) {
        player.sendMessage(Component.text("盤面の置きたい部分を向くと、候補のパーティクルが表示されます。", TextColor.color(0xFFFFFF)));
        player.sendMessage(Component.text("その状態で、右クリック・使用すると石を置けます。", TextColor.color(0xFFFFFF)));
        player.sendMessage(Component.text("石を置くと、次のプレイヤーに手番が移ります。", TextColor.color(0xFFFFFF)));
        player.sendMessage(Component.text("禁じ手はありません。", TextColor.color(0xFFFFFF)));
    }

    private void startGame(Player p) {
        if (players.contains(p)) return;

        if (players.size() < 2 && !logic.isGameStarted()) {
            players.add(p);
        } else {
            p.sendMessage("すでにゲームが進行しています。");
            return;
        }

        logic.startGame();

        sendGuide(p);
        sendGameStartMessage();
        prepareBoard();
    }

    private void resetGame() {
        logic.resetGame();
        players.clear();
        // renderBoard();
    }

    private void stopGame() {
        for (Player player : getVisitingPlayers()) {
            player.sendMessage(Component.text("五目並べが終了されました", TextColor.color(0xFFFFFF)));
        }

        for (Player player : players) {
            player.sendMessage(Component.text("ゲームが終了しました。", TextColor.color(0xFFFFFF)));
        }

        resetGame();
    }

    /* 盤面の構造
     * - posMin, posMaxで盤面の土台となるブロック座標が指定される
     * - 盤面の模様（格子）は土台の直上のitem_frameに描画される
     * - 配置可能なポイントは、格子上の各点にある、scaleが変更されたitem_frameに描画される
     */
    private void prepareBoard() {
        if (world == null) {
            plugin.getLogger().warning("World is not set. Cannot prepare board.");
            return;
        }

        // 盤面の土台を設置
        var posMin = config.getPosMin();
        var posMax = config.getPosMax();

        var gridBase = posMin.clone().add(new Vector(0, 1, 0));
        var gridBlockCount = posMax.getBlockX() - posMin.getBlockX() + 1;
        double gridSpacing = gridBlockCount / (config.getBoardSize() + 1.0);

        for (int x = posMin.getBlockX(); x <= posMax.getBlockX(); x++) {
            for (int z = posMin.getBlockZ(); z <= posMax.getBlockZ(); z++) {
                world.getBlockAt(x, posMin.getBlockY(), z).setType(BOARD_MATERIAL);
            }
        }

        // 既存のItemDisplayを削除
        var boundingBox = BoundingBox.of(posMin, posMax).expand(1, 2, 1);
        world.getNearbyEntities(boundingBox).stream()
                .filter(entity -> entity instanceof ItemDisplay)
                .forEach(Entity::remove);

        // アイテムフレーム用地図の準備
        gridRenderer.setImageSize(128 * gridBlockCount);
        gridRenderer.setGridCount(config.getBoardSize());
        gridRenderer.createBoardImage();

        // 格子のアイテムフレームを設置
        for (int x = 0; x < gridBlockCount; x++) {
            for (int z = 0; z < gridBlockCount; z++) {
                // アイテムフレームの位置を計算
                var location = gridBase.clone()
                        .add(new Vector(x + 0.5, 0, z + 0.5))
                        .toLocation(world);
                // アイテムフレームを取得
                var itemFrame = world.getNearbyEntities(location, 0.1, 0.1, 0.1)
                        .stream()
                        .filter(entity -> entity instanceof ItemFrame)
                        .map(entity -> (ItemFrame) entity)
                        .findFirst()
                        .orElse(null);

                // アイテムフレームが存在し、地図が設定されている場合は再利用
                if (itemFrame != null && itemFrame.getItem().getType() == Material.FILLED_MAP) {
                    // 既存のアイテムフレームを再利用
                    itemFrame.setVisibleByDefault(true);
                    itemFrame.setFixed(true);
                } else {
                    // アイテムフレームが存在しない場合は新規作成
                    itemFrame = world.spawn(location, ItemFrame.class, frame -> {
                        frame.setVisibleByDefault(true);
                        frame.setFixed(true);
                        frame.setItemDropChance(0.0F);
                    });
                    var mapItem = ItemStack.of(Material.FILLED_MAP, 1);
                    // アイテムフレームに地図を設定
                    itemFrame.setItem(mapItem);
                }

                // アイテムフレームに設定された地図を確認
                var mapMeta = (MapMeta) itemFrame.getItem().getItemMeta();

                if (!mapMeta.hasMapView()) {
                    // 空の地図を作成
                    var emptyMapView = plugin.getServer().createMap(world);
                    mapMeta.setMapView(emptyMapView);
                }

                var mapView = Objects.requireNonNull(mapMeta.getMapView());
                // 地図のレンダラを指定
                mapView.getRenderers().forEach(mapView::removeRenderer);
                mapView.addRenderer(gridRenderer.createMapRenderer(x, z));
                // 地図をアイテムとしての地図に設定

                // アイテムに地図設定を書き戻す
                itemFrame.getItem().setItemMeta(mapMeta);
            }
        }

        // 配置可能なポイントのアイテムフレームを設置

        double itemSize = gridSpacing * 0.5;
        double itemOffset = 0.05; // アイテムの高さオフセット
        var mat = new Matrix4f().scale((float) itemSize, 0.1F, (float) itemSize);
        for (int i = 1; i <= config.getBoardSize(); i++) {
            for (int j = 1; j <= config.getBoardSize(); j++) {
                var pos = gridBase.clone()
                        .add(new Vector(i * gridSpacing, itemOffset, j * gridSpacing));
                world.spawn(pos.toLocation(world), ItemDisplay.class, itemDisplay -> {
                    itemDisplay.setItemStack(ItemStack.of(EMPTY_MATERIAL));
                    itemDisplay.setTransformationMatrix(mat);
                    itemDisplay.setPersistent(true);
                    itemDisplay.setVisibleByDefault(true);
                });
            }
        }
    }
}
