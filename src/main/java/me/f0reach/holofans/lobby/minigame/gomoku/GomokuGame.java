package me.f0reach.holofans.lobby.minigame.gomoku;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2i;

import java.util.*;
import java.util.stream.Collectors;

public class GomokuGame implements CommandExecutor, Listener {
    private GomokuLogic logic;
    private final GomokuConfig config;
    private final Plugin plugin;
    private final GomokuRenderer gridRenderer;

    private final Material BOARD_MATERIAL = Material.BROWN_CONCRETE;
    private final Material EMPTY_MATERIAL = Material.AIR;
    private final Material BLACK_MATERIAL = Material.BLACK_WOOL;
    private final Material WHITE_MATERIAL = Material.WHITE_WOOL;
    private final double ITEM_OFFSET = 0.05; // アイテムの高さオフセット

    private World world;

    // 一時的な変数
    private final List<Player> players;
    private Vector gridBase;
    private double gridSpacing;
    private BoundingBox boardRayTracingBox;
    private final Map<Player, ItemDisplay> placementDisplay;

    public GomokuGame(Plugin plugin) {
        this.plugin = plugin;
        this.config = new GomokuConfig(plugin);
        this.players = new ArrayList<>();
        this.placementDisplay = new HashMap<>();

        // Register command executor
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Objects.requireNonNull(plugin.getServer().getPluginCommand("gomoku")).setExecutor(this);

        this.logic = new GomokuLogic(config.getBoardSize());
        this.gridRenderer = new GomokuRenderer();

        reloadConfig();

        // タイマーを設定して定期的にonTickを呼び出す
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::onPeriod, 0L, 2L);
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
        player.sendMessage(Component.text("盤面の置きたい部分を向くと、候補が表示されます。", TextColor.color(0xFFFFFF)));
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

