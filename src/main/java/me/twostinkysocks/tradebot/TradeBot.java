package me.twostinkysocks.tradebot;

import me.twostinkysocks.tradebot.db.Database;
import me.twostinkysocks.tradebot.discord.Bootstrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class TradeBot extends JavaPlugin implements CommandExecutor {

    public static TradeBot instance;

    @Override
    public void onEnable() {
        instance = this;
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIExpansion().register();
        }
        getCommand("tradebot").setExecutor(this);
        getCommand("tradebot").setTabCompleter(this);
        new Database();
        load();
    }

    public void load() {
        if(!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }
        File config = new File(this.getDataFolder(), "config.yml");
        if(!config.exists()) {
            saveDefaultConfig();
        }
        this.reloadConfig();
        if(!getConfig().getBoolean("disabled", false)) {
            Bootstrapper.startup();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(label.equals("tradebot")) {
            if(!sender.hasPermission("tradebot.manage")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
                return true;
            }
            if(args.length == 0 || (!args[0].equals("reload") && !args[0].equals("stop") && !args[0].equals("start"))) {
                sender.sendMessage(ChatColor.RED + "Usage: /tradebot <reload|stop|start>");
                return true;
            }
            switch(args[0]) {
                case "reload":
                    sender.sendMessage(ChatColor.AQUA + "Reloading TradeBot...");
                    load();
                    sender.sendMessage(ChatColor.GREEN + "Reloaded!");
                    break;
                case "start":
                    if(Bootstrapper.isOn()) {
                        sender.sendMessage(ChatColor.RED + "TradeBot is already running!");
                    } else {
                        sender.sendMessage(ChatColor.AQUA + "Starting TradeBot...");
                        Bootstrapper.startup();
                        sender.sendMessage(ChatColor.GREEN + "Started TradeBot!");
                    }
                    break;
                case "stop":
                    if(!Bootstrapper.isOn()) {
                        sender.sendMessage(ChatColor.RED + "TradeBot isn't running!");
                    } else {
                        sender.sendMessage(ChatColor.AQUA + "Stopping TradeBot...");
                        Bootstrapper.shutdown();
                        sender.sendMessage(ChatColor.GREEN + "Stopped TradeBot!");
                    }
                    break;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if(alias.equals("tradebot")) {
            if(args.length == 1) {
                StringUtil.copyPartialMatches(args[0], List.of("reload", "start", "stop"), completions);
            }
        }
        return completions;
    }

    @Override
    public void onDisable() {
        Bootstrapper.shutdown();
    }
}
