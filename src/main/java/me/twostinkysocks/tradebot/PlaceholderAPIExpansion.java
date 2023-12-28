package me.twostinkysocks.tradebot;

import github.scarsz.discordsrv.DiscordSRV;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.twostinkysocks.tradebot.db.Database;
import org.bukkit.OfflinePlayer;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    @Override
    public String getAuthor() {
        return "2stinkysocks";
    }

    @Override
    public String getIdentifier() {
        return "TradeBot";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if(params.equalsIgnoreCase("rep")){
            String discordID = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
            if(discordID == null) {
                return String.valueOf(0);
            }
            int pos = Database.instance.getPos(discordID);
            return String.valueOf(pos);
        }
        return null; // Placeholder is unknown by the Expansion
    }

}