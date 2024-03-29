package me.twostinkysocks.tradebot.discord;

import github.scarsz.discordsrv.DiscordSRV;
import me.twostinkysocks.tradebot.TradeBot;
import me.twostinkysocks.tradebot.db.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bot extends ListenerAdapter {

    private static final String[] merchantRoles = new String[]{"720383911221526562", "848407083564138566", "708132965116543087", "708133387247812722", "865294530332590111"};

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        switch(e.getName()) {
            case "setrep":
                User userr = e.getOption("user").getAsUser();
                int poss = e.getOption("pos").getAsInt();
                int negg = e.getOption("neg").getAsInt();
                Database.instance.setPosAndNeg(e.getGuild(), userr.getId(), poss, negg);
                e.reply("Updated " + userr.getName() + "'s rep.").queue();
                break;
            case "verify":
                String discordID = e.getUser().getId();
                UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordID);
                if(uuid == null) {
                    e.reply("Your account is not linked! Run `/discord link` ingame and follow the instructions, then run this command again!").setEphemeral(true).queue();
                } else {
                    e.reply("Successfully linked your discord account to minecraft account: " + Bukkit.getOfflinePlayer(uuid).getName()).setEphemeral(true).queue();
                    if(!e.getMember().getRoles().stream().anyMatch(r -> r.getId().equals("862108357509906443"))) {
                        e.getGuild().addRoleToMember(UserSnowflake.fromId(discordID), Bootstrapper.getJDA().getRoleById("862108357509906443")).queue();
                    }
                    if(e.getMember().getRoles().stream().anyMatch(r -> r.getId().equals("862369389907017748"))) {
                        e.getGuild().removeRoleFromMember(UserSnowflake.fromId(discordID), Bootstrapper.getJDA().getRoleById("862369389907017748")).queue();
                    }
                }
                break;
            case "ping":
                long time = System.currentTimeMillis();
                e.reply("Pong!").setEphemeral(true) // reply or acknowledge
                        .flatMap(v ->
                                e.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                        ).queue();
                break;
            case "leaderboard":
                // id, pos
                int page;
                try {
                    page = e.getInteraction().getOption("page") == null ? 1 : e.getInteraction().getOption("page").getAsInt();
                } catch(ArithmeticException ex) {
                    e.reply("Invalid page number!").setEphemeral(true).queue();
                    break;
                }
                if(page < 1) {
                    e.getInteraction().reply("Page must be greater or equal to 1!").setEphemeral(true).queue();
                } else {
                    e.deferReply().queue();
                    Map<String, Integer> lb = Database.instance.getLeaderboard(page);
                    EmbedBuilder eb = new EmbedBuilder().setColor(Color.decode("#4287F5")).setAuthor("Top Traders - Page " + page);
                    e.getGuild().findMembers(m -> lb.keySet().contains(m.getId())).onSuccess(members -> {
                        String description = "";
                        for(String id : lb.keySet()) {
                            String line = lb.get(id) + " - " + (Bootstrapper.getJDA().getUserById(id) == null ? Database.instance.getName(id) : Bootstrapper.getJDA().getUserById(id).getAsMention()) + "\n";
                            description += line;
                        }
                        eb.setDescription(description);
                        e.getHook().editOriginalEmbeds(eb.build()).queue();
                    }).onError(t -> TradeBot.instance.getLogger().severe("Error while caching guild members: " + t.getMessage()));
                }
                break;
            case "profile":
                e.deferReply().queue();
                User user = null;
                if(e.getInteraction().getOption("user") == null) {
                    user = e.getUser();
                } else {
                    user = e.getInteraction().getOption("user").getAsUser();
                }
                int pos = 0;
                int neg = 0;
                if(!Database.instance.exists(user.getId())) {
                    Database.instance.setPos(e.getGuild(), user.getId(), 0);
                } else {
                    pos = Database.instance.getPos(user.getId());
                    neg = Database.instance.getNeg(user.getId());
                }
                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(Color.decode("#4287F5"))
                        .setAuthor("Trade Information")
                        .setDescription(user.getAsMention())
                        .addField(new MessageEmbed.Field(":white_check_mark: Positive Rep", String.valueOf(pos), true))
                        .addField(new MessageEmbed.Field(":x: Negative Rep", String.valueOf(neg), true));
                e.getHook().editOriginalEmbeds(eb.build()).queue();
                break;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        String messageContent = e.getMessage().getContentRaw();
        if(e.getMessage().getChannel().getId().equals(TradeBot.instance.getConfig().getString("positive-channel"))) {
            Pattern pat = Pattern.compile("^<@\\d{18}> +\\d{1,2}\\/10|^<@!\\d{18}> +\\d{1,2}\\/10|^<@\\d{17}> +\\d{1,2}\\/10|^<@!\\d{17}> +\\d{1,2}\\/10");
            Matcher matcher = pat.matcher(messageContent);
            if(!matcher.find()) {
                e.getMessage().delete().queueAfter(100, TimeUnit.MILLISECONDS);
                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setAuthor("Invalid Review Format", null, e.getMessage().getAuthor().getAvatarUrl())
                        .setTimestamp(Instant.now())
                        .setDescription("Your review in " + e.getMessage().getChannel().getAsMention() + " has been removed since it was not following the correct format.\n\n>>> " + e.getMessage().getContentDisplay())
                        .addField(new MessageEmbed.Field("**Please re-write it so that it follows the required format**:\n\n@`[Player's name]` `[#]`/10 `[Any additional info]`\n\nThank you!", "\n\n~Trade Server Staff", false));
                e.getMessage().getAuthor().openPrivateChannel().flatMap(ch -> ch.sendMessageEmbeds(eb.build())).queue();
            } else {
                Pattern pat2 = Pattern.compile("\\d{18}|\\d{17}");
                Matcher matcher2 = pat2.matcher(messageContent);
                if(matcher2.find() && matcher2.group().equals(e.getMessage().getAuthor().getId())) {
                    e.getMessage().delete().queueAfter(100, TimeUnit.MILLISECONDS);
                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(Color.RED)
                            .setAuthor("Invalid Review Format", null, e.getMessage().getAuthor().getAvatarUrl())
                            .setTimestamp(Instant.now())
                            .addField(new MessageEmbed.Field("You cannot give yourself a review!", "\n\n~Trade Server Staff", false));
                    e.getMessage().getAuthor().openPrivateChannel().flatMap(ch -> ch.sendMessageEmbeds(eb.build())).queue();
                } else {
                    Pattern pat3 = Pattern.compile("\\d{18}|\\d{17}");
                    Matcher matcher3 = pat3.matcher(messageContent);
                    matcher3.find();
                    String otherID = matcher3.group();
                    Database.instance.incrPos(e.getGuild(), otherID);
                }
            }
        } else if(e.getMessage().getChannel().getId().equals(TradeBot.instance.getConfig().getString("negative-channel"))) {
            Pattern pat = Pattern.compile("^<@\\d{18}> +\\d{1,2}\\/10|^<@!\\d{18}> +\\d{1,2}\\/10|^<@\\d{17}> +\\d{1,2}\\/10|^<@!\\d{17}> +\\d{1,2}\\/10");
            Matcher matcher = pat.matcher(messageContent);
            if(!matcher.find()) {
                pat = Pattern.compile("^.+ +\\d{1,2}\\/10|^<@!\\d{18}> +\\d{1,2}\\/10");
                matcher = pat.matcher(messageContent);
                if(!matcher.find()) {
                    e.getMessage().delete().queueAfter(100, TimeUnit.MILLISECONDS);
                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(Color.RED)
                            .setAuthor("Invalid Review Format", null, e.getMessage().getAuthor().getAvatarUrl())
                            .setTimestamp(Instant.now())
                            .setDescription("Your review in " + e.getMessage().getChannel().getAsMention() + " has been removed since it was not following the correct format.\n\n>>> " + e.getMessage().getContentDisplay())
                            .addField(new MessageEmbed.Field("**Please re-write it so that it follows the required format**:\n\n@`[Player's name]` `[#]`/10 `[Any additional info]`\n\nThank you!", "\n\n~Trade Server Staff", false));
                    e.getMessage().getAuthor().openPrivateChannel().flatMap(ch -> ch.sendMessageEmbeds(eb.build())).queue();
                }
            } else {
                pat = Pattern.compile("\\d{18}|\\d{17}");
                matcher = pat.matcher(messageContent);
                if(matcher.find() && matcher.group().equals(e.getMessage().getAuthor().getId())) {
                    e.getMessage().delete().queueAfter(100, TimeUnit.MILLISECONDS);
                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(Color.RED)
                            .setAuthor("Invalid Review Format", null, e.getMessage().getAuthor().getAvatarUrl())
                            .setTimestamp(Instant.now())
                            .addField(new MessageEmbed.Field("You cannot give yourself a review!", "\n\n~Trade Server Staff", false));
                    e.getMessage().getAuthor().openPrivateChannel().flatMap(ch -> ch.sendMessageEmbeds(eb.build())).queue();
                } else {
                    pat = Pattern.compile("\\d{18}|\\d{17}");
                    matcher = pat.matcher(messageContent);
                    matcher.find();
                    String otherID = matcher.group();
                    Database.instance.incrNeg(e.getGuild(), otherID);
                }
            }
        }
        List<String> channels = (List<String>) TradeBot.instance.getConfig().getList("auction-channels");
        for(String id : channels) {
            if(e.getChannel().getId().equals(id) && !e.getMessage().getAuthor().getId().equals("797951630045478953")) {
                e.getChannel().getHistory().retrievePast(2)
                        .map(messages -> messages.get(1))
                        .queue(prevMessage -> {
                            int prevBid = 0;
                            boolean lowerAllowed = false;
                            if(!prevMessage.getAuthor().getId().equals("797951630045478953")) {
                                lowerAllowed = true;
                            } else {
                                if(prevMessage.getEmbeds().size() > 0) {
                                    String desc = prevMessage.getEmbeds().get(0).getDescription();
                                    Pattern pat = Pattern.compile("^\\*\\*\\d+");
                                    Matcher matcher = pat.matcher(desc);
                                    matcher.find();
                                    prevBid = Integer.parseInt(matcher.group().replace("**", ""));
                                } else {
                                    return;
                                }
                            }
                            String bidMessage = e.getMessage().getContentRaw();
                            Matcher matcher = Pattern.compile("^\\d+").matcher(bidMessage);
                            matcher.find();
                            if(!bidMessage.matches("^\\d+( .+)?") && e.getMessage().getMember().getRoles().stream().noneMatch(r -> r.getId().equals("725819997812817953"))) {
                                e.getMessage().delete().queueAfter(100, TimeUnit.MILLISECONDS);
                                EmbedBuilder eb = new EmbedBuilder()
                                        .setColor(Color.RED)
                                        .setAuthor("Invalid Bid", null, e.getMessage().getAuthor().getAvatarUrl())
                                        .setTimestamp(Instant.now())
                                        .setDescription("Your bid in " + e.getMessage().getChannel().getAsMention() + " was invalid\n\n>>> " + e.getMessage().getContentDisplay())
                                        .addField(new MessageEmbed.Field("All bids in this channel must begin with a number!", "\n\n~Trade Server Staff", false));
                                e.getMessage().getAuthor().openPrivateChannel().flatMap(ch -> ch.sendMessageEmbeds(eb.build())).queue();
                            } else if(!lowerAllowed && bidMessage.matches("^\\d+( .+)?") && Integer.parseInt(matcher.group()) <= prevBid) {
                                e.getMessage().delete().queueAfter(100, TimeUnit.MILLISECONDS);
                                EmbedBuilder eb = new EmbedBuilder()
                                        .setColor(Color.RED)
                                        .setAuthor("Invalid Bid", null, e.getMessage().getAuthor().getAvatarUrl())
                                        .setDescription("Your bid in " + e.getMessage().getChannel().getAsMention() + " was invalid\n\n>>> " + e.getMessage().getContentDisplay())
                                        .addField(new MessageEmbed.Field("All bids in this channel must be greater than the previous bid!", "\n\n~Trade Server Staff", false));
                                e.getMessage().getAuthor().openPrivateChannel().flatMap(ch -> ch.sendMessageEmbeds(eb.build())).queue();
                            } else if(bidMessage.matches("^\\d+( .+)?")) {
                                boolean hasMerch = false;
                                for(String merchRoleID : merchantRoles) {
                                    if(e.getMessage().getMember().getRoles().stream().anyMatch(r -> r.getId().equals(merchRoleID))) {
                                        hasMerch = true;
                                    }
                                }
                                if(hasMerch) {
                                    e.getMessage().delete().queueAfter(100, TimeUnit.MILLISECONDS, a -> {
                                        EmbedBuilder eb = new EmbedBuilder()
                                                .setColor(Color.decode("#018CFD"))
                                                .setAuthor("New Bid!", null, e.getMessage().getAuthor().getAvatarUrl())
                                                .setTimestamp(Instant.now());
                                        Matcher matcher2 = Pattern.compile("^\\d+").matcher(e.getMessage().getContentRaw());
                                        matcher2.find();
                                        String desc = "**" + matcher2.group() + "** - " + e.getMessage().getAuthor().getAsMention();
                                        if(e.getMessage().getContentRaw().matches("^\\d+ \\D+")) {
                                            matcher2 = Pattern.compile("\\D+").matcher(e.getMessage().getContentRaw());
                                            matcher2.find();
                                            String customMsg = matcher2.group();
                                            if(customMsg.length() > 60) {
                                                customMsg = customMsg.substring(0, 60) + "...";
                                            }
                                            desc += "\n\n" + customMsg;
                                        }
                                        eb.setDescription(desc);
                                        e.getChannel().sendMessageEmbeds(eb.build()).queue();
                                    });
                                } else {
                                    e.getMessage().delete().queueAfter(100, TimeUnit.MILLISECONDS);
                                    EmbedBuilder eb = new EmbedBuilder()
                                            .setColor(Color.RED)
                                            .setAuthor("Invalid Bid", null, e.getMessage().getAuthor().getAvatarUrl())
                                            .setDescription("You are not allowed to place bids! Contact trade server staff if you believe this is a mistake.");
                                    e.getMessage().getAuthor().openPrivateChannel().flatMap(ch -> ch.sendMessageEmbeds(eb.build())).queue();
                                }
                            }
                        });
            }
        }
    }
}
