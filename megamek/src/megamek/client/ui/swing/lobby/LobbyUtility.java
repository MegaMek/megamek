/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.lobby;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.BattleArmorHandlesTank;
import megamek.common.Bay;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.Game;
import megamek.common.IPlayer;
import megamek.common.MapSettings;
import megamek.common.TankTrailerHitch;
import megamek.common.Transporter;
import megamek.common.force.Force;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

/** 
 * This class provides static helper functions for the Lobby aka ChatLounge. 
 * @author Simon
 *
 */
public class LobbyUtility {

    /**
     * Returns true when the starting position of the given player is valid
     * in the given game. This is not the case only when the options "Double Blind"
     * and "Exclusive Starting Positions" are on and the starting position overlaps
     * with that of other players, if "Teams Share Vision" is off, or enemy players,
     * if "Teams Share Vision" is on.
     * <P>See also {@link #startPosOverlap(IPlayer, IPlayer)}
     */
    static boolean isValidStartPos(Game game, IPlayer player) {
        return isValidStartPos(game, player, player.getStartingPos());
    }

    /**
     * Returns true when the given starting position pos is valid for the given player
     * in the given game. This is not the case only when the options "Double Blind"
     * and "Exclusive Starting Positions" are on and the starting position overlaps
     * with that of other players, if "Teams Share Vision" is off, or enemy players,
     * if "Teams Share Vision" is on.
     * <P>See also {@link #startPosOverlap(IPlayer, IPlayer)}
     */
    static boolean isValidStartPos(Game game, IPlayer player, int pos) {
        if (!isExclusiveDeployment(game)) {
            return true;
        } else {
            if (isTeamsShareVision(game)) {
                return !game.getPlayersVector().stream().filter(p -> p.isEnemyOf(player))
                        .anyMatch(p -> startPosOverlap(pos, p.getStartingPos()));
            } else {
                return !game.getPlayersVector().stream().filter(p -> !p.equals(player))
                        .anyMatch(p -> startPosOverlap(pos, p.getStartingPos()));
            }
        }
    }
    
    /**
     * Returns true when double blind and exclusive deployment are on,
     * meaning that player's deployment zones may not overlap.
     */
    static boolean isExclusiveDeployment(Game game) {
        final GameOptions gOpts = game.getOptions();
        return gOpts.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                && gOpts.booleanOption(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT);
    }  
    
    /**
     * Returns true when blind drop is on.
     */
    static boolean isBlindDrop(Game game) {
        final GameOptions gOpts = game.getOptions();
        return gOpts.booleanOption(OptionsConstants.BASE_BLIND_DROP);
    } 
    
    /**
     * Returns true when real blind drop is on.
     */
    static boolean isRealBlindDrop(Game game) {
        final GameOptions gOpts = game.getOptions();
        return gOpts.booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
    }  
    
    /**
     * Returns true when teams share vision is on, reagardless of whether
     * double blind is on.
     */
    static boolean isTeamsShareVision(Game game) {
        final GameOptions gOpts = game.getOptions();
        return gOpts.booleanOption(OptionsConstants.ADVANCED_TEAM_VISION);
    } 
    
    /** Returns true if the given entities all belong to the same player. */
    static boolean haveSingleOwner(Collection<Entity> entities) {
        return entities.stream().mapToInt(e -> e.getOwner().getId()).distinct().count() == 1;
    }
    
    /** Returns true if any of the given entities are embarked (transported by something). */ 
    static boolean containsTransportedUnit(Collection<Entity> entities) {
        return entities.stream().anyMatch(e -> e.getTransportId() != Entity.NONE);
    }
    
    /** 
     * Returns true when the given board name does not start with one of the control strings
     * of MapSettings signalling a random, generated or surprise board. 
     */ 
    @SuppressWarnings("deprecation")
    static boolean isBoardFile(String board) {
        return !board.startsWith(MapSettings.BOARD_GENERATED)
                && !board.startsWith(MapSettings.BOARD_RANDOM)
                && !board.startsWith(MapSettings.BOARD_SURPRISE);
    }
    
