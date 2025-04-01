package me.f0reach.holofans.lobby;

import me.f0reach.holofans.lobby.minigame.othello.OthelloGame;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public final class HolofansLobby extends JavaPlugin implements CommandExecutor, Listener {
    private String lobbyWorld = null;
    private Vector lobbyLocation = null;
    private double lobbyYaw = 0;
    private int teleportDelay = 20 * 3;
    private OthelloGame othelloGame;

    private final HashMap<UUID, Integer> pendingTeleports = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        try {
            lobbyWorld = getConfig().getString("lobbyWorld");
            lobbyLocation = getConfig().getVector("lobbyPos");
            lobbyYaw = getConfig().getDouble("lobbyYaw");
            teleportDelay = getConfig().getInt("teleportDelay");
        } catch (Exception e) {
            getLogger().severe("Failed to load lobby location from config");
            // Disable the plugin if the lobby location is not set
            getServer().getPluginManager().disablePlugin(this);
        }

        getCommand("lobby").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        othelloGame = new OthelloGame(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // lobby command
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("lobby") && args.length == 0) {
                var world = getServer().getWorld(lobbyWorld);
                if (world == null) {
                    getLogger().warning("Lobby world not found");
                    return true;
                }

                if (pendingTeleports.containsKey(player.getUniqueId())) {
                    player.sendMessage("すでに移動中です");
                    return true;
                }

                if (world == player.getWorld()) {
                    player.sendMessage("もうロビーにいます");
                    return true;
                }

                pendingTeleports.put(player.getUniqueId(), teleportDelay);

                var countdownBar = BossBar.bossBar(Component.text("ロビー行"), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_10);
                // Show countdown as title
                player.showBossBar(countdownBar);

                getServer().getScheduler().runTaskTimer(this, task -> {
                    var countdown = pendingTeleports.get(player.getUniqueId());
                    if (countdown == 0) {
                        var location = lobbyLocation.toLocation(world);
                        location.setYaw((float) lobbyYaw);
                        player.teleport(location);
                        pendingTeleports.remove(player.getUniqueId());
                        player.hideBossBar(countdownBar);

                        task.cancel();
                        return;
                    }

                    if (countdown > teleportDelay) countdown = teleportDelay;

                    countdownBar.progress((float) countdown / teleportDelay);
                    pendingTeleports.put(player.getUniqueId(), countdown - 1);
                }, 0L, 1L);

                return true;
            }
        }

        // reload
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("lobby") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("lobby.admin")) {
                    player.sendMessage("権限がありません");
                    return true;
                }

                reloadConfig();
                try {
                    lobbyWorld = getConfig().getString("lobbyWorld");
                    lobbyLocation = getConfig().getVector("lobbyPos");
                    lobbyYaw = getConfig().getDouble("lobbyYaw");
                    teleportDelay = getConfig().getInt("teleportDelay");
                } catch (Exception e) {
                    getLogger().severe("Failed to load lobby location from config");
                    // Disable the plugin if the lobby location is not set
                    getServer().getPluginManager().disablePlugin(this);
                }

                player.sendMessage("ロビーの設定をリロードしました");
                return true;
            }
        }

        return false;
    }

    // Reset in join
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        pendingTeleports.remove(player.getUniqueId());
    }

    // Remove food level decrease in lobby
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        var entity = event.getEntity();
        if (entity instanceof Player player && player.getWorld().getName().equals(lobbyWorld)) {
            var decreased = player.getFoodLevel() > event.getFoodLevel();
            if (decreased)
                event.setCancelled(true);
        }
    }

    // Increase countdown by damage
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        var entity = event.getEntity();
        if (entity instanceof Player player) {
            var countdown = pendingTeleports.get(player.getUniqueId());
            if (countdown != null) {
                pendingTeleports.put(player.getUniqueId(), countdown + 5);
            }
        }
    }
}
