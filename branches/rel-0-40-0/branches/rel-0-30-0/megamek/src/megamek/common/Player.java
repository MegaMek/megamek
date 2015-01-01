/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2006 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import java.io.*;
import java.util.*;

import megamek.common.event.GamePlayerChangeEvent;

/**
 * Represents a player in the game.
 */
public final class Player extends TurnOrdered
    implements Serializable
{
    static final long serialVersionUID = 6545367826571905191L;
    public static final int     PLAYER_NONE = -1;
    public static final int     TEAM_NONE = 0;

    public static final String  colorNames[] = {"Blue", "Yellow", "Red",
    "Green", "White", "Cyan", "Pink", "Orange", "Gray", "Brown", "Purple"};

    public static final String  teamNames[] = {"No Team", "Team 1", "Team 2",
    "Team 3", "Team 4", "Team 5"};
    public static final int     MAX_TEAMS = teamNames.length;

    private transient IGame  game;

    private String          name = "unnamed";
    private int             id;

    private int             team = TEAM_NONE;

    private boolean         done = false; // done with phase
    private boolean         ghost = false; // disconnected player
    private boolean         observer = false;

    private boolean         see_entire_board = false; // Player can observe
                                                      // double blind games
    
    private int             colorIndex = 0;

    // these are game-specific, and maybe should be seperate from the player object
    private int             startingPos = 0;

    // number of minefields
    private int num_mf_conv  = 0;
    private int num_mf_cmd   = 0;
    private int num_mf_vibra = 0;
    
    // hexes that are automatically hit by artillery
    private Vector artyAutoHitHexes = new Vector();

    /**
     * The "no camo" category.
     */
    public static final String NO_CAMO = "-- No Camo --";
    
    /**
     * The category for camos in the root directory.
     */
    public static final String ROOT_CAMO = "-- General --";

    private String camoCategory = Player.NO_CAMO;

    private String camoFileName = null;

    private Vector visibleMinefields = new Vector();
    
    private boolean admitsDefeat = false;
    
    public Vector getMinefields() {
        return visibleMinefields;
    }
    
    public void addMinefield(Minefield mf) {
        visibleMinefields.addElement(mf);
    }
    
    public void addMinefields(Vector minefields) {
        for (int i = 0; i < minefields.size(); i++) {
            visibleMinefields.addElement(minefields.elementAt(i));
        }
    }
    
    public void removeMinefield(Minefield mf) {
        visibleMinefields.removeElement(mf);
    }
    
    public void removeMinefields() {
        visibleMinefields.removeAllElements();
    }
    
    public void removeArtyAutoHitHexes() {
        artyAutoHitHexes.removeAllElements();
    }
    
    public boolean containsMinefield(Minefield mf) {
        return visibleMinefields.contains(mf);
    }
    
    public boolean hasMinefields() {
        return (num_mf_cmd > 0) || (num_mf_conv > 0) || (num_mf_vibra > 0);
    }
    
    public void setNbrMFConventional(int nbrMF) {
        num_mf_conv = nbrMF;
    }
    
    public void setNbrMFCommand(int nbrMF) {
        num_mf_cmd = nbrMF;
    }
    
    public void setNbrMFVibra(int nbrMF) {
        num_mf_vibra = nbrMF;
    }
    
    public int getNbrMFConventional() {
        return num_mf_conv;
    }
    
    public int getNbrMFCommand() {
        return num_mf_cmd;
    }
    
    public int getNbrMFVibra() {
        return num_mf_vibra;
    }
    
    public void setCamoCategory(String name) {
        this.camoCategory = name;
    }
    
    public String getCamoCategory() {
        return camoCategory;
    }
    
    public void setCamoFileName(String name) {
        this.camoFileName = name;
    }
    
    public String getCamoFileName() {
        return camoFileName;
    }

    public Player(int id, String name) {
        this.name = name;
        this.id = id;
    }

    public void setGame(IGame game) {
        this.game = game;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
        game.processGameEvent(new GamePlayerChangeEvent(this,this));
    }

    public boolean isGhost() {
        return ghost;
    }

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }

    public boolean isObserver() {
        if (game != null && game.getPhase() == IGame.PHASE_VICTORY)
            return false;
        else
            return observer;
    }

    public void setSeeAll(boolean see_all)
    {
        this.see_entire_board = see_all;
    }

    // This simply returns the value, without checking the observer flag
    public boolean getSeeAll()
    {
        return see_entire_board;
    }
    
    // If observer is false, see_entire_board does nothing
    public boolean canSeeAll() {
        return (observer && see_entire_board);
    }

    public void setObserver(boolean observer) {
        this.observer = observer;
        // If not an observer, clear the set see all flag 
        if (!observer)
            this.setSeeAll(false);
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int index) {
        this.colorIndex = index;
    }

    public int getStartingPos() {
        return startingPos;
    }

    public void setStartingPos(int startingPos) {
        this.startingPos = startingPos;
    }

    public boolean isEnemyOf(Player other) {
        return (id != other.getId() && (team == TEAM_NONE || team != other.getTeam()));
    }

    /**
     * Two players are equal if their ids are equal
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Player other = (Player)object;
        return other.getId() == this.id;
    }

    public int hashCode() {
        return getId();
    }
    
    public void setAdmitsDefeat(boolean admitsDefeat) {
        this.admitsDefeat = admitsDefeat;
    }
    
    public boolean admitsDefeat() {
        return admitsDefeat;
    }
    
    public void setArtyAutoHitHexes(Vector artyAutoHitHexes) {
        this.artyAutoHitHexes = artyAutoHitHexes;
    }
    
    public Vector getArtyAutoHitHexes() {
        return artyAutoHitHexes;
    }

    public boolean hasTAG() {
        for (Enumeration e = game.getSelectedEntities(new EntitySelector() {
                    private final int ownerId = getId();
                        public boolean accept( Entity entity ) {
                            if (entity.getOwner() == null)
                                return false;
                            if ( ownerId == entity.getOwner().getId())
                                return true;
                            return false;
                        }
                    }
                ); e.hasMoreElements(); ) {
            Entity m = (Entity)e.nextElement();
            if (m.hasTAG()) {
                return true;
            }
            // A player can't be on two teams.
        }
        return false;
    }

    public boolean hasHomingRounds() {
        for (Enumeration e = game.getSelectedEntities(new EntitySelector() {
                    private final int ownerId = getId();
                        public boolean accept( Entity entity ) {
                            if (entity.getOwner() == null)
                                return false;
                            if ( ownerId == entity.getOwner().getId())
                                return true;
                            return false;
                        }
                    }
                ); e.hasMoreElements(); ) {
            Entity m = (Entity)e.nextElement();
            if (m.hasHomingRounds()) {
                return true;
            }
            // A player can't be on two teams.
        }
        return false;
    }
}