    /** Returns a formatted and colored tooltip string warning that a board is invalid. */
    static String invalidBoardTip() {
        return UIUtil.guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor())
                + Messages.getString("ChatLounge.map.invalidTip") + "</FONT>";
    }
    
    /** 
     * Draws the given text (the board name or special text) as a label on the
     * lower edge of the image for which the graphics g is given.
     */
    static void drawMinimapLabel(String text, int w, int h, Graphics g, boolean invalid) {
        if (text.length() == 0) {
            return;
        }
        GUIPreferences.AntiAliasifSet(g);
        // The text size may grow with the width of the image, but no bigger than 16*guiscale
        // to avoid huge text
        int fontSize = Math.min(w / 10, UIUtil.scaleForGUI(16));
        Font font = new Font("Dialog", Font.PLAIN, fontSize);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        int th = fm.getAscent() + fm.getDescent(); // The text height
        int cx = (w - fm.stringWidth(text)) / 2; // The left edge for centered text
        // When the text is wider than the image, let the text start close to the left edge 
        cx = Math.max(w / 20, cx);
        int cy = h - th / 2;
        Color col = new Color(250, 250, 250, 140);
        if (text.startsWith(Messages.getString("ChatLounge.MapSurprise"))) {
            col = new Color(250, 250, 50, 140);
        } else if (text.startsWith(Messages.getString("ChatLounge.MapGenerated"))) {
            col = new Color(50, 50, 250, 140);
        }
        if (invalid) {
            col = GUIPreferences.getInstance().getWarningColor();
        }
        g.setColor(col);
        g.fillRoundRect(cx - 3, cy - fm.getAscent(), w - 2 * cx + 6, th, fontSize/2, fontSize/2);
        // Clip the text to inside the image with a margin of w/20
        g.setClip(w / 20, 0, w - w / 10, h);
        g.setColor(Color.BLACK);
        g.drawString(text, cx, cy);
        g.setClip(null);
    }
    
    /** 
     * Removes the board size ("16x17") and file path from the given board name if it is
     * a board file. Also, reconstructs the text if it's a surprise map or generated map.
     */
    public static String cleanBoardName(String boardName, MapSettings mapSettings) {
        // Remove the file path
        if (isBoardFile(boardName)) {
            boardName = new File(boardName).getName();
        }
        // Construct the text if it's a surprise map
        if (boardName.startsWith(MapSettings.BOARD_SURPRISE)) {
            int numBoards = extractSurpriseMaps(boardName).size();
            boardName = Messages.getString("ChatLounge.MapSurprise") + " (" + numBoards + " boards)";
        }
        // Construct the text if it's a generated map
        if (boardName.startsWith(MapSettings.BOARD_GENERATED)) {
            boardName = Messages.getString("ChatLounge.MapGenerated");
        }
        // Remove board sizes ("16x17")
        String boardSize = mapSettings.getBoardWidth() + "x" + mapSettings.getBoardHeight();
        return boardName.replace(boardSize, "").replace(".board", "").trim();
    }
    
    /** 
     * Specialized method that returns a list of board names from the given 
     * boardsString that starts with the prefix for a Surprise board.  
     */ 
    public static ArrayList<String> extractSurpriseMaps(String boardsString) {
        if (boardsString.startsWith(MapSettings.BOARD_SURPRISE)) {
            boardsString = boardsString.substring(MapSettings.BOARD_SURPRISE.length());
        }
        String[] boards = boardsString.split("\n");
        ArrayList<String> result = new ArrayList<String>();
        result.addAll(Arrays.asList(boards));
        return result;
    }
    
    /** 
     * Converts an id list of the form 1,2,4,12 to a set of corresponding entities.
     * Ignores entity ids that don't exist. The resulting list may be empty but not null. 
     */ 
    public static HashSet<Entity> getEntities(Game game, String idList) {
        StringTokenizer st = new StringTokenizer(idList, ",");
        HashSet<Entity> result = new HashSet<>();
        while (st.hasMoreTokens()) {
            int id = Integer.parseInt(st.nextToken());
            Entity entity = game.getEntity(id);
            if (entity != null) {
                result.add(entity);
            }
        }
        return result;
    }
    
    /** 
     * Converts an id list of the form 1,2,4,12 to a set of corresponding forces.
     * Ignores force ids that don't exist. The resulting list may be empty but not null. 
     */ 
    public static HashSet<Force> getForces(Game game, String idList) {
        StringTokenizer st = new StringTokenizer(idList, ",");
        HashSet<Force> result = new HashSet<>();
        while (st.hasMoreTokens()) {
            int id = Integer.parseInt(st.nextToken());
            Force force = game.getForces().getForce(id);
            if (force != null) {
                result.add(force);
            }
        }
        return result;
    }
    
    /** 
     * Returns true if a and b share at least one non-hierarchic C3 system
     * (C3i, Naval C3, Nova CEWS). Symmetrical (the order of a and b does not matter). 
     */
    public static boolean sameNhC3System(Entity a, Entity b) {
        return (a.hasC3i() && b.hasC3i()) 
                || (a.hasNavalC3() && b.hasNavalC3()) 
                || (a.hasNovaCEWS() && b.hasNovaCEWS());
    }
    
    /** Returns the string with some content shortened like Battle Armor -> BA */
    static String abbreviateUnitName(String unitName) {
        return unitName
                .replace("(Standard)", "").replace("Battle Armor", "BA")
                .replace("Standard", "Std.").replace("Vehicle", "Veh.")
                .replace("Medium", "Med.").replace("Support", "Spt.")
                .replace("Heavy", "Hvy.").replace("Light", "Lgt.");
    }
    
    static boolean hasYellowWarning(Entity entity) {
        return (entity instanceof FighterSquadron && entity.getLoadedUnits().isEmpty())
                || ((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || (entity.hasNovaCEWS() && (entity.calculateFreeC3Nodes() == 2))
                || ((entity.getC3Master() == null) && entity.hasC3S());
    }
    
    /** 
     * Returns true when the entities can embark onto loader given the other constraints.
     * If false, the passed errorMsg contains a suitable error message for display.
     */
    static boolean validateLobbyLoad(Collection<Entity> entities, Entity loader, int bayNumber,
            boolean loadRear, StringBuilder errorMsg) {
        // Protomek loading uses only 1 entity, get that (doesnt matter if it's something else):
        Entity soleProtomek = entities.stream().findAny().get();
        double capacity;
        boolean hasEnoughCargoCapacity;
        String errorMessage = "";
        
        if (bayNumber != -1) {
            Bay bay = loader.getBayById(bayNumber);
            if (null != bay) {
                double loadSize = entities.stream().mapToDouble(bay::spaceForUnit).sum();
                capacity = bay.getUnused();
                hasEnoughCargoCapacity = loadSize <= capacity;
                errorMessage = Messages.getString("LoadingBay.baytoomany",
                        (int) bay.getUnusedSlots(), bay.getDefaultSlotDescription());
            } else if (loader.hasETypeFlag(Entity.ETYPE_MECH)
                    && soleProtomek.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                // We're also using bay number to distinguish between front and rear locations
                // for protomech mag clamp systems
                hasEnoughCargoCapacity = entities.size() == 1;
                errorMessage = Messages.getString("LoadingBay.protostoomany");
            } else {
                hasEnoughCargoCapacity = false;
                errorMessage = Messages.getString("LoadingBay.bayNumberNotFound", bayNumber);
            }
        } else {
            HashMap<Long, Double> capacities = new HashMap<>();
            HashMap<Long, Double> counts = new HashMap<>();
            HashMap<Transporter, Double> potentialLoad = new HashMap<>();
            // Get the counts and capacities for all present types
            for (Entity e : entities) {
                long entityType = e.getEntityType();
                long loaderType = loader.getEntityType();
                double unitSize;
                if ((entityType & Entity.ETYPE_MECH) != 0) {
                    entityType = Entity.ETYPE_MECH;
                    unitSize = 1;
                } else if ((entityType & Entity.ETYPE_INFANTRY) != 0) {
                    entityType = Entity.ETYPE_INFANTRY;
                    boolean useCount = true;
                    if ((loaderType & Entity.ETYPE_TANK) != 0) {
                        // This is a super hack... When getting
                        // capacities, troopspace gives unused space in
                        // terms of tons, and BattleArmorHandles gives
                        // it in terms of unit count. If I call
                        // getUnused, it sums these together, and is
                        // meaningless, so we'll go through all
                        // transporters....
                        boolean hasTroopSpace = false;
                        for (Transporter t : loader.getTransports()) {
                            if (t instanceof TankTrailerHitch) {
                                continue;
                            }
                            double loadWeight = e.getWeight();
                            if (potentialLoad.containsKey(t)) {
                                loadWeight += potentialLoad.get(t);
                            }
                            if (!(t instanceof BattleArmorHandlesTank) && t.canLoad(e)
                                    && (loadWeight <= t.getUnused())) {
                                hasTroopSpace = true;
                                potentialLoad.put(t, loadWeight);
                                break;
                            }
                        }
                        if (hasTroopSpace) {
                            useCount = false;
                        }
                    }
                    // TroopSpace uses tonnage
                    // bays and BA handlebars use a count
                    if (useCount) {
                        unitSize = 1;
                    } else {
                        unitSize = e.getWeight();
                    }
                } else if ((entityType & Entity.ETYPE_PROTOMECH) != 0) {
                    entityType = Entity.ETYPE_PROTOMECH;
                    unitSize = 1;
                    // Loading using mag clamps; user can specify front or rear.
                    // Make use of bayNumber field
                    if ((loaderType & Entity.ETYPE_MECH) != 0) {
                        bayNumber = loadRear? 1 : 0;
                    }
                } else if ((entityType & Entity.ETYPE_DROPSHIP) != 0) {
                    entityType = Entity.ETYPE_DROPSHIP;
                    unitSize = 1;
                } else if ((entityType & Entity.ETYPE_JUMPSHIP) != 0) {
                    entityType = Entity.ETYPE_JUMPSHIP;
                    unitSize = 1;
                } else if ((entityType & Entity.ETYPE_AERO) != 0) {
                    entityType = Entity.ETYPE_AERO;
                    unitSize = 1;
                } else if ((entityType & Entity.ETYPE_TANK) != 0) {
                    entityType = Entity.ETYPE_TANK;
                    unitSize = 1;
                } else {
                    unitSize = 1;
                }

                Double count = counts.get(entityType);
                if (count == null) {
                    count = 0.0;
                }
                count = count + unitSize;
                counts.put(entityType, count);

                Double cap = capacities.get(entityType);
                if (cap == null) {
                    cap = loader.getUnused(e);
                    capacities.put(entityType, cap);
                }
            }
            hasEnoughCargoCapacity = true;
            capacity = 0;
            for (Long typeId : counts.keySet()) {
                double currCount = counts.get(typeId);
                double currCapacity = capacities.get(typeId);
                if (currCount > currCapacity) {
                    hasEnoughCargoCapacity = false;
                    capacity = currCapacity;
                    String messageName;
                    if (typeId == Entity.ETYPE_INFANTRY) {
                        messageName = "LoadingBay.nonbaytoomanyInf";
                    } else {
                        messageName = "LoadingBay.nonbaytoomany";
                    }
                    errorMessage = Messages.getString(messageName, currCount,
                            Entity.getEntityTypeName(typeId), currCapacity);
                }
            }
        }
        if (loader instanceof FighterSquadron 
                && entities.stream().anyMatch(e -> !e.isFighter() || e instanceof FighterSquadron)) {
            errorMessage = "Only aerospace and conventional fighters can join squadrons.";
            hasEnoughCargoCapacity = false;
        }
        errorMsg.append(errorMessage);
        return hasEnoughCargoCapacity;
    }

    
    // PRIVATE
    //  
    //

    /** 
     * Returns true when the two starting positions overlap, i.e.
     * if they are equal or adjacent (e.g. E and NE, SW and S).
     * ANY overlaps all others. 
     */
    private static boolean startPosOverlap(int pos1, int pos2) {
        if (pos1 > 10) {
            pos1 -= 10;
        }
        if (pos2 > 10) {
            pos2 -= 10;
        }
        if (pos1 == pos2) {
            return true;
        }
        int a = Math.max(pos1, pos2);
        int b = Math.min(pos1, pos2);
        // Out of bounds values:
        if (b < 0 || a > 10) {
            throw new IllegalArgumentException("The given starting position is invalid!");
        }
        // ANY (0) overlaps all others, EDG (9) overlaps all others but CTR (10)
        if (b == 0 || a == 9) {
            return true;
        }
        // EDG and CTR don't overlap
        if (a == 10 && b == 9) {
            return false;
        }
        // the rest of the positions overlap if they're 1 apart
        // NW = 1 and W = 8 also overlap
        return ((a - b == 1) || (a == 8 && b == 1));
    }
}
