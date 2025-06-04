package me.f0reach.holofans.lobby;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class LobbyCommand implements CommandExecutor, Listener {
    private final HolofansLobby plugin;
    private final LobbyConfig config;


    private final HashMap<UUID, Integer> pendingTeleports = new HashMap<>();

    public LobbyCommand(HolofansLobby plugin) {
        this.plugin = plugin;
        this.config = new LobbyConfig(plugin);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void reloadConfig() {
        config.reloadConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        // lobby command
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("lobby") && args.length == 0) {
                var world = plugin.getServer().getWorld(config.getLobbyWorld());
                if (world == null) {
                    plugin.getLogger().warning("Lobby world not found");
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

                pendingTeleports.put(player.getUniqueId(), config.getTeleportDelay());

                var countdownBar = BossBar.bossBar(Component.text("ロビー行"), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_10);
                // Show countdown as title
                player.showBossBar(countdownBar);

                plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
                    var countdown = pendingTeleports.get(player.getUniqueId());
                    if (countdown == 0) {
                        var location = config.getLobbyLocation().toLocation(world);
                        location.setYaw((float) config.getLobbyYaw());
                        player.teleport(location);
                        pendingTeleports.remove(player.getUniqueId());
                        player.hideBossBar(countdownBar);

                        var title = Title.title(Component.text("ロビー"), Component.empty());
                        player.showTitle(title);

                        task.cancel();
                        return;
                    }

                    if (countdown > config.getTeleportDelay()) countdown = config.getTeleportDelay();

                    countdownBar.progress((float) countdown / config.getTeleportDelay());
                    pendingTeleports.put(player.getUniqueId(), countdown - 1);
                }, 0L, 1L);

                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        pendingTeleports.remove(player.getUniqueId());
    }

    // Remove food level decrease in lobby
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        var entity = event.getEntity();
        if (entity instanceof Player player && player.getWorld().getName().equals(config.getLobbyWorld())) {
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
