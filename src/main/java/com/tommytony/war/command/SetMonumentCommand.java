package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.structure.Monument;

/**
 * Places a monument
 *
 * @author Tim Düsterhus
 */
public class SetMonumentCommand extends AbstractZoneMakerCommand {
    public SetMonumentCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
        super(handler, sender, args);
    }

    @Override
    public boolean handle() {
        if (!(this.getSender() instanceof Player)) {
            this.badMsg("You can't do this if you are not in-game.");
            return true;
        }

        Player player = (Player) this.getSender();

        if (this.args.length != 1) {
            return false;
        }

        Warzone zone = Warzone.getZoneByLocation(player);

        if (zone == null) {
            return false;
        } else if (!this.isSenderAuthorOfZone(zone)) {
            return true;
        }

        if (this.args[0].equals(zone.getName())) {
            return false;
        }

        if (zone.hasMonument(this.args[0])) {
            // move the existing monument
            Monument monument = zone.getMonument(this.args[0]);
            monument.getVolume().resetBlocks();
            monument.setLocation(player.getLocation());
            this.msg("Monument " + monument.getName() + " was moved.");
        } else {
            // create a new monument
            Monument monument = new Monument(this.args[0], zone, player.getLocation());
            zone.getMonuments().add(monument);
            this.msg("Monument " + monument.getName() + " created.");
        }

        WarzoneYmlMapper.save(zone, false);

        return true;
    }
}
