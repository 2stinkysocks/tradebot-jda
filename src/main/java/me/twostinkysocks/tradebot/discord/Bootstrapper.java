package me.twostinkysocks.tradebot.discord;

import me.twostinkysocks.tradebot.TradeBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.time.Duration;
import java.util.EnumSet;

public class Bootstrapper {

    private static JDA jda = null;

    private static boolean isOn = false;

    public static void startup() {
        shutdown();
        TradeBot.instance.getLogger().info("Starting JDA...");
        String token = TradeBot.instance.getConfig().getString("token");
        if(token == null) {
            TradeBot.instance.getLogger().severe("Failed to start JDA: configure the token in the config first");
        } else {
           jda = JDABuilder
                   .createDefault(token)
                   .setMemberCachePolicy(MemberCachePolicy.ALL)
                   .setActivity(Activity.watching("your trades"))
                   .addEventListeners(new Bot())
                   .setEnabledIntents(EnumSet.allOf(GatewayIntent.class))
                   .build();
           try {
               jda.awaitReady();
               setupCommands();
               TradeBot.instance.getLogger().info("Successfully started JDA");
               isOn = true;
           } catch (InterruptedException e) {
               TradeBot.instance.getLogger().severe("Failed to start JDA: error during login");
           }
        }

    }

    public static JDA getJDA() {
        return jda;
    }

    private static void setupCommands() {
        jda.updateCommands().addCommands(
                Commands.slash("ping", "Check if the bot is online")
                        .setGuildOnly(true),
                Commands.slash("leaderboard", "Display the trades leaderboard")
                        .addOption(OptionType.INTEGER, "page", "The leaderboard page to view", false)
                        .setGuildOnly(true),
                Commands.slash("profile", "Display a user's profile")
                        .addOption(OptionType.USER, "user", "The user's profile to view", false)
                        .setGuildOnly(true)
        ).queue();
    }

    public static void shutdown() {
        if(jda == null) return;
        TradeBot.instance.getLogger().info("JDA shutting down gracefully...");
        jda.shutdown();
        try {
            jda.awaitShutdown(Duration.ofSeconds(15));
            jda.shutdownNow();
            TradeBot.instance.getLogger().info("Successfully shut down JDA");
            isOn = false;
            jda = null;
        } catch (InterruptedException e) {
            TradeBot.instance.getLogger().severe("JDA failed to gracefully shutdown!");
        }
    }

    public static boolean isOn() {
        return isOn;
    }
}
