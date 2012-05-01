package com.tommytony.war.structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;


import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.utility.SignHelper;
import com.tommytony.war.volume.BlockInfo;
import com.tommytony.war.volume.Volume;
import com.tommytony.war.volume.ZoneVolume;

/**
 *
 * @author tommytony
 *
 */
public class ZoneLobby {
	private final Warzone warzone;
	private BlockFace wall;
	private Volume volume;
	BlockInfo lobbyMiddleWallBlock = null; // on the zone wall, one above the zone lobby floor

	BlockInfo warHubLinkGate = null;

	Map<String, BlockInfo> teamGateBlocks = new HashMap<String, BlockInfo>();
	BlockInfo autoAssignGate = null;

	BlockInfo zoneTeleportBlock = null;

	private final int lobbyHeight = 3;
	private int lobbyHalfSide;
	private final int lobbyDepth = 10;

	/**
	 * Use this constructor with /setzonelobby <n/s/e/w>
	 *
	 * @param war
	 * @param warzone
	 * @param wall
	 *                On which wall of the warzone will the lobby be stuck to at mid-weight
	 */
	public ZoneLobby(Warzone warzone, BlockFace wall) {
		this.warzone = warzone;
		int lobbyWidth = warzone.getTeams().size() * 4 + 5;
		this.lobbyHalfSide = lobbyWidth / 2;
		if (this.lobbyHalfSide < 7) {
			this.lobbyHalfSide = 7;
		}
		this.setWall(wall);
	}

	/**
	 * Use this constructor with /setzonelobby <zonename>. Makes sure the lobby is not sticking inside the zone.
	 *
	 * @param war
	 * @param warzone
	 * @param wall
	 *                On which wall of the warzone will the lobby be stuck to at mid-weight
	 */
	public ZoneLobby(Warzone warzone, Location playerLocation) {
		this.warzone = warzone;
		int lobbyWidth = warzone.getTeams().size() * 4 + 5;
		this.lobbyHalfSide = lobbyWidth / 2;
		if (this.lobbyHalfSide < 7) {
			this.lobbyHalfSide = 7;
		}
		this.setLocation(playerLocation);
	}

