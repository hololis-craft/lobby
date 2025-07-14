package me.f0reach.holofans.lobby;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.milkbowl.vault2.economy.Economy;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

public class LobbyCommand implements CommandExecutor, Listener {
    private final HolofansLobby plugin;
    private final LobbyConfig config;
    private Economy economy;

    private final HashMap<UUID, Integer> pendingTeleports = new HashMap<>();

    public LobbyCommand(HolofansLobby plugin) {
        this.plugin = plugin;
        this.config = new LobbyConfig(plugin);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            var vault = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (vault != null) {
                economy = vault.getProvider();
            } else {
                plugin.getLogger().warning("Vault economy provider not found");
            }
        } else {
            plugin.getLogger().warning("Vault plugin not found, economy features will be disabled");
        }
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

                    var location = config.getLobbyLocation().toLocation(world);
                    location.setYaw((float) config.getLobbyYaw());
                    player.teleport(location);

                    return true;
                }

                if (config.getLobbyPrice() > 0 && economy != null) {
                    // Check if player has enough money
                    var price = BigDecimal.valueOf(config.getLobbyPrice());
                    if (!economy.has(plugin.getName(), player.getUniqueId(), price)) {
                        player.sendMessage("ロビーに移動するには " + config.getLobbyPrice() + " 円が必要です");
                        return true;
                    }

                    // Deduct money
                    try {
                        var result = economy.withdraw(plugin.getName(), player.getUniqueId(), price);
                        if (!result.transactionSuccess()) {
                            player.sendMessage("ロビーに移動するための料金の引き落としに失敗しました");
                            return true;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to withdraw money from player " + player.getName() + ": " + e.getMessage());
                        player.sendMessage("ロビーに移動するための料金の引き落としに失敗しました");
                        return true;
                    }
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
