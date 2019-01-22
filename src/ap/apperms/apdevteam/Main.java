/**
 * Original code written in Java 8 by Foxtrot2400
 * Credit to TylerS1066 and Exswordion
 * Tested in Spigot 1.10.2
 */

package ap.apperms.apdevteam; //Package name - Do not change unless you change the path

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.permission.Permission; //When compiling **MAKE SURE** you have vault in your modules or this will error
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger; //Java Logger


public class Main extends JavaPlugin {
    private static final Logger log = Logger.getLogger("Minecraft"); //Sets up our logger for basic feedback to the console
    private static Permission perms;  //Set up perms with vault
    private String[] donorRanks = getConfig().getStringList("donorRanks").toArray(new String[0]); // Pulls our donor ranks list from config
    private String[] staffRanks = getConfig().getStringList("staffRanks").toArray(new String[0]); // Pulls our admin ranks list from config

    @Override
    public void onDisable() {
        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion())); //Notify the console the plugin is disabled
    }

    @Override
    public void onEnable() {
        log.info(String.format("[%s] v%s initializing.", getDescription().getName(), getDescription().getVersion()));   //Notify the console the plugin has started up
        getConfig().options().copyDefaults(true); // Does some magic that I'm not 200% sure of... but stuff works because of it

        if(!getConfig().contains("staffRanks")) {        //Both of these are a first-time setup of the plugin. If the config file --
            getConfig().createSection("staffRanks"); //doesn't have the staffRanks or donorRanks section, it makes them along with an example rank
            List<String> adminValues = new ArrayList<>();
            adminValues.add("admin1");
            getConfig().set("staffRanks", adminValues);
        }
        if(!getConfig().contains("donorRanks")){
            getConfig().createSection("donorRanks"); //Makes the section
            List<String> donorValues = new ArrayList<>();
            donorValues.add("donor1");
            getConfig().set("donorRanks", donorValues);  //Creates our example rank
        }
        saveConfig(); //Spigot's save config feature
        setupPermissions(); // Set up our basic permission parameters
    }

    private boolean setupPermissions() { //Set up basic permission parameters
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class); //Sets up our perms provider
        perms = rsp.getProvider(); //Creates our variable for our permission provider
        return perms != null;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = Bukkit.getPlayer(args[0]);            //Initialize who the target player is

        if (player == null) {
            sendMessage(sender, "Player not found.");
            return false;
        }

        if (cmd.getName().equalsIgnoreCase("apgivedonor")) {
            if (args.length != 2) {
                sendMessage(sender, "Wrong number of arguments!"); //If there isn't only a player and rank specified, warn the console and return
                return false;
            }
            else {
                for(String testRank : donorRanks) {
                    if(testRank.equalsIgnoreCase(args[1])){
                        addDonor(player, testRank.toLowerCase()); //If we can find the rank in our known list of ranks, add it (lowercase)
                        sendMessage(sender, "Donor rank given.");
                        return true;                            //If we find the rank, tell the sender and return
                    }
                }
                sendMessage(sender, "Donor rank not found!");
                return false;                                   //If we can't find the rank in our known lists, tell the sender
            }

        }
        else if (cmd.getName().equalsIgnoreCase("apremovedonor")) {
            removeDonor(player); //Remove the donor permissions of a player
            sendMessage(sender, "Donor rank removed.");
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("apfixdonor")) {
            fixDonor(player);   //Fixes the donor permissions for a player by re-stacking their ranks (top shows up to everyone)
            sendMessage(sender, "Donor rank fixed.");
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("aplistgroups")){
            if(!(sender instanceof Player)){ //If the console is running this command, use the logger version.
                listGroupsConsole(player);
                return true;
            }
            else{ //Otherwise, if a player is using it, send this to the player requesting the command
                listGroups((Player) sender, player);
                return true;
            }
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
        //Remove all donor ranks for a player
        for (String testRank : donorRanks) {
            if(perms.playerInGroup(player, testRank)) {
                delRank(player, testRank.toLowerCase());
            }
        }
    }

    private void fixDonor(Player player) {
        //Read donor after a rank up (has to be triggered externally)
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
    private void listGroups(Player p, Player targetPlayer){
        String[] groups = perms.getPlayerGroups(targetPlayer); //Get the target player's groups
        p.sendMessage(ChatColor.DARK_GREEN + "[APPerms] " + ChatColor.YELLOW + "Groups for " + targetPlayer.getDisplayName()); //
        p.sendMessage(groups); //Send the list of groups to the player
    }
    private void listGroupsConsole(Player targetPlayer){
        String[] groups = perms.getPlayerGroups(targetPlayer); //Get player's groups as a list from Vault
        log.info("[APPerms] Groups for " + targetPlayer.getDisplayName()); //Display "Groups for" as a setup
        for(String group : groups){
            log.info(group);       //Since the console is weird, send each group individually as its own string vs as a list
        }
    }
    private void addRank(Player p, String rank) {
        perms.playerAddGroup(p, rank);           //Modularized call to easily add ranks for a player
    }
    private void delRank(Player p, String rank) {
        perms.playerRemoveGroup(p, rank);        //Modularized call to easily remove ranks for a player
    }
    private void sendMessage(CommandSender sender, String msg){ //Send message to either player or console, strings only (no lists)
        if(sender instanceof Player){ //If the sender is a player
            Player p = (Player) sender; //Initialize the player
            p.sendMessage(ChatColor.DARK_GREEN+"[APPerms] " + ChatColor.YELLOW + msg); //Send the message intended for the player
        }
        else //If the sender isn't a player, it must be the console requesting the info
        {
            log.info("[APPerms] " + msg); //Send the message to the console without fancy formatting
        }
    }
}
