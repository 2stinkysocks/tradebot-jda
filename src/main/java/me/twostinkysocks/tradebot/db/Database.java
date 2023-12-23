package me.twostinkysocks.tradebot.db;

import me.twostinkysocks.tradebot.TradeBot;
import me.twostinkysocks.tradebot.discord.Bootstrapper;
import net.dv8tion.jda.api.entities.Guild;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.logging.Level;

public class Database {
    Connection connection;
    public String table = "rep";

    public static Database instance = new Database();

    private Database() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();
            s.executeUpdate(SQLiteCreateTokensTable);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initialize();
    }

    public String SQLiteCreateTokensTable = "CREATE TABLE IF NOT EXISTS rep (" +
            "`id` varchar(32) NOT NULL," +
            "`pos` int(11) NOT NULL," +
            "`neg` int(11) NOT NULL," +
            "`name` varchar(32) NOT NULL," +
            "`uuid` varchar(40) NOT NULL," +
            "PRIMARY KEY (`id`)" +
            ");";


    public Connection getSQLConnection() {
        String dbname = "database";
        File dataFolder = new File(TradeBot.instance.getDataFolder(), dbname +".db");
        if (!dataFolder.exists()){
            try {
                dataFolder.createNewFile();
            } catch (IOException e) {
                TradeBot.instance.getLogger().log(Level.SEVERE, "File write error: "+ dbname +".db");
            }
        }
        try {
            if(connection!=null&&!connection.isClosed()){
                return connection;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            return connection;
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, "You need the SQLite JDBC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    public void initialize(){
        connection = getSQLConnection();
        try{
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + table + " WHERE id = ?;");
            ResultSet rs = ps.executeQuery();
            close(ps,rs);

        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, "Unable to retreive connection", ex);
        }
    }

    public Integer getPos(String id) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + table + " WHERE id = '"+id+"';");

            rs = ps.executeQuery();
            while(rs.next()){
                if(rs.getString("id").equalsIgnoreCase(id)){
                    return rs.getInt("pos");
                }
            }
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return 0;
    }

    public LinkedHashMap<String, Integer> getLeaderboard(int page) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM "+table+" ORDER BY pos DESC LIMIT 10 OFFSET "+((page-1)*10)+";");

            rs = ps.executeQuery();
            LinkedHashMap<String, Integer> lb = new LinkedHashMap<>();
            while(rs.next()){
                lb.put(rs.getString("id"), rs.getInt("pos"));
            }
            return lb;
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return new LinkedHashMap<>();
    }

    public Integer getNeg(String id) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + table + " WHERE id = '"+id+"';");

            rs = ps.executeQuery();
            while(rs.next()){
                if(rs.getString("id").equalsIgnoreCase(id)){
                    return rs.getInt("neg");
                }
            }
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return 0;
    }

    public String getName(String id) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + table + " WHERE id = '"+id+"';");

            rs = ps.executeQuery();
            while(rs.next()){
                if(rs.getString("id").equalsIgnoreCase(id)){
                    return rs.getString("name");
                }
            }
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return null;
    }

    public void setPos(Guild guild, String id, int pos) {
        if(Bootstrapper.getJDA().getUserById(id) != null) {
            setPosGuranteedCache(id, pos);
        } else {
            guild.loadMembers()
                    .onSuccess(e -> Bukkit.getScheduler().runTaskLater(TradeBot.instance, () -> setPosGuranteedCache(id, pos), 1L))
                    .onError(e -> TradeBot.instance.getLogger().severe("Error while caching member with id: " + id));
        }
    }

    public void setNeg(Guild guild, String id, int neg) {
        if(Bootstrapper.getJDA().getUserById(id) != null) {
            setNegGuranteedCache(id, neg);
        } else {
            guild.loadMembers()
                    .onSuccess(e -> Bukkit.getScheduler().runTaskLater(TradeBot.instance, () -> setNegGuranteedCache(id, neg), 1L))
                    .onError(e -> TradeBot.instance.getLogger().severe("Error while caching member with id: " + id));
        }
    }

    public void setPosAndNeg(Guild guild, String id, int pos, int neg) {
        if(Bootstrapper.getJDA().getUserById(id) != null) {
            setPosAndNegGuranteedCache(id, pos, neg);
        } else {
            guild.loadMembers()
                    .onSuccess(e -> Bukkit.getScheduler().runTaskLater(TradeBot.instance, () -> setPosAndNegGuranteedCache(id, pos, neg), 1L))
                    .onError(e -> TradeBot.instance.getLogger().severe("Error while caching member with id: " + id));
        }
    }

    public boolean exists(String id) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT (count(*) > 0) as id FROM "+table+" WHERE id = '"+id+"' LIMIT 1;");

            rs = ps.executeQuery();
            while(rs.next()){
                if(rs.getBoolean(1)){
                    return true;
                }
            }
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return false;
    }

    private void setPosGuranteedCache(String id, int pos) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            if(exists(id)) {
                conn = getSQLConnection();
                ps = conn.prepareStatement("UPDATE "+table+" SET pos = "+pos+" WHERE id = '"+id+"';");
            } else {
                conn = getSQLConnection();
                ps = conn.prepareStatement("INSERT INTO "+table+" (id, pos, neg, name, uuid) VALUES('"+id+"',"+pos+",0,'"+ Bootstrapper.getJDA().getUserById(id).getName().replaceAll("'", "")+"', 'unknown');");
            }
            ps.executeUpdate();
            return;
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return;
    }

    public void incrPos(Guild guild, String id) {
        setPos(guild, id, getPos(id)+1);
    }

    public void incrNeg(Guild guild, String id) {
        setNeg(guild, id, getNeg(id)+1);
    }

    public UUID getUUID(String id) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + table + " WHERE id = '"+id+"';");

            rs = ps.executeQuery();
            while(rs.next()){
                if(rs.getString("uuid").equalsIgnoreCase(id)){
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return null;
    }

    public void setUUID(String id, UUID uuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getSQLConnection();
            if(exists(id)) {
                ps = conn.prepareStatement("UPDATE "+table+" SET uuid = "+uuid.toString()+" WHERE id = '"+id+"';");
            } else {
                ps = conn.prepareStatement("INSERT INTO "+table+" (id, pos, neg, name, uuid) VALUES('"+id+"',0,0,'"+ Bootstrapper.getJDA().getUserById(id).getName().replaceAll("'", "")+"', '"+uuid.toString()+"');");
            }
            ps.executeUpdate();
            return;
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return;
    }

    private void setNegGuranteedCache(String id, int neg) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            if(exists(id)) {
                conn = getSQLConnection();
                ps = conn.prepareStatement("UPDATE "+table+" SET neg = "+neg+" WHERE id = '"+id+"';");
            } else {
                conn = getSQLConnection();
                ps = conn.prepareStatement("INSERT INTO "+table+" (id, pos, neg, name, uuid) VALUES('"+id+"',0,"+neg+",'"+ Bootstrapper.getJDA().getUserById(id).getName().replaceAll("'", "")+"', 'unknown');");
            }
            ps.executeUpdate();
            return;
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return;
    }

    private void setPosAndNegGuranteedCache(String id, int pos, int neg) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            if(exists(id)) {
                conn = getSQLConnection();
                ps = conn.prepareStatement("UPDATE "+table+" SET pos = "+pos+", neg = "+neg+" WHERE id = '"+id+"';");
            } else {
                conn = getSQLConnection();
                ps = conn.prepareStatement("INSERT INTO "+table+" (id, pos, neg, name, uuid) VALUES('"+id+"',"+pos+","+neg+",'"+ Bootstrapper.getJDA().getUserById(id).getName().replaceAll("'", "")+"', 'unknown');");
            }
            ps.executeUpdate();
            return;
        } catch (SQLException ex) {
            TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                TradeBot.instance.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return;
    }


    public void close(PreparedStatement ps,ResultSet rs){
        try {
            if (ps != null)
                ps.close();
            if (rs != null)
                rs.close();
        } catch (SQLException ex) {
            Error.close(ex);
        }
    }
}
