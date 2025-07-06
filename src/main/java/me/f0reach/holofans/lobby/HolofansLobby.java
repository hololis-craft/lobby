package me.f0reach.holofans.lobby;

import me.f0reach.holofans.lobby.minigame.gomoku.GomokuGame;
import me.f0reach.holofans.lobby.minigame.othello.OthelloGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class HolofansLobby extends JavaPlugin implements CommandExecutor, Listener {
    private LobbyCommand lobbyCommand;
    private OthelloGame othelloGame;
    private GomokuGame gomokuGame;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        getCommand("lobby").setExecutor(this);
        lobbyCommand = new LobbyCommand(this);
        othelloGame = new OthelloGame(this);
        gomokuGame = new GomokuGame(this);

        reloadConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (lobbyCommand != null) {
            lobbyCommand.reloadConfig();
        }
        if (othelloGame != null) {
            othelloGame.reloadConfig();
        }
        if (gomokuGame != null) {
            gomokuGame.reloadConfig();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        // lobby command
        if (lobbyCommand.onCommand(sender, command, label, args)) {
            return true;
        }

        // reload
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("lobby") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("lobby.admin")) {
                    player.sendMessage("権限がありません");
                    return true;
                }

                reloadConfig();

                player.sendMessage("ロビーの設定をリロードしました");
                return true;
            }
        }

        return false;
    }

    // Reset in join
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    }
}
