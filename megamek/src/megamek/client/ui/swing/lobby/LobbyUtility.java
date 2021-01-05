/*  
* MegaMek - Copyright (C) 2020 - The MegaMek Team  
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
package megamek.client.ui.swing.lobby;

import java.util.Collection;

import megamek.MegaMek;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.MapSettings;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

public class LobbyUtility {

    /**
     * Returns true when the starting position of the given player is valid
     * in the given game. This is not the case only when the options "Double Blind"
     * and "Exclusive Starting Positions" are on and the starting position overlaps
     * with that of other players, if "Teams Share Vision" is off, or enemy players,
     * if "Teams Share Vision" is on.
     * <P>See also {@link #startPosOverlap(IPlayer, IPlayer)}
     */
    static boolean isValidStartPos(IGame game, IPlayer player) {
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
    static boolean isValidStartPos(IGame game, IPlayer player, int pos) {
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
    static boolean isExclusiveDeployment(IGame game) {
        final GameOptions gOpts = game.getOptions();
        return gOpts.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                && gOpts.booleanOption(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT);
    }  
    
    /**
     * Returns true when teams share vision is on, reagardless of whether
     * double blind is on.
     */
    static boolean isTeamsShareVision(IGame game) {
        final GameOptions gOpts = game.getOptions();
        return gOpts.booleanOption(OptionsConstants.ADVANCED_TEAM_VISION);
    } 
    
    /** Returns true if the given entities all belong to the same player. */
    static boolean haveSingleOwner(Collection<Entity> entities) {
        return entities.stream().mapToInt(e -> e.getOwner().getId()).distinct().count() == 1;
    }
    
    /**
     * Returns true if no two of the given entities are enemies. This is
     * true when all entities belong to a single player. If they belong to 
     * different players, it is true when all belong to the same team and 
     * that team is one of Teams 1 through 5 (not "No Team").
     * <P>Returns true when entities is empty or has only one entity. The case of
     * entities being empty should be considered by the caller.  
     */
    static boolean areAllied(Collection<Entity> entities) {
        if (entities.size() == 0) {
            MegaMek.getLogger().warning("Empty collection of entities received, cannot determine if no entities are all allied.");
            return true;
        }
        Entity randomEntry = entities.stream().findAny().get();
        return !entities.stream().anyMatch(e -> e.isEnemyOf(randomEntry));
    }
    
    /** Returns true if any of the given entities are embarked (transported by something). */ 
    static boolean containsTransportedUnit(Collection<Entity> entities) {
        return entities.stream().anyMatch(e -> e.getTransportId() != Entity.NONE);
    }
    
    /** 
     * Returns true when the given board name does not start with one of the control strings
     * of MapSettings signalling a random, generated pr surprise board. 
     */ 
    static boolean isBoardFile(String board) {
        return !board.startsWith(MapSettings.BOARD_GENERATED)
                && !board.startsWith(MapSettings.BOARD_RANDOM)
                && !board.startsWith(MapSettings.BOARD_SURPRISE);
    }
    
    // PRIVATE
    
    
    
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
