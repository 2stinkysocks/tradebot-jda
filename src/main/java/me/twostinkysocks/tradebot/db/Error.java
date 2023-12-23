package me.twostinkysocks.tradebot.db;

import me.twostinkysocks.tradebot.TradeBot;

import java.util.logging.Level;

public class Error {
    public static void execute(Exception ex){
        TradeBot.instance.getLogger().log(Level.SEVERE, "Couldn't execute MySQL statement: ", ex);
    }
    public static void close(Exception ex){
        TradeBot.instance.getLogger().log(Level.SEVERE, "Failed to close MySQL connection: ", ex);
    }
}
