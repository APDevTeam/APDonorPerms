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
    FileConfiguration config = this.getConfig();
    String[] donorRanks = this.getConfig().getStringList("donorranks").toArray(new String[0]);
    String[] ranks = this.getConfig().getStringList("adminranks").toArray(new String[0]);

    @Override
    public void onDisable() {
        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }

    @Override
    public void onEnable() {
        log.info(String.format("[%s] v%s initializing.", getDescription().getName(), getDescription().getVersion()));
        config.options().copyDefaults(true);

        if(!config.contains("adminranks")) {
            config.createSection("adminranks");
            List<String> adminvalues = new ArrayList<>();
            adminvalues.add("admin1");
            config.set("adminranks", adminvalues);
        }
        if(!config.contains("donorranks")){
            config.createSection("donorranks");
            List<String> donorvalues = new ArrayList<>();
            donorvalues.add("donor1");
            config.set("donorranks", donorvalues);
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
            if (args.length == 1){
                log.warning("No donor rank specified!");
                return false;
            }
            else {
                for(String donorGroupName: donorRanks) {
                    if(donorGroupName.toLowerCase() == args[1].toLowerCase())
                    log.info("Rank found");
                    addDonor(player, args[1].toLowerCase());
                }
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("apremovedonor")) {
            removeDonor(player);
            return true;
        } else {
            return false;
        }

    }

    private void addDonor(Player player, String donorRank) {
        perms.playerAddGroup(null, player, donorRank);
        for (String string : ranks){
            player.sendMessage(string);
        }
        for (String string : donorRanks){
            player.sendMessage(string);
        }
        fixStaff(player);
    }

    private void removeDonor(Player player) {
        for (String removeRank : donorRanks) {
            perms.playerRemoveGroup(null, player, removeRank);
        }
    }

    private void fixStaff(Player player) {
        for(String adminGroup: ranks) {
            if (perms.playerInGroup(null, player, adminGroup)) {
                perms.playerRemoveGroup(null, player, adminGroup);
                perms.playerAddGroup(null, player, adminGroup);

            }
        }
    }

}


