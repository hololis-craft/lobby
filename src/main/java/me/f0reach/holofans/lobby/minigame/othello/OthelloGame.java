package me.f0reach.holofans.lobby.minigame.othello;

import de.tr7zw.nbtapi.NBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OthelloGame implements CommandExecutor, Listener {
    private final Plugin plugin;
    private final OthelloLogic logic = new OthelloLogic();
    private final OthelloConfig config;
    private final Map<Player, OthelloPlayer> players = new HashMap<>();
    private boolean firstPlaced = false;

    private static final Material BLACK_DISK = Material.BLACK_CARPET;
    private static final Material WHITE_DISK = Material.WHITE_CARPET;

    public OthelloGame(Plugin plugin) {
        this.plugin = plugin;
        config = new OthelloConfig(plugin);
        config.load();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginCommand("othello").setExecutor(this);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkExit, 0, 20 * 5);
    }

    private boolean hasTwoPlayers() {
        return players.size() == 2;
    }

    private boolean hasNoPlayers() {
        return players.isEmpty();
    }

    private Vector2i intoBoardPos(Vector pos) {
        var isIncreaseX = config.getPos1().getBlockX() < config.getPos2().getBlockX();
        var isIncreaseZ = config.getPos1().getBlockZ() < config.getPos2().getBlockZ();
        var x = (int) Math.floor(pos.getX());
        var y = (int) Math.floor(pos.getZ());
        var vec = new Vector2i(Math.abs(x - config.getPos1().getBlockX()) / 2,
                Math.abs(y - config.getPos1().getBlockZ()) / 2);
        if (!isIncreaseX) {
            vec = new Vector2i(7 - vec.x(), vec.y());
        }
        if (!isIncreaseZ) {
            vec = new Vector2i(vec.x(), 7 - vec.y());
        }

        return vec;
    }

    private Vector fromBoardPos(Vector2i pos) {
        var isIncreaseX = config.getPos1().getBlockX() < config.getPos2().getBlockX();
        var isIncreaseZ = config.getPos1().getBlockZ() < config.getPos2().getBlockZ();
        return new Vector(
                config.getPos1().getBlockX() + pos.x() * 2 * (isIncreaseX ? 1 : -1),
                config.getPos1().getBlockY(),
                config.getPos1().getBlockZ() + pos.y() * 2 * (isIncreaseZ ? 1 : -1)
        );
    }

    // Messages
    private void sendGameStartChoice(Player player) {
        player.sendMessage(
                Component.text("オセロを開始しますか？ ")
                        .append(Component.text("[はい]", TextColor.color(0x00FF00))
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/othello start"))
                                .hoverEvent(HoverEvent.showText(Component.text("ゲームを開始します")))
                        )
        );
    }

    private void sendGameEndChoice(Player player) {
        player.sendMessage(
                Component.text("オセロを終了しますか？ ")
                        .append(Component.text("[はい]", TextColor.color(0xFF0000))
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/othello end"))
                                .hoverEvent(HoverEvent.showText(Component.text("ゲームを終了します")))
                        )
        );
    }

    private void sendGameStartMessage() {
        if (players.size() != 1 && players.size() != 2) {
            return;
        }

        var isCPU = players.size() == 1;
        var playerVs = players.keySet().stream().map(Player::getName).collect(Collectors.joining(" vs "));
        var title = Title.title(
                Component.text("オセロ", TextColor.color(0xFFFFFF)),
                Component.text(isCPU ? playerVs + " vs CPU" : playerVs)
        );
        for (var player : players.keySet()) {
            player.showTitle(title);
        }
    }

    private void sendInterruptMessage(Player player) {
        player.sendMessage(Component.text("ゲームが中断されました"));
    }

    private void sendOpponentEndedMessage(Player player) {
        player.sendMessage(Component.text("相手がゲームを終了しました、引き続きCPUとプレイすることもできます"));
    }

    private void sendNotYourTurnMessage(Player player) {
        player.sendMessage(Component.text("あなたのターンではありません"));
    }

    private void sendTurnSkipMessage(Player player) {
        if (players.containsKey(player)) {
            var isOpponent = players.get(player) != logic.getCurrentPlayer();
            player.sendMessage(Component.text(isOpponent ? "相手のターンをスキップしました" : "あなたのターンをスキップしました"));
        }
    }

    private void sendInvalidPlacementMessage(Player player) {
        var title = Title.title(
                Component.empty(),
                Component.text("そこにはディスクを置けません")
        );
        player.showTitle(title);
    }

    private void sendNotifyTurn(Player player) {
        if (!players.containsKey(player)) return;
        var opponent = players.keySet().stream().filter(p -> p != player).findFirst().orElse(null);
        var opponentName = opponent != null ? opponent.getName() : "CPU";
        var turn = logic.getCurrentPlayer() == players.get(player) ? "あなたのターン" : opponentName + "のターン";
        var title = Title.title(
                Component.empty(),
                Component.text(turn)
        );
        player.showTitle(title);
    }

    // Items
    private ItemStack getDiskBlock(OthelloPlayer player) {
        var diskBlock = player == OthelloPlayer.BLACK ? BLACK_DISK : WHITE_DISK;
        var diskName = player == OthelloPlayer.BLACK ? "黒" : "白";
        var itemStack = ItemStack.of(diskBlock, 1);
        itemStack.editMeta(meta -> {
            meta.displayName(Component.text(diskName + "のディスク"));
            meta.lore(List.of(Component.text(diskName)));
            meta.setEnchantmentGlintOverride(true);
        });
        NBT.modifyComponents(itemStack, nbt -> {
            // Can be placed on concrete
            var predicates = nbt.getOrCreateCompound("minecraft:can_place_on")
                    .getCompoundList("predicates");
            predicates.addCompound().setString("blocks", "minecraft:green_concrete");
            // predicates.addCompound().setString("blocks", "minecraft:lime_terracotta");
        });
        return itemStack;
    }

    private void giveDiskBlock(Player player) {
        var playerDisk = players.get(player);
        var itemStack = getDiskBlock(playerDisk);
        player.getInventory().addItem(itemStack);
    }

    private void clearDiskBlock(Player player) {
        var removeList = new LinkedList<ItemStack>();
        player.getInventory().forEach(stack -> {
            var isDisk = stack != null &&
                    (stack.getType() == BLACK_DISK ||
                            stack.getType() == WHITE_DISK) &&
                    stack.getItemMeta().hasEnchantmentGlintOverride();
            if (isDisk) {
                removeList.add(stack);
            }
        });
        removeList.forEach(stack -> player.getInventory().remove(stack));
    }

    // Renderer
    private void renderBoard(World world) {
        for (var x = 0; x < 8; x++) {
            for (var y = 0; y < 8; y++) {
                var disk = logic.getDisk(x, y);
                var diskBlock = disk == OthelloDisk.BLACK ? BLACK_DISK :
                        disk == OthelloDisk.WHITE ? WHITE_DISK : Material.AIR;
                var blockPos = fromBoardPos(new Vector2i(x, y));
                var block = world.getBlockAt(blockPos.toLocation(world));
                block.setType(diskBlock);
            }
        }
    }

    // BBox
    private boolean isPlayerInGameArea(Player player) {
        return player.getWorld().getName().equals(config.getWorld()) && config.getBoundingBox().contains(player.getLocation().toVector());
    }

    // Logic
    private void nextTurn() {
        logic.nextTurn();
        if (logic.shouldSkipTurn()) {
            for (var p : players.keySet()) {
                sendTurnSkipMessage(p);
                sendNotifyTurn(p);
            }
            logic.nextTurn();
        }

        if (logic.isGameOver()) {
            var winner = logic.getWinner();
            var message = winner == OthelloPlayer.BLACK ? "黒" : "白";
            var winnerName = players.entrySet().stream()
                    .filter(e -> e.getValue() == winner)
                    .map(Map.Entry::getKey)
                    .map(Player::getName)
                    .findFirst()
                    .orElse("CPU");
            message += "(" + winnerName + ") の勝利です";
            var title = Title.title(
                    Component.text("ゲーム終了", TextColor.color(0xFFFFFF)),
                    Component.text(message)
            );
            for (var p : players.keySet()) {
                p.sendMessage(Component.text(message));
                p.showTitle(title);
                clearDiskBlock(p);
            }
            players.clear();
            firstPlaced = false;
        }
    }

    // CPU
    private void cpuTurn() {
        if (hasTwoPlayers()) return;
        var shouldRunCPU = players.size() == 1 && logic.getCurrentPlayer() != players.get(players.keySet().iterator().next());
        if (!shouldRunCPU) return;

        logic.nextByCPU();
        nextTurn();

        for (var p : players.keySet()) {
            sendNotifyTurn(p);
        }
    }

    // Auto exit
    private void checkExit() {
        var world = plugin.getServer().getWorld(config.getWorld());
        if (world == null) return;
        int players = world.getNearbyPlayers(
                config.getBoundingBox().getCenter().toLocation(world),
                30
        ).size();

        if (players == 0) {
            this.players.clear();
            firstPlaced = false;
        }
    }

    // When player enters the game bounding box, start the game
    @EventHandler
    public void onPlayerMoveEvent(@NotNull PlayerMoveEvent event) {
        var player = event.getPlayer();
        if (!player.getWorld().getName().equals(config.getWorld())) {
            return;
        }

        var isInside = config.getBoundingBox().contains(event.getTo().toVector());
        var wasInside = config.getBoundingBox().contains(event.getFrom().toVector());

        if (players.containsKey(player)) {
            if (!isInside && wasInside) {
                // Player left the game bounding box
                sendGameEndChoice(player);
            }
            return;
        }

        if (!isInside) return;

        var canJoinGame = !hasTwoPlayers();
        if (!wasInside && canJoinGame) {
            sendGameStartChoice(player);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(commandSender instanceof Player player)) {
            return false;
        }

        if (strings.length == 0) {
            return false;
        }

        if (strings[0].equalsIgnoreCase("start")) {
            if (hasTwoPlayers() || firstPlaced) {
                player.sendMessage(Component.text("すでにゲームが進行しています"));
                return true;
            }

            if (players.containsKey(player)) {
                player.sendMessage(Component.text("すでにゲームに参加しています"));
                return true;
            }

            // Check bounding box
            if (!isPlayerInGameArea(player)) {
                player.sendMessage(Component.text("ゲームエリア外です"));
                return true;
            }

            if (hasNoPlayers()) {
                firstPlaced = false;
                logic.reset();
            }

            var disk = players.size() == 1 ? OthelloPlayer.WHITE : OthelloPlayer.BLACK;

            players.put(player, disk);
            giveDiskBlock(player);
            sendGameStartMessage();
            renderBoard(player.getWorld());
            return true;
        }

        if (strings[0].equalsIgnoreCase("end")) {
            if (!players.containsKey(player)) {
                player.sendMessage(Component.text("ゲームに参加していません"));
                return true;
            }

            players.remove(player);
            clearDiskBlock(player);
            sendInterruptMessage(player);
            if (!hasNoPlayers()) {
                sendOpponentEndedMessage(players.keySet().iterator().next());
            }
            return true;
        }

        return false;
    }

    // Place disk event
    @EventHandler
    public void onPlaceBlockEvent(@NotNull BlockPlaceEvent event) {
        var player = event.getPlayer();
        if (!players.containsKey(player)) return;
        if (!isPlayerInGameArea(player)) return;

        if (event.getBlock().getType() != WHITE_DISK && event.getBlock().getType() != BLACK_DISK) {
            return;
        }

        event.setCancelled(true);

        var playerDisk = players.get(player);
        if (playerDisk != logic.getCurrentPlayer()) {
            sendNotYourTurnMessage(player);
            return;
        }

        var disk = playerDisk == OthelloPlayer.BLACK ? OthelloDisk.BLACK : OthelloDisk.WHITE;
        var expectedDisk = event.getBlock().getType() == BLACK_DISK ? OthelloDisk.BLACK : OthelloDisk.WHITE;

        if (disk != expectedDisk) {
            return;
        }

        var pos = intoBoardPos(event.getBlock().getLocation().toVector());
        if (logic.getDisk(pos.x(), pos.y()) != OthelloDisk.EMPTY) {
            return;
        }

        var placed = logic.placeDisk(pos);
        if (!placed) {
            sendInvalidPlacementMessage(player);
            renderBoard(player.getWorld());
            return;
        }

        event.setCancelled(false);

        nextTurn();

        // Check if the game is over
        if (logic.isGameOver()) {
            return;
        }

        for (var p : players.keySet()) {
            sendNotifyTurn(p);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cpuTurn();
            renderBoard(player.getWorld());
        }, 20 * 2L);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            giveDiskBlock(player);
            renderBoard(player.getWorld());
        });
    }
}
