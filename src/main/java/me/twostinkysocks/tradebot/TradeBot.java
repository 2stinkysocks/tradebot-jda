package me.twostinkysocks.tradebot;

import me.twostinkysocks.tradebot.discord.Bootstrapper;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class TradeBot extends JavaPlugin {

    public static TradeBot instance;

    @Override
    public void onEnable() {
        instance = this;
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
        Bootstrapper.startup();
    }

    @Override
    public void onDisable() {
        Bootstrapper.shutdown();
    }
}
