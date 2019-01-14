/**
 * Original code written in Java 8 by Foxtrot2400
 * Credit to TylerS0166 and Exswordion
 * Tested in Spigot 1.10.2, 1.11.2, and 1.12.2
 */

package ap.apperms.apdevteam; //Package name - Do not change unless you change the path

import net.milkbowl.vault.permission.Permission; //When compiling **MAKE SURE** you have vault in your modules or this will error
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger; //Java Logger


public class Main extends JavaPlugin {
    private static final Logger log = Logger.getLogger("Minecraft"); //Sets up our logger for basic feedback to the console
    private static Permission perms; //Set up perms with vault
    private String[] donorRanks = getConfig().getStringList("donorRanks").toArray(new String[0]);
    private String[] staffRanks = getConfig().getStringList("staffRanks").toArray(new String[0]);

    @Override
    public void onDisable() {
        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }

    @Override
    public void onEnable() {
        log.info(String.format("[%s] v%s initializing.", getDescription().getName(), getDescription().getVersion()));
        getConfig().options().copyDefaults(true);

        if(!getConfig().contains("staffRanks")) {
            getConfig().createSection("staffRanks");
            List<String> adminValues = new ArrayList<>();
            adminValues.add("admin1");
            getConfig().set("staffRanks", adminValues);
        }
        if(!getConfig().contains("donorRanks")){
            getConfig().createSection("donorRanks");
            List<String> donorValues = new ArrayList<>();
            donorValues.add("donor1");
            getConfig().set("donorRanks", donorValues);
        }
        saveConfig();
        setupPermissions();
    }

    private boolean setupPermissions() { //Set up basic permission parameters
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = Bukkit.getPlayer(args[0]);            //Initialize who the target player is
        if (player == null) {
            log.warning("**WARNING** Player not found."); //If the player doesn't exist, tell the user & fail
            return false;
        }

        if (cmd.getName().equalsIgnoreCase("apgivedonor")) {
            if (args.length != 2) {
                log.warning("Wrong number of arguments!");
                return false;
            }
            else {
                for(String testRank : donorRanks) {
                    if(testRank.equalsIgnoreCase(args[1]) {
                        log.info("Rank found");
                        addDonor(player, testRank.toLowerCase());
                    }
                }
            }
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("apremovedonor")) {
            removeDonor(player);
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("apfixdonor")) {
            fixDonor(player);
            return true;
        }
        else {
            return false;
        }

    }

    private void addDonor(Player player, String donorRank) {
        for (String testRank : staffRanks) {
            if (perms.getPrimaryGroup(player).equalsIgnoreCase(testRank)) {
                //Player's primary group is found in staff ranks
                delRank(player, testRank);
                addRank(player, donorRank);
                addRank(player, testRank);
                return;  //Remove old rank, add donor, add old rank, return
            }
        }
        addRank(player, donorRank); //Just add donor if they are not staff
    }

    private void removeDonor(Player player) {
        //Remove all donator ranks
        for (String testRank : donorRanks) {
            if(perms.playerInGroup(player, testRank))
                delRank(player, testRank);
        }
    }

    private void fixDonor(Player player) {
        //Readd donator after a rankup (has to be triggered externally)
        for (String testRank : donorRanks) {
            if(perms.playerInGroup(player, testRank))
                for(String staffTest : staffRanks)
                {
                    if(perms.getPrimaryGroup(player).equalsIgnoreCase((staffTest))) {
                        //Player is staff, do not override top rank
                        return;
                    }
                }
                //Player is not staff, override top rank
                delRank(player, testRank);
                addRank(player, testRank);
        }
    }

    private void addRank(Player p, String rank) {
        perms.playerAddGroup(p, rank);
    }
    private void delRank(Player p, String rank) {
        perms.playerRemoveGroup(p, rank);
    }
}


