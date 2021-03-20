/*
 * MegaMek - Copyright (C) 2021 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package megamek.server.commands;

import megamek.MegaMek;
import megamek.common.IGame;
import megamek.common.util.SerializationHelper;
import megamek.server.Server;
import megamek.common.Game;
import megamek.common.Entity;
import megamek.common.IPlayer;
import megamek.common.net.IConnection;
import megamek.common.IStartingPositions;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Saves the current game to MMS format
 */
public class ConvertMMSCommand extends ServerCommand {

    private static final String COMMENT_MARK = "#";
    private static final String SEPARATOR_PROPERTY = "=";
    private static final String SEPARATOR_COMMA = ",";
    private static final String SEPARATOR_SPACE = " ";
    private static final String SEPARATOR_COLON = ":";
    private static final String SEPARATOR_UNDERSCORE = "_";
    private static final String FILE_SUFFIX_BOARD = ".board";
    private static final String PARAM_MMSVERSION = "MMSVersion";
    private static final String PARAM_GAME_OPTIONS_FILE = "GameOptionsFile";
    private static final String PARAM_GAME_EXTERNAL_ID = "ExternalId";
    private static final String PARAM_FACTIONS = "Factions";
    private static final String PARAM_MAP_WIDTH = "MapWidth";
    private static final String PARAM_MAP_HEIGHT = "MapHeight";
    private static final String PARAM_BOARD_WIDTH = "BoardWidth";
    private static final String PARAM_BOARD_HEIGHT = "BoardHeight";
    private static final String PARAM_BRIDGE_CF = "BridgeCF";
    private static final String PARAM_MAPS = "Maps";
    private static final String PARAM_MAP_DIRECTORIES = "RandomDirs";
    private static final String PARAM_TEAM = "Team";
    private static final String PARAM_LOCATION = "Location";
    private static final String PARAM_MINEFIELDS = "Minefields";
    private static final String PARAM_DAMAGE = "Damage";
    private static final String PARAM_SPECIFIC_DAMAGE = "DamageSpecific";
    private static final String PARAM_CRITICAL_HIT = "CritHit";
    private static final String PARAM_AMMO_AMOUNT = "SetAmmoTo";
    private static final String PARAM_AMMO_TYPE = "SetAmmoType";
    private static final String PARAM_PILOT_HITS = "PilotHits";
    private static final String PARAM_EXTERNAL_ID = "ExternalID";
    private static final String PARAM_ADVANTAGES = "Advantages";
    private static final String PARAM_AUTO_EJECT = "AutoEject";
    private static final String PARAM_COMMANDER = "Commander";
    private static final String PARAM_DEPLOYMENT_ROUND = "DeploymentRound";
    private static final String PARAM_CAMO = "Camo";
    private static final String PARAM_ALTITUDE = "Altitude";
    private static final String MAP_RANDOM = "RANDOM";

    /**
     * Creates a new instance of SaveGameCommand
     * @param server
     */
    public ConvertMMSCommand(Server server) {
        super(server, "convertMMS",
              "Converts a savegame to an MMS scenario. Usage: /convertMMS [scenario filename]");
    }

