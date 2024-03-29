package com.tommytony.war.command;

import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.tommytony.war.War;


/**
 * Handles commands received by War
 *
 * @author Tim Düsterhus
 * @package bukkit.tommytony.war
 */
public class WarCommandHandler {

    /**
     * Handles a command
     *
     * @param sender
     *                The sender of the command
     * @param cmd
     *                The command
     * @param args
     *                The arguments
     * @return Success
     */
    public boolean handle(CommandSender sender, Command cmd, String[] args) {
        String command = cmd.getName();
        String[] arguments;

        // parse prefixed commands
        if ((command.equals("war") || command.equals("War")) && args.length > 0) {
            command = args[0];
            arguments = new String[args.length - 1];
            System.arraycopy(args, 1, arguments, 0, arguments.length);

            if (arguments.length == 1 && (arguments[0].equals("help") || arguments[0].equals("h"))) {
                // show /war help
                War.war.badMsg(sender, cmd.getUsage());
                return true;
            }
        } else if (command.equals("war") || command.equals("War")) {
            // show /war help
            War.war.msg(sender, cmd.getUsage());
            return true;
        } else {
            arguments = args;
        }

        AbstractWarCommand commandObj = null;
        try {
            if (command.equals("warhub")) {
                commandObj = new WarhubCommand(this, sender, arguments);
            } else if (command.equals("zones") || command.equals("warzones")) {
                commandObj = new WarzonesCommand(this, sender, arguments);
            } else if (command.equals("zone") || command.equals("warzone")) {
                commandObj = new WarzoneCommand(this, sender, arguments);
            } else if (command.equals("teams")) {
                commandObj = new TeamsCommand(this, sender, arguments);
            } else if (command.equals("join")) {
                commandObj = new JoinCommand(this, sender, arguments);
            } else if (command.equals("leave")) {
                commandObj = new LeaveCommand(this, sender, arguments);
            } else if (command.equals("team")) {
                commandObj = new TeamCommand(this, sender, arguments);
            } else if (command.equals("setzone")) {
                commandObj = new SetZoneCommand(this, sender, arguments);
            } else if (command.equals("deletezone")) {
                commandObj = new DeleteZoneCommand(this, sender, arguments);
            } else if (command.equals("setzonelobby")) {
                commandObj = new SetZoneLobbyCommand(this, sender, arguments);
            } else if (command.equals("savezone")) {
                commandObj = new SaveZoneCommand(this, sender, arguments);
            } else if (command.equals("resetzone")) {
                commandObj = new ResetZoneCommand(this, sender, arguments);
            } else if (command.equals("nextbattle")) {
                commandObj = new NextBattleCommand(this, sender, arguments);
            } else if (command.equals("renamezone")) {
                commandObj = new RenameZoneCommand(this, sender, arguments);
            } else if (command.equals("setteam")) {
                commandObj = new SetTeamCommand(this, sender, arguments);
            } else if (command.equals("deleteteam")) {
                commandObj = new DeleteTeamCommand(this, sender, arguments);
            } else if (command.equals("setteamflag")) {
                commandObj = new SetTeamFlagCommand(this, sender, arguments);
            } else if (command.equals("deleteteamflag")) {
                commandObj = new DeleteTeamFlagCommand(this, sender, arguments);
            } else if (command.equals("setmonument")) {
                commandObj = new SetMonumentCommand(this, sender, arguments);
            } else if (command.equals("deletemonument")) {
                commandObj = new DeleteMonumentCommand(this, sender, arguments);
            } else if (command.equals("setbomb")) {
                commandObj = new SetBombCommand(this, sender, arguments);
            } else if (command.equals("deletebomb")) {
                commandObj = new DeleteBombCommand(this, sender, arguments);
            } else if (command.equals("setcake")) {
                commandObj = new SetCakeCommand(this, sender, arguments);
            } else if (command.equals("deletecake")) {
                commandObj = new DeleteCakeCommand(this, sender, arguments);
            }else if (command.equals("setteamconfig") || command.equals("teamcfg")) {
                commandObj = new SetTeamConfigCommand(this, sender, arguments);
            } else if (command.equals("setzoneconfig") || command.equals("zonecfg")) {
                commandObj = new SetZoneConfigCommand(this, sender, arguments);
            } else if (command.equals("setwarhub")) {
                commandObj = new SetWarHubCommand(this, sender, arguments);
            } else if (command.equals("deletewarhub")) {
                commandObj = new DeleteWarhubCommand(this, sender, arguments);
            } else if (command.equals("loadwar")) {
                commandObj = new LoadWarCommand(this, sender, arguments);
            } else if (command.equals("unloadwar")) {
                commandObj = new UnloadWarCommand(this, sender, arguments);
            } else if (command.equals("setwarconfig") || command.equals("warcfg")) {
                commandObj = new SetWarConfigCommand(this, sender, arguments);
            } else if (command.equals("zonemaker") || command.equals("zm")) {
                commandObj = new ZoneMakerCommand(this, sender, arguments);
            }
            // we are not responsible for any other command
        } catch (NotWarAdminException e) {
            War.war.badMsg(sender, "You can't do this if you are not a War admin (permission war.admin).");
        } catch (NotZoneMakerException e) {
            War.war.badMsg(sender, "You can't do this if you are not a warzone maker (permission war.zonemaker).");
        } catch (Exception e) {
            War.war.log("An error occured while handling command " + cmd.getName() + ". Exception:" + e.getClass().toString() + " " + e.getMessage(), Level.WARNING);
            e.printStackTrace();
        }

        if(commandObj != null) {
            boolean handled = commandObj.handle();
            if(!handled) {
                War.war.badMsg(sender, cmd.getUsage());
            }
        }

        return true;
    }
}
