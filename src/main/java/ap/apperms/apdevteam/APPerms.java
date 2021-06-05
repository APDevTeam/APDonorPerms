package ap.apperms.apdevteam;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class APPerms extends JavaPlugin {
    private static final Logger log = Logger.getLogger("Minecraft"); //Sets up our logger for basic feedback to the console
    private static Permission perms;  //Set up perms with vault
    private String[] donorRanks = getConfig().getStringList("donorRanks").toArray(new String[0]); // Pulls our donor ranks list from config
    private String[] staffRanks = getConfig().getStringList("staffRanks").toArray(new String[0]); // Pulls our admin ranks list from config
    private String[] otherRanks = getConfig().getStringList("otherRanks").toArray(new String[0]); // Pulls our other ranks list from the config
    private String playerRank = getConfig().getString("playerRank");
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
        if(!getConfig().contains("otherRanks")){
            getConfig().createSection("otherRanks"); //Makes the section
            List<String> donorValues = new ArrayList<>();
            donorValues.add("other1");
            getConfig().set("otherRanks", donorValues);  //Creates our example rank
        }
        if(!getConfig().contains("playerRank")){
            getConfig().set("playerRank", "Player");
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
        Player player = null;
        if(args.length > 0) {
            player = Bukkit.getPlayer(args[0]);            //Initialize who the target player is
        }
        if (player == null) {
            if(cmd.getName().equalsIgnoreCase("listgroups") | cmd.getName().equalsIgnoreCase("fixranks") | cmd.getName().equalsIgnoreCase("fixStaff")){
                if (sender instanceof Player){
                    player = (Player) sender;
                }
                else{
                    sendMessage(sender, "Wrong number of arguments!");
                    return false;
                }
            }
            else if(args.length < 1) {
                sendMessage(sender, "Wrong number of arguments!");
                return false;
            }
            else {
                sendMessage(sender, "Player not found.");
                return false;
            }
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
        else if (cmd.getName().equalsIgnoreCase("fixranks")) {
            fixRanks(player);   //Fixes the donor permissions for a player by re-stacking their ranks (top shows up to everyone)
            sendMessage(sender, "Rank stacking fixed.");
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("fixStaff")) {
            fixStaff(player);
            sendMessage(sender, "Added player rank and restacked ranks.");
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("listgroups")){
            if(!(sender instanceof Player)){ //If the console is running this command, use the logger version.
                listGroupsConsole(player);
                return true;
            }
            else{ //Otherwise, if a player is using it, send this to the player requesting the command
                listGroups((Player) sender, player);
                return true;
            }
        }
        else if (cmd.getName().equalsIgnoreCase("apgiveplot")){ // This is an implementation from APPlotPerm, which allows
                                                                            // for admins to give plot donor permissions with the correct stacking for displayed permissions
            //If the player doesn't exist, tell the user & fail
            if(player == null) {
                sendMessage(sender, "**WARNING** Player not found."); //If player isn't found, alert the sender
                return false;
            }
            int num;
            try
            {
                if (args.length == 1)
                    num = 1;    //If a number isn't specified, assume one
                else
                    num = Integer.parseInt(args[1]); //Otherwise, set our add plots number to whatever the user specified
            }
            catch(NumberFormatException e)
            {
                sendMessage(sender, "**WARNING** Invalid number."); //If the number is not a number, alert the user
                return true;
            }

            if (!perms.playerInGroup(player,"plotDonor")) //If player doesn't have the plotDonor rank, give it to them and set them to num plots
            {
                perms.playerAddGroup(null, player, "plotDonor"); // This is where implementation of APPlotPerm to APPerms becomes necessary.
                fixPlotPerm(player);                                          // APPerms allows for easy permission stacking, which APPlotPerm can now take advantage of
                setPlot(player, num);                                         // through these two modules.
            }
            else //Else, add num plots
            {
                addPlot(sender, player, num);
            }
            return true;
        }
        else {
            return false;
        }

    }
    private void setPlot(Player p, int num)
    {
        String temp = "plots.plot." + num;
        perms.playerAdd("donorPlots", p, temp); //Add the player's correct number of plots
    }
    private void unSetPlot(Player p, int num)
    {
        String temp = "plots.plot." + num;
        perms.playerRemove("donorPlots", p, temp); //Remove the old plot permission number as it is not needed
    }
    private void addPlot(CommandSender sender, Player player, int num)
    {
        int current = getPlotCount(player);

        if(current == -1)
        {
            sendMessage(sender, (String.format("Player %s plot count not found.", player.getDisplayName()))); //If we can't find the number of plots already, alert the sender
            return;
        }
        else if(current >= 20)
        {
            sendMessage(sender, (String.format("Player %s already has 20 plots.", player.getDisplayName()))); //If the player already has 20 plots, alert the sender
            return;
        }

        unSetPlot(player, current);         //Unsets the old plot number
        setPlot(player,current + num); //Sets the new plot number
        sendMessage(sender, (String.format("Given player %s permission for %s plots.", player.getDisplayName(), current+num))); //Alert the sender the action is complete
    }
    private int getPlotCount(Player p) //Gets the current plot count of a player
    {
        String testStr;
        for(int i = 0; i <= 20; i++)
        {
            testStr = "plots.plot." + i;
            if(perms.playerHas("donorPlots", p, testStr))
            {
                return i;
            }
        }
        return -1;
    }
    private void addDonor(Player player, String donorRank) {
        for (String testRank : staffRanks) {
            if (perms.playerInGroup(player, testRank)) {
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
    private void fixStaff(Player player) {
        addRank(player, playerRank);
        fixRanks(player);
    }
    private void fixRanks(Player player) {
        //Fix all rank stacking after a rank up (has to be triggered externally)

        for (String testRank : otherRanks) {
            if (perms.playerInGroup(player, testRank)) { //Stack misc ranks to top first
                redoRank(player, testRank);
            }
        }
        for (String testRank : donorRanks) {
            if (perms.playerInGroup(player, testRank)) { //Put donor ranks on top of misc ranks
                redoRank(player, testRank);
            }
        }
        for (String testRank : staffRanks) {
            if (perms.playerInGroup(player, testRank)) { //Put admin ranks on top of all of those
                redoRank(player, testRank);
            }
        }

    }
    private void fixPlotPerm(Player player){ //This is our fix for APPlotPerms permission stacking
        for(String staffTest : staffRanks){
            for(String testRank: donorRanks){ //This stacks any donor ranks above the plotDonor permission
                redoRank(player, testRank);
            }

            if(perms.playerInGroup(player, staffTest)){ //This stacks any admin ranks above both donor and plotDonor permissions
                redoRank(player, staffTest);
                return;
            }
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
    private void redoRank(Player p, String rank) {
        delRank(p, rank);
        addRank(p, rank);

    }

    private void addRank(Player p, String rank) {
        perms.playerAddGroup(null, p, rank);           //Modularized call to easily add ranks for a player
    }

    private void delRank(Player p, String rank) {
        perms.playerRemoveGroup(null, p, rank);        //Modularized call to easily remove ranks for a player
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