    /**
     * Run the command to create the MMS file
     * @param connId
     * @param args
     */
    @Override
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId, "Observers cannot save the game.");
            return;
        }
        if (args.length > 1) {
            makeMMS(connId, args[1]);
        } else {
            server.sendServerChat(connId, "You must provide an output scenario file name");
        }
    }

    /**
     * Create the MMS output file
     * @param connId
     * @param scenarioFile
     * @return
     */
    private boolean makeMMS(int connId, String scenarioFile) {
        String localPath = "data" + File.separator + "scenarios" + File.separator;
        String sFinalFile = scenarioFile;
        
        // Validate the filename
        if (!sFinalFile.endsWith(".mms")) {
            sFinalFile = scenarioFile + ".mms";
        }
        File sDir = new File("." + File.separator + "data" + File.separator + "scenarios");
        
        // Create the directory for the file if it doesn't exist
        if (!sDir.exists()) {
            sDir.mkdir();
        }
        sFinalFile = sDir + File.separator + sFinalFile;
        
        try {
            // Setup the file to output to            
            BufferedWriter writer = new BufferedWriter(new FileWriter(sFinalFile));
            
            // Write the header
            writer.write("# Megamek Convert to MMS");
            writer.newLine();
            writer.write("# This does not output maps directly, but the map size. All maps are set as Random by default");
            writer.newLine();
            writer.write(PARAM_MMSVERSION + SEPARATOR_PROPERTY + "1");
            writer.newLine();
            writer.write("Name=ChangeMe");
            writer.newLine();
            writer.write("Description=ChangeMe");
            writer.newLine();
            
            /* Maps section. Get the number of boards and output them. this does not work.
            writer.write(PARAM_BOARD_HEIGHT + SEPARATOR_PROPERTY + server.getGame().getBoard().getNumBoardsHeight());
            writer.newLine();
            writer.write(PARAM_BOARD_WIDTH + SEPARATOR_PROPERTY + server.getGame().getBoard().getNumBoardsWidth());
            writer.newLine();
             */
            
            final int hexWidth = server.getGame().getBoard().getWidth();
            final int hexHeight = server.getGame().getBoard().getHeight();  
            int numBoardsWidth = 1;
            int numBoardsHeight = 1;
            
            // Check for standard battletech width and height, and break into sub-maps
            if (((hexWidth % 16) == 0) && ((hexHeight % 17) == 0))  {
                numBoardsWidth = hexWidth / 16;
                numBoardsHeight = hexHeight / 17;
            }
            
            writer.write(PARAM_BOARD_HEIGHT + SEPARATOR_PROPERTY + numBoardsHeight);
            writer.newLine();
            writer.write(PARAM_BOARD_WIDTH + SEPARATOR_PROPERTY + numBoardsWidth);
            writer.newLine();
            
            writer.write(PARAM_MAP_WIDTH + SEPARATOR_PROPERTY + (hexWidth / numBoardsWidth));
            writer.newLine();
            writer.write(PARAM_MAP_HEIGHT + SEPARATOR_PROPERTY + (hexHeight / numBoardsHeight));
            writer.newLine();
            
            // Random only for now
            writer.write(PARAM_MAPS + SEPARATOR_PROPERTY + MAP_RANDOM);
            writer.newLine();
            
            // Factions section. This is the list of players. Output the list to file, and index the starting positions
            String[] factions = {};
            int[] playerPos = {};
            writer.write(PARAM_FACTIONS + SEPARATOR_PROPERTY);
            for (Enumeration<IPlayer> players = server.getGame().getPlayers(); players.hasMoreElements();) {
                 if (factions.length != 0) {
                     writer.append(SEPARATOR_COMMA);
                 }
                 IPlayer p = players.nextElement();
                 writer.append(p.getName());
                 factions = Arrays.copyOf(factions, factions.length + 1);
                 factions[factions.length - 1] = p.getName();
                playerPos = Arrays.copyOf(playerPos, playerPos.length + 1);
                playerPos[playerPos.length - 1] = p.getStartingPos();
            }
            writer.newLine();
            
            // Output the starting locations for the players
            for (int i = 0; i < factions.length; i++) {
                writer.write(PARAM_LOCATION + SEPARATOR_UNDERSCORE + factions[i] + SEPARATOR_PROPERTY + 
                             IStartingPositions.START_LOCATION_NAMES[playerPos[i]]);
                writer.newLine();
            }
            
            // Output the units for each player
            for (Enumeration<IPlayer> players = server.getGame().getPlayers(); players.hasMoreElements();) {
                IPlayer p = players.nextElement();
                int numUnits = server.getGame().getEntitiesOwnedBy(p);
                List<Entity> PlayerUnits = server.getGame().getPlayerEntities(p, true);
                
                for (int counter = 0; counter < PlayerUnits.size(); counter++) {
                    writer.write("Unit" + SEPARATOR_UNDERSCORE + p.getName() +
                                 SEPARATOR_UNDERSCORE + (counter+1) + SEPARATOR_PROPERTY +
                                 PlayerUnits.get(counter).getShortName() + SEPARATOR_COMMA +
                                 PlayerUnits.get(counter).getCrew().getName() + SEPARATOR_COMMA +
                                 PlayerUnits.get(counter).getCrew().getGunnery() + SEPARATOR_COMMA +
                                 PlayerUnits.get(counter).getCrew().getPiloting());
                    writer.newLine();
                }
                // Need to add lines to look up the mech position, and add it in terms of distance from NW corner (N,0,0 is NW corner)
                // Values are X,Y
                
                // need to look at damage. For mechs this is DamageSpecific=N<X>:<remaining armor>
                // eg: N0:5,N1:14,R1:3,N2:11,R2:3,N3:11,R3:3,N4:9,N5:9,N6:12,N7:12
                // These are: N0=Head, N1=CT, R1=CTR, N2=RT,R2=RTR,N3=LT,R3=RTR,N4=RA,N5=LA,N6=RL,N7=RL
                
                // For random damage, use Unit_<player>_<unitNum>_Damage=<amount>
            }
            
            writer.close();
        } catch (IOException ignored) {
            server.sendServerChat(connId, "file write failed.");
            return false;
        }
        server.sendServerChat(connId, "File saved as " + sFinalFile);       
        return true;
    }
}