	/**
	 * Convenience ctor when loading form disk. This figures out the middle wall block of the lobby from the volume instead of the other way around.
	 */
	public ZoneLobby(Warzone warzone, BlockFace wall, Volume volume) {
		this.warzone = warzone;
		int lobbyWidth = warzone.getTeams().size() * 4 + 5;
		this.lobbyHalfSide = lobbyWidth / 2;
		if (this.lobbyHalfSide < 7) {
			this.lobbyHalfSide = 7;
		}
		this.wall = wall;
		this.setVolume(volume);

		// we're setting the zoneVolume directly, so we need to figure out the lobbyMiddleWallBlock on our own
		if (wall == BlockFace.NORTH) {
			this.lobbyMiddleWallBlock = new BlockInfo(BlockInfo.getBlock(warzone.getWorld(), volume.getCornerOne()).getRelative(BlockFace.UP).getRelative(BlockFace.EAST, this.lobbyHalfSide));
		} else if (wall == BlockFace.EAST) {
			this.lobbyMiddleWallBlock = new BlockInfo(BlockInfo.getBlock(warzone.getWorld(), volume.getCornerOne()).getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH, this.lobbyHalfSide));
		} else if (wall == BlockFace.SOUTH) {
			this.lobbyMiddleWallBlock = new BlockInfo(BlockInfo.getBlock(warzone.getWorld(), volume.getCornerOne()).getRelative(BlockFace.UP).getRelative(BlockFace.WEST, this.lobbyHalfSide));
		} else if (wall == BlockFace.WEST) {
			this.lobbyMiddleWallBlock = new BlockInfo(BlockInfo.getBlock(warzone.getWorld(), volume.getCornerOne()).getRelative(BlockFace.UP).getRelative(BlockFace.NORTH, this.lobbyHalfSide));
		}
	}

	public static ZoneLobby getLobbyByLocation(Location location) {
		for (Warzone warzone : War.war.getWarzones()) {
			if (warzone.getLobby() != null && warzone.getLobby().getVolume() != null && warzone.getLobby().getVolume().contains(location)) {
				return warzone.getLobby();
			}
		}
		return null;
	}

	public static ZoneLobby getLobbyByLocation(Player player) {
		return ZoneLobby.getLobbyByLocation(player.getLocation());
	}

	/**
	 * Changes the lobby's position. Orientation is determined from the player location. Creates volume or resets. Saves new lobby blocks.
	 *
	 * @param playerLocation
	 */
	public void setLocation(Location playerLocation) {
		World lobbyWorld = playerLocation.getWorld();
		this.createVolumeOrReset(lobbyWorld);

		// Lobby orientation
		int yaw = 0;
		if (playerLocation.getYaw() >= 0) {
			yaw = (int) (playerLocation.getYaw() % 360);
		} else {
			yaw = (int) (360 + (playerLocation.getYaw() % 360));
		}
		BlockFace facing = null;
		BlockFace opposite = null;
		if ((yaw >= 0 && yaw < 45) || (yaw >= 315 && yaw <= 360)) {
			facing = BlockFace.WEST;
			opposite = BlockFace.EAST;
		} else if (yaw >= 45 && yaw < 135) {
			facing = BlockFace.NORTH;
			opposite = BlockFace.SOUTH;
		} else if (yaw >= 135 && yaw < 225) {
			facing = BlockFace.EAST;
			opposite = BlockFace.WEST;
		} else if (yaw >= 225 && yaw < 315) {
			facing = BlockFace.SOUTH;
			opposite = BlockFace.NORTH;
		}

		this.wall = opposite; // a player facing south places a lobby that looks just like a lobby stuck to the north wall

		this.calculateLobbyWidth();
		this.lobbyMiddleWallBlock = new BlockInfo(lobbyWorld.getBlockAt(playerLocation.getBlockX(), playerLocation.getBlockY(), playerLocation.getBlockZ()).getRelative(facing, 6));

		Block corner1 = null;
		Block corner2 = null;
		int x = this.lobbyMiddleWallBlock.getX();
		int y = this.lobbyMiddleWallBlock.getY();
		int z = this.lobbyMiddleWallBlock.getZ();

		if (this.wall == BlockFace.NORTH) {
			corner1 = lobbyWorld.getBlockAt(x, y - 1, z + this.lobbyHalfSide);
			corner2 = lobbyWorld.getBlockAt(x - this.lobbyDepth, y + 1 + this.lobbyHeight, z - this.lobbyHalfSide);
		} else if (this.wall == BlockFace.EAST) {
			corner1 = lobbyWorld.getBlockAt(x - this.lobbyHalfSide, y - 1, z);
			corner2 = lobbyWorld.getBlockAt(x + this.lobbyHalfSide, y + 1 + this.lobbyHeight, z - this.lobbyDepth);
		} else if (this.wall == BlockFace.SOUTH) {
			corner1 = lobbyWorld.getBlockAt(x, y - 1, z - this.lobbyHalfSide);
			corner2 = lobbyWorld.getBlockAt(x + this.lobbyDepth, y + 1 + this.lobbyHeight, z + this.lobbyHalfSide);
		} else if (this.wall == BlockFace.WEST) {
			corner1 = lobbyWorld.getBlockAt(x + this.lobbyHalfSide, y - 1, z);
			corner2 = lobbyWorld.getBlockAt(x - this.lobbyHalfSide, y + 1 + this.lobbyHeight, z + this.lobbyDepth);
		}

		this.saveLobbyBlocks(corner1, corner2);
	}

	/**
	 * Classic way of creating a lobby. Lobby position goes to middle of zone wall. Creates volume or resets. Saves new lobby blocks.
	 *
	 * @param newWall
	 */
	public void setWall(BlockFace newWall) {
		this.createVolumeOrReset(this.warzone.getWorld());	// when attached to the warzone, lobby is in same world
		this.wall = newWall;

		ZoneVolume zoneVolume = this.warzone.getVolume();
		this.calculateLobbyWidth();

		Block corner1 = null;
		Block corner2 = null;

		if (this.wall == BlockFace.NORTH) {
			int wallStart = zoneVolume.getMinZ();
			int wallEnd = zoneVolume.getMaxZ();
			int x = zoneVolume.getMinX();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int y = zoneVolume.getCenterY();
			this.lobbyMiddleWallBlock = new BlockInfo(this.warzone.getWorld().getBlockAt(x, y, wallCenterPos));
			corner1 = this.warzone.getWorld().getBlockAt(x, y - 1, wallCenterPos + this.lobbyHalfSide);
			corner2 = this.warzone.getWorld().getBlockAt(x - this.lobbyDepth, y + 1 + this.lobbyHeight, wallCenterPos - this.lobbyHalfSide);
		} else if (this.wall == BlockFace.EAST) {
			int wallStart = zoneVolume.getMinX();
			int wallEnd = zoneVolume.getMaxX();
			int z = zoneVolume.getMinZ();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int y = zoneVolume.getCenterY();
			this.lobbyMiddleWallBlock = new BlockInfo(this.warzone.getWorld().getBlockAt(wallCenterPos, y, z));
			corner1 = this.warzone.getWorld().getBlockAt(wallCenterPos - this.lobbyHalfSide, y - 1, z);
			corner2 = this.warzone.getWorld().getBlockAt(wallCenterPos + this.lobbyHalfSide, y + 1 + this.lobbyHeight, z - this.lobbyDepth);
		} else if (this.wall == BlockFace.SOUTH) {
			int wallStart = zoneVolume.getMinZ();
			int wallEnd = zoneVolume.getMaxZ();
			int x = zoneVolume.getMaxX();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int y = zoneVolume.getCenterY();
			this.lobbyMiddleWallBlock = new BlockInfo(this.warzone.getWorld().getBlockAt(x, y, wallCenterPos));
			corner1 = this.warzone.getWorld().getBlockAt(x, y - 1, wallCenterPos - this.lobbyHalfSide);
			corner2 = this.warzone.getWorld().getBlockAt(x + this.lobbyDepth, y + 1 + this.lobbyHeight, wallCenterPos + this.lobbyHalfSide);
		} else if (this.wall == BlockFace.WEST) {
			int wallStart = zoneVolume.getMinX();
			int wallEnd = zoneVolume.getMaxX();
			int z = zoneVolume.getMaxZ();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int y = zoneVolume.getCenterY();
			this.lobbyMiddleWallBlock = new BlockInfo(this.warzone.getWorld().getBlockAt(wallCenterPos, y, z));
			corner1 = this.warzone.getWorld().getBlockAt(wallCenterPos + this.lobbyHalfSide, y - 1, z);
			corner2 = this.warzone.getWorld().getBlockAt(wallCenterPos - this.lobbyHalfSide, y + 1 + this.lobbyHeight, z + this.lobbyDepth);
		}

		this.saveLobbyBlocks(corner1, corner2);
	}

	private void createVolumeOrReset(World lobbyWorld) {
		if (this.volume == null) {
			// no previous wall
			this.volume = new Volume("lobby", lobbyWorld);
		} else if (this.volume.isSaved()) {
			this.volume.resetBlocks();
			this.volume.setWorld(lobbyWorld);	// set world for the case where where are changing lobby location between worlds	
		}
	}

	private void calculateLobbyWidth() {
		int noOfTeams = this.warzone.getTeams().size();
		if (this.warzone.getWarzoneConfig().getBoolean(WarzoneConfig.AUTOASSIGN)) {
			noOfTeams = 1;
		}
		int lobbyWidth = noOfTeams * 4 + 5;
		this.lobbyHalfSide = lobbyWidth / 2;
		if (this.lobbyHalfSide < 7) {
			this.lobbyHalfSide = 7;
		}
	}

	private void saveLobbyBlocks(Block corner1, Block corner2) {
		if (corner1 != null && corner2 != null) {
			// save the blocks, wide enough for three team gates, 3+1 high and 10 deep, extruding out from the zone wall.
			this.volume.setCornerOne(corner1);
			this.volume.setCornerTwo(corner2);
			this.volume.saveBlocks();
		}
	}

	public void initialize() {
		// maybe the number of teams change, now reset the gate positions
		if (this.lobbyMiddleWallBlock != null && this.volume != null /* && volume.isSaved() */) {
			this.setGatePositions(BlockInfo.getBlock(this.volume.getWorld(), this.lobbyMiddleWallBlock));
			// flatten the area (set all but floor to air, then replace any floor air blocks with glass)
			this.volume.clearBlocksThatDontFloat();
			this.volume.setToMaterial(Material.AIR);
			this.volume.setFaceMaterial(BlockFace.DOWN, Material.GLASS); // beautiful

			// add war hub link gate
			if (War.war.getWarHub() != null) {
				Block linkGateBlock = BlockInfo.getBlock(this.volume.getWorld(), this.warHubLinkGate);
				this.placeGate(linkGateBlock, Material.OBSIDIAN);
				// add warhub sign
				String[] lines = new String[4];
				lines[0] = "";
				lines[1] = "To War hub";
				lines[2] = "";
				lines[3] = "";
				this.resetGateSign(linkGateBlock, lines, false);
			}

			// add team gates or single auto assign gate
			this.placeAutoAssignGate();
			for (String teamName : this.teamGateBlocks.keySet()) {
				BlockInfo gateInfo = this.teamGateBlocks.get(teamName);
				this.placeGate(BlockInfo.getBlock(this.volume.getWorld(), gateInfo), TeamKind.teamKindFromString(teamName));
			}
			for (Team t : this.warzone.getTeams()) {
				this.resetTeamGateSign(t);
			}

			// set zone tp
			this.zoneTeleportBlock = new BlockInfo(BlockInfo.getBlock(this.volume.getWorld(), this.lobbyMiddleWallBlock).getRelative(this.wall, 6));
			int yaw = 0;
			if (this.wall == BlockFace.WEST) {
				yaw = 180;
			} else if (this.wall == BlockFace.SOUTH) {
				yaw = 90;
			} else if (this.wall == BlockFace.EAST) {
				yaw = 0;
			} else if (this.wall == BlockFace.NORTH) {
				yaw = 270;
			}
			this.warzone.setTeleport(new Location(this.volume.getWorld(), this.zoneTeleportBlock.getX(), this.zoneTeleportBlock.getY(), this.zoneTeleportBlock.getZ(), yaw, 0));

			// set zone sign
			Block zoneSignBlock = BlockInfo.getBlock(this.volume.getWorld(), this.lobbyMiddleWallBlock).getRelative(this.wall, 4);
			byte data = 0;
			if (this.wall == BlockFace.NORTH) {
				data = (byte) 4;
			} else if (this.wall == BlockFace.EAST) {
				data = (byte) 8;
			} else if (this.wall == BlockFace.SOUTH) {
				data = (byte) 12;
			} else if (this.wall == BlockFace.WEST) {
				data = (byte) 0;
			}
			String[] lines = new String[4];
			lines[0] = "Warzone";
			lines[1] = this.warzone.getName();
			if (this.autoAssignGate != null) {
				lines[2] = "Enter the auto-";
				lines[3] = "assign gate.";
			} else {
				lines[2] = "";
				lines[3] = "Pick your team.";
			}
			SignHelper.setToSign(War.war, zoneSignBlock, data, lines);

			// lets get some light in here
			if (this.wall == BlockFace.NORTH || this.wall == BlockFace.SOUTH) {
				BlockInfo.getBlock(this.volume.getWorld(), this.lobbyMiddleWallBlock).getRelative(BlockFace.DOWN).getRelative(BlockFace.WEST, this.lobbyHalfSide - 1).getRelative(this.wall, 9).setType(Material.GLOWSTONE);
				BlockInfo.getBlock(this.volume.getWorld(), this.lobbyMiddleWallBlock).getRelative(BlockFace.DOWN).getRelative(BlockFace.EAST, this.lobbyHalfSide - 1).getRelative(this.wall, 9).setType(Material.GLOWSTONE);
			} else {
				BlockInfo.getBlock(this.volume.getWorld(), this.lobbyMiddleWallBlock).getRelative(BlockFace.DOWN).getRelative(BlockFace.NORTH, this.lobbyHalfSide - 1).getRelative(this.wall, 9).setType(Material.GLOWSTONE);
				BlockInfo.getBlock(this.volume.getWorld(), this.lobbyMiddleWallBlock).getRelative(BlockFace.DOWN).getRelative(BlockFace.SOUTH, this.lobbyHalfSide - 1).getRelative(this.wall, 9).setType(Material.GLOWSTONE);
			}
		} else {
			War.war.log("Failed to initalize zone lobby for zone " + this.warzone.getName(), java.util.logging.Level.WARNING);
		}
	}

	private void setGatePositions(Block lobbyMiddleWallBlock) {
		BlockFace leftSide = null; // look at the zone
		BlockFace rightSide = null;
		if (this.wall == BlockFace.NORTH) {
			leftSide = BlockFace.EAST;
			rightSide = BlockFace.WEST;
		} else if (this.wall == BlockFace.EAST) {
			leftSide = BlockFace.SOUTH;
			rightSide = BlockFace.NORTH;
		} else if (this.wall == BlockFace.SOUTH) {
			leftSide = BlockFace.WEST;
			rightSide = BlockFace.EAST;
		} else if (this.wall == BlockFace.WEST) {
			leftSide = BlockFace.NORTH;
			rightSide = BlockFace.SOUTH;
		}
		this.teamGateBlocks.clear();
		if (this.warzone.getWarzoneConfig().getBoolean(WarzoneConfig.AUTOASSIGN)) {
			this.autoAssignGate = new BlockInfo(lobbyMiddleWallBlock);
		} else {
			this.autoAssignGate = null;
			for (int doorIndex = 0; doorIndex < this.warzone.getTeams().size(); doorIndex++) {
				// 0 at center, 1 to the left, 2 to the right, 3 to the left, etc
				Team team = this.warzone.getTeams().get(doorIndex);
				if (this.warzone.getTeams().size() % 2 == 0) {
					// even number of teams
					if (doorIndex % 2 == 0) {
						this.teamGateBlocks.put(team.getName(), new BlockInfo(lobbyMiddleWallBlock.getRelative(rightSide, doorIndex * 2 + 2)));
					} else {
						this.teamGateBlocks.put(team.getName(), new BlockInfo(lobbyMiddleWallBlock.getRelative(leftSide, doorIndex * 2)));
					}

				} else {
					if (doorIndex == 0) {
						this.teamGateBlocks.put(team.getName(), new BlockInfo(lobbyMiddleWallBlock));
					} else if (doorIndex % 2 == 0) {
						this.teamGateBlocks.put(team.getName(), new BlockInfo(lobbyMiddleWallBlock.getRelative(rightSide, doorIndex * 2)));
					} else {
						this.teamGateBlocks.put(team.getName(), new BlockInfo(lobbyMiddleWallBlock.getRelative(leftSide, doorIndex * 2 + 2)));
					}
				}
			}
		}
		this.warHubLinkGate = new BlockInfo(lobbyMiddleWallBlock.getRelative(this.wall, 9));
	}

	private void placeGate(Block block, TeamKind teamKind) {
		if (block != null) {
			BlockFace leftSide = null; // look at the zone
			BlockFace rightSide = null;
			if (this.wall == BlockFace.NORTH) {
				leftSide = BlockFace.EAST;
				rightSide = BlockFace.WEST;
			} else if (this.wall == BlockFace.EAST) {
				leftSide = BlockFace.SOUTH;
				rightSide = BlockFace.NORTH;
			} else if (this.wall == BlockFace.SOUTH) {
				leftSide = BlockFace.WEST;
				rightSide = BlockFace.EAST;
			} else if (this.wall == BlockFace.WEST) {
				leftSide = BlockFace.NORTH;
				rightSide = BlockFace.SOUTH;
			}
			block.getRelative(BlockFace.DOWN).setType(Material.GLOWSTONE);
			this.setBlock(block.getRelative(leftSide), teamKind);
			this.setBlock(block.getRelative(rightSide).getRelative(BlockFace.UP), teamKind);
			this.setBlock(block.getRelative(leftSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP), teamKind);
			this.setBlock(block.getRelative(rightSide), teamKind);
			this.setBlock(block.getRelative(leftSide).getRelative(BlockFace.UP), teamKind);
			this.setBlock(block.getRelative(rightSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP), teamKind);
			this.setBlock(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP), teamKind);
		}
	}

	private void placeGate(Block block, Material material) {
		if (block != null) {
			BlockFace leftSide = null; // look at the zone
			BlockFace rightSide = null;
			if (this.wall == BlockFace.NORTH) {
				leftSide = BlockFace.EAST;
				rightSide = BlockFace.WEST;
			} else if (this.wall == BlockFace.EAST) {
				leftSide = BlockFace.SOUTH;
				rightSide = BlockFace.NORTH;
			} else if (this.wall == BlockFace.SOUTH) {
				leftSide = BlockFace.WEST;
				rightSide = BlockFace.EAST;
			} else if (this.wall == BlockFace.WEST) {
				leftSide = BlockFace.NORTH;
				rightSide = BlockFace.SOUTH;
			}
			block.getRelative(BlockFace.DOWN).setType(Material.GLOWSTONE);
			this.setBlock(block.getRelative(leftSide), material);
			this.setBlock(block.getRelative(rightSide).getRelative(BlockFace.UP), material);
			this.setBlock(block.getRelative(leftSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP), material);
			this.setBlock(block.getRelative(rightSide), material);
			this.setBlock(block.getRelative(leftSide).getRelative(BlockFace.UP), material);
			this.setBlock(block.getRelative(rightSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP), material);
			this.setBlock(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP), material);
		}
	}

	private void setBlock(Block block, TeamKind kind) {
		block.setType(kind.getMaterial());
		block.setData(kind.getData());
	}

	private void setBlock(Block block, Material material) {
		block.setType(material);
	}

	private void placeAutoAssignGate() {
		if (this.autoAssignGate != null) {
			BlockFace leftSide = null; // look at the zone
			BlockFace rightSide = null;
			if (this.wall == BlockFace.NORTH) {
				leftSide = BlockFace.EAST;
				rightSide = BlockFace.WEST;
			} else if (this.wall == BlockFace.EAST) {
				leftSide = BlockFace.SOUTH;
				rightSide = BlockFace.NORTH;
			} else if (this.wall == BlockFace.SOUTH) {
				leftSide = BlockFace.WEST;
				rightSide = BlockFace.EAST;
			} else if (this.wall == BlockFace.WEST) {
				leftSide = BlockFace.NORTH;
				rightSide = BlockFace.SOUTH;
			}
			List<Team> teams = this.warzone.getTeams();
			
			Block autoAssignGateBlock = BlockInfo.getBlock(this.volume.getWorld(), this.autoAssignGate);
			this.setBlock(autoAssignGateBlock.getRelative(BlockFace.DOWN), (Material.GLOWSTONE));
			int size = teams.size();
			if (size > 0) {
				TeamKind[] doorBlockKinds = new TeamKind[7];
				for (int i = 0; i < 7; i++) {
					doorBlockKinds[i] = teams.get(i % size).getKind();
				}
				this.setBlock(autoAssignGateBlock.getRelative(leftSide), doorBlockKinds[0]);
				this.setBlock(autoAssignGateBlock.getRelative(leftSide).getRelative(BlockFace.UP), doorBlockKinds[1]);
				this.setBlock(autoAssignGateBlock.getRelative(leftSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP), doorBlockKinds[2]);
				this.setBlock(autoAssignGateBlock.getRelative(BlockFace.UP).getRelative(BlockFace.UP), doorBlockKinds[3]);
				this.setBlock(autoAssignGateBlock.getRelative(rightSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP), doorBlockKinds[4]);
				this.setBlock(autoAssignGateBlock.getRelative(rightSide).getRelative(BlockFace.UP), doorBlockKinds[5]);
				this.setBlock(autoAssignGateBlock.getRelative(rightSide), doorBlockKinds[6]);
			}
		}
	}

	public boolean isInTeamGate(Team team, Location location) {
		BlockInfo info = this.teamGateBlocks.get(team.getName());
		if (info != null) {
			if (location.getBlockX() == info.getX() && location.getBlockY() == info.getY() && location.getBlockZ() == info.getZ()) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isAutoAssignGate(Location location) {
		if (this.autoAssignGate != null 
				&& (location.getBlockX() == this.autoAssignGate.getX() 
						&& location.getBlockY() == this.autoAssignGate.getY() 
						&& location.getBlockZ() == this.autoAssignGate.getZ())) {
			return true;
		}
		
		return false;
	}

	public Volume getVolume() {
		return this.volume;
	}

	public void setVolume(Volume volume) {
		this.volume = volume;
	}

	public BlockFace getWall() {
		return this.wall;
	}

	public boolean isInWarHubLinkGate(Location location) {
		if (this.warHubLinkGate != null 
				&& location.getBlockX() == this.warHubLinkGate.getX() 
				&& location.getBlockY() == this.warHubLinkGate.getY() 
				&& location.getBlockZ() == this.warHubLinkGate.getZ()) {
			return true;
		}
		
		return false;
	}

	public boolean blockIsAGateBlock(Block block, BlockFace blockWall) {
		if (blockWall == this.wall) {
			for (String teamName : this.teamGateBlocks.keySet()) {
				BlockInfo gateInfo = this.teamGateBlocks.get(teamName);
				if (this.isPartOfGate(BlockInfo.getBlock(this.volume.getWorld(), gateInfo), block)) {
					return true;
				}
			}
			
			if (this.autoAssignGate != null && this.isPartOfGate(BlockInfo.getBlock(this.volume.getWorld(), this.autoAssignGate), block)) {
				// auto assign
				return true;
			}
		}
		return false;
	}

	private boolean isPartOfGate(Block gateBlock, Block block) {
		if (gateBlock != null) {
			BlockFace leftSide = null; // look at the zone
			BlockFace rightSide = null;
			if (this.wall == BlockFace.NORTH) {
				leftSide = BlockFace.EAST;
				rightSide = BlockFace.WEST;
			} else if (this.wall == BlockFace.EAST) {
				leftSide = BlockFace.SOUTH;
				rightSide = BlockFace.NORTH;
			} else if (this.wall == BlockFace.SOUTH) {
				leftSide = BlockFace.WEST;
				rightSide = BlockFace.EAST;
			} else if (this.wall == BlockFace.WEST) {
				leftSide = BlockFace.NORTH;
				rightSide = BlockFace.SOUTH;
			}
			return (block.getX() == gateBlock.getX() && block.getY() == gateBlock.getY() && block.getZ() == gateBlock.getZ()) || (block.getX() == gateBlock.getRelative(BlockFace.UP).getX() && block.getY() == gateBlock.getRelative(BlockFace.UP).getY() && block.getZ() == gateBlock.getRelative(BlockFace.UP).getZ()) || (block.getX() == gateBlock.getRelative(leftSide).getX() && block.getY() == gateBlock.getRelative(leftSide).getY() && block.getZ() == gateBlock.getRelative(leftSide).getZ()) || (block.getX() == gateBlock.getRelative(leftSide).getRelative(BlockFace.UP).getX() && block.getY() == gateBlock.getRelative(leftSide).getRelative(BlockFace.UP).getY() && block.getZ() == gateBlock.getRelative(leftSide).getRelative(BlockFace.UP).getZ()) || (block.getX() == gateBlock.getRelative(leftSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP).getX() && block.getY() == gateBlock.getRelative(leftSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP).getY() && block.getZ() == gateBlock.getRelative(leftSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP).getZ()) || (block.getX() == gateBlock.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getX() && block.getY() == gateBlock.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getY() && block.getZ() == gateBlock.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getZ()) || (block.getX() == gateBlock.getRelative(rightSide).getRelative(BlockFace.UP).getX() && block.getY() == gateBlock.getRelative(rightSide).getRelative(BlockFace.UP).getY() && block.getZ() == gateBlock.getRelative(rightSide).getRelative(BlockFace.UP).getZ()) || (block.getX() == gateBlock.getRelative(rightSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP).getX() && block.getY() == gateBlock.getRelative(rightSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP).getY() && block.getZ() == gateBlock.getRelative(rightSide).getRelative(BlockFace.UP).getRelative(BlockFace.UP).getZ()) || (block.getX() == gateBlock.getRelative(rightSide).getX() && block.getY() == gateBlock.getRelative(rightSide).getY() && block.getZ() == gateBlock.getRelative(rightSide).getZ()) || (block.getX() == gateBlock.getX() && block.getY() == gateBlock.getY() - 1 && block.getZ() == gateBlock.getZ());
		}
		return false;
	}

	public Warzone getZone() {
		return this.warzone;
	}

	public void resetTeamGateSign(Team team) {
		BlockInfo info = this.teamGateBlocks.get(team.getName());
		if (info != null) {
			this.resetTeamGateSign(team, BlockInfo.getBlock(this.volume.getWorld(), info));
		}
	}

	private void resetTeamGateSign(Team team, Block gate) {
		if (gate != null) {
			String[] lines = new String[4];
			lines[0] = "Team " + team.getName();
			lines[1] = team.getPlayers().size() + "/" + team.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE) + " players";
			lines[2] = team.getPoints() + "/" + team.getTeamConfig().resolveInt(TeamConfig.MAXSCORE) + " pts";
			if (team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL) == -1) {
				lines[3] = "unlimited lives";
			} else {
				lines[3] = team.getRemainingLifes() + "/" + team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL) + " lives left";
			}
			this.resetGateSign(gate, lines, true);
		}
	}

	private void resetGateSign(Block gate, String[] lines, boolean awayFromWall) {
		Block block = null;
		BlockFace direction = null;
		if (awayFromWall) {
			direction = this.wall;
		} else if (this.wall == BlockFace.NORTH) {
			direction = BlockFace.SOUTH;
		} else if (this.wall == BlockFace.EAST) {
			direction = BlockFace.WEST;
		} else if (this.wall == BlockFace.SOUTH) {
			direction = BlockFace.NORTH;
		} else if (this.wall == BlockFace.WEST) {
			direction = BlockFace.EAST;
		}
		byte data = 0;
		if (this.wall == BlockFace.NORTH) {
			block = gate.getRelative(direction).getRelative(BlockFace.EAST);
			if (awayFromWall) {
				data = (byte) 4;
			} else {
				data = (byte) 12;
			}
		} else if (this.wall == BlockFace.EAST) {
			block = gate.getRelative(direction).getRelative(BlockFace.SOUTH);
			if (awayFromWall) {
				data = (byte) 8;
			} else {
				data = (byte) 0;
			}
		} else if (this.wall == BlockFace.SOUTH) {
			block = gate.getRelative(direction).getRelative(BlockFace.WEST);
			if (awayFromWall) {
				data = (byte) 12;
			} else {
				data = (byte) 4;
			}
		} else if (this.wall == BlockFace.WEST) {
			block = gate.getRelative(direction).getRelative(BlockFace.NORTH);
			if (awayFromWall) {
				data = (byte) 0;
			} else {
				data = (byte) 8;
			}
		}

		SignHelper.setToSign(War.war, block, data, lines);
	}

	public boolean isLeavingZone(Location location) {
		BlockFace inside = null;
		BlockFace left = null;
		BlockFace right = null;
		if (this.wall == BlockFace.NORTH) {
			inside = BlockFace.SOUTH;
			left = BlockFace.WEST;
			right = BlockFace.EAST;
		} else if (this.wall == BlockFace.EAST) {
			inside = BlockFace.WEST;
			left = BlockFace.NORTH;
			right = BlockFace.SOUTH;
		} else if (this.wall == BlockFace.SOUTH) {
			inside = BlockFace.NORTH;
			left = BlockFace.EAST;
			right = BlockFace.WEST;
		} else if (this.wall == BlockFace.WEST) {
			inside = BlockFace.EAST;
			left = BlockFace.SOUTH;
			right = BlockFace.NORTH;
		}
		if (this.autoAssignGate != null) {
			if (this.leaving(location, BlockInfo.getBlock(this.volume.getWorld(), this.autoAssignGate), inside, left, right)) {
				return true;
			}
		}
		for (String teamName : this.teamGateBlocks.keySet()) {

			BlockInfo info = this.teamGateBlocks.get(teamName);
			if (this.leaving(location, BlockInfo.getBlock(this.volume.getWorld(), info), inside, left, right)) {
				return true;
			}
		}
		return false;
	}

	private boolean leaving(Location location, Block gate, BlockFace inside, BlockFace left, BlockFace right) {
		// 3x4x1 deep
		Volume gateExitVolume = new Volume("tempGateExit", location.getWorld());
		Block out = gate.getRelative(inside);
		gateExitVolume.setCornerOne(out.getRelative(left).getRelative(BlockFace.DOWN));
		gateExitVolume.setCornerTwo(gate.getRelative(right, 1).getRelative(BlockFace.UP, 2));

		if (gateExitVolume.contains(location)) {
			return true;
		}

		return false;
	}
}
