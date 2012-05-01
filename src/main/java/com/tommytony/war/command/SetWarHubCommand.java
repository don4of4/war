package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.WarYmlMapper;
import com.tommytony.war.structure.WarHub;

/**
 * Places the warhub
 *
 * @author Tim Düsterhus
 */
public class SetWarHubCommand extends AbstractWarAdminCommand {
	public SetWarHubCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotWarAdminException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}

		if (this.args.length != 0) {
			return false;
		}
		Player player = (Player) this.getSender();

		if (War.war.getWarzones().size() > 0) {
			if (War.war.getWarHub() != null) {
				// reset existing hub
				War.war.getWarHub().getVolume().resetBlocks();
				War.war.getWarHub().setLocation(player.getLocation());
				War.war.getWarHub().initialize();
				this.msg("War hub moved.");
			} else {
				War.war.setWarHub(new WarHub(player.getLocation()));
				War.war.getWarHub().initialize();
				for (Warzone zone : War.war.getWarzones()) {
					if (zone.getLobby() != null) {
						zone.getLobby().getVolume().resetBlocks();
						zone.getLobby().initialize();
					}
				}
				this.msg("War hub created.");
			}
			WarYmlMapper.save();
		} else {
			this.msg("No warzones yet.");
		}

		return true;
	}
}
