package me.f0reach.holofans.lobby.minigame.gomoku;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GomokuGame implements CommandExecutor, Listener {
    private GomokuLogic logic;
    private final GomokuConfig config;
    private final Plugin plugin;

    private final Material EMPTY_MATERIAL = Material.BARRIER;
    private final Material BLACK_MATERIAL = Material.BLACK_WOOL;
    private final Material WHITE_MATERIAL = Material.WHITE_WOOL;

    private final List<Player> players;

    private World world;

    public GomokuGame(Plugin plugin) {
        this.plugin = plugin;
        this.config = new GomokuConfig(plugin);
        this.players = new ArrayList<>();
        if (!config.isValid()) {
            plugin.getLogger().severe("Gomoku configuration is invalid. Please check your config.yml.");
            return;
        }

        // Register command executor
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Objects.requireNonNull(plugin.getServer().getPluginCommand("gomoku")).setExecutor(this);

        this.world = plugin.getServer().getWorld(config.getWorld());
        if (world == null) {
            plugin.getLogger().warning("World not found: " + config.getWorld());
            return;
        }

        this.logic = new GomokuLogic(config.getBoardSize());

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::onFiveTick, 0L, 5L);
    }

    public void reloadConfig() {
        config.load();
        if (!config.isValid()) {
            plugin.getLogger().severe("Gomoku configuration is invalid. Please check your config.yml.");
            return;
        }
        this.world = plugin.getServer().getWorld(config.getWorld());
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

    private Vector toPosition(Vector2i position) {
        return config.getPosMin().clone()
                .add(config.getDirection1().clone().multiply(position.x() * 2))
                .add(config.getDirection2().clone().multiply(position.y() * 2));
    }

    private void renderBoard() {
        var world = plugin.getServer().getWorld(config.getWorld());
        if (world == null) {
            plugin.getLogger().warning("World not found: " + config.getWorld());
            return;
        }

        for (int x = 0; x < config.getBoardSize(); x++) {
            for (int z = 0; z < config.getBoardSize(); z++) {
                var cell = logic.getStone(x, z);
                var blockMaterial = switch (cell) {
                    case 1 -> BLACK_MATERIAL;
                    case 2 -> WHITE_MATERIAL;
                    default -> Material.AIR;
                };
                var position = toPosition(new Vector2i(x, z));

                var block = world.getBlockAt(position.toLocation(world));
                block.setType(blockMaterial);
            }
        }
    }

    private void sendGameStartMessage() {
        var isCPU = players.size() == 1;
        var playerText = players.stream().map(Player::getName).collect(Collectors.joining(" vs "));
        var playerVs = isCPU ? playerText + " vs CPU" : playerText;
        var title = Title.title(
                Component.text("五目並べ", TextColor.color(0xFFFFFF)),
                Component.text(playerVs)
        );

        var titlePlayers = world.getPlayers().stream()
                .filter(player -> config.getPlayableArea().contains(
                        player.getLocation().toVector()))
                .toList();
        for (Player player : titlePlayers) {
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

        sendGuide(p);
        sendGameStartMessage();
        renderBoard();
    }

    private void resetGame() {
        logic.resetGame();
        players.clear();
        renderBoard();
    }

    private void stopGame() {
        logic.resetGame();
        players.clear();
        for (Player player : world.getPlayers()) {
            if (config.getPlayableArea().contains(player.getLocation().toVector())) {
                player.sendMessage(Component.text("五目並べが終了されました", TextColor.color(0xFFFFFF)));
            }
        }

        for (Player player : players) {
            player.sendMessage(Component.text("ゲームが終了しました。", TextColor.color(0xFFFFFF)));
        }
    }

    private Vector2i getNearestBoardPositionFromBlock(Vector vec) {
        if (!vec.isInAABB(config.getPosMin(), config.getPosMax())) return null;

        var pos = vec.clone().subtract(config.getPosMin());
        if (pos.lengthSquared() == 0) {
            return new Vector2i(0, 0);
        }

        var x = (int) Math.round(pos.dot(config.getDirection1()) / config.getDirection1().lengthSquared() / 2.0);
        var z = (int) Math.round(pos.dot(config.getDirection2()) / config.getDirection2().lengthSquared() / 2.0);

        if (x < 0 || x >= config.getBoardSize() || z < 0 || z >= config.getBoardSize()) {
            return null; // 範囲外
        }

        return new Vector2i(x, z);
    }

    private Vector2i getNearestBoardPositionFromPlayer(Player player) {
        var eyeLocation = player.getEyeLocation();
        var offsetedArea = config.getBoardArea().clone()
                .shift(config.getRayTraceOffset());
        var rayTrace = offsetedArea.rayTrace(
                eyeLocation.toVector(),
                eyeLocation.getDirection(),
                100);

        if (rayTrace == null) return null;

        var position = rayTrace.getHitPosition().subtract(config.getRayTraceOffset());
        return getNearestBoardPositionFromBlock(position);
    }

    private void spawnBlockParticle(Vector2i position, Player p) {
        var blockPosition = toPosition(position);
        // ブロックを囲うように8点のパーティクルを生成
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    var begin = blockPosition.clone().add(new Vector(dx, dy, dz)).toLocation(world);
                    var xVec = dx == 0 ? 0.5 : -0.5;
                    var yVec = dy == 0 ? 0.5 : -0.5;
                    var zVec = dz == 0 ? 0.5 : -0.5;
                    p.spawnParticle(Particle.REVERSE_PORTAL,
                            begin,
                            0, xVec, 0, 0, 0.05, null, true);
                    p.spawnParticle(Particle.REVERSE_PORTAL,
                            begin,
                            0, 0, yVec, 0, 0.05, null, true);
                    p.spawnParticle(Particle.REVERSE_PORTAL,
                            begin,
                            0, 0, 0, zVec, 0.05, null, true);
                }
            }
        }

        // 中心にブロックのパーティクルを生成
        var center = blockPosition.clone().add(new Vector(0.5, 0.5, 0.5)).toLocation(world);
        p.spawnParticle(Particle.COMPOSTER, center, 10,
                0.2, 0.2, 0.2, 0.05, null, true);
    }

    private void onFiveTick() {
        for (Player player : players) {
            if (!player.isOnline() ||
                    !config.getPlayableArea().contains(player.getLocation().toVector())) {
                stopGame();
                break;
            }

            var position = getNearestBoardPositionFromPlayer(player);
            if (position == null) continue;
            spawnBlockParticle(position, player);
        }
    }
}