        placementDisplay.values().forEach(Entity::remove);
        placementDisplay.clear();
    }

    private void stopGame() {
        for (Player player : getVisitingPlayers()) {
            player.sendMessage(Component.text("五目並べが終了されました", TextColor.color(0xFFFFFF)));
        }

        for (Player player : players) {
            if (!player.isOnline()) continue;
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

        gridBase = posMin.clone().add(new Vector(0, 1, 0));
        var gridBlockCount = posMax.getBlockX() - posMin.getBlockX() + 1;
        gridSpacing = gridBlockCount / (config.getBoardSize() + 1.0);

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

        // レイキャスト用のボックスを設定
        var gridEnd = gridBase.clone().add(new Vector(gridBlockCount, ITEM_OFFSET, gridBlockCount));
        boardRayTracingBox = BoundingBox.of(gridBase, gridEnd);

        renderStone();
    }

    private Location getStoneLocation(int x, int z) {
        return gridBase.clone()
                .add(new Vector((x + 1) * gridSpacing, ITEM_OFFSET, (z + 1) * gridSpacing))
                .toLocation(world);
    }

    private Vector2i getGridIndex(Vector location) {
        int xIndex = (int) Math.round(((location.getX() - gridBase.getX()) / gridSpacing)) - 1;
        int zIndex = (int) Math.round(((location.getZ() - gridBase.getZ()) / gridSpacing)) - 1;

        return new Vector2i(xIndex, zIndex);
    }

    private void renderStone() {
        double itemSize = gridSpacing * 0.5;
        var mat = new Matrix4f().scale((float) itemSize, 0.1F, (float) itemSize);
        // 盤面のレンダリングを行う
        for (int x = 0; x < config.getBoardSize(); x++) {
            for (int z = 0; z < config.getBoardSize(); z++) {
                var pos = getStoneLocation(x, z);
                var itemDisplay = world.getNearbyEntities(pos, itemSize, itemSize, itemSize)
                        .stream()
                        .filter(entity -> entity instanceof ItemDisplay)
                        .map(entity -> (ItemDisplay) entity)
                        .findFirst()
                        .orElse(null);

                if (itemDisplay == null) {
                    // アイテムディスプレイが存在しない場合は新規作成
                    itemDisplay = world.spawn(pos, ItemDisplay.class, display -> {
                        display.setTransformationMatrix(mat);
                    });
                }

                switch (logic.getStone(x, z)) {
                    case 1 -> itemDisplay.setItemStack(ItemStack.of(BLACK_MATERIAL));
                    case 2 -> itemDisplay.setItemStack(ItemStack.of(WHITE_MATERIAL));
                    default -> itemDisplay.setItemStack(ItemStack.of(EMPTY_MATERIAL));
                }
            }
        }
    }

    private void renderPlacementDisplay(Player player) {
        if (!logic.isGameStarted() || logic.isGameOver()) return;
        var playerIndex = players.indexOf(player);
        if (playerIndex != logic.getCurrentPlayer() - 1) return;
        if (boardRayTracingBox == null) return;

        var rayTraceResult = boardRayTracingBox.rayTrace(
                player.getEyeLocation().toVector(),
                player.getEyeLocation().getDirection(),
                10.0
        );

        if (rayTraceResult == null) {
            return; // レイキャストがヒットしなかった場合は何もしない
        }

        var gridIndex = getGridIndex(rayTraceResult.getHitPosition());
        if (gridIndex.x() < 0 || gridIndex.x() >= config.getBoardSize() ||
                gridIndex.y() < 0 || gridIndex.y() >= config.getBoardSize()) {
            return; // 範囲外の座標は無視
        }

        var stonePosition = getStoneLocation(gridIndex.x(), gridIndex.y());

        // アイテムディスプレイを取得または作成
        ItemDisplay display = placementDisplay.get(player);
        if (display == null) {
            display = world.spawn(stonePosition, ItemDisplay.class, itemDisplay -> {
                double itemSize = gridSpacing * 0.7;
                var mat = new Matrix4f().scale((float) itemSize, 0.1F, (float) itemSize);
                itemDisplay.setVisibleByDefault(false);
                itemDisplay.setPersistent(false);
                itemDisplay.setTransformationMatrix(mat);
                itemDisplay.setGlowColorOverride(Color.ORANGE);
            });
            placementDisplay.put(player, display);
            player.showEntity(this.plugin, display);
        }

        // アイテムディスプレイの位置を更新
        display.teleport(stonePosition);
        display.setGlowing(true);
        // アイテムディスプレイのアイテムを設定
        if (logic.getStone(gridIndex.x(), gridIndex.y()) != 0) {
            display.setGlowing(false);
            display.setItemStack(ItemStack.of(EMPTY_MATERIAL));
        } else {
            if (logic.getCurrentPlayer() == 1) {
                display.setItemStack(ItemStack.of(BLACK_MATERIAL));
            } else {
                display.setItemStack(ItemStack.of(WHITE_MATERIAL));
            }
        }
    }

    private void onPeriod() {
        // プレイヤーごとに配置表示を更新
        for (Player player : players) {
            if (!player.isOnline() || !player.getWorld().equals(world)) {
                // オフラインまたは異なるワールドの場合は終了
                stopGame();
                return;
            }
            renderPlacementDisplay(player);
        }
    }

    private void handlePlaceStone(Player player) {
        if (!logic.isGameStarted() || logic.isGameOver()) {
            return;
        }

        var rayTraceResult = boardRayTracingBox.rayTrace(
                player.getEyeLocation().toVector(),
                player.getEyeLocation().getDirection(),
                10.0
        );

        if (rayTraceResult == null) {
            return; // レイキャストがヒットしなかった場合は無視
        }

        var gridIndex = getGridIndex(rayTraceResult.getHitPosition());
        var playerIndex = players.indexOf(player);

        if (playerIndex < 0 || playerIndex != logic.getCurrentPlayer() - 1) {
            player.sendActionBar(Component.text("あなたの手番ではありません。", TextColor.color(0xFF0000)));
            return; // プレイヤーの手番でない場合は無視
        }

        // 石を置く処理
        if (!logic.placeStone(gridIndex.x(), gridIndex.y())) {
            player.sendActionBar(Component.text("そこには石を置くことができません。", TextColor.color(0xFF0000)));
            return; // 石を置けない場合は無視
        }

        // 石を置いた後、描画を更新
        renderStone();

        // placementDisplayを見えないように
        var display = placementDisplay.get(player);
        if (display != null) {
            display.setItemStack(ItemStack.of(EMPTY_MATERIAL));
            display.setGlowing(false);
        }
    }

    @EventHandler
    private void onUse(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (!players.contains(player) || !logic.isGameStarted() || logic.isGameOver()) {
            return; // ゲームが開始されていない、または終了している場合は何もしない
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // 右クリックで石を置く
            handlePlaceStone(player);
        }
    }

    @EventHandler
    private void onInteract(PlayerInteractEntityEvent event) {
        var player = event.getPlayer();
        if (!players.contains(player) || !logic.isGameStarted() || logic.isGameOver()) {
            return; // ゲームが開始されていない、または終了している場合は何もしない
        }

        handlePlaceStone(player);
    }
}
